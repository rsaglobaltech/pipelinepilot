package com.pipelinepilot.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Connection settings for the remote Jenkins instance.
 *
 * The non-secret fields (URL, username) are persisted in the IDE config.
 * The API token is stored separately in the OS-backed [PasswordSafe] and is
 * never written to plain XML (see PROJECT_PLAN.md §7 — Security).
 */
@State(
    name = "PipelinePilotSettings",
    storages = [Storage("pipelinepilot.xml")],
)
class PipelinePilotSettings : PersistentStateComponent<PipelinePilotSettings.State> {

    data class State(
        var jenkinsUrl: String = "",
        var username: String = "",
        /** Append the path for the companion plugin's IDE endpoints. */
        var validatePath: String = "/pipeline-model-converter/validate",
        var validationTimeoutMillis: Int = 5000,
        // Companion-plugin sandbox endpoints (PROJECT_PLAN.md §6).
        var runSandboxPath: String = "/ide/runSandbox",
        var replayPath: String = "/ide/replay",
        var logsPath: String = "/ide/logs",
        var sessionPath: String = "/ide/session",
        var logPollIntervalMillis: Int = 700,
        /** Local checkouts of shared libraries (each has vars/ and src/). Newline-separated in UI. */
        var sharedLibraryRoots: MutableList<String> = mutableListOf(),
        // Phase 4: AI assistance (Anthropic Messages API).
        var aiEnabled: Boolean = false,
        var aiModel: String = "claude-sonnet-4-6",
        var aiMaxTokens: Int = 2048,
    )

    private var state = State()

    override fun getState(): State = state
    override fun loadState(s: State) = XmlSerializerUtil.copyBean(s, state)

    // --- secret token via PasswordSafe -------------------------------------

    private val tokenAttributes: CredentialAttributes
        get() = CredentialAttributes(generateServiceName("PipelinePilot", "apiToken"))

    var apiToken: String
        get() = PasswordSafe.instance.getPassword(tokenAttributes).orEmpty()
        set(value) = PasswordSafe.instance.setPassword(tokenAttributes, value.ifBlank { null })

    private val aiKeyAttributes: CredentialAttributes
        get() = CredentialAttributes(generateServiceName("PipelinePilot", "anthropicApiKey"))

    /** Anthropic API key — stored in PasswordSafe, never in plain XML. */
    var anthropicApiKey: String
        get() = PasswordSafe.instance.getPassword(aiKeyAttributes).orEmpty()
        set(value) = PasswordSafe.instance.setPassword(aiKeyAttributes, value.ifBlank { null })

    fun isAiReady(): Boolean = state.aiEnabled && anthropicApiKey.isNotBlank()

    fun isConfigured(): Boolean = state.jenkinsUrl.isNotBlank()

    companion object {
        fun getInstance(): PipelinePilotSettings =
            ApplicationManager.getApplication().getService(PipelinePilotSettings::class.java)
    }
}
