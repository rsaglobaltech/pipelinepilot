package com.pipelinepilot.library

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.pipelinepilot.JenkinsfileDetector

/**
 * Suggests shared-library global steps (from configured local `vars/`) while
 * editing a Jenkinsfile.
 */
class SharedLibraryCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val file = parameters.originalFile
        if (!JenkinsfileDetector.isJenkinsfile(file)) return

        for (step in SharedLibrarySupport.varSteps()) {
            result.addElement(
                LookupElementBuilder.create(step.name)
                    .withIcon(AllIcons.Nodes.Method)
                    .withTypeText("shared library step", true)
                    .withInsertHandler { ctx, _ ->
                        // Append call parens for a step invocation if not already present.
                        val doc = ctx.document
                        val tail = ctx.tailOffset
                        if (tail >= doc.textLength || doc.charsSequence.getOrNull(tail) != '(') {
                            doc.insertString(tail, "()")
                            ctx.editor.caretModel.moveToOffset(tail + 1)
                        }
                    }
            )
        }
    }
}
