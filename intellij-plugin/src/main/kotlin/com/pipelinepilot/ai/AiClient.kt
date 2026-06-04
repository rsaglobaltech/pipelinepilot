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
 * Provider-agnostic client for the OpenAI **Chat Completions** API
 * (`POST {baseUrl}/chat/completions`). Because this format is a de-facto standard,
 * the same code works with OpenAI, Azure OpenAI, OpenRouter, local servers
 * (Ollama, LM Studio, vLLM), opencode, and any other compatible gateway — just
 * point the base URL (and optional key) at the provider in settings.
 */
class AiClient(
    private val settings: PipelinePilotSettings = PipelinePilotSettings.getInstance(),
) {
    private val mapper = ObjectMapper()
    private val json = "application/json".toMediaType()

    private fun client(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun endpoint(): String =
        settings.state.aiBaseUrl.trimEnd('/') + "/chat/completions"

    /**
     * Sends [systemPrompt] + a single user message [userContent], returns the
     * assistant's text. Throws on transport/HTTP/auth failure.
     */
    fun complete(systemPrompt: String, userContent: String): String {
        require(settings.state.aiBaseUrl.isNotBlank()) { "AI base URL not set" }

        val root: ObjectNode = mapper.createObjectNode()
        root.put("model", settings.state.aiModel)
        root.put("max_tokens", settings.state.aiMaxTokens)
        root.put("temperature", 0.2)

        val messages: ArrayNode = root.putArray("messages")
        messages.addObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        }
        messages.addObject().apply {
            put("role", "user")
            put("content", userContent)
        }

        val builder = Request.Builder()
            .url(endpoint())
            .post(mapper.writeValueAsString(root).toRequestBody(json))
        // Key is optional — local providers (Ollama, LM Studio) often need none.
        settings.aiApiKey.takeIf { it.isNotBlank() }?.let { builder.header("Authorization", "Bearer $it") }

        client().newCall(builder.build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("AI request failed (HTTP ${resp.code}): $text")
            val node = mapper.readTree(text)
            return node.get("choices")?.firstOrNull()
                ?.get("message")?.get("content")?.asText()
                ?: error("AI response had no choices[0].message.content")
        }
    }

    companion object {
        /**
         * Lightweight connectivity/auth check: GET {baseUrl}/models. Returns null on
         * success, otherwise a short human-readable error. Does not spend tokens.
         */
        fun testConnection(baseUrl: String, apiKey: String): String? {
            if (baseUrl.isBlank()) return "Base URL is empty"
            val client = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build()
            val url = baseUrl.trimEnd('/') + "/models"
            val builder = Request.Builder().url(url).get()
            if (apiKey.isNotBlank()) builder.header("Authorization", "Bearer $apiKey")
            return try {
                client.newCall(builder.build()).execute().use { resp ->
                    if (resp.isSuccessful) null
                    else "HTTP ${resp.code}: ${resp.body?.string()?.take(200).orEmpty()}"
                }
            } catch (e: Exception) {
                "${e.javaClass.simpleName}: ${e.message}"
            }
        }
    }
}
