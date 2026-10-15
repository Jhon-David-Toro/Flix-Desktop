package com.flix.music.core.data.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.flix.music.core.data.repository.FavoritesRepositoryImpl
import com.flix.music.core.data.repository.HistoryRepositoryImpl
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The library database on a real SQLite file, as the desktop app opens it. */
class DesktopDatabaseTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val fadeToBlack = Track("9HZ_tx8aWuA", "Fade To Black", listOf(ArtistRef("UC1", "Metallica")), null, null, null)

    private fun open(): FlixDatabase = Room.databaseBuilder<FlixDatabase>(name = folder.root.resolve("flix.db").absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

    @Test
    fun `favoritos e historial se guardan y siguen ahí al reabrir`() = runBlocking {
        open().run {
            FavoritesRepositoryImpl(favoritesDao()).toggle(fadeToBlack)
            HistoryRepositoryImpl(historyDao()).record(fadeToBlack)
            close()
        }

        val reopened = open()
        assertEquals(listOf(fadeToBlack), FavoritesRepositoryImpl(reopened.favoritesDao()).favorites.first().map { favorite -> favorite.track })
        assertEquals(listOf(fadeToBlack), HistoryRepositoryImpl(reopened.historyDao()).history.first().map { entry -> entry.track })
        reopened.close()
    }
}
