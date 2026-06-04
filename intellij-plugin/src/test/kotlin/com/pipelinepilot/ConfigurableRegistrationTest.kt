package com.pipelinepilot

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.wm.ToolWindowEP
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Regression guard: the app-level extensions must actually register. This caught a
 * `defaultExtensionPointName` (wrong) vs `defaultExtensionNs` (correct) typo that
 * silently dropped the entire <extensions> block.
 */
class ConfigurableRegistrationTest : BasePlatformTestCase() {

    fun testApplicationConfigurableRegistered() {
        val ids = Configurable.APPLICATION_CONFIGURABLE.extensionList.map { it.id }
        assertTrue("Settings page not registered. ids=$ids", ids.contains("com.pipelinepilot.settings"))
    }

    fun testToolWindowRegistered() {
        val ids = ToolWindowEP.EP_NAME.extensionList.map { it.id }
        assertTrue("Tool window not registered. ids=$ids", ids.contains("PipelinePilot"))
    }
}
