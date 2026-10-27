package com.sonora.music.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.sonora.music.data.Song
import com.sonora.music.playback.PlayerState
import com.sonora.music.playback.Progress
import com.sonora.music.ui.components.Artwork
import com.sonora.music.ui.components.LikeButton
import com.sonora.music.ui.components.PlayPauseButton
import com.sonora.music.ui.components.ToggleIcon
import com.sonora.music.ui.components.bounceClick
import com.sonora.music.ui.components.lerpColor
import com.sonora.music.ui.components.rememberArtColor
import com.sonora.music.ui.theme.SonoraColors
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.absoluteValue

class FullPlayerActions(
    val onCollapse: () -> Unit,
    val onTogglePlay: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onNextItem: () -> Unit,
    val onPreviousItem: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onShuffle: () -> Unit,
    val onRepeat: () -> Unit,
    val onToggleLike: () -> Unit,
    val onOpenQueue: () -> Unit,
    val onAddToPlaylist: () -> Unit,
    val onGoToAlbum: () -> Unit,
    val onGoToArtist: () -> Unit,
)

@Composable
fun FullPlayer(
    song: Song,
    previous: Song?,
    next: Song?,
    state: PlayerState,
    progress: StateFlow<Progress>,
    liked: Boolean,
    actions: FullPlayerActions,
    modifier: Modifier = Modifier,
) {
    val artColor by rememberArtColor(song.art, song.album)
    val top by animateColorAsState(artColor, tween(900), label = "bgTop")
    val mid by animateColorAsState(lerpColor(artColor, Color.Black, 0.6f), tween(900), label = "bgMid")

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to top, 0.55f to mid, 1f to SonoraColors.Background))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            TopBar(song, actions)
            Spacer(Modifier.weight(1f))
            ArtworkPager(song, previous, next, state.isPlaying, artColor, actions)
            Spacer(Modifier.weight(1f))
            Column(Modifier.padding(horizontal = 24.dp)) {
                TitleRow(song, liked, actions.onToggleLike)
                Spacer(Modifier.height(14.dp))
                val p by progress.collectAsState()
                SeekBar(p, actions.onSeek)
                Spacer(Modifier.height(8.dp))
                Controls(state, actions)
                Spacer(Modifier.height(18.dp))
                BottomRow(actions.onOpenQueue)
            }
        }
    }
}

@Composable
private fun TopBar(song: Song, actions: FullPlayerActions) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = actions.onCollapse) {
            Icon(Icons.Rounded.KeyboardArrowDown, "Réduire", tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "LECTURE DEPUIS L'ALBUM",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
            Text(
                song.album,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Rounded.MoreVert, "Options", tint = Color.White)
            }
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                containerColor = SonoraColors.SurfaceHighest,
                shape = RoundedCornerShape(12.dp),
            ) {
                DropdownMenuItem(
                    text = { Text("Ajouter à une playlist") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                    onClick = { menu = false; actions.onAddToPlaylist() },
                )
                DropdownMenuItem(
                    text = { Text("Voir l'album") },
                    leadingIcon = { Icon(Icons.Rounded.Album, null) },
                    onClick = { menu = false; actions.onGoToAlbum() },
                )
                DropdownMenuItem(
                    text = { Text("Voir l'artiste") },
                    leadingIcon = { Icon(Icons.Rounded.Person, null) },
                    onClick = { menu = false; actions.onGoToArtist() },
                )
            }
        }
    }
}

/**
 * Swipeable artwork: the pager holds [previous, current, next]. Settling on a
 * neighbour skips to it; a new pager is created for each song so the current
 * page is always centered.
 */
