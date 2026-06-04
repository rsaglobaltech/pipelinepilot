package com.pipelinepilot.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.pipelinepilot.JenkinsfileDetector
import com.pipelinepilot.api.Severity
import com.pipelinepilot.api.ValidateRequest
import com.pipelinepilot.api.ValidationClient

/**
 * Manual "Validate Jenkinsfile" action (Ctrl+Alt+V / editor context menu).
 * Saves the document, runs the remote linter off the EDT, and reports a summary
 * notification. Inline markers are produced separately by the ExternalAnnotator.
 */
class ValidateJenkinsfileAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = JenkinsfileDetector.isJenkinsfile(file)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document

        ApplicationManager.getApplication().runWriteAction {
            FileDocumentManager.getInstance().saveDocument(document)
        }
        val content = document.text

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Validating Jenkinsfile") {
            override fun run(indicator: ProgressIndicator) {
                val response = ValidationClient().validate(ValidateRequest(jenkinsfile = content))
                val errors = response.diagnostics.count { it.severity == Severity.ERROR }
                notify(project, response.valid, errors)
            }
        })
    }

    private fun notify(project: Project, valid: Boolean, errorCount: Int) {
        val group = NotificationGroupManager.getInstance().getNotificationGroup("PipelinePilot")
        val (type, text) = if (valid) {
            NotificationType.INFORMATION to "Jenkinsfile successfully validated."
        } else {
            NotificationType.ERROR to "Jenkinsfile validation failed: $errorCount error(s). See inline markers."
        }
        group.createNotification(text, type).notify(project)
    }
}
