package com.pipelinepilot.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinterOutputParserTest {

    @Test
    fun `success output yields valid response`() {
        val r = LinterOutputParser.parse("Jenkinsfile successfully validated.")
        assertTrue(r.valid)
        assertTrue(r.diagnostics.isEmpty())
    }

    @Test
    fun `parses line and column from workflow error`() {
        val text = """
            Errors encountered validating Jenkinsfile:
            WorkflowScript: 5: Missing required section "agent" @ line 5, column 1.
        """.trimIndent()

        val r = LinterOutputParser.parse(text)
        assertFalse(r.valid)
        assertEquals(1, r.diagnostics.size)
        val d = r.diagnostics.first()
        assertEquals(Severity.ERROR, d.severity)
        assertEquals(5, d.line)
        assertEquals(1, d.column)
        assertEquals("Missing required section \"agent\"", d.message)
    }

    @Test
    fun `multiple errors parsed independently`() {
        val text = """
            Errors encountered validating Jenkinsfile:
            WorkflowScript: 3: unexpected token @ line 3, column 9.
            WorkflowScript: 7: expected '}' @ line 7, column 2.
        """.trimIndent()

        val r = LinterOutputParser.parse(text)
        assertEquals(2, r.diagnostics.size)
        assertEquals(3, r.diagnostics[0].line)
        assertEquals(7, r.diagnostics[1].line)
    }
}
