package com.pipelinepilot.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAssistantTest {

    @Test
    fun `extracts groovy code block`() {
        val reply = """
            The agent section is missing. Here is the fix:
            ```groovy
            pipeline {
                agent any
            }
            ```
        """.trimIndent()

        val code = AiAssistant.extractGroovyBlock(reply)
        assertTrue(code!!.contains("agent any"))
        assertTrue(code.startsWith("pipeline {"))
    }

    @Test
    fun `returns null when no code block`() {
        assertNull(AiAssistant.extractGroovyBlock("Just prose, no code."))
    }

    @Test
    fun `strips code block leaving explanation`() {
        val reply = "Root cause here.\n```groovy\npipeline {}\n```"
        assertEquals("Root cause here.", AiAssistant.stripCode(reply))
    }

    @Test
    fun `parses findings json array`() {
        val reply = """
            Here is the review:
            [
              {"line": 3, "severity": "WARNING", "title": "No agent", "recommendation": "Add agent any"},
              {"line": 7, "severity": "INFO", "title": "Use parallel", "recommendation": "Run tests in parallel"}
            ]
        """.trimIndent()
        val f = AiAssistant.parseFindings(reply)
        assertEquals(2, f.size)
        assertEquals(3, f[0].line)
        assertEquals("WARNING", f[0].severity)
        assertEquals("No agent", f[0].title)
    }

    @Test
    fun `falls back to single finding when not json`() {
        val f = AiAssistant.parseFindings("Looks good overall, minor nits.")
        assertEquals(1, f.size)
        assertEquals(1, f[0].line)
    }

    @Test
    fun `empty reply yields no findings`() {
        assertTrue(AiAssistant.parseFindings("   ").isEmpty())
    }
}
