package com.example.symbolgraph

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch

class SymbolGraphAction : AnAction(), DumbAware {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR)
            ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE) ?: return

        // Rider's C# editor does not expose a usable IntelliJ PSI symbol. Use the
        // document/project fallback so a C# file does not become an anonymous node.
        if (file.virtualFile?.extension.equals("cs", ignoreCase = true)) {
            TextSymbolGraphBuilder.build(project, editor, file.virtualFile)?.let { model ->
                SymbolGraphDialog(project, model).show()
                return
            }
        }

        val symbol = resolveSymbol(editor, file)
        if (symbol == null) {
            Messages.showInfoMessage(project, "Place the caret on a function or variable.", "Symbol Graph")
            return
        }

        val model = buildModel(symbol)
        SymbolGraphDialog(project, model).show()
    }

    private fun resolveSymbol(editor: Editor, file: com.intellij.psi.PsiFile): PsiNamedElement? {
        val requestedOffset = if (editor.selectionModel.hasSelection()) {
            editor.selectionModel.selectionStart
        } else {
            editor.caretModel.offset
        }
        val offset = requestedOffset.coerceIn(0, (file.textLength - 1).coerceAtLeast(0))
        var element: PsiElement? = file.findElementAt(offset)
        repeat(4) {
            if (element == null) return@repeat
            val reference: PsiReference? = element!!.reference
            val resolved = reference?.resolve()
            if (resolved is PsiNamedElement && !resolved.name.isNullOrBlank()) return resolved
            if (element is PsiNamedElement && element !is com.intellij.psi.PsiFile && !element.name.isNullOrBlank()) {
                return element as PsiNamedElement
            }
            element = element!!.parent
        }
        return null
    }

    private fun buildModel(symbol: PsiNamedElement): GraphModel {
        val definitionFile = symbol.containingFile?.virtualFile
        val definition = GraphNode(
            id = "definition",
            title = symbol.name ?: "<anonymous>",
            fileName = definitionFile?.name ?: "Unknown file",
            path = definitionFile?.path ?: "",
            offset = symbol.textOffset,
            definition = true
        )
        val usages = ReferencesSearch.search(symbol).findAll()
            .asSequence()
            .mapNotNull { it.element.containingFile?.virtualFile?.let { file -> file to it.element.textOffset } }
            .distinct()
            .take(80)
            .mapIndexed { index, (file, offset) ->
                GraphNode("usage-$index", symbol.name ?: "usage", file.name, file.path, offset, false)
            }
            .toList()
        return GraphModel(definition, usages)
    }
}
