package com.pipelinepilot.api

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.pipelinepilot.settings.PipelinePilotSettings
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Drives the companion-plugin sandbox lifecycle:
 *   POST   {runSandboxPath}        -> create ephemeral remote execution
 *   GET    {logsPath}/{id}?start=N -> progressive log polling
 *   DELETE {sessionPath}/{id}      -> destroy temporary execution (cleanup)
 *
 * No local emulation: the run happens on the real Jenkins instance. The REPLAY
 * strategy is preferred so the original pipeline config is never mutated.
 */
class SandboxClient(
    private val settings: PipelinePilotSettings = PipelinePilotSettings.getInstance(),
    private val http: JenkinsHttp = JenkinsHttp(settings),
) {
    private val json = "application/json".toMediaType()
    private val mapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    /** Create a sandbox session. Throws on transport/HTTP failure so callers can report it. */
    fun runSandbox(request: RunSandboxRequest): RunSandboxResponse {
        require(http.isConfigured()) { "Jenkins URL not configured (Settings | Tools | PipelinePilot)" }
        val client = http.client()
        val body = mapper.writeValueAsString(request).toRequestBody(json)
        val builder = Request.Builder().url(http.baseUrl + settings.state.runSandboxPath).post(body)
        http.applyAuth(builder)
        http.applyCrumb(client, builder)

        client.newCall(builder.build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Sandbox start failed (HTTP ${resp.code}): $text")
            return mapper.readValue(text, RunSandboxResponse::class.java)
        }
    }

    /** Replay a pipeline (whole, single stage, or only-failed). Returns a new session. */
    fun replay(request: ReplayRequest): RunSandboxResponse {
        require(http.isConfigured()) { "Jenkins URL not configured (Settings | Tools | PipelinePilot)" }
        val client = http.client()
        val body = mapper.writeValueAsString(request).toRequestBody(json)
        val builder = Request.Builder().url(http.baseUrl + settings.state.replayPath).post(body)
        http.applyAuth(builder)
        http.applyCrumb(client, builder)

        client.newCall(builder.build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Replay failed (HTTP ${resp.code}): $text")
            return mapper.readValue(text, RunSandboxResponse::class.java)
        }
    }

    /** Poll one chunk of logs starting at [start]. */
    fun fetchLogs(sessionId: String, start: Long): LogChunk {
        val client = http.client(timeoutMillis = settings.state.validationTimeoutMillis.toLong())
        val url = "${http.baseUrl}${settings.state.logsPath}?sessionId=$sessionId&start=$start"
        val builder = http.applyAuth(Request.Builder().url(url)).get()
        client.newCall(builder.build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Log fetch failed (HTTP ${resp.code}): $text")
            return mapper.readValue(text, LogChunk::class.java)
        }
    }

    /**
     * Destroy the temporary execution. Best-effort; swallows failures.
     * Uses POST (not HTTP DELETE) so Jenkins' `@RequirePOST` CSRF protection applies.
     */
    fun deleteSession(sessionId: String) {
        runCatching {
            val client = http.client()
            val url = "${http.baseUrl}${settings.state.sessionPath}?sessionId=$sessionId"
            val builder = Request.Builder().url(url).post("".toRequestBody(json))
            http.applyAuth(builder)
            http.applyCrumb(client, builder)
            client.newCall(builder.build()).execute().close()
        }
    }
}
