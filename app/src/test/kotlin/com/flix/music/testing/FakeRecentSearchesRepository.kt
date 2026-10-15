package com.flix.music.testing

import com.flix.music.core.data.repository.RecentSearchesRepository
import com.flix.music.core.data.repository.withRecent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory [RecentSearchesRepository] with the same ordering rules as the real one. */
class FakeRecentSearchesRepository(initial: List<String> = emptyList()) : RecentSearchesRepository {

    override val recentSearches: StateFlow<List<String>>
        get() = recents

    private val recents = MutableStateFlow(initial)

    override suspend fun add(query: String) {
        recents.value = recents.value.withRecent(query)
    }

    override suspend fun remove(query: String) {
        recents.value = recents.value.filterNot { it.equals(query, ignoreCase = true) }
    }

    override suspend fun clear() {
        recents.value = emptyList()
    }
}
