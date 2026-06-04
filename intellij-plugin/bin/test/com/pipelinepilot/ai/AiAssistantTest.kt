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
}
