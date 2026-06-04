package com.pipelinepilot.validation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.pipelinepilot.JenkinsfileDetector
import com.pipelinepilot.ai.AiAssistant
import com.pipelinepilot.ai.AiSuggestFixIntention
import com.pipelinepilot.api.Diagnostic
import com.pipelinepilot.api.Severity
import com.pipelinepilot.api.ValidateRequest
import com.pipelinepilot.api.ValidateResponse
import com.pipelinepilot.api.ValidationClient

/** Carries the file text + document snapshot from the EDT to the background pass. */
data class AnnotationInput(val text: String, val document: Document?)

/**
 * Runs the remote Jenkins linter on a background thread and renders the returned
 * [Diagnostic]s as inline annotations with gutter markers.
 *
 * Registered on the Groovy language; bails out immediately for non-Jenkinsfiles.
 */
class JenkinsfileExternalAnnotator(
    private val client: ValidationClient = ValidationClient(),
) : ExternalAnnotator<AnnotationInput, ValidateResponse>() {

    override fun collectInformation(file: PsiFile): AnnotationInput? {
        if (!JenkinsfileDetector.isJenkinsfile(file)) return null
        val doc = file.viewProvider.document
        return AnnotationInput(file.text, doc)
    }

    override fun doAnnotate(info: AnnotationInput?): ValidateResponse? {
        val input = info ?: return null
        return client.validate(ValidateRequest(jenkinsfile = input.text))
    }

    override fun apply(file: PsiFile, result: ValidateResponse?, holder: AnnotationHolder) {
        val response = result ?: return
        if (response.valid && response.diagnostics.isEmpty()) return
        val doc = file.viewProvider.document ?: return

        val aiReady = AiAssistant.getInstance(file.project).isReady()
        for (d in response.diagnostics) {
            val range = rangeFor(d, doc, file.textLength)
            val builder = holder.newAnnotation(d.severity.toHighlight(), d.message).range(range)
            if (d.severity == Severity.ERROR) {
                if (d.message.contains("agent", ignoreCase = true)) {
                    builder.withFix(AddAgentAnyIntention())
                }
                if (aiReady) builder.withFix(AiSuggestFixIntention(d.message))
            }
            builder.create()
        }
    }

    private fun rangeFor(d: Diagnostic, doc: Document, fileLength: Int): TextRange {
        // Prefer explicit offsets when the backend provides them.
        val start = d.offsetStart
        val end = d.offsetEnd
        if (start != null && end != null && start in 0..fileLength) {
            return TextRange(start, end.coerceAtMost(fileLength))
        }
        val lineIdx = (d.line - 1).coerceIn(0, (doc.lineCount - 1).coerceAtLeast(0))
        val lineStart = doc.getLineStartOffset(lineIdx)
        val lineEnd = doc.getLineEndOffset(lineIdx)
        val colOffset = (lineStart + (d.column - 1)).coerceIn(lineStart, lineEnd)
        // Highlight from the reported column to end of line; if column is at EOL,
        // highlight the whole line so the marker is visible.
        return if (colOffset < lineEnd) TextRange(colOffset, lineEnd)
        else TextRange(lineStart, lineEnd.coerceAtLeast(lineStart + 1).coerceAtMost(fileLength))
    }

    private fun Severity.toHighlight(): HighlightSeverity = when (this) {
        Severity.ERROR -> HighlightSeverity.ERROR
        Severity.WARNING -> HighlightSeverity.WARNING
        Severity.INFO -> HighlightSeverity.WEAK_WARNING
    }
}
