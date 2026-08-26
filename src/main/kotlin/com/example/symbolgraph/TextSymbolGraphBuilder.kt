package com.example.symbolgraph

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

/** Builds a conservative C# symbol graph when Rider's C# PSI is unavailable. */
object TextSymbolGraphBuilder {
    private val declarationModifiers =
        "(?:public|private|protected|internal|static|readonly|const|volatile|async|virtual|override|abstract|sealed|partial|extern|new|ref|out|in|unsafe|file)"

    fun build(project: Project, editor: Editor, currentFile: VirtualFile?): GraphModel? {
        currentFile ?: return null
        val currentText = editor.document.text
        val name = identifierAt(currentText, editor)
            ?: return null
        val files = linkedMapOf<String, Pair<VirtualFile, String>>()
        files[currentFile.path] = currentFile to currentText
        FilenameIndex.getAllFilesByExt(project, "cs", GlobalSearchScope.projectScope(project))
            .forEach { file ->
                if (file.path == currentFile.path || files.containsKey(file.path)) return@forEach
                runCatching { files[file.path] = file to VfsUtilCore.loadText(file) }
            }

        var definitionFile: VirtualFile = currentFile
        var definitionOffset = editor.caretModel.offset.coerceIn(0, currentText.length)
        val usages = ArrayList<GraphNode>()
        var foundDeclaration = false
        files.values.forEach { (file, text) ->
            val masked = maskCode(text)
            if (!foundDeclaration) {
                findDeclarationOffset(masked, name)?.let { offset ->
                    definitionFile = file
                    definitionOffset = offset
                    foundDeclaration = true
                }
            }
            identifierOccurrences(masked, name).forEach { offset ->
                if (file.path == definitionFile.path && offset == definitionOffset) return@forEach
                if (usages.size < 80) {
                    usages += GraphNode(
                        id = "usage-${usages.size}",
                        title = name,
                        fileName = file.name,
                        path = file.path,
                        offset = offset,
                        definition = false
                    )
                }
            }
        }

        val definition = GraphNode(
            id = "definition",
            title = name,
            fileName = definitionFile.name,
            path = definitionFile.path,
            offset = definitionOffset,
            definition = true
        )
        return GraphModel(definition, usages)
    }

    private fun identifierAt(text: String, editor: Editor): String? {
        if (text.isEmpty()) return null
        val selected = editor.selectionModel.selectedText?.trim()
        if (!selected.isNullOrBlank() && selected.all(::isIdentifierPart)) return selected
        var offset = editor.caretModel.offset.coerceIn(0, text.length - 1)
        if (!isIdentifierPart(text[offset]) && offset > 0 && isIdentifierPart(text[offset - 1])) offset--
        if (!isIdentifierPart(text[offset])) return null
        var start = offset
        var end = offset + 1
        while (start > 0 && isIdentifierPart(text[start - 1])) start--
        while (end < text.length && isIdentifierPart(text[end])) end++
        return text.substring(start, end)
    }

    private fun isIdentifierPart(char: Char): Boolean = char == '_' || char.isLetterOrDigit()

    private fun findDeclarationOffset(masked: String, name: String): Int? {
        val escaped = Regex.escape(name)
        val qualifiedType = "[A-Za-z_][A-Za-z0-9_.<>?,\\[\\]]*"
        val patterns = listOf(
            Regex("(?m)^\\s*(?:$declarationModifiers\\s+)*(?:$qualifiedType\\s+)+$escaped\\s*(?=\\(|[=;:,\\[])"),
            Regex("(?m)^\\s*(?:$declarationModifiers\\s+)*$escaped\\s*(?=\\()"),
            Regex("(?m)\\b$qualifiedType\\s+$escaped\\s*(?=[=;,)])")
        )
        return patterns.asSequence()
            .mapNotNull { pattern -> pattern.find(masked)?.let { it.range.first + it.value.indexOf(name) } }
            .firstOrNull()
    }

    private fun identifierOccurrences(masked: String, name: String): Sequence<Int> {
        val pattern = Regex("(?<![A-Za-z0-9_])${Regex.escape(name)}(?![A-Za-z0-9_])")
        return pattern.findAll(masked).map { it.range.first }
    }

    /** Replace comments and literals with spaces while preserving offsets/newlines. */
    private fun maskCode(text: String): String {
        val chars = text.toCharArray()
        var state = 0 // 0 normal, 1 line comment, 2 block comment, 3 string, 4 char
        var i = 0
        while (i < chars.size) {
            when (state) {
                0 -> when {
                    chars[i] == '/' && i + 1 < chars.size && chars[i + 1] == '/' -> {
                        chars[i] = ' '; chars[i + 1] = ' '; i += 2; state = 1
                    }
                    chars[i] == '/' && i + 1 < chars.size && chars[i + 1] == '*' -> {
                        chars[i] = ' '; chars[i + 1] = ' '; i += 2; state = 2
                    }
                    chars[i] == '"' -> { chars[i] = ' '; i++; state = 3 }
                    chars[i] == '\'' -> { chars[i] = ' '; i++; state = 4 }
                    else -> i++
                }
                1 -> if (chars[i] == '\n' || chars[i] == '\r') state = 0 else { chars[i] = ' '; i++ }
                2 -> if (chars[i] == '*' && i + 1 < chars.size && chars[i + 1] == '/') {
                    chars[i] = ' '; chars[i + 1] = ' '; i += 2; state = 0
                } else { if (chars[i] != '\n' && chars[i] != '\r') chars[i] = ' '; i++ }
                3, 4 -> when {
                    chars[i] == '\\' -> { chars[i] = ' '; if (i + 1 < chars.size) chars[i + 1] = ' '; i += 2 }
                    chars[i] == if (state == 3) '"' else '\'' -> { chars[i] = ' '; i++; state = 0 }
                    else -> { if (chars[i] != '\n' && chars[i] != '\r') chars[i] = ' '; i++ }
                }
            }
        }
        return String(chars)
    }
}
