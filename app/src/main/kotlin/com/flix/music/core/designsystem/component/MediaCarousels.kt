package com.flix.music.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flix.music.core.designsystem.theme.FlixTheme
import com.flix.music.core.model.Album
import com.flix.music.core.model.Artist
import com.flix.music.resources.Res
import com.flix.music.resources.type_artist
import org.jetbrains.compose.resources.stringResource

/** Horizontal carousel of circular artist cards (design `acard`: 84 px → 108 dp). */
@Composable
fun ArtistCarousel(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = FlixTheme.spacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.itemGap),
    ) {
        items(artists, key = { it.id }) { artist ->
            MediaCard(
                title = artist.name,
                subtitle = stringResource(Res.string.type_artist),
                artworkUrl = artist.artworkUrl,
                onClick = { onArtistClick(artist) },
                circular = true,
                modifier = Modifier.width(ARTIST_CARD_WIDTH),
            )
        }
    }
}

/** Horizontal carousel of square album cards (design pattern "Carrusel: tarjetas 136 dp"). */
@Composable
fun AlbumCarousel(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = FlixTheme.spacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(FlixTheme.spacing.itemGap),
    ) {
        items(albums, key = { it.id }) { album ->
            MediaCard(
                title = album.name,
                subtitle = album.artistName,
                artworkUrl = album.artworkUrl,
                onClick = { onAlbumClick(album) },
                modifier = Modifier.width(ALBUM_CARD_WIDTH),
            )
        }
    }
}

private val ARTIST_CARD_WIDTH = 108.dp
private val ALBUM_CARD_WIDTH = 136.dp
