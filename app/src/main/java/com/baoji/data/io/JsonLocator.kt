package com.baoji.data.io

/**
 * Maps JSON paths such as `plans[0].exercises[1].kcalPerSet` to 1-based line numbers.
 * Objects and arrays are keyed by their own path and point at their opening bracket.
 * Assumes the text is already syntactically valid (checked by kotlinx first).
 */
class JsonLocator private constructor(private val lines: Map<String, Int>) {

    fun lineOf(path: String): Int? = lines[path]

    /** Line of [path], or of its nearest existing ancestor. */
    fun lineNear(path: String): Int {
        var p = path
        while (true) {
            lines[p]?.let { return it }
            if (p.isEmpty()) return 1
            p = parent(p)
        }
    }

    companion object {
        fun index(text: String): JsonLocator {
            val out = HashMap<String, Int>()
            Scanner(text, out).run()
            return JsonLocator(out)
        }

        fun lineAtOffset(text: String, offset: Int): Int {
            val end = offset.coerceIn(0, text.length)
            var line = 1
            for (i in 0 until end) if (text[i] == '\n') line++
            return line
        }

        internal fun parent(path: String): String {
            val dot = path.lastIndexOf('.')
            val bracket = path.lastIndexOf('[')
            val cut = maxOf(dot, bracket)
            return if (cut <= 0) "" else path.substring(0, cut)
        }

        fun child(parent: String, key: String) = if (parent.isEmpty()) key else "$parent.$key"
        fun item(parent: String, index: Int) = "$parent[$index]"
    }

    private class Scanner(private val s: String, private val out: MutableMap<String, Int>) {
        private var i = 0
        private var line = 1

        fun run() {
            skipWs()
            value("")
        }

        private fun value(path: String) {
            skipWs()
            out.putIfAbsent(path, line)
            when (s.getOrNull(i)) {
                '{' -> obj(path)
                '[' -> arr(path)
                '"' -> string()
                else -> literal()
            }
        }

        private fun obj(path: String) {
            i++ // {
            skipWs()
            if (s.getOrNull(i) == '}') { i++; return }
            while (i < s.length) {
                skipWs()
                val keyLine = line
                val key = string()
                val childPath = child(path, key)
                out[childPath] = keyLine
                skipWs()
                i++ // :
                value(childPath)
                skipWs()
                if (s.getOrNull(i) == ',') { i++; continue }
                i++ // }
                return
            }
        }

        private fun arr(path: String) {
            i++ // [
            skipWs()
            if (s.getOrNull(i) == ']') { i++; return }
            var idx = 0
            while (i < s.length) {
                value(item(path, idx++))
                skipWs()
                if (s.getOrNull(i) == ',') { i++; continue }
                i++ // ]
                return
            }
        }

        private fun string(): String {
            val sb = StringBuilder()
            i++ // opening quote
            while (i < s.length) {
                val c = s[i++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> { sb.append(s.getOrNull(i) ?: ' '); i++ }
                    '\n' -> { line++; sb.append(c) }
                    else -> sb.append(c)
                }
            }
            return sb.toString()
        }

        private fun literal() {
            while (i < s.length && s[i] !in ",}] \t\r\n") i++
        }

        private fun skipWs() {
            while (i < s.length && s[i].isWhitespace()) {
                if (s[i] == '\n') line++
                i++
            }
        }
    }
}
