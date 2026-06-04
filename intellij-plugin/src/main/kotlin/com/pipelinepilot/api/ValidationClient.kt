package com.pipelinepilot.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.pipelinepilot.settings.PipelinePilotSettings
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Talks to the Jenkins declarative linter endpoint:
 *   POST {jenkinsUrl}{validatePath}   (default /pipeline-model-converter/validate)
 *   body: form field  jenkinsfile=<content>
 *
 * Handles CSRF crumb acquisition and basic auth (username + API token).
 * Returns a normalized [ValidateResponse] with [Diagnostic]s parsed from the
 * linter's text output.
 */
class ValidationClient(
    private val settings: PipelinePilotSettings = PipelinePilotSettings.getInstance(),
) {
    private val mapper = ObjectMapper()

    private fun http(): OkHttpClient {
        val t = settings.state.validationTimeoutMillis.toLong()
        return OkHttpClient.Builder()
            .connectTimeout(t, TimeUnit.MILLISECONDS)
            .readTimeout(t, TimeUnit.MILLISECONDS)
            .build()
    }

    private fun authHeader(): String? {
        val user = settings.state.username
        val token = settings.apiToken
        return if (user.isNotBlank() && token.isNotBlank()) Credentials.basic(user, token) else null
    }

    /** Jenkins CSRF protection: fetch a crumb header to attach to mutating requests. */
    private fun fetchCrumb(client: OkHttpClient): Pair<String, String>? {
        val url = settings.state.jenkinsUrl + "/crumbIssuer/api/json"
        val req = Request.Builder().url(url).apply {
            authHeader()?.let { header("Authorization", it) }
        }.get().build()
        return runCatching {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val node = mapper.readTree(resp.body?.string() ?: return null)
                val field = node.get("crumbRequestField")?.asText() ?: return null
                val crumb = node.get("crumb")?.asText() ?: return null
                field to crumb
            }
        }.getOrNull()
    }

    fun validate(request: ValidateRequest): ValidateResponse {
        if (!settings.isConfigured()) {
            return ValidateResponse(
                valid = false,
                diagnostics = listOf(
                    Diagnostic(Severity.WARNING, "PipelinePilot: Jenkins URL not configured (Settings | Tools | PipelinePilot)", 1)
                ),
            )
        }

        val client = http()
        val url = settings.state.jenkinsUrl + settings.state.validatePath
        val body = FormBody.Builder().add("jenkinsfile", request.jenkinsfile).build()

        val builder = Request.Builder().url(url).post(body)
        authHeader()?.let { builder.header("Authorization", it) }
        fetchCrumb(client)?.let { (field, crumb) -> builder.header(field, crumb) }

        return runCatching {
            client.newCall(builder.build()).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                LinterOutputParser.parse(text)
            }
        }.getOrElse { e ->
            ValidateResponse(
                valid = false,
                diagnostics = listOf(
                    Diagnostic(Severity.WARNING, "PipelinePilot: validation request failed — ${e.message}", 1)
                ),
            )
        }
    }
}

/**
 * Parses Jenkins declarative linter text output.
 *
 * Success: "Jenkinsfile successfully validated."
 * Failure example:
 *   Errors encountered validating Jenkinsfile:
 *   WorkflowScript: 5: Missing required section "agent" @ line 5, column 1.
 */
object LinterOutputParser {

    private val LINE_COL = Regex("""@ line (\d+), column (\d+)""")
    private val WORKFLOW_PREFIX = Regex("""^WorkflowScript:\s*\d+:\s*""")

    fun parse(text: String): ValidateResponse {
        if (text.contains("successfully validated", ignoreCase = true)) {
            return ValidateResponse(valid = true)
        }

        val diagnostics = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("Errors encountered") }
            .mapNotNull { line ->
                val m = LINE_COL.find(line)
                val lineNo = m?.groupValues?.get(1)?.toIntOrNull() ?: 1
                val colNo = m?.groupValues?.get(2)?.toIntOrNull() ?: 1
                val message = line
                    .replace(WORKFLOW_PREFIX, "")
                    .replace(LINE_COL, "")
                    .trim()
                    .trimEnd('.', ' ')
                    .trim()
                if (message.isBlank()) null
                else Diagnostic(Severity.ERROR, message, lineNo, colNo)
            }
            .toList()

        return ValidateResponse(
            valid = diagnostics.isEmpty(),
            diagnostics = diagnostics.ifEmpty {
                listOf(Diagnostic(Severity.WARNING, "PipelinePilot: unrecognized linter response", 1))
            },
        )
    }
}
