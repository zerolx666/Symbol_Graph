package com.example.symbolgraph

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.JScrollPane

class SymbolGraphPanel(private val project: Project, private val model: GraphModel) : JScrollPane() {
    init {
        viewport.view = GraphCanvas()
        border = null
    }

    private inner class GraphCanvas : JPanel() {
        private val nodeWidth = 210
        private val nodeHeight = 72
        private val gapX = 100
        private val gapY = 22
        private val positions = LinkedHashMap<GraphNode, Point>()

        init {
            background = Color(0xF7F8FA)
            layoutNodes()
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(event: MouseEvent) {
                    if (event.clickCount != 2) return
                    positions.entries.firstOrNull { (_, p) ->
                        event.x in p.x..(p.x + nodeWidth) && event.y in p.y..(p.y + nodeHeight)
                    }?.key?.let(::openNode)
                }
            })
        }

        private fun layoutNodes() {
            val rows = maxOf(1, model.usages.size)
            val height = rows * (nodeHeight + gapY) + 80
            preferredSize = Dimension(nodeWidth * 2 + gapX + 80, height)
            positions[model.definition] = Point(30, (height - nodeHeight) / 2)
            model.usages.forEachIndexed { index, node ->
                positions[node] = Point(nodeWidth + gapX + 50, 30 + index * (nodeHeight + gapY))
            }
        }

        override fun paintComponent(graphics: Graphics) {
            super.paintComponent(graphics)
            val g = graphics.create() as Graphics2D
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val source = positions[model.definition] ?: return
            model.usages.forEach { usage ->
                val target = positions[usage] ?: return@forEach
                drawArrow(g, source.x + nodeWidth, source.y + nodeHeight / 2, target.x, target.y + nodeHeight / 2)
            }
            positions.forEach { (node, point) -> drawNode(g, node, point) }
            g.dispose()
        }

        private fun drawNode(g: Graphics2D, node: GraphNode, point: Point) {
            g.color = if (node.definition) Color(0xDCEBFF) else Color.WHITE
            g.fillRoundRect(point.x, point.y, nodeWidth, nodeHeight, 8, 8)
            g.color = if (node.definition) Color(0x2F6FEB) else Color(0xA8B1BE)
            g.stroke = BasicStroke(if (node.definition) 2f else 1f)
            g.drawRoundRect(point.x, point.y, nodeWidth, nodeHeight, 8, 8)
            g.color = Color(0x1F2328)
            g.font = g.font.deriveFont(Font.BOLD, 13f)
            g.drawString(node.title.take(27), point.x + 12, point.y + 23)
            g.font = g.font.deriveFont(Font.PLAIN, 12f)
            g.color = Color(0x57606A)
            g.drawString(node.fileName.take(29), point.x + 12, point.y + 44)
            g.color = Color(0x8C959F)
            g.font = g.font.deriveFont(Font.PLAIN, 10f)
            g.drawString(if (node.definition) "definition" else "usage", point.x + 12, point.y + 61)
        }

        private fun drawArrow(g: Graphics2D, x1: Int, y1: Int, x2: Int, y2: Int) {
            g.color = Color(0x8C959F)
            g.stroke = BasicStroke(1.4f)
            g.drawLine(x1, y1, x2, y2)
            val angle = kotlin.math.atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
            val size = 8.0
            val left = Point((x2 - size * kotlin.math.cos(angle - 0.45)).toInt(), (y2 - size * kotlin.math.sin(angle - 0.45)).toInt())
            val right = Point((x2 - size * kotlin.math.cos(angle + 0.45)).toInt(), (y2 - size * kotlin.math.sin(angle + 0.45)).toInt())
            g.drawLine(x2, y2, left.x, left.y)
            g.drawLine(x2, y2, right.x, right.y)
        }

        private fun openNode(node: GraphNode) {
            val file = LocalFileSystem.getInstance().findFileByPath(node.path) ?: return
            OpenFileDescriptor(project, file, node.offset).navigate(true)
        }
    }
}
