package com.pipelinepilot.ai

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.pipelinepilot.settings.PipelinePilotSettings
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

/**
 * End-to-end check of [AiClient] against a tiny embedded OpenAI-compatible server:
 * verifies the request shape (model, system+user messages, Bearer key) and that the
 * `choices[0].message.content` response is parsed back correctly.
 */
class AiClientIntegrationTest : BasePlatformTestCase() {

    private lateinit var server: HttpServer
    private var port = 0
    private var lastBody = ""
    private var lastAuth: String? = null

    override fun setUp() {
        super.setUp()
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        port = server.address.port
        server.createContext("/v1/chat/completions") { ex ->
            lastAuth = ex.requestHeaders.getFirst("Authorization")
            lastBody = ex.requestBody.readBytes().toString(StandardCharsets.UTF_8)
            val resp = """{"choices":[{"index":0,"message":{"role":"assistant","content":"pong"}}]}"""
            val bytes = resp.toByteArray()
            ex.responseHeaders.add("Content-Type", "application/json")
            ex.sendResponseHeaders(200, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        server.start()
    }

    override fun tearDown() {
        try {
            server.stop(0)
        } finally {
            super.tearDown()
        }
    }

    fun testCompleteSendsRequestAndParsesResponse() {
        val s = PipelinePilotSettings.getInstance()
        s.state.aiBaseUrl = "http://127.0.0.1:$port/v1"
        s.state.aiModel = "test-model"
        s.aiApiKey = "sk-test-123"

        val out = AiClient(s).complete("system here", "ping")

        assertEquals("pong", out)
        assertEquals("Bearer sk-test-123", lastAuth)
        assertTrue("body: $lastBody", lastBody.contains("\"model\":\"test-model\""))
        assertTrue("body: $lastBody", lastBody.contains("\"role\":\"system\""))
        assertTrue("body: $lastBody", lastBody.contains("\"content\":\"ping\""))
    }
}
