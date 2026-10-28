package com.baoji.data.catalog

/**
 * Substring search over Chinese names. Every space-separated word of the query must appear.
 * Ranking: the main name (before any （…） or ［…］) equal to, starting with, or containing the
 * query, then a match only in the qualifiers/aliases. Within a rank "代表值" rows and shorter names win.
 */
object CatalogSearch {

    private const val REPRESENTATIVE = "代表值"
    private val WHITESPACE = Regex("""\s+""")

    fun <T> search(items: List<T>, query: String, limit: Int = 40, name: (T) -> String): List<T> {
        val words = query.trim().split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        val first = words.first()
        return items.asSequence()
            .filter { item -> name(item).let { n -> words.all { n.contains(it, ignoreCase = true) } } }
            .sortedWith(
                compareBy<T>(
                    { rank(mainName(name(it)), first) },
                    { if (name(it).contains(REPRESENTATIVE)) 0 else 1 },
                    { name(it).length },
                ),
            )
            .take(limit)
            .toList()
    }

    internal fun mainName(name: String): String =
        name.substringBefore('（').substringBefore('［').substringBefore('(').trim()

    private fun rank(main: String, word: String): Int = when {
        main.equals(word, ignoreCase = true) -> 0
        main.startsWith(word, ignoreCase = true) -> 1
        main.contains(word, ignoreCase = true) -> 2
        else -> 3
    }
}
