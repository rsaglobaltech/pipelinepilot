package com.pipelinepilot.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.pipelinepilot.api.StageStatus
import com.pipelinepilot.sandbox.SandboxService
import com.pipelinepilot.sandbox.StageGraphModel
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JMenuItem

/**
 * Renders the pipeline stage graph: top-level stages laid out left→right with
 * arrows, nested/parallel children stacked beneath their parent. Colored by
 * status. Right-click a stage to replay just that stage.
 */
class StageGraphPanel(private val project: Project) : JPanel() {

    private val model = StageGraphModel.getInstance(project)
    private var roots: List<StageGraphModel.Node> = emptyList()

    // name -> on-screen rectangle, for hit testing the context menu.
    private val hitBoxes = mutableMapOf<Rectangle, String>()

    private val boxW = 120
    private val boxH = 34
    private val hGap = 40
    private val vGap = 14
    private val margin = 16

    init {
        background = JBColor.background()
        model.addListener { newRoots ->
            ApplicationManager.getApplication().invokeLater {
                roots = newRoots
                revalidate(); repaint()
            }
        }
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) = maybePopup(e)
            override fun mouseReleased(e: MouseEvent) = maybePopup(e)
        })
    }

    private fun maybePopup(e: MouseEvent) {
        if (!e.isPopupTrigger) return
        val stage = hitBoxes.entries.firstOrNull { it.key.contains(e.point) }?.value ?: return
        JPopupMenu().apply {
            add(JMenuItem("Replay stage \"$stage\"").apply {
                addActionListener { SandboxService.getInstance(project).replayStage(stage) }
            })
        }.show(this, e.x, e.y)
    }

    override fun getPreferredSize(): Dimension {
        val cols = roots.size.coerceAtLeast(1)
        val maxDepthRows = roots.maxOfOrNull { 1 + countDescendants(it) } ?: 1
        return Dimension(
            margin * 2 + cols * boxW + (cols - 1).coerceAtLeast(0) * hGap,
            margin * 2 + maxDepthRows * (boxH + vGap),
        )
    }

    private fun countDescendants(node: StageGraphModel.Node): Int =
        node.children.sumOf { 1 + countDescendants(it) }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        hitBoxes.clear()
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        var x = margin
        var prevRight: Int? = null
        val y = margin
        for (root in roots) {
            if (prevRight != null) drawArrow(g2, prevRight, y + boxH / 2, x, y + boxH / 2)
            drawNode(g2, root, x, y)
            prevRight = x + boxW
            x += boxW + hGap
        }
    }

    /** Draws a node and its children stacked below it; returns nothing. */
    private fun drawNode(g2: Graphics2D, node: StageGraphModel.Node, x: Int, y: Int) {
        val rect = Rectangle(x, y, boxW, boxH)
        hitBoxes[rect] = node.name

        g2.color = colorFor(node.status)
        g2.fillRoundRect(x, y, boxW, boxH, 10, 10)
        g2.color = JBColor.border()
        g2.drawRoundRect(x, y, boxW, boxH, 10, 10)
        g2.color = JBColor.foreground()
        val label = node.name.let { if (it.length > 16) it.take(15) + "…" else it }
        g2.drawString(label, x + 8, y + boxH / 2 + 5)

        var childY = y + boxH + vGap
        for (child in node.children) {
            g2.color = JBColor.border()
            g2.drawLine(x + 10, y + boxH, x + 10, childY + boxH / 2)
            g2.drawLine(x + 10, childY + boxH / 2, x + 16, childY + boxH / 2)
            drawNode(g2, child, x + 16, childY)
            childY += (1 + countDescendants(child)) * (boxH + vGap)
        }
    }

    private fun drawArrow(g2: Graphics2D, x1: Int, y1: Int, x2: Int, y2: Int) {
        g2.color = JBColor.GRAY
        g2.drawLine(x1, y1, x2, y2)
        g2.drawLine(x2, y2, x2 - 6, y2 - 4)
        g2.drawLine(x2, y2, x2 - 6, y2 + 4)
    }

    private fun colorFor(status: StageStatus): Color = when (status) {
        StageStatus.SUCCESS -> JBColor(Color(0xDCEFD8), Color(0x2E4D2E))
        StageStatus.FAILED -> JBColor(Color(0xF5D6D6), Color(0x5A2E2E))
        StageStatus.RUNNING -> JBColor(Color(0xD6E4F5), Color(0x2E3F5A))
        StageStatus.SKIPPED -> JBColor(Color(0xE8E8E8), Color(0x3C3C3C))
        StageStatus.PENDING -> JBColor(Color(0xF5F0D6), Color(0x4D472E))
    }
}
