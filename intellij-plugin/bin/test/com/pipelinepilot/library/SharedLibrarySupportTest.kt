package com.pipelinepilot.library

import org.junit.Assert.assertEquals
import org.junit.Test

class SharedLibrarySupportTest {

    @Test
    fun `parses single library declaration`() {
        val libs = SharedLibrarySupport.declaredLibraries("@Library('my-shared-lib') _")
        assertEquals(listOf("my-shared-lib"), libs)
    }

    @Test
    fun `parses library with ref and double quotes`() {
        val libs = SharedLibrarySupport.declaredLibraries("""@Library("platform@v1.2") import x""")
        assertEquals(listOf("platform@v1.2"), libs)
    }

    @Test
    fun `parses multiple declarations`() {
        val text = """
            @Library('lib-a') _
            @Library('lib-b@main') _
        """.trimIndent()
        assertEquals(listOf("lib-a", "lib-b@main"), SharedLibrarySupport.declaredLibraries(text))
    }
}