@Composable
private fun ArtworkPager(
    song: Song,
    previous: Song?,
    next: Song?,
    playing: Boolean,
    glowColor: Color,
    actions: FullPlayerActions,
) {
    val scale by animateFloatAsState(
        if (playing) 1f else 0.86f,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
        label = "artScale",
    )
    val glow = rememberInfiniteTransition(label = "glow")
    val pulse by glow.animateFloat(
        0.92f, 1.08f,
        infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Soft glow behind the cover
        Box(
            Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(1f)
                .graphicsLayer {
                    val s = if (playing) pulse * scale else scale
                    scaleX = s * 1.1f
                    scaleY = s * 1.1f
                    alpha = 0.55f
                }
                .background(
                    Brush.radialGradient(listOf(glowColor.copy(alpha = 0.9f), Color.Transparent)),
                    CircleShape,
                )
        )
        AnimatedContent(
            targetState = PagerContent(song, previous, next),
            contentKey = { it.song.id },
            transitionSpec = {
                (fadeIn(tween(350)) + scaleIn(tween(350), initialScale = 0.92f)).togetherWith(fadeOut(tween(250)))
            },
            label = "pager",
        ) { content ->
            key(content.song.id, content.previous?.id, content.next?.id) {
                val pages = listOfNotNull(content.previous, content.song, content.next)
                val currentPage = if (content.previous != null) 1 else 0
                val pager = rememberPagerState(initialPage = currentPage) { pages.size }
                val onPrev by rememberUpdatedState(actions.onPreviousItem)
                val onNext by rememberUpdatedState(actions.onNextItem)
                LaunchedEffect(pager) {
                    snapshotFlow { pager.settledPage }.collect { page ->
                        when {
                            page < currentPage -> onPrev()
                            page > currentPage -> onNext()
                        }
                    }
                }
                HorizontalPager(
                    state = pager,
                    pageSpacing = 16.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) { page ->
                    val item = pages[page]
                    val offset = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue
                    Box(Modifier.fillMaxWidth().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
                        Artwork(
                            item.art,
                            item.album,
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .graphicsLayer {
                                    val s = scale * (1f - offset.coerceIn(0f, 1f) * 0.12f)
                                    scaleX = s
                                    scaleY = s
                                    alpha = 1f - offset.coerceIn(0f, 1f) * 0.4f
                                }
                                .shadow(28.dp, RoundedCornerShape(10.dp)),
                            RoundedCornerShape(10.dp),
                        )
                    }
                }
            }
        }
    }
}

private data class PagerContent(val song: Song, val previous: Song?, val next: Song?)

@Composable
private fun TitleRow(song: Song, liked: Boolean, onToggleLike: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AnimatedContent(
            targetState = song,
            transitionSpec = {
                (fadeIn(tween(400, delayMillis = 100)) + slideInVertically { it / 2 })
                    .togetherWith(fadeOut(tween(150)) + slideOutVertically { -it / 2 })
            },
            modifier = Modifier.weight(1f),
            label = "title",
        ) { s ->
            Column {
                Text(
                    s.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2500),
                )
                Text(
                    s.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        LikeButton(liked, onToggleLike, size = 28.dp, inactiveTint = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
private fun Controls(state: PlayerState, actions: FullPlayerActions) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToggleIcon(Icons.Rounded.Shuffle, state.shuffle, actions.onShuffle, "Aléatoire")
        Icon(
            Icons.Rounded.SkipPrevious, "Précédent", tint = Color.White,
            modifier = Modifier.size(46.dp).bounceClick(0.8f, onClick = actions.onPrevious),
        )
        PlayPauseButton(
            playing = state.isPlaying,
            onClick = actions.onTogglePlay,
            size = 72.dp,
            container = Color.White,
            content = Color.Black,
        )
        Icon(
            Icons.Rounded.SkipNext, "Suivant", tint = Color.White,
            modifier = Modifier.size(46.dp).bounceClick(0.8f, onClick = actions.onNext),
        )
        ToggleIcon(
            if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            state.repeatMode != Player.REPEAT_MODE_OFF,
            actions.onRepeat,
            "Répéter",
        )
    }
}

@Composable
private fun BottomRow(onOpenQueue: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Speaker, null, tint = SonoraColors.Green, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Cet appareil", style = MaterialTheme.typography.labelLarge, color = SonoraColors.Green)
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Rounded.QueueMusic, "File d'attente", tint = Color.White,
            modifier = Modifier.size(24.dp).bounceClick(0.8f, onClick = onOpenQueue),
        )
    }
}
