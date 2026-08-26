package com.example.symbolgraph

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

class SymbolGraphDialog(private val project: Project, private val model: GraphModel) : DialogWrapper(project) {
    init {
        title = "Symbol Graph: ${model.definition.title}"
        setSize(980, 640)
        init()
    }

    override fun createCenterPanel(): JComponent = SymbolGraphPanel(project, model)
}
