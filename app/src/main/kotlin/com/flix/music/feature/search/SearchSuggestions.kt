package com.flix.music.feature.search


/** Recent searches containing [text], shown while typing (design: "Escribiendo"). */
internal fun typingSuggestions(
    text: String,
    recentSearches: List<String>,
    max: Int = MAX_TYPING_SUGGESTIONS,
): List<String> =
    recentSearches
        .filter { recent -> recent.contains(text, ignoreCase = true) && !recent.equals(text, ignoreCase = true) }
        .take(max)

/**
 * Recent searches spelled similarly to [query], closest first, for the
 * "¿Quizás buscabas?" chips of the no-results state.
 */
internal fun similarRecentSearches(
    query: String,
    recentSearches: List<String>,
    max: Int = MAX_DID_YOU_MEAN,
): List<String> {
    val normalizedQuery = query.lowercase()
    val maxDistance = maxOf(MIN_TYPO_DISTANCE, (normalizedQuery.length * TYPO_RATIO).toInt())
    return recentSearches
        .filterNot { recent -> recent.equals(query, ignoreCase = true) }
        .map { recent -> recent to editDistance(normalizedQuery, recent.lowercase()) }
        .filter { (_, distance) -> distance <= maxDistance }
        .sortedBy { (_, distance) -> distance }
        .take(max)
        .map { (recent, _) -> recent }
}

/** Levenshtein distance: minimum single-character edits to turn [first] into [second]. */
internal fun editDistance(first: String, second: String): Int {
    var previousRow = IntArray(second.length + 1) { index -> index }
    for (firstIndex in first.indices) {
        val currentRow = IntArray(second.length + 1)
        currentRow[0] = firstIndex + 1
        for (secondIndex in second.indices) {
            val substitutionCost = if (first[firstIndex] == second[secondIndex]) 0 else 1
            currentRow[secondIndex + 1] = minOf(
                currentRow[secondIndex] + 1,
                previousRow[secondIndex + 1] + 1,
                previousRow[secondIndex] + substitutionCost,
            )
        }
        previousRow = currentRow
    }
    return previousRow[second.length]
}

private const val MAX_TYPING_SUGGESTIONS = 3
private const val MAX_DID_YOU_MEAN = 2
private const val MIN_TYPO_DISTANCE = 2
private const val TYPO_RATIO = 0.35
