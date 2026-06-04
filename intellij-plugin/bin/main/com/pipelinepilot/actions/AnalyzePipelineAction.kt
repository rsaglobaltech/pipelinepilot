package com.pipelinepilot.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.wm.ToolWindowManager
import com.pipelinepilot.JenkinsfileDetector
import com.pipelinepilot.ai.AiAssistant
import com.pipelinepilot.sandbox.PipelinePilotLogConsole

/**
 * "Analyze Pipeline (AI)" — sends the Jenkinsfile to the AI for optimization and
 * best-practice recommendations, printed into the PipelinePilot Logs console.
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
        val content = e.getData(CommonDataKeys.EDITOR)?.document?.text ?: return
        val assistant = AiAssistant.getInstance(project)
        val log = PipelinePilotLogConsole.getInstance(project)

        ToolWindowManager.getInstance(project).getToolWindow("PipelinePilot")?.activate(null)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Analyzing pipeline (AI)") {
            override fun run(indicator: ProgressIndicator) {
                val result = runCatching { assistant.analyzePipeline(content) }.getOrElse { ex ->
                    ApplicationManager.getApplication().invokeLater { log.error("AI analysis failed: ${ex.message}") }
                    notify(project, "AI analysis failed: ${ex.message}", NotificationType.ERROR)
                    return
                }
                ApplicationManager.getApplication().invokeLater {
                    log.banner("\n=== AI Pipeline Insights ===")
                    result.lines().forEach { log.banner(it) }
                }
                notify(project, "AI analysis complete — see PipelinePilot Logs", NotificationType.INFORMATION)
            }
        })
    }

    private fun notify(project: com.intellij.openapi.project.Project, text: String, type: NotificationType) {
        NotificationGroupManager.getInstance().getNotificationGroup("PipelinePilot")
            .createNotification(text, type).notify(project)
    }
}
