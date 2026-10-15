package com.flix.music.feature.player

import androidx.compose.runtime.Composable
import com.flix.music.core.player.PlaybackSource
import com.flix.music.core.player.PlaybackStatus
import com.flix.music.core.player.RepeatMode
import com.flix.music.resources.Res
import com.flix.music.resources.ic_pause_filled
import com.flix.music.resources.ic_play_arrow_filled
import com.flix.music.resources.ic_replay
import com.flix.music.resources.player_pause
import com.flix.music.resources.player_play
import com.flix.music.resources.player_repeat_all
import com.flix.music.resources.player_repeat_off
import com.flix.music.resources.player_repeat_one
import com.flix.music.resources.player_replay
import com.flix.music.resources.player_source_album
import com.flix.music.resources.player_source_artist
import com.flix.music.resources.player_source_downloads
import com.flix.music.resources.player_source_favorites
import com.flix.music.resources.player_source_genre
import com.flix.music.resources.player_source_history
import com.flix.music.resources.player_source_search
import com.flix.music.resources.player_source_song
import com.flix.music.resources.queue_from_downloads
import com.flix.music.resources.queue_from_favorites
import com.flix.music.resources.queue_from_history
import com.flix.music.resources.queue_from_named
import com.flix.music.resources.queue_from_search
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal data class PlayPauseAction(val icon: DrawableResource, val label: StringResource)

/**
 * Icon and label of the play/pause control for each state (design "Estados de
 * reproducción"): while loading, the mini player shows play with a ring; the Full
 * Player already shows pause, because the intent is to play.
 */
internal fun playPauseAction(status: PlaybackStatus, isMiniPlayer: Boolean): PlayPauseAction = when (status) {
    PlaybackStatus.Playing, PlaybackStatus.Buffering ->
        PlayPauseAction(Res.drawable.ic_pause_filled, Res.string.player_pause)
    PlaybackStatus.Loading ->
        if (isMiniPlayer) PlayPauseAction(Res.drawable.ic_play_arrow_filled, Res.string.player_play)
        else PlayPauseAction(Res.drawable.ic_pause_filled, Res.string.player_pause)
    PlaybackStatus.Ended -> PlayPauseAction(Res.drawable.ic_replay, Res.string.player_replay)
    else -> PlayPauseAction(Res.drawable.ic_play_arrow_filled, Res.string.player_play)
}

@Composable
internal fun playbackSourceLabel(source: PlaybackSource): String = when (source) {
    is PlaybackSource.Search -> stringResource(Res.string.player_source_search, source.query)
    is PlaybackSource.Genre -> stringResource(Res.string.player_source_genre, source.name)
    PlaybackSource.Favorites -> stringResource(Res.string.player_source_favorites)
    PlaybackSource.History -> stringResource(Res.string.player_source_history)
    PlaybackSource.Downloads -> stringResource(Res.string.player_source_downloads)
    is PlaybackSource.Playlist -> source.name
    is PlaybackSource.Artist -> stringResource(Res.string.player_source_artist, source.name)
    is PlaybackSource.Album -> stringResource(Res.string.player_source_album, source.name)
    is PlaybackSource.Song -> stringResource(Res.string.player_source_song, source.title)
}

/** Second half of the Cola header: "A continuación · de tu búsqueda". */
@Composable
internal fun queueSourceSuffix(source: PlaybackSource): String = when (source) {
    is PlaybackSource.Search -> stringResource(Res.string.queue_from_search)
    is PlaybackSource.Genre -> stringResource(Res.string.queue_from_named, source.name)
    PlaybackSource.Favorites -> stringResource(Res.string.queue_from_favorites)
    PlaybackSource.History -> stringResource(Res.string.queue_from_history)
    PlaybackSource.Downloads -> stringResource(Res.string.queue_from_downloads)
    is PlaybackSource.Playlist -> stringResource(Res.string.queue_from_named, source.name)
    is PlaybackSource.Artist -> stringResource(Res.string.queue_from_named, source.name)
    is PlaybackSource.Album -> stringResource(Res.string.queue_from_named, source.name)
    is PlaybackSource.Song -> stringResource(Res.string.queue_from_named, source.title)
}

internal fun RepeatMode.label(): StringResource = when (this) {
    RepeatMode.Off -> Res.string.player_repeat_off
    RepeatMode.All -> Res.string.player_repeat_all
    RepeatMode.One -> Res.string.player_repeat_one
}
