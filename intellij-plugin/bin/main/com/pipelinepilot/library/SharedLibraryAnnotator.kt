package com.pipelinepilot.library

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.pipelinepilot.JenkinsfileDetector

/**
 * Marks `@Library('name')` declarations in a Jenkinsfile. Libraries with a local
 * checkout configured in settings are informational; unresolved ones get a weak
 * warning hinting that completion/navigation won't work for their steps.
 */
class SharedLibraryAnnotator : Annotator {

    private val annotation = Regex("""@Library\(\s*\[?\s*['"]([^'"]+)['"]""")

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PsiFile) return
        if (!JenkinsfileDetector.isJenkinsfile(element)) return

        val text = element.text
        val resolvedRoots = SharedLibrarySupport.roots()

        for (match in annotation.findAll(text)) {
            val rawName = match.groupValues[1]
            val libName = rawName.substringBefore('@')
            val range = TextRange(match.range.first, match.range.last + 1)
            val resolved = resolvedRoots.isNotEmpty()

            if (resolved) {
                holder.newAnnotation(HighlightSeverity.INFORMATION, "Shared library: $libName")
                    .range(range).create()
            } else {
                holder.newAnnotation(
                    HighlightSeverity.WEAK_WARNING,
                    "Shared library '$libName' has no local checkout — add one in Settings | Tools | PipelinePilot for step completion/navigation"
                ).range(range).create()
            }
        }
    }
}
