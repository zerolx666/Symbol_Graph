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
import java.awt.geom.Path2D
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.JScrollPane

class SymbolGraphPanel(private val project: Project, private val model: GraphModel) : JScrollPane() {
    init {
        viewport.view = GraphCanvas()
        viewport.background = Color(0x12151B)
        background = Color(0x12151B)
        border = null
    }

    private inner class GraphCanvas : JPanel() {
        private val nodeWidth = 210
        private val nodeHeight = 72
        private val gapX = 130
        private val gapY = 22
        private val canvasPadding = 42
        private val positions = LinkedHashMap<GraphNode, Point>()

        init {
            background = Color(0x12151B)
            isOpaque = true
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
            val height = rows * (nodeHeight + gapY) + canvasPadding * 2
            preferredSize = Dimension(nodeWidth * 2 + gapX + canvasPadding * 2, height)
            positions[model.definition] = Point(canvasPadding, (height - nodeHeight) / 2)
            model.usages.forEachIndexed { index, node ->
                positions[node] = Point(nodeWidth + gapX + canvasPadding, canvasPadding + index * (nodeHeight + gapY))
            }
        }

        override fun paintComponent(graphics: Graphics) {
            super.paintComponent(graphics)
            val g = graphics.create() as Graphics2D
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED)
            drawGrid(g)
            val source = positions[model.definition] ?: return
            drawConnections(g, source)
            positions.forEach { (node, point) -> drawNode(g, node, point) }
            g.dispose()
        }

        private fun drawGrid(g: Graphics2D) {
            g.color = Color(0x1B2028)
            g.stroke = BasicStroke(1f)
            var x = 18
            while (x < width) {
                g.drawLine(x, 0, x, height)
                x += 24
            }
            var y = 18
            while (y < height) {
                g.drawLine(0, y, width, y)
                y += 24
            }
        }

        private fun drawConnections(g: Graphics2D, source: Point) {
            if (model.usages.isEmpty()) return
            val sourceX = source.x + nodeWidth
            val sourceY = source.y + nodeHeight / 2
            val railX = sourceX + gapX / 2
            val targets = model.usages.mapNotNull { positions[it] }
            if (targets.isEmpty()) return

            // Draw one shared trunk, then orthogonal branches to each usage node.
            g.color = Color(0x657284)
            g.stroke = BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            val trunk = Path2D.Double()
            trunk.moveTo(sourceX.toDouble(), sourceY.toDouble())
            trunk.lineTo(railX.toDouble(), sourceY.toDouble())
            trunk.lineTo(railX.toDouble(), targets.minOf { it.y + nodeHeight / 2 }.toDouble())
            trunk.lineTo(railX.toDouble(), targets.maxOf { it.y + nodeHeight / 2 }.toDouble())
            g.draw(trunk)

            targets.forEach { target ->
                val targetY = target.y + nodeHeight / 2
                val branch = Path2D.Double()
                branch.moveTo(railX.toDouble(), targetY.toDouble())
                branch.lineTo((target.x - 14).toDouble(), targetY.toDouble())
                g.draw(branch)
                drawArrowHead(g, target.x - 3, targetY)
            }
        }

        private fun drawNode(g: Graphics2D, node: GraphNode, point: Point) {
            // A restrained dark palette keeps the graph readable while the red arrows stay prominent.
            g.color = if (node.definition) Color(0x24344A) else Color(0x20252D)
            g.fillRoundRect(point.x, point.y, nodeWidth, nodeHeight, 8, 8)
            g.color = if (node.definition) Color(0x4D9DFF) else Color(0x586575)
            g.stroke = BasicStroke(if (node.definition) 2.2f else 1.2f)
            g.drawRoundRect(point.x, point.y, nodeWidth, nodeHeight, 8, 8)
            g.color = Color(0xF2F5F8)
            g.font = g.font.deriveFont(Font.BOLD, 13f)
            g.drawString(node.title.take(27), point.x + 12, point.y + 23)
            g.font = g.font.deriveFont(Font.PLAIN, 12f)
            g.color = Color(0xB4BFCC)
            g.drawString(node.fileName.take(29), point.x + 12, point.y + 44)
            g.color = Color(0x7F8B9A)
            g.font = g.font.deriveFont(Font.PLAIN, 10f)
            g.drawString(if (node.definition) "definition" else "usage", point.x + 12, point.y + 61)
        }

        private fun drawArrowHead(g: Graphics2D, tipX: Int, tipY: Int) {
            val size = 8
            val arrow = java.awt.Polygon(
                intArrayOf(tipX, tipX - size, tipX - size),
                intArrayOf(tipY, tipY - size / 2, tipY + size / 2),
                3
            )
            g.color = Color(0xFF4D5F)
            g.fillPolygon(arrow)
        }

        private fun openNode(node: GraphNode) {
            val file = LocalFileSystem.getInstance().findFileByPath(node.path) ?: return
            OpenFileDescriptor(project, file, node.offset).navigate(true)
        }
    }
}
