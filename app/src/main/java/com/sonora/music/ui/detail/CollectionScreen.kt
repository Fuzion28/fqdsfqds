package com.sonora.music.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.music.data.ArtRequest
import com.sonora.music.data.Library
import com.sonora.music.data.Playlist
import com.sonora.music.data.Song
import com.sonora.music.ui.AppNav
import com.sonora.music.ui.LocalBottomInset
import com.sonora.music.ui.MainViewModel
import com.sonora.music.ui.components.Artwork
import com.sonora.music.ui.components.Carousel
import com.sonora.music.ui.components.CoverCard
import com.sonora.music.ui.components.GradientArt
import com.sonora.music.ui.components.NameDialog
import com.sonora.music.ui.components.PlayPauseButton
import com.sonora.music.ui.components.PlaylistCover
import com.sonora.music.ui.components.SectionHeader
import com.sonora.music.ui.components.SongActions
import com.sonora.music.ui.components.SongRow
import com.sonora.music.ui.components.bounceClick
import com.sonora.music.ui.components.formatTotalDuration
import com.sonora.music.ui.components.lerpColor
import com.sonora.music.ui.components.rememberArtColor
import com.sonora.music.ui.home.LikedGradient
import com.sonora.music.ui.theme.SonoraColors
import kotlin.random.Random

sealed interface CollectionSource {
    data class AlbumSource(val id: Long) : CollectionSource
    data class ArtistSource(val id: Long) : CollectionSource
    data class MixSource(val artistId: Long) : CollectionSource
    data class PlaylistSource(val id: String) : CollectionSource
    data object Liked : CollectionSource
    data object RecentlyAdded : CollectionSource
}

private enum class CoverStyle { Art, Circle, Liked, Recent, Playlist }

private data class CollectionData(
    val title: String,
    val kind: String,
    val subtitle: String,
    val songs: List<Song>,
    val art: ArtRequest?,
    val seed: String,
    val style: CoverStyle,
    val numbered: Boolean = false,
    val playlist: Playlist? = null,
    val artistId: Long? = null,
)

private fun resolve(source: CollectionSource, library: Library, liked: List<Long>, playlists: List<Playlist>): CollectionData? =
    when (source) {
        is CollectionSource.AlbumSource -> library.albumsById[source.id]?.let { album ->
            CollectionData(
                album.title, "Album", album.artist, album.songs, album.art, album.title, CoverStyle.Art,
                numbered = true, artistId = album.artistId,
            )
        }
        is CollectionSource.ArtistSource -> library.artistsById[source.id]?.let { artist ->
            CollectionData(
                artist.name, "Artiste", "${artist.albums.size} albums", artist.songs, artist.art, artist.name,
                CoverStyle.Circle, artistId = artist.id,
            )
        }
        is CollectionSource.MixSource -> library.artistsById[source.artistId]?.let { artist ->
            val random = Random(artist.id)
            val others = library.songs.filter { it.artistId != artist.id }.shuffled(random).take(12)
            val songs = (artist.songs.shuffled(random).take(18) + others).shuffled(random)
            CollectionData(
                "Mix ${artist.name}", "Mix", "${artist.name} et bien plus encore", songs, artist.art, artist.name,
                CoverStyle.Art,
            )
        }
        is CollectionSource.PlaylistSource -> playlists.firstOrNull { it.id == source.id }?.let { p ->
            CollectionData(
                p.name, "Playlist", "Créée par vous", p.songIds.mapNotNull { library.songsById[it] },
                null, p.name, CoverStyle.Playlist, playlist = p,
            )
        }
        CollectionSource.Liked -> CollectionData(
            "Titres likés", "Playlist", "Vos coups de cœur", liked.mapNotNull { library.songsById[it] },
            null, "liked", CoverStyle.Liked,
        )
        CollectionSource.RecentlyAdded -> CollectionData(
            "Ajoutés récemment", "Playlist", "Les derniers fichiers de votre appareil",
            library.songs.sortedByDescending { it.dateAdded }.take(50), null, "recent", CoverStyle.Recent,
        )
    }

