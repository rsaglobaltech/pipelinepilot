package com.pipelinepilot.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/** Settings UI under: Settings | Tools | PipelinePilot for Jenkins. */
class PipelinePilotConfigurable : Configurable {

    private val urlField = JBTextField()
    private val userField = JBTextField()
    private val tokenField = JBPasswordField()
    private val validatePathField = JBTextField()
    private val sharedLibsArea = JBTextArea(4, 40)
    private val aiEnabledCheck = javax.swing.JCheckBox("Enable AI assistance (OpenAI-compatible)")
    private val aiProviderCombo = ComboBox(AiProvider.entries.map { it.displayName }.toTypedArray())
    private val aiBaseUrlField = JBTextField()
    private val aiKeyField = JBPasswordField()
    private val aiModelField = JBTextField()
    private val aiTestButton = JButton("Test Connection")
    private val aiStatusLabel = JBLabel(" ")

    private var suppressProviderEvent = false

    /** Pings the AI endpoint with the current (unsaved) field values; updates the status label. */
    private fun testAiConnection() {
        val baseUrl = aiBaseUrlField.text.trim()
        val key = String(aiKeyField.password)
        aiTestButton.isEnabled = false
        aiStatusLabel.foreground = JBColor.GRAY
        aiStatusLabel.text = "Testing…"
        ApplicationManager.getApplication().executeOnPooledThread {
            val error = com.pipelinepilot.ai.AiClient.testConnection(baseUrl, key)
            ApplicationManager.getApplication().invokeLater {
                if (error == null) {
                    aiStatusLabel.foreground = JBColor(0x2E7D32, 0x6FBF73) // green
                    aiStatusLabel.text = "● Connected"
                } else {
                    aiStatusLabel.foreground = JBColor(0xC62828, 0xEF5350) // red
                    aiStatusLabel.text = "● Failed: ${error.take(120)}"
                }
                aiTestButton.isEnabled = true
            }
        }
    }

    /** When the provider changes, auto-fill base URL + model (still editable). */
    private fun onProviderChanged() {
        if (suppressProviderEvent) return
        val p = AiProvider.fromDisplayName(aiProviderCombo.selectedItem as? String)
        if (p == AiProvider.CUSTOM) return
        aiBaseUrlField.text = p.defaultBaseUrl
        aiModelField.text = p.defaultModel
    }

    private var panel: JPanel? = null

    override fun getDisplayName(): String = "PipelinePilot for Jenkins"

    override fun createComponent(): JComponent {
        val p = FormBuilder.createFormBuilder()
            .addLabeledComponent("Jenkins URL:", urlField)
            .addLabeledComponent("Username:", userField)
            .addLabeledComponent("API token:", tokenField)
            .addLabeledComponent("Validate endpoint path:", validatePathField)
            .addLabeledComponent("Shared library roots (one path per line):", sharedLibsArea)
            .addComponent(aiEnabledCheck)
            .addLabeledComponent("AI provider:", aiProviderCombo)
            .addLabeledComponent("AI base URL:", aiBaseUrlField)
            .addLabeledComponent("AI API key:", aiKeyField)
            .addLabeledComponent("AI model:", aiModelField)
            .addComponent(JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                add(aiTestButton)
                add(javax.swing.Box.createHorizontalStrut(10))
                add(aiStatusLabel)
            })
            .addComponentFillVertically(JPanel(), 0)
            .panel
        aiProviderCombo.addActionListener { onProviderChanged() }
        aiTestButton.addActionListener { testAiConnection() }
        panel = p
        reset()
        return p
    }

    override fun isModified(): Boolean {
        val s = PipelinePilotSettings.getInstance()
        return urlField.text != s.state.jenkinsUrl ||
            userField.text != s.state.username ||
            validatePathField.text != s.state.validatePath ||
            sharedLibsArea.text != s.state.sharedLibraryRoots.joinToString("\n") ||
            aiEnabledCheck.isSelected != s.state.aiEnabled ||
            (aiProviderCombo.selectedItem as? String) != AiProvider.fromId(s.state.aiProvider).displayName ||
            aiBaseUrlField.text != s.state.aiBaseUrl ||
            aiModelField.text != s.state.aiModel ||
            String(aiKeyField.password) != s.aiApiKey ||
            String(tokenField.password) != s.apiToken
    }

    override fun apply() {
        val s = PipelinePilotSettings.getInstance()
        s.state.jenkinsUrl = urlField.text.trim().removeSuffix("/")
        s.state.username = userField.text.trim()
        s.state.validatePath = validatePathField.text.trim()
        s.state.sharedLibraryRoots = sharedLibsArea.text.lines()
            .map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        s.state.aiEnabled = aiEnabledCheck.isSelected
        s.state.aiProvider = AiProvider.fromDisplayName(aiProviderCombo.selectedItem as? String).id
        s.state.aiBaseUrl = aiBaseUrlField.text.trim()
        s.state.aiModel = aiModelField.text.trim()
        s.aiApiKey = String(aiKeyField.password)
        s.apiToken = String(tokenField.password)
    }

    override fun reset() {
        val s = PipelinePilotSettings.getInstance()
        urlField.text = s.state.jenkinsUrl
        userField.text = s.state.username
        validatePathField.text = s.state.validatePath
        sharedLibsArea.text = s.state.sharedLibraryRoots.joinToString("\n")
        aiEnabledCheck.isSelected = s.state.aiEnabled
        suppressProviderEvent = true
        aiProviderCombo.selectedItem = AiProvider.fromId(s.state.aiProvider).displayName
        suppressProviderEvent = false
        aiBaseUrlField.text = s.state.aiBaseUrl
        aiModelField.text = s.state.aiModel
        aiKeyField.text = s.aiApiKey
        tokenField.text = s.apiToken
    }
}
