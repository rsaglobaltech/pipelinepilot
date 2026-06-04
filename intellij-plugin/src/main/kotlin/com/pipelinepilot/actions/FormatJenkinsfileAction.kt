package com.pipelinepilot.actions

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.pipelinepilot.JenkinsfileDetector

/**
 * "Format Jenkinsfile" — reformats the current pipeline file using the IDE's code
 * formatter (Groovy, since Jenkinsfiles are associated with the Groovy file type).
 * Works for `Jenkinsfile`, `*.jenkinsfile`, and `Jenkinsfile.<env>`.
 */
class FormatJenkinsfileAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = JenkinsfileDetector.isJenkinsfile(file)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        if (!JenkinsfileDetector.isJenkinsfile(psiFile)) return

        // ReformatCodeProcessor wraps the change in its own write command + undo step.
        ReformatCodeProcessor(project, psiFile, null, false).run()

        NotificationGroupManager.getInstance().getNotificationGroup("PipelinePilot")
            .createNotification("Jenkinsfile formatted.", NotificationType.INFORMATION)
            .notify(project)
    }
}
