package com.flix.music.core.data.repository

import kotlinx.coroutines.flow.Flow

/** Remembers, per Library filter, whether it shows a list or a grid (design: "recordada por filtro"). */
interface LibraryLayoutRepository {

    /** Keys of the filters shown as a grid. */
    val gridFilters: Flow<Set<String>>

    suspend fun setGrid(filterKey: String, grid: Boolean)
}
