package com.sonora.music.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.music.data.Album
import com.sonora.music.data.Library
import com.sonora.music.playback.PlayerState
import com.sonora.music.ui.AppNav
import com.sonora.music.ui.LocalBottomInset
import com.sonora.music.ui.MainViewModel
import com.sonora.music.ui.components.Artwork
import com.sonora.music.ui.components.Carousel
import com.sonora.music.ui.components.CoverCard
import com.sonora.music.ui.components.Equalizer
import com.sonora.music.ui.components.GradientArt
import com.sonora.music.ui.components.MixBadge
import com.sonora.music.ui.components.PlayPauseButton
import com.sonora.music.ui.components.SectionHeader
import com.sonora.music.ui.components.SongActions
import com.sonora.music.ui.components.bounceClick
import com.sonora.music.ui.components.enterAnimation
import com.sonora.music.ui.components.rememberArtColor
import com.sonora.music.ui.components.shimmer
import com.sonora.music.ui.theme.SonoraColors
import java.util.Calendar

val LikedGradient = listOf(Color(0xFF4A1FD6), Color(0xFF8C7BEA), Color(0xFFC4EFD9))

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Bonjour"
    in 12..17 -> "Bon après-midi"
    else -> "Bonsoir"
}

@Composable
fun HomeScreen(
    vm: MainViewModel,
    library: Library,
    playerState: PlayerState,
    nav: AppNav,
    onSongActions: SongActions,
) {
    val recentIds by vm.store.recent.collectAsState()
    val liked by vm.store.liked.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val listState = rememberLazyListState()
    val current = playerState.currentId?.let { library.songsById[it] }

    val recentSongs = remember(recentIds, library) { recentIds.mapNotNull { library.songsById[it] }.take(15) }
    val quickAlbums = remember(recentSongs, library) {
        (recentSongs.mapNotNull { library.albumsById[it.albumId] } + library.albums.shuffled())
            .distinctBy { it.id }
            .take(5)
    }
    val recentlyAdded = remember(library) { library.songs.sortedByDescending { it.dateAdded }.take(15) }
    val mixes = remember(library) {
        library.artists.filter { it.songs.size >= 3 }.sortedByDescending { it.songs.size }.take(8)
    }

    // Header tint follows the last played album, fading out as you scroll.
    val headerSeed = recentSongs.firstOrNull()
    val headerColor by rememberArtColor(headerSeed?.art, headerSeed?.album ?: "home", SonoraColors.GreenDark)
    val animatedHeader by animateColorAsState(headerColor, tween(1200), label = "header")
    val headerAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 0f
            else 1f - (listState.firstVisibleItemScrollOffset / 600f).coerceIn(0f, 1f)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(340.dp)
                .graphicsLayer { alpha = headerAlpha }
                .background(Brush.verticalGradient(listOf(animatedHeader.copy(alpha = 0.75f), Color.Transparent)))
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = LocalBottomInset.current),
        ) {
            item(key = "header") {
                Row(
                    Modifier
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp)
                        .enterAnimation(0),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(greeting(), style = MaterialTheme.typography.headlineMedium, color = Color.White, modifier = Modifier.weight(1f))
                    RefreshButton(refreshing) { vm.loadLibrary() }
                }
            }

            if (!library.loaded) {
                item(key = "loading") { LoadingSkeleton() }
                return@LazyColumn
            }
            if (library.songs.isEmpty()) {
                item(key = "empty") { EmptyLibrary { vm.loadLibrary() } }
                return@LazyColumn
            }

            item(key = "quick") {
                QuickGrid(
                    likedCount = liked.size,
                    albums = quickAlbums,
                    currentAlbumId = current?.albumId,
                    isPlaying = playerState.isPlaying,
                    onLiked = nav::liked,
                    onAlbum = { nav.album(it.id) },
                    modifier = Modifier.enterAnimation(1),
                )
            }

            item(key = "hero") {
                ShuffleHero(
                    count = library.songs.size,
                    playing = playerState.isPlaying && playerState.shuffle,
                    onPlay = { vm.shuffle(library.songs) },
                    modifier = Modifier.enterAnimation(2),
                )
            }

            if (recentSongs.isNotEmpty()) {
                item(key = "recentHeader") { SectionHeader("Écoutés récemment", Modifier.enterAnimation(3)) }
                item(key = "recentRow") {
                    Box(Modifier.enterAnimation(3)) {
                        Carousel(recentSongs, key = { it.id }) { song ->
                            CoverCard(
                                song.title, song.artist, song.art, song.album,
                                onClick = { vm.play(recentSongs, recentSongs.indexOf(song)) },
                                size = 130.dp,
                            )
                        }
                    }
                }
            }

            item(key = "addedHeader") {
                SectionHeader("Ajoutés récemment", Modifier.enterAnimation(4), action = "Tout afficher", onAction = nav::recentlyAdded)
            }
            item(key = "addedRow") {
                Box(Modifier.enterAnimation(4)) {
                    Carousel(recentlyAdded, key = { it.id }) { song ->
                        CoverCard(
                            song.title, song.artist, song.art, song.album,
                            onClick = { vm.play(recentlyAdded, recentlyAdded.indexOf(song)) },
                        )
                    }
                }
            }

            if (mixes.isNotEmpty()) {
                item(key = "mixHeader") { SectionHeader("Vos mix") }
                item(key = "mixRow") {
                    Carousel(mixes.withIndex().toList(), key = { it.value.id }) { (i, artist) ->
                        val color = SonoraColors.Accents[i % SonoraColors.Accents.size]
                        CoverCard(
                            "Mix ${artist.name}",
                            artist.songs.drop(1).take(3).joinToString { it.artist }.ifEmpty { artist.name },
                            artist.art, artist.name,
                            onClick = { nav.mix(artist.id) },
                            badge = { MixBadge(color, "Mix ${i + 1}", 150.dp) },
                        )
                    }
                }
            }

            item(key = "albumsHeader") { SectionHeader("Vos albums") }
            item(key = "albumsRow") {
                Carousel(library.albums.take(20), key = { it.id }) { album ->
                    CoverCard(album.title, album.artist, album.art, album.title, onClick = { nav.album(album.id) })
                }
            }

            item(key = "artistsHeader") { SectionHeader("Vos artistes") }
            item(key = "artistsRow") {
                Carousel(library.artists.sortedByDescending { it.songs.size }.take(20), key = { it.id }) { artist ->
                    CoverCard(
                        artist.name, "Artiste", artist.art, artist.name,
                        onClick = { nav.artist(artist.id) },
                        size = 130.dp,
                        circle = true,
                    )
                }
            }
            item(key = "end") { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun RefreshButton(refreshing: Boolean, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "refresh")
    val rotation by t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "rot")
    Box(Modifier.size(44.dp).bounceClick(0.8f, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Rounded.Refresh, "Actualiser", tint = Color.White,
            modifier = Modifier.graphicsLayer { rotationZ = if (refreshing) rotation else 0f },
        )
    }
}

