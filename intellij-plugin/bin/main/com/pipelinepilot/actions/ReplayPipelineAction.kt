package com.pipelinepilot.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager
import com.pipelinepilot.sandbox.SandboxService

/** Replays the whole last-run pipeline. */
class ReplayPipelineAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ToolWindowManager.getInstance(project).getToolWindow("PipelinePilot")?.activate(null)
        SandboxService.getInstance(project).replayPipeline()
    }
}
