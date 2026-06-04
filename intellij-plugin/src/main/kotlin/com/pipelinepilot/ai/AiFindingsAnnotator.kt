package com.pipelinepilot.ai

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.pipelinepilot.JenkinsfileDetector

/**
 * Renders the cached AI review findings as inline gutter markers + hover tooltips on the
 * Jenkinsfile, so each flagged element is visible directly in the editor (not only in the
 * review popup). Findings come from [AiFindingsHolder], populated by the analyze action.
 */
class AiFindingsAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PsiFile) return
        if (!JenkinsfileDetector.isJenkinsfile(element)) return

        val path = element.virtualFile?.path ?: return
        val findings = AiFindingsHolder.getInstance(element.project).get(path)
        if (findings.isEmpty()) return

        val doc = element.viewProvider.document ?: return
        for (f in findings) {
            val lineIdx = (f.line - 1).coerceIn(0, (doc.lineCount - 1).coerceAtLeast(0))
            val start = doc.getLineStartOffset(lineIdx)
            val end = doc.getLineEndOffset(lineIdx)
            holder.newAnnotation(f.severity.toHighlight(), "AI: ${f.title}")
                .range(TextRange(start, end))
                .tooltip(buildString {
                    append("<b>PipelinePilot AI</b> — ").append(escape(f.title)).append("<br/>")
                    append(escape(f.recommendation))
                })
                .create()
        }
    }

    private fun String.toHighlight(): HighlightSeverity = when (uppercase()) {
        "ERROR" -> HighlightSeverity.WARNING
        else -> HighlightSeverity.WEAK_WARNING
    }

    private fun escape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
