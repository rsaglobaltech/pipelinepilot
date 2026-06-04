package com.pipelinepilot.ai

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.pipelinepilot.settings.PipelinePilotSettings

data class FixSuggestion(val explanation: String, val correctedJenkinsfile: String?)

/**
 * AI-assisted diagnostics for Jenkins pipelines. Wraps [AiClient] with task-specific
 * system prompts. All calls are blocking — invoke from a background task.
 */
@Service(Service.Level.PROJECT)
class AiAssistant(private val project: Project) {

    private val settings get() = PipelinePilotSettings.getInstance()
    private val client = AiClient()

    fun isReady(): Boolean = settings.isAiReady()

    /** Suggest a fix for [diagnostic] in [jenkinsfile]. Returns explanation + optional corrected file. */
    fun suggestFix(jenkinsfile: String, diagnostic: String): FixSuggestion {
        val reply = client.complete(
            systemPrompt = FIX_SYSTEM_PROMPT,
            userContent = buildString {
                appendLine("Linter error:")
                appendLine(diagnostic)
                appendLine()
                appendLine("Jenkinsfile:")
                appendLine("```groovy")
                appendLine(jenkinsfile)
                appendLine("```")
            },
        )
        return FixSuggestion(
            explanation = stripCode(reply).trim(),
            correctedJenkinsfile = extractGroovyBlock(reply),
        )
    }

    /** Return optimization / best-practice recommendations for [jenkinsfile]. */
    fun analyzePipeline(jenkinsfile: String): String =
        client.complete(
            systemPrompt = ANALYZE_SYSTEM_PROMPT,
            userContent = "Analyze this Jenkinsfile:\n```groovy\n$jenkinsfile\n```",
        )

    companion object {
        fun getInstance(project: Project): AiAssistant = project.service()

        private val CODE_BLOCK = Regex("""```(?:groovy)?\s*\n([\s\S]*?)```""")

        private const val FIX_SYSTEM_PROMPT =
            "You are a Jenkins Pipeline expert. Given a declarative or scripted Jenkinsfile and a " +
                "linter error, explain the root cause in 1-2 sentences, then provide the full corrected " +
                "Jenkinsfile inside a single ```groovy code block. Keep changes minimal and idiomatic."

        private const val ANALYZE_SYSTEM_PROMPT =
            "You are a Jenkins Pipeline expert. Review the Jenkinsfile for performance, caching, " +
                "parallelism, security, and maintainability. Return a concise bulleted list of concrete, " +
                "prioritized recommendations. Do not rewrite the whole file unless asked."

        fun extractGroovyBlock(text: String): String? =
            CODE_BLOCK.find(text)?.groupValues?.get(1)?.trimEnd()

        fun stripCode(text: String): String = text.replace(CODE_BLOCK, "").trim()
    }
}
