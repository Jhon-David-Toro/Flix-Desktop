package com.flix.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.component.NowPlayingMarker
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.designsystem.theme.ThemeMode
import com.flix.music.core.model.AlbumRef
import com.flix.music.core.model.AppTheme
import com.flix.music.core.model.ArtistRef
import com.flix.music.core.model.Lyrics
import com.flix.music.core.model.Track
import com.flix.music.core.model.UserSettings
import com.flix.music.core.player.PlaybackSource
import com.flix.music.core.player.PlaybackStatus
import com.flix.music.core.player.PlayerState
import com.flix.music.core.player.RepeatMode
import com.flix.music.feature.player.LyricsPanelContent
import com.flix.music.feature.player.LyricsUiState
import com.flix.music.feature.player.NowPlayingContent
import com.flix.music.feature.player.PlayerBar
import com.flix.music.feature.player.PlayerBarActions
import com.flix.music.feature.player.QueueActions
import com.flix.music.feature.player.QueuePanel
import com.flix.music.core.player.QueueItem
import com.flix.music.feature.player.VolumeBar
import com.flix.music.feature.search.SearchActions
import com.flix.music.feature.search.SearchContent
import com.flix.music.feature.search.SearchFilter
import com.flix.music.feature.search.SearchUiState
import com.flix.music.feature.settings.SettingsActions
import com.flix.music.feature.settings.SettingsContent
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/** Snapshots of the desktop screens with sample data, written to `build/snapshots`. */
class ScreensSnapshotTest {

    /** Sample covers drawn here, so the screenshots show artwork without the network. */
    private fun cover(index: Int): String {
        val hues = floatArrayOf(0.62f, 0.02f, 0.08f, 0.45f)
        val file = java.io.File("build/snapshots/covers/cover-$index.png").apply { parentFile.mkdirs() }
        val image = java.awt.image.BufferedImage(600, 600, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.paint = java.awt.GradientPaint(0f, 0f, java.awt.Color.getHSBColor(hues[index], 0.8f, 0.9f), 600f, 600f, java.awt.Color.getHSBColor(hues[index] + 0.1f, 0.9f, 0.25f))
        g.fillRect(0, 0, 600, 600)
        g.paint = java.awt.Color(255, 255, 255, 60)
        g.fillOval(150, 150, 300, 300)
        g.fillOval(250, 250, 100, 100)
        g.dispose()
        javax.imageio.ImageIO.write(image, "png", file)
        return file.toURI().toString().replaceFirst("file:/", "file:///")
    }

    private val metallica = ArtistRef("UC1", "Metallica")
    private val tracks = listOf(
        Track("1", "Fade To Black", listOf(metallica), AlbumRef("a1", "Ride The Lightning"), 417.seconds, cover(0)),
        Track("2", "Master of Puppets", listOf(metallica), AlbumRef("a2", "Master of Puppets"), 516.seconds, cover(1)),
        Track("3", "One", listOf(metallica), AlbumRef("a3", "...And Justice for All"), 446.seconds, cover(2)),
        Track("4", "Nothing Else Matters", listOf(metallica), AlbumRef("a4", "Metallica"), 388.seconds, cover(3)),
    )
    private val playerState = PlayerState(
        status = PlaybackStatus.Playing,
        current = tracks.first(),
        source = PlaybackSource.Search("metallica"),
        position = 161.seconds,
        bufferedPosition = 300.seconds,
        duration = 417.seconds,
        shuffleEnabled = true,
        repeatMode = RepeatMode.All,
        hasNext = true,
        hasPrevious = true,
    )

    @Test
    fun `búsqueda con resultados y barra del reproductor`() = snapshot("search-results") {
        Column(Modifier.fillMaxSize().background(FlixTheme.colors.background)) {
            Box(Modifier.weight(1f)) {
                SearchContent(
                    query = "metallica",
                    uiState = SearchUiState.Results(tracks, emptyList(), emptyList()),
                    filter = SearchFilter.Songs,
                    recentSearches = emptyList(),
                    actions = SearchActions(),
                    nowPlaying = NowPlayingMarker(trackId = "1", isPlaying = true),
                )
            }
            PlayerBar(state = playerState, volume = 70, actions = PlayerBarActions())
        }
    }

    @Test
    fun `búsqueda en reposo`() = snapshot("search-idle") {
        Box(Modifier.fillMaxSize().background(FlixTheme.colors.background), contentAlignment = Alignment.TopStart) {
            SearchContent(
                query = "",
                uiState = SearchUiState.Idle,
                filter = SearchFilter.All,
                recentSearches = listOf("fade to black", "bad bunny"),
                actions = SearchActions(),
            )
        }
    }

    @Test
    fun `ajustes en oscuro y en claro`() {
        listOf(ThemeMode.Dark to "settings-dark", ThemeMode.Light to "settings-light").forEach { (mode, name) ->
            snapshot(name, height = 700, themeMode = mode) {
                Box(Modifier.fillMaxSize().background(FlixTheme.colors.background)) {
                    SettingsContent(
                        settings = UserSettings(theme = AppTheme.System, autoplay = true),
                        startsWithWindows = true,
                        canStartWithWindows = true,
                        version = "1.0.0",
                        actions = SettingsActions(),
                    )
                }
            }
        }
    }

    @Test
    fun `reproductor a pantalla completa con la letra sincronizada`() = snapshot("now-playing") {
        Column(Modifier.fillMaxSize().background(FlixTheme.colors.background)) {
            Box(Modifier.weight(1f)) {
                NowPlayingContent(
                    state = playerState.copy(position = 98.seconds),
                    isFavorite = true,
                    onToggleFavorite = {},
                    onClose = {},
                    lyrics = {
                        LyricsPanelContent(
                            lyricsState = LyricsUiState.Ready(
                                Lyrics.fromLrc(
                                    listOf(
                                        "[01:20]Apagaron la última luz",
                                        "[01:30]y el eco se quedó",
                                        "[01:36]la pantalla sigue encendida",
                                        "[01:42]aunque ya no quede nadie",
                                        "[01:48]sentado en la fila siete",
                                        "[01:54]esperando los créditos",
                                    ).joinToString("\n"),
                                ),
                            ),
                            position = 98.seconds,
                            onLineClick = {},
                            onRetry = {},
                        )
                    },
                    queue = {},
                )
            }
            PlayerBar(state = playerState.copy(position = 98.seconds), volume = 70, actions = PlayerBarActions())
        }
    }

    @Test
    fun `volumen al mínimo, a la mitad y al máximo`() = snapshot("volume", width = 200, height = 90) {
        Column(Modifier.fillMaxSize().background(FlixTheme.colors.surfaceElevated)) {
            listOf(0, 50, 100).forEach { level ->
                VolumeBar(volume = level, onVolumeChange = {}, contentDescription = "Volumen", modifier = Modifier.width(160.dp))
            }
        }
    }

    @Test
    fun `cola sin cuadrado negro ni rojo en reposo`() = snapshot("queue", height = 700) {
        Box(Modifier.fillMaxSize().background(FlixTheme.colors.background)) {
            androidx.compose.runtime.CompositionLocalProvider(
                com.flix.music.core.designsystem.component.LocalSnackbarHostState provides androidx.compose.material3.SnackbarHostState(),
            ) {
            QueuePanel(
                state = playerState.copy(upNext = tracks.drop(1).mapIndexed { index, track -> QueueItem(index + 1, track) }),
                actions = QueueActions(),
            )
            }
        }
    }
}
