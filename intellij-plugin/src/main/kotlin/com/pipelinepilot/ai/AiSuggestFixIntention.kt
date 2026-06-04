package com.pipelinepilot.ai

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiFile

/**
 * Quick-fix offered on a Jenkinsfile linter error: asks the AI for a corrected
 * file, shows the explanation, and applies the corrected content on confirmation.
 */
class AiSuggestFixIntention(private val diagnostic: String) : IntentionAction {

    override fun getText(): String = "PipelinePilot: Suggest fix (AI)"
    override fun getFamilyName(): String = "PipelinePilot AI"
    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
        file != null && AiAssistant.getInstance(project).isReady()

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val doc = editor?.document ?: return
        val original = doc.text
        val assistant = AiAssistant.getInstance(project)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Asking AI for a fix") {
            override fun run(indicator: ProgressIndicator) {
                val suggestion = runCatching { assistant.suggestFix(original, diagnostic) }.getOrElse { e ->
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(project, e.message ?: "AI request failed", "PipelinePilot AI")
                    }
                    return
                }
                ApplicationManager.getApplication().invokeLater {
                    val corrected = suggestion.correctedJenkinsfile
                    if (corrected == null) {
                        Messages.showInfoMessage(project, suggestion.explanation, "PipelinePilot AI — Suggestion")
                        return@invokeLater
                    }
                    val choice = Messages.showYesNoDialog(
                        project,
                        suggestion.explanation + "\n\nApply the suggested fix?",
                        "PipelinePilot AI — Suggested Fix",
                        "Apply", "Cancel", Messages.getQuestionIcon(),
                    )
                    if (choice == Messages.YES) {
                        WriteCommandAction.runWriteCommandAction(project, "Apply AI Fix", null, {
                            doc.setText(corrected)
                        }, file)
                    }
                }
            }
        })
    }
}
