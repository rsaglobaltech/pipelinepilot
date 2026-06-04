package com.pipelinepilot.ai

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.pipelinepilot.settings.PipelinePilotSettings

data class FixSuggestion(val explanation: String, val correctedJenkinsfile: String?)

/** One element of the pipeline the AI flagged for improvement. */
data class AiFinding(
    val line: Int = 1,
    val severity: String = "INFO",
    val title: String = "",
    val recommendation: String = "",
)

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

    /**
     * Review [jenkinsfile] and return structured, per-element findings (each with the
     * 1-based line to improve). Used to drive the inline AI review popup.
     */
    fun reviewPipeline(jenkinsfile: String): List<AiFinding> {
        val numbered = jenkinsfile.lineSequence()
            .mapIndexed { i, l -> "${i + 1}: $l" }
            .joinToString("\n")
        val reply = client.complete(
            systemPrompt = REVIEW_SYSTEM_PROMPT,
            userContent = "Review this Jenkinsfile (each line is prefixed with its number):\n$numbered",
        )
        return parseFindings(reply)
    }

    companion object {
        fun getInstance(project: Project): AiAssistant = project.service()

        private val CODE_BLOCK = Regex("""```(?:groovy)?\s*\n([\s\S]*?)```""")

        private const val FIX_SYSTEM_PROMPT =
            "You are a Jenkins Pipeline expert. Given a declarative or scripted Jenkinsfile and a " +
                "linter error, explain the root cause in 1-2 sentences, then provide the full corrected " +
                "Jenkinsfile inside a single ```groovy code block. Keep changes minimal and idiomatic."

        private const val REVIEW_SYSTEM_PROMPT =
            "You are a Jenkins Pipeline expert performing a code review. Identify each concrete " +
                "element of the Jenkinsfile that should be improved — performance, caching, parallelism, " +
                "security, error handling, maintainability, and missing best practices.\n" +
                "Respond with ONLY a JSON array (no prose, no markdown fences). Each item:\n" +
                "{\"line\": <1-based line number of the element>, \"severity\": \"ERROR|WARNING|INFO\", " +
                "\"title\": \"<short summary>\", \"recommendation\": \"<specific, actionable fix>\"}\n" +
                "Order by importance. Return [] if the pipeline is already solid."

        private val mapper: ObjectMapper = ObjectMapper()
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        private val JSON_ARRAY = Regex("""\[[\s\S]*]""")

        fun extractGroovyBlock(text: String): String? =
            CODE_BLOCK.find(text)?.groupValues?.get(1)?.trimEnd()

        fun stripCode(text: String): String = text.replace(CODE_BLOCK, "").trim()

        /** Parse the model's JSON array of findings; tolerant of stray prose/fences. */
        fun parseFindings(reply: String): List<AiFinding> {
            val json = JSON_ARRAY.find(reply)?.value ?: return fallback(reply)
            return runCatching {
                mapper.readValue(json, Array<AiFinding>::class.java).toList()
                    .filter { it.title.isNotBlank() || it.recommendation.isNotBlank() }
            }.getOrElse { fallback(reply) }
        }

        private fun fallback(reply: String): List<AiFinding> {
            val text = reply.trim()
            return if (text.isEmpty()) emptyList()
            else listOf(AiFinding(1, "INFO", "AI review", text.take(2000)))
        }
    }
}
