package com.sonora.music.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.music.data.Album
import com.sonora.music.data.Artist
import com.sonora.music.data.Library
import com.sonora.music.playback.PlayerState
import com.sonora.music.ui.AppNav
import com.sonora.music.ui.LocalBottomInset
import com.sonora.music.ui.MainViewModel
import com.sonora.music.ui.components.Artwork
import com.sonora.music.ui.components.SongActions
import com.sonora.music.ui.components.SongRow
import com.sonora.music.ui.components.bounceClick
import com.sonora.music.ui.components.enterAnimation
import com.sonora.music.ui.theme.SonoraColors
import java.text.Normalizer

private enum class Filter(val label: String) { All("Tout"), Songs("Titres"), Albums("Albums"), Artists("Artistes") }

private fun String.normalized(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

@Composable
fun SearchScreen(
    vm: MainViewModel,
    library: Library,
    playerState: PlayerState,
    nav: AppNav,
    songActions: SongActions,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(Filter.All) }
    val focus = LocalFocusManager.current

    val q = query.trim().normalized()
    val songs = remember(q, library) {
        if (q.isEmpty()) emptyList()
        else library.songs.filter { it.title.normalized().contains(q) || it.artist.normalized().contains(q) || it.album.normalized().contains(q) }
    }
    val albums = remember(q, library) {
        if (q.isEmpty()) emptyList() else library.albums.filter { it.title.normalized().contains(q) || it.artist.normalized().contains(q) }
    }
    val artists = remember(q, library) {
        if (q.isEmpty()) emptyList() else library.artists.filter { it.name.normalized().contains(q) }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalBottomInset.current),
    ) {
        item(key = "title") {
            Text(
                "Rechercher",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 20.dp, bottom = 14.dp),
            )
        }
        stickyHeader(key = "field") {
            Box(Modifier.background(SonoraColors.Background).padding(horizontal = 16.dp, vertical = 8.dp)) {
                SearchField(query, { query = it })
            }
        }

        if (q.isEmpty()) {
            item(key = "browseTitle") {
                Text(
                    "Parcourir tout",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 12.dp),
                )
            }
            val tiles = library.artists.sortedByDescending { it.songs.size }.take(16)
            tiles.chunked(2).forEachIndexed { rowIndex, row ->
                item(key = "browse$rowIndex") {
                    Row(
                        Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .enterAnimation(rowIndex),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { artist ->
                            BrowseTile(artist, Modifier.weight(1f)) { nav.artist(artist.id) }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            return@LazyColumn
        }

        item(key = "chips") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Filter.entries) { f ->
                    Chip(f.label, f == filter) { filter = f; focus.clearFocus() }
                }
            }
        }

        if (songs.isEmpty() && albums.isEmpty() && artists.isEmpty()) {
            item(key = "none") { NoResults(query) }
            return@LazyColumn
        }

        val showArtists = filter == Filter.All || filter == Filter.Artists
        val showAlbums = filter == Filter.All || filter == Filter.Albums
        val showSongs = filter == Filter.All || filter == Filter.Songs

        if (showArtists) {
            items(if (filter == Filter.All) artists.take(3) else artists, key = { "ar${it.id}" }) { artist ->
                ArtistResult(artist, Modifier.animateItem()) { focus.clearFocus(); nav.artist(artist.id) }
            }
        }
        if (showAlbums) {
            items(if (filter == Filter.All) albums.take(4) else albums, key = { "al${it.id}" }) { album ->
                AlbumResult(album, Modifier.animateItem()) { focus.clearFocus(); nav.album(album.id) }
            }
        }
        if (showSongs) {
            items(songs, key = { "s${it.id}" }) { song ->
                SongRow(
                    song = song,
                    isCurrent = song.id == playerState.currentId,
                    isPlaying = playerState.isPlaying,
                    onClick = { focus.clearFocus(); vm.play(songs, songs.indexOf(song)) },
                    actions = songActions,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .shadow(6.dp, RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = Color.Black)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text("Que souhaitez-vous écouter ?", color = Color(0xFF555555), style = MaterialTheme.typography.bodyLarge)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(SonoraColors.GreenDark),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AnimatedVisibility(value.isNotEmpty(), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Icon(
                Icons.Rounded.Close, "Effacer", tint = Color.Black,
                modifier = Modifier.bounceClick(0.8f) { onValueChange("") },
            )
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) SonoraColors.Green else SonoraColors.SurfaceHigh, tween(250), label = "chipBg")
    val fg by animateColorAsState(if (selected) Color.Black else Color.White, tween(250), label = "chipFg")
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .bounceClick(0.92f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun BrowseTile(artist: Artist, modifier: Modifier, onClick: () -> Unit) {
    val color = SonoraColors.accentFor(artist.name)
    Box(
        modifier
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .bounceClick(0.95f, onClick = onClick),
    ) {
        Text(
            artist.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(12.dp).fillMaxWidth(0.65f),
        )
        Artwork(
            artist.art, artist.name,
            Modifier
                .align(Alignment.BottomEnd)
                .size(70.dp)
                .graphicsLayer {
                    rotationZ = 25f
                    translationX = 18f
                    translationY = 10f
                }
                .shadow(8.dp, RoundedCornerShape(4.dp)),
            RoundedCornerShape(4.dp),
        )
    }
}

@Composable
private fun ArtistResult(artist: Artist, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(artist.art, artist.name, Modifier.size(54.dp), CircleShape, icon = Icons.Rounded.Person)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(artist.name, style = MaterialTheme.typography.bodyLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Artiste · ${artist.songs.size} titres", style = MaterialTheme.typography.bodyMedium, color = SonoraColors.TextSecondary)
        }
    }
}

@Composable
private fun AlbumResult(album: Album, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(album.art, album.title, Modifier.size(54.dp), RoundedCornerShape(4.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(album.title, style = MaterialTheme.typography.bodyLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Album · ${album.artist}", style = MaterialTheme.typography.bodyMedium, color = SonoraColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun NoResults(query: String) {
    Column(
        Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.SearchOff, null, tint = SonoraColors.TextSecondary, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("Aucun résultat pour « $query »", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text(
            "Vérifiez l'orthographe ou essayez un autre mot-clé.",
            style = MaterialTheme.typography.bodyMedium,
            color = SonoraColors.TextSecondary,
        )
    }
}

