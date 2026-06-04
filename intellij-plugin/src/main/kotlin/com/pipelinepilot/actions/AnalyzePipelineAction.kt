package com.pipelinepilot.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.pipelinepilot.JenkinsfileDetector
import com.pipelinepilot.ai.AiAssistant
import com.pipelinepilot.ai.AiFindingsHolder
import com.pipelinepilot.ai.AiReviewDialog

/**
 * "Analyze Pipeline (AI)" — reviews the Jenkinsfile and opens an inline review popup
 * anchored to the file, listing each element to improve. Selecting a finding jumps the
 * editor to the relevant line.
 */
class AnalyzePipelineAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val project = e.project
        e.presentation.isEnabledAndVisible =
            JenkinsfileDetector.isJenkinsfile(file) &&
            project != null && AiAssistant.getInstance(project).isReady()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val vFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val fileName = vFile?.name ?: "Jenkinsfile"
        val content = editor.document.text
        val assistant = AiAssistant.getInstance(project)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Reviewing pipeline (AI)") {
            override fun run(indicator: ProgressIndicator) {
                val findings = runCatching { assistant.reviewPipeline(content) }.getOrElse { ex ->
                    notify(project, "AI review failed: ${ex.message}", NotificationType.ERROR)
                    return
                }
                ApplicationManager.getApplication().invokeLater {
                    if (findings.isEmpty()) {
                        vFile?.let { AiFindingsHolder.getInstance(project).clear(it.path) }
                        notify(project, "AI review: no issues found — pipeline looks solid.", NotificationType.INFORMATION)
                    } else {
                        // Inline gutter markers: cache findings + refresh the daemon.
                        vFile?.let { AiFindingsHolder.getInstance(project).set(it.path, findings) }
                        psiFile?.let { DaemonCodeAnalyzer.getInstance(project).restart(it) }
                        AiReviewDialog(project, fileName, editor, findings).show()
                    }
                }
            }
        })
    }

    private fun notify(project: Project, text: String, type: NotificationType) {
        NotificationGroupManager.getInstance().getNotificationGroup("PipelinePilot")
            .createNotification(text, type).notify(project)
    }
}
