package com.pipelinepilot.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.wm.ToolWindowManager
import com.pipelinepilot.JenkinsfileDetector
import com.pipelinepilot.sandbox.SandboxService

/**
 * "Run in Sandbox" (Ctrl+Enter on a Jenkinsfile). Saves the file, opens the
 * PipelinePilot tool window, and streams a remote sandbox execution into it.
 */
class RunInSandboxAction : AnAction() {

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

        ToolWindowManager.getInstance(project).getToolWindow("PipelinePilot")?.activate(null)
        SandboxService.getInstance(project).run(jenkinsfile = content)
    }
}
