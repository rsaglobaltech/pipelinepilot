package com.pipelinepilot.validation

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * Deterministic (no-AI) quick-fix for the common declarative error
 * "Missing required section 'agent'": inserts `agent any` right after the
 * opening `pipeline {` line.
 */
class AddAgentAnyIntention : IntentionAction {

    private val pipelineOpen = Regex("""pipeline\s*\{""")

    override fun getText(): String = "PipelinePilot: Add 'agent any'"
    override fun getFamilyName(): String = "PipelinePilot"
    override fun startInWriteAction(): Boolean = true

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        val text = editor?.document?.text ?: return false
        return pipelineOpen.containsMatchIn(text) && !Regex("""\bagent\b""").containsMatchIn(text)
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val doc = editor?.document ?: return
        val match = pipelineOpen.find(doc.text) ?: return
        val insertAt = match.range.last + 1
        val lineStart = doc.getLineStartOffset(doc.getLineNumber(match.range.first))
        val indent = doc.text.substring(lineStart, match.range.first) + "    "
        WriteCommandAction.runWriteCommandAction(project, "Add agent any", null, {
            doc.insertString(insertAt, "\n${indent}agent any")
        }, file)
    }
}
