package com.sonora.music.ui.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.music.data.Library
import com.sonora.music.data.Playlist
import com.sonora.music.ui.AppNav
import com.sonora.music.ui.LocalBottomInset
import com.sonora.music.ui.components.Artwork
import com.sonora.music.ui.components.GradientArt
import com.sonora.music.ui.components.PlaylistCover
import com.sonora.music.ui.components.bounceClick
import com.sonora.music.ui.home.LikedGradient
import com.sonora.music.ui.theme.SonoraColors

private enum class LibFilter(val label: String) { Playlists("Playlists"), Albums("Albums"), Artists("Artistes") }

private enum class SortMode(val label: String) { Recent("Récents"), Alphabetical("Alphabétique") }

/** A row in the library: one shape for every kind of item. */
private data class LibItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val kind: LibFilter?,
    val sortKey: String,
    val recency: Long,
    val onClick: () -> Unit,
    val cover: @Composable (Modifier, Shape) -> Unit,
    val circle: Boolean = false,
    val pinned: Boolean = false,
)

@Composable
fun LibraryScreen(
    library: Library,
    liked: List<Long>,
    playlists: List<Playlist>,
    nav: AppNav,
    onCreatePlaylist: () -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf<LibFilter?>(null) }
    var grid by rememberSaveable { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf(SortMode.Recent) }

    val items = buildList {
        if (filter == null || filter == LibFilter.Playlists) {
            add(
                LibItem(
                    "liked", "Titres likés", "Playlist · ${liked.size} titres", LibFilter.Playlists, "", Long.MAX_VALUE,
                    onClick = nav::liked,
                    cover = { m, s -> GradientArt(LikedGradient, Icons.Rounded.Favorite, m, s) },
                    pinned = true,
                )
            )
            playlists.forEach { p ->
                add(
                    LibItem(
                        "p${p.id}", p.name, "Playlist · ${p.songIds.size} titres", LibFilter.Playlists,
                        p.name.lowercase(), p.createdAt,
                        onClick = { nav.playlist(p.id) },
                        cover = { m, s -> PlaylistCover(p, library, m, s) },
                    )
                )
            }
        }
        if (filter == null || filter == LibFilter.Albums) {
            library.albums.forEach { a ->
                add(
                    LibItem(
                        "a${a.id}", a.title, "Album · ${a.artist}", LibFilter.Albums, a.title.lowercase(),
                        a.songs.maxOf { it.dateAdded },
                        onClick = { nav.album(a.id) },
                        cover = { m, s -> Artwork(a.art, a.title, m, s) },
                    )
                )
            }
        }
        if (filter == null || filter == LibFilter.Artists) {
            library.artists.forEach { ar ->
                add(
                    LibItem(
                        "r${ar.id}", ar.name, "Artiste", LibFilter.Artists, ar.name.lowercase(),
                        ar.songs.maxOf { it.dateAdded },
                        onClick = { nav.artist(ar.id) },
                        cover = { m, s -> Artwork(ar.art, ar.name, m, s, icon = Icons.Rounded.Person) },
                        circle = true,
                    )
                )
            }
        }
    }.sortedWith(
        compareByDescending<LibItem> { it.pinned }.then(
            when (sort) {
                SortMode.Recent -> compareByDescending<LibItem> { it.recency }
                SortMode.Alphabetical -> compareBy<LibItem> { it.sortKey }
            }
        )
    )

    Column(Modifier.fillMaxSize()) {
        Header(onCreatePlaylist)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(LibFilter.entries) { f ->
                FilterChip(f.label, filter == f) { filter = if (filter == f) null else f }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.bounceClick {
                    sort = if (sort == SortMode.Recent) SortMode.Alphabetical else SortMode.Recent
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Rounded.Sort, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                AnimatedContent(sort, label = "sort") { Text(it.label, style = MaterialTheme.typography.labelLarge, color = Color.White) }
            }
            Spacer(Modifier.weight(1f))
            AnimatedContent(
                grid,
                transitionSpec = { (scaleIn(initialScale = 0.5f) + fadeIn()).togetherWith(fadeOut()) },
                label = "layoutToggle",
            ) { isGrid ->
                Icon(
                    if (isGrid) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                    "Changer l'affichage",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp).bounceClick(0.8f) { grid = !grid },
                )
            }
        }

        AnimatedContent(
            targetState = grid,
            transitionSpec = { fadeIn(tween(300)).togetherWith(fadeOut(tween(200))) },
            label = "libraryLayout",
        ) { isGrid ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isGrid) 3 else 1),
                contentPadding = PaddingValues(
                    start = if (isGrid) 12.dp else 0.dp,
                    end = if (isGrid) 12.dp else 0.dp,
                    top = 4.dp,
                    bottom = LocalBottomInset.current,
                ),
                horizontalArrangement = Arrangement.spacedBy(if (isGrid) 12.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(if (isGrid) 16.dp else 0.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items, key = { it.key }) { item ->
                    if (isGrid) GridEntry(item, Modifier.animateItem()) else ListEntry(item, Modifier.animateItem())
                }
                if (items.size <= 1) {
                    item(span = { GridItemSpan(maxLineSpan) }) { CreateHint(onCreatePlaylist) }
                }
            }
        }
    }
}

@Composable
private fun Header(onCreate: () -> Unit) {
    Row(
        Modifier
            .statusBarsPadding()
            .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(Brush.linearGradient(listOf(SonoraColors.Green, Color(0xFF0D73EC))), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("S", color = Color.Black, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(12.dp))
        Text("Bibliothèque", style = MaterialTheme.typography.headlineSmall, color = Color.White, modifier = Modifier.weight(1f))
        Box(Modifier.size(44.dp).bounceClick(0.8f, onClick = onCreate), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, "Créer une playlist", tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) SonoraColors.Green else SonoraColors.SurfaceHigh, tween(250), label = "fbg")
    val fg by animateColorAsState(if (selected) Color.Black else Color.White, tween(250), label = "ffg")
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
private fun ListEntry(item: LibItem, modifier: Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .bounceClick(0.98f, onClick = item.onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shape = if (item.circle) CircleShape else RoundedCornerShape(4.dp)
        item.cover(Modifier.size(64.dp), shape)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.pinned) {
                    Icon(Icons.Rounded.PushPin, null, tint = SonoraColors.Green, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(item.subtitle, style = MaterialTheme.typography.bodyMedium, color = SonoraColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun GridEntry(item: LibItem, modifier: Modifier) {
    Column(modifier.bounceClick(onClick = item.onClick)) {
        val shape = if (item.circle) CircleShape else RoundedCornerShape(6.dp)
        item.cover(Modifier.fillMaxWidth().aspectRatio(1f), shape)
        Spacer(Modifier.height(6.dp))
        Text(item.title, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.subtitle, style = MaterialTheme.typography.labelSmall, color = SonoraColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CreateHint(onCreate: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Créez votre première playlist", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text("C'est simple, on va vous aider.", style = MaterialTheme.typography.bodyMedium, color = SonoraColors.TextSecondary)
        Spacer(Modifier.height(18.dp))
        Box(
            Modifier
                .background(Color.White, RoundedCornerShape(50))
                .bounceClick(onClick = onCreate)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Créer une playlist", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