@Composable
fun CollectionScreen(vm: MainViewModel, source: CollectionSource, nav: AppNav, baseActions: SongActions) {
    val library by vm.library.collectAsState()
    val liked by vm.store.liked.collectAsState()
    val playlists by vm.store.playlists.collectAsState()
    val playerState by vm.player.state.collectAsState()

    val data = remember(source, library, liked, playlists) { resolve(source, library, liked, playlists) }
    if (data == null) {
        // The collection disappeared (deleted playlist, rescanned library…).
        Box(Modifier.fillMaxSize().statusBarsPadding(), contentAlignment = Alignment.Center) {
            Text("Introuvable", color = SonoraColors.TextSecondary)
        }
        return
    }

    val actions = remember(baseActions, data.playlist?.id) {
        val playlist = data.playlist
        if (playlist == null) baseActions
        else SongActions(
            isLiked = baseActions.isLiked,
            onToggleLike = baseActions.onToggleLike,
            onAddToQueue = baseActions.onAddToQueue,
            onAddToPlaylist = baseActions.onAddToPlaylist,
            onGoToAlbum = baseActions.onGoToAlbum,
            onGoToArtist = baseActions.onGoToArtist,
            onRemove = { vm.store.removeFromPlaylist(playlist.id, it.id) },
        )
    }

    val fallback = when (data.style) {
        CoverStyle.Liked -> Color(0xFF4A1FD6)
        CoverStyle.Recent -> Color(0xFF0D73EC)
        else -> SonoraColors.accentFor(data.seed)
    }
    val coverArt = data.art ?: data.songs.firstOrNull()?.art
    val artColor by rememberArtColor(if (data.style == CoverStyle.Liked || data.style == CoverStyle.Recent) null else coverArt, data.seed, fallback)
    val color by animateColorAsState(artColor, tween(700), label = "collectionColor")

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var headerHeight by remember { mutableIntStateOf(1) }
    val scroll by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else Float.MAX_VALUE
        }
    }
    val collapsed by remember {
        derivedStateOf { scroll > headerHeight - with(density) { 140.dp.toPx() } }
    }

    val songIds = remember(data.songs) { data.songs.map { it.id }.toSet() }
    val playingHere = playerState.currentId in songIds
    val onPlay = {
        if (playingHere) vm.player.togglePlay() else vm.play(data.songs, 0)
    }

    var menu by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(460.dp)
                .graphicsLayer { translationY = -(scroll.coerceAtMost(2000f)) * 0.5f }
                .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.35f), Color.Transparent)))
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = LocalBottomInset.current),
        ) {
            item(key = "header") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .onSizeChanged { headerHeight = it.height }
                        .statusBarsPadding()
                        .padding(top = 56.dp),
                ) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val shape = if (data.style == CoverStyle.Circle) CircleShape else RoundedCornerShape(6.dp)
                        val coverModifier = Modifier
                            .size(240.dp)
                            .graphicsLayer {
                                val s = scroll.coerceAtMost(1000f)
                                val k = 1f - (s / 900f).coerceIn(0f, 0.45f)
                                scaleX = k
                                scaleY = k
                                alpha = 1f - (s / 650f).coerceIn(0f, 1f)
                                translationY = s * 0.45f
                            }
                            .shadow(30.dp, shape)
                        when (data.style) {
                            CoverStyle.Liked -> GradientArt(LikedGradient, Icons.Rounded.Favorite, coverModifier, shape)
                            CoverStyle.Recent -> GradientArt(
                                listOf(Color(0xFF0D73EC), Color(0xFF1ED760)), Icons.Rounded.Schedule, coverModifier, shape,
                            )
                            CoverStyle.Playlist -> PlaylistCover(data.playlist!!, library, coverModifier, shape)
                            CoverStyle.Circle -> Artwork(coverArt, data.seed, coverModifier, shape, icon = Icons.Rounded.Person)
                            CoverStyle.Art -> Artwork(coverArt, data.seed, coverModifier, shape, icon = Icons.Rounded.MusicNote)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        data.title,
                        style = if (data.title.length > 22) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        data.subtitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .then(
                                if (data.artistId != null && data.style != CoverStyle.Circle) {
                                    Modifier.bounceClick { nav.artist(data.artistId) }
                                } else Modifier
                            ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${data.kind} · ${data.songs.size} titres · ${formatTotalDuration(data.songs.sumOf { it.durationMs })}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (data.playlist != null) {
                            Box {
                                Box(Modifier.size(48.dp).bounceClick(0.8f) { menu = true }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.MoreVert, "Options", tint = SonoraColors.TextSecondary)
                                }
                                DropdownMenu(
                                    expanded = menu,
                                    onDismissRequest = { menu = false },
                                    containerColor = SonoraColors.SurfaceHighest,
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Renommer") },
                                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                        onClick = { menu = false; rename = true },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Supprimer la playlist") },
                                        leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                        onClick = { menu = false; confirmDelete = true },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier.size(48.dp).bounceClick(0.8f) { vm.shuffle(data.songs) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Shuffle, "Lecture aléatoire",
                                tint = if (playingHere && playerState.shuffle) SonoraColors.Green else SonoraColors.TextSecondary,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        PlayPauseButton(playing = playingHere && playerState.isPlaying, onClick = onPlay, size = 56.dp)
                    }
                }
            }

            if (data.songs.isEmpty()) {
                item(key = "empty") {
                    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Rien ici pour l'instant", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (data.style == CoverStyle.Liked) "Touchez le cœur sur un titre pour le retrouver ici."
                            else "Ajoutez des titres depuis le menu ⋮ d'une chanson.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SonoraColors.TextSecondary,
                        )
                    }
                }
            }

            itemsIndexed(data.songs, key = { _, song -> song.id }) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = song.id == playerState.currentId,
                    isPlaying = playerState.isPlaying,
                    onClick = { vm.play(data.songs, index) },
                    actions = actions,
                    index = index,
                    showArtwork = !data.numbered,
                    modifier = Modifier.animateItem(),
                )
            }

            if (data.style == CoverStyle.Circle) {
                val artist = data.artistId?.let { library.artistsById[it] }
                if (artist != null && artist.albums.isNotEmpty()) {
                    item(key = "albums") {
                        Column {
                            SectionHeader("Discographie")
                            Carousel(artist.albums, key = { it.id }) { album ->
                                CoverCard(
                                    album.title,
                                    if (album.year > 0) "${album.year} · Album" else "Album",
                                    album.art, album.title,
                                    onClick = { nav.album(album.id) },
                                )
                            }
                        }
                    }
                }
            }
        }

        TopBar(
            title = data.title,
            color = lerpColor(color, Color.Black, 0.35f),
            scroll = { scroll },
            collapsed = collapsed,
            playing = playingHere && playerState.isPlaying,
            onBack = { nav.back() },
            onPlay = onPlay,
        )
    }

    if (rename && data.playlist != null) {
        NameDialog(
            title = "Renommer la playlist",
            initial = data.playlist.name,
            confirmLabel = "Enregistrer",
            onDismiss = { rename = false },
            onConfirm = { vm.store.renamePlaylist(data.playlist.id, it.trim().ifEmpty { data.playlist.name }); rename = false },
        )
    }
    if (confirmDelete && data.playlist != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = SonoraColors.SurfaceHigh,
            title = { Text("Supprimer « ${data.playlist.name} » ?") },
            text = { Text("Cette action est définitive.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    nav.back()
                    vm.store.deletePlaylist(data.playlist.id)
                }) { Text("Supprimer", color = Color(0xFFFF5A5F)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Annuler", color = SonoraColors.TextSecondary) }
            },
        )
    }
}

@Composable
private fun TopBar(
    title: String,
    color: Color,
    scroll: () -> Float,
    collapsed: Boolean,
    playing: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { }
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = (scroll().coerceAtMost(10000f) / 500f).coerceIn(0f, 1f) }
                .background(color)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                    .bounceClick(0.85f, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Retour", tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        val a = ((scroll().coerceAtMost(10000f) - 350f) / 250f).coerceIn(0f, 1f)
                        alpha = a
                        translationY = (1f - a) * 20f
                    },
            )
            AnimatedVisibility(
                visible = collapsed,
                enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                PlayPauseButton(playing = playing, onClick = onPlay, size = 44.dp)
            }
        }
    }
}
