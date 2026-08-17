package com.netly.app.data.updater.util

object MarkdownUtils {

    /**
     * Converts GitHub release markdown into readable Android UI text:
     * - Converts headers (## Header) into clean section titles
     * - Converts bullet symbols (* item, - item) into clean dots (• item)
     * - Strips bold/italic markdown (**text**, *text*, __text__, _text_)
     * - Strips markdown links ([Text](url) -> Text)
     * - Strips HTML tags (<br>, <p>, etc.)
     * - Cleans up multiple empty lines
     */
    fun sanitizeMarkdown(markdown: String?): String? {
        if (markdown.isNullOrBlank()) return null

        val lines = markdown.trim().lines()
        val cleanedLines = mutableListOf<String>()

        for (rawLine in lines) {
            var line = rawLine.trim()

            // Skip empty lines or horizontal divider lines
            if (line.matches(Regex("^[-*_]{3,}$"))) {
                continue
            }

            // 1. Remove HTML tags
            line = line.replace(Regex("<[^>]*>"), "")

            // 2. Normalize markdown links [Label](url) -> Label
            line = line.replace(Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)"), "$1")

            // 3. Remove images ![Alt](url)
            line = line.replace(Regex("!\\[[^\\]]*\\]\\([^\\)]+\\)"), "")

            // 4. Headers (#, ##, ###, ####)
            if (line.startsWith("#")) {
                line = line.replace(Regex("^#+\\s*"), "")
            }
            // 5. Bullet points (- or * or +)
            else if (line.matches(Regex("^[-*+]\\s+.*"))) {
                line = line.replace(Regex("^[-*+]\\s+"), "• ")
            }

            // 6. Remove bold markers (**text** or __text__)
            line = line.replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            line = line.replace(Regex("__([^_]+)__"), "$1")

            // 7. Remove italic markers (*text* or _text_)
            line = line.replace(Regex("(?<=\\s|^)\\*([^*]+)\\*(?=\\s|$)"), "$1")
            line = line.replace(Regex("(?<=\\s|^)_([^_]+)_(?=\\s|$)"), "$1")

            // 8. Remove inline code (`code`)
            line = line.replace(Regex("`([^`]+)`"), "$1")

            cleanedLines.add(line.trim())
        }

        // Join and collapse 3+ consecutive newlines to 2
        val result = cleanedLines.joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim()
        return result.ifBlank { null }
    }
}