@Composable
private fun QuickGrid(
    likedCount: Int,
    albums: List<Album>,
    currentAlbumId: Long?,
    isPlaying: Boolean,
    onLiked: () -> Unit,
    onAlbum: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    // null stands for the "Titres likés" tile.
    val tiles: List<Album?> = listOf<Album?>(null) + albums
    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { album ->
                    if (album == null) {
                        QuickTile("Titres likés", "$likedCount titres", Modifier.weight(1f), onLiked, playing = false) {
                            GradientArt(LikedGradient, Icons.Rounded.Favorite, Modifier.size(56.dp))
                        }
                    } else {
                        QuickTile(
                            album.title, album.artist, Modifier.weight(1f), { onAlbum(album) },
                            playing = album.id == currentAlbumId, isPlaying = isPlaying,
                        ) {
                            Artwork(album.art, album.title, Modifier.size(56.dp))
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickTile(
    title: String,
    subtitle: String,
    modifier: Modifier,
    onClick: () -> Unit,
    playing: Boolean,
    isPlaying: Boolean = false,
    art: @Composable () -> Unit,
) {
    Row(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.09f))
            .bounceClick(0.96f, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        art()
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (playing) SonoraColors.Green else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = SonoraColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (playing) {
            Equalizer(isPlaying, Modifier.padding(end = 10.dp), size = 14.dp)
        }
    }
}

@Composable
private fun ShuffleHero(count: Int, playing: Boolean, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "hero")
    val shift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse), label = "shift")
    Box(
        modifier
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .fillMaxWidth()
            .height(150.dp)
            .shadow(18.dp, RoundedCornerShape(18.dp), spotColor = SonoraColors.Green)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1ED760), Color(0xFF0D73EC), Color(0xFF8D67AB)),
                    start = androidx.compose.ui.geometry.Offset(-400f + shift * 800f, 0f),
                    end = androidx.compose.ui.geometry.Offset(600f + shift * 800f, 600f),
                )
            )
            .bounceClick(0.97f, onClick = onPlay),
    ) {
        Icon(
            Icons.Rounded.Shuffle, null,
            tint = Color.White.copy(alpha = 0.12f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(170.dp)
                .graphicsLayer { rotationZ = -12f + shift * 24f; translationX = 40f },
        )
        Column(Modifier.align(Alignment.CenterStart).padding(20.dp)) {
            Text("LECTURE ALÉATOIRE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(4.dp))
            Text("Toute votre musique", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Black)
            Text("$count titres, mélangés pour vous", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
        }
        PlayPauseButton(
            playing = playing,
            onClick = onPlay,
            size = 52.dp,
            container = Color.Black,
            content = SonoraColors.Green,
            elevated = false,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

@Composable
private fun LoadingSkeleton() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(6.dp)).shimmer())
                Box(Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(6.dp)).shimmer())
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(18.dp)).shimmer())
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(3) { Box(Modifier.size(140.dp).clip(RoundedCornerShape(6.dp)).shimmer()) }
        }
    }
}

@Composable
private fun EmptyLibrary(onRefresh: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(32.dp)
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(96.dp).background(SonoraColors.SurfaceHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.MusicOff, null, tint = SonoraColors.TextSecondary, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Aucune musique trouvée", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(
            "Ajoutez des fichiers audio (MP3, FLAC, M4A…) sur votre téléphone puis actualisez.",
            style = MaterialTheme.typography.bodyMedium,
            color = SonoraColors.TextSecondary,
        )
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .background(Color.White, RoundedCornerShape(50))
                .bounceClick(onClick = onRefresh)
                .padding(horizontal = 28.dp, vertical = 12.dp)
        ) {
            Text("Actualiser", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

