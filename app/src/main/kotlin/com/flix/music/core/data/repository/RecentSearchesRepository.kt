package com.flix.music.core.data.repository

import kotlinx.coroutines.flow.Flow

/** Recent search queries, stored only on the device. */
interface RecentSearchesRepository {

    /** Most recent first. Emits again on every change. */
    val recentSearches: Flow<List<String>>

    suspend fun add(query: String)

    suspend fun remove(query: String)

    suspend fun clear()
}

const val MAX_RECENT_SEARCHES = 5

/**
 * Returns this list with [query] moved to the front, without case-insensitive
 * duplicates and capped at [max] entries.
 */
fun List<String>.withRecent(query: String, max: Int = MAX_RECENT_SEARCHES): List<String> =
    (listOf(query) + filterNot { it.equals(query, ignoreCase = true) }).take(max)
