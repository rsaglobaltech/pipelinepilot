package com.pipelinepilot.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

/** Settings UI under: Settings | Tools | PipelinePilot for Jenkins. */
class PipelinePilotConfigurable : Configurable {

    private val urlField = JBTextField()
    private val userField = JBTextField()
    private val tokenField = JBPasswordField()
    private val validatePathField = JBTextField()
    private val sharedLibsArea = JBTextArea(4, 40)
    private val aiEnabledCheck = javax.swing.JCheckBox("Enable AI assistance (Anthropic)")
    private val aiKeyField = JBPasswordField()
    private val aiModelField = JBTextField()

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
            .addLabeledComponent("Anthropic API key:", aiKeyField)
            .addLabeledComponent("AI model:", aiModelField)
            .addComponentFillVertically(JPanel(), 0)
            .panel
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
            aiModelField.text != s.state.aiModel ||
            String(aiKeyField.password) != s.anthropicApiKey ||
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
        s.state.aiModel = aiModelField.text.trim()
        s.anthropicApiKey = String(aiKeyField.password)
        s.apiToken = String(tokenField.password)
    }

    override fun reset() {
        val s = PipelinePilotSettings.getInstance()
        urlField.text = s.state.jenkinsUrl
        userField.text = s.state.username
        validatePathField.text = s.state.validatePath
        sharedLibsArea.text = s.state.sharedLibraryRoots.joinToString("\n")
        aiEnabledCheck.isSelected = s.state.aiEnabled
        aiModelField.text = s.state.aiModel
        aiKeyField.text = s.anthropicApiKey
        tokenField.text = s.apiToken
    }
}
