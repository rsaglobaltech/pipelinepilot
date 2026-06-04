package com.pipelinepilot.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.content.ContentFactory
import com.pipelinepilot.sandbox.PipelinePilotLogConsole
import com.pipelinepilot.sandbox.SandboxService
import com.pipelinepilot.sandbox.SandboxSession
import java.awt.BorderLayout
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JPanel

/**
 * Builds the "PipelinePilot" tool window with two tabs:
 *   - Logs:     streaming console of the active sandbox run
 *   - Sessions: list of sandbox sessions with a Stop action
 */
class PipelinePilotToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        // --- Logs tab ---
        val logConsole = PipelinePilotLogConsole.getInstance(project)
        val logsPanel = JPanel(BorderLayout()).apply {
            add(logConsole.console.component, BorderLayout.CENTER)
        }
        toolWindow.contentManager.addContent(
            contentFactory.createContent(logsPanel, "Logs", false)
        )

        // --- Sessions tab ---
        val service = SandboxService.getInstance(project)
        val model = DefaultListModel<SandboxSession>()
        val list = JBList(model).apply {
            cellRenderer = javax.swing.DefaultListCellRenderer().let { base ->
                javax.swing.ListCellRenderer { l, value, idx, sel, focus ->
                    base.getListCellRendererComponent(
                        l, "${value.sessionId}  —  ${value.state}", idx, sel, focus
                    )
                }
            }
        }
        val stopButton = JButton("Stop & Clean Up").apply {
            addActionListener {
                list.selectedValue?.let { service.stop(it) }
            }
        }
        val sessionsPanel = JPanel(BorderLayout()).apply {
            add(com.intellij.ui.components.JBScrollPane(list), BorderLayout.CENTER)
            add(stopButton, BorderLayout.SOUTH)
        }

        service.addListener { sessions ->
            ApplicationManager.getApplication().invokeLater {
                model.clear()
                sessions.forEach { model.addElement(it) }
            }
        }

        toolWindow.contentManager.addContent(
            contentFactory.createContent(sessionsPanel, "Sessions", false)
        )

        // --- Stages tab ---
        val stagesPanel = JPanel(BorderLayout()).apply {
            val graph = StageGraphPanel(project)
            add(com.intellij.ui.components.JBScrollPane(graph), BorderLayout.CENTER)
            val replayBar = JPanel().apply {
                add(JButton("Replay Pipeline").apply {
                    addActionListener { service.replayPipeline() }
                })
                add(JButton("Replay Failed Stages").apply {
                    addActionListener { service.replayFailed() }
                })
            }
            add(replayBar, BorderLayout.NORTH)
        }
        toolWindow.contentManager.addContent(
            contentFactory.createContent(stagesPanel, "Stages", false)
        )
    }
}
