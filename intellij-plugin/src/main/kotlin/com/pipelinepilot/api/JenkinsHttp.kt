package com.pipelinepilot.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.pipelinepilot.settings.PipelinePilotSettings
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Shared Jenkins HTTP concerns: client construction, basic auth, and CSRF crumb
 * acquisition. Used by both validation and sandbox clients.
 */
class JenkinsHttp(
    private val settings: PipelinePilotSettings = PipelinePilotSettings.getInstance(),
    private val mapper: ObjectMapper = ObjectMapper(),
) {
    val baseUrl: String get() = settings.state.jenkinsUrl

    fun isConfigured(): Boolean = settings.isConfigured()

    fun client(timeoutMillis: Long = settings.state.validationTimeoutMillis.toLong()): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
            .build()

    fun authHeader(): String? {
        val user = settings.state.username
        val token = settings.apiToken
        return if (user.isNotBlank() && token.isNotBlank()) Credentials.basic(user, token) else null
    }

    fun applyAuth(builder: Request.Builder): Request.Builder {
        authHeader()?.let { builder.header("Authorization", it) }
        return builder
    }

    /** Jenkins CSRF protection: returns (headerName, crumb) or null if disabled/unavailable. */
    fun fetchCrumb(client: OkHttpClient): Pair<String, String>? {
        val req = applyAuth(Request.Builder().url("$baseUrl/crumbIssuer/api/json")).get().build()
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

    fun applyCrumb(client: OkHttpClient, builder: Request.Builder): Request.Builder {
        fetchCrumb(client)?.let { (field, crumb) -> builder.header(field, crumb) }
        return builder
    }
}
