package com.pipelinepilot.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.pipelinepilot.settings.PipelinePilotSettings
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Thin client for the Anthropic Messages API. The system prompt is sent with
 * `cache_control: ephemeral` so repeated calls within the cache window reuse it
 * (cheaper, faster) — see the claude-api skill guidance.
 */
class AiClient(
    private val settings: PipelinePilotSettings = PipelinePilotSettings.getInstance(),
) {
    private val mapper = ObjectMapper()
    private val json = "application/json".toMediaType()
    private val endpoint = "https://api.anthropic.com/v1/messages"

    private fun client(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Sends [systemPrompt] + a single user message [userContent], returns the
     * assistant's text. Throws on transport/HTTP/auth failure.
     */
    fun complete(systemPrompt: String, userContent: String): String {
        val key = settings.anthropicApiKey
        require(key.isNotBlank()) { "Anthropic API key not set" }

        val root: ObjectNode = mapper.createObjectNode()
        root.put("model", settings.state.aiModel)
        root.put("max_tokens", settings.state.aiMaxTokens)

        // System as a content block array so we can attach cache_control.
        val systemArr: ArrayNode = root.putArray("system")
        systemArr.addObject().apply {
            put("type", "text")
            put("text", systemPrompt)
            putObject("cache_control").put("type", "ephemeral")
        }

        val messages: ArrayNode = root.putArray("messages")
        messages.addObject().apply {
            put("role", "user")
            putArray("content").addObject().apply {
                put("type", "text")
                put("text", userContent)
            }
        }

        val req = Request.Builder()
            .url(endpoint)
            .header("x-api-key", key)
            .header("anthropic-version", "2023-06-01")
            .post(mapper.writeValueAsString(root).toRequestBody(json))
            .build()

        client().newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("AI request failed (HTTP ${resp.code}): $text")
            val node = mapper.readTree(text)
            return node.get("content")?.firstOrNull { it.get("type")?.asText() == "text" }
                ?.get("text")?.asText()
                ?: error("AI response had no text content")
        }
    }
}
