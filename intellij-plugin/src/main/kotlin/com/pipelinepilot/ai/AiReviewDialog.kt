package com.pipelinepilot.ai

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.ListCellRenderer

/**
 * Non-modal review popup anchored to the analyzed Jenkinsfile. Lists each AI finding;
 * selecting one jumps the editor to the relevant line and shows the recommendation.
 */
class AiReviewDialog(
    private val project: Project,
    private val fileName: String,
    private val editor: Editor?,
    private val findings: List<AiFinding>,
) : DialogWrapper(project, false) {

    private val model = DefaultListModel<AiFinding>().apply { findings.forEach(::addElement) }
    private val list = JBList(model)
    private val detail = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        border = JBUI.Borders.empty(8)
    }

    init {
        title = "AI Pipeline Review — $fileName  (${findings.size} ${if (findings.size == 1) "finding" else "findings"})"
        isModal = false
        init()
    }

    override fun createCenterPanel(): JComponent {
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.cellRenderer = FindingRenderer()
        list.addListSelectionListener {
            val f = list.selectedValue ?: return@addListSelectionListener
            detail.text = f.recommendation
            navigateTo(f.line)
        }
        if (!model.isEmpty) list.selectedIndex = 0

        val left = JBScrollPane(list).apply { preferredSize = Dimension(320, 360) }
        val right = JBScrollPane(detail).apply { preferredSize = Dimension(420, 360) }

        return JPanel(BorderLayout()).apply {
            add(left, BorderLayout.WEST)
            add(right, BorderLayout.CENTER)
            preferredSize = Dimension(760, 380)
        }
    }

    private fun navigateTo(line: Int) {
        val e = editor ?: return
        val doc = e.document
        val idx = (line - 1).coerceIn(0, (doc.lineCount - 1).coerceAtLeast(0))
        val offset = doc.getLineStartOffset(idx)
        e.caretModel.moveToOffset(offset)
        e.selectionModel.selectLineAtCaret()
        e.scrollingModel.scrollToCaret(ScrollType.CENTER)
    }

    override fun createActions() = arrayOf(applyFixAction, okAction)

    private val applyFixAction = object : DialogWrapperAction("Apply Fix (AI)") {
        override fun doAction(e: java.awt.event.ActionEvent) = applySelectedFix()
    }

    /** Ask the AI to apply the selected finding to the whole file, then replace the document. */
    private fun applySelectedFix() {
        val finding = list.selectedValue ?: return
        val ed = editor ?: return
        val original = ed.document.text
        val assistant = AiAssistant.getInstance(project)
        val instruction = "Line ${finding.line} — ${finding.title}: ${finding.recommendation}"

        isOKActionEnabled = false
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Applying AI fix") {
            override fun run(indicator: ProgressIndicator) {
                val suggestion = runCatching { assistant.suggestFix(original, instruction) }.getOrElse { ex ->
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(project, ex.message ?: "AI request failed", "PipelinePilot AI")
                        isOKActionEnabled = true
                    }
                    return
                }
                ApplicationManager.getApplication().invokeLater {
                    val corrected = suggestion.correctedJenkinsfile
                    if (corrected == null) {
                        Messages.showInfoMessage(project, suggestion.explanation, "PipelinePilot AI — Suggestion")
                    } else {
                        WriteCommandAction.runWriteCommandAction(project, "Apply AI Fix", null, {
                            ed.document.setText(corrected)
                        })
                    }
                    isOKActionEnabled = true
                }
            }
        })
    }

    private class FindingRenderer : ListCellRenderer<AiFinding> {
        private val base = com.intellij.ui.components.JBLabel()
        override fun getListCellRendererComponent(
            list: JList<out AiFinding>, value: AiFinding, index: Int,
            selected: Boolean, focused: Boolean,
        ): Component {
            base.text = "  ${dot(value.severity)}  L${value.line} — ${value.title}"
            base.isOpaque = true
            base.background = if (selected) list.selectionBackground else list.background
            base.foreground = if (selected) list.selectionForeground else colorFor(value.severity)
            base.border = JBUI.Borders.empty(4, 2)
            return base
        }

        private fun dot(sev: String) = "●"
        private fun colorFor(sev: String): Color = when (sev.uppercase()) {
            "ERROR" -> JBColor(0xC62828, 0xEF5350)
            "WARNING" -> JBColor(0xE08600, 0xE0A030)
            else -> JBColor(0x2E7D32, 0x6FBF73)
        }
    }
}
