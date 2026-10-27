package com.sonora.music.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonora.music.data.Song
import com.sonora.music.playback.Progress
import com.sonora.music.ui.components.Artwork
import com.sonora.music.ui.components.LikeButton
import com.sonora.music.ui.components.PlayPauseButton
import com.sonora.music.ui.components.lerpColor
import com.sonora.music.ui.components.rememberArtColor
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Progress,
    liked: Boolean,
    onToggleLike: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artColor by rememberArtColor(song.art, song.album)
    val background by animateColorAsState(lerpColor(artColor, Color.Black, 0.45f), tween(700), label = "miniBg")
    val swipe = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val width = remember { floatArrayOf(1f) }

    Box(
        modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .height(60.dp)
            .shadow(16.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .onSizeChanged { width[0] = it.width.toFloat() }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onExpand)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            val threshold = width[0] * 0.25f
                            val value = swipe.value
                            if (abs(value) > threshold) {
                                swipe.animateTo(if (value < 0) -width[0] else width[0], tween(160))
                                if (value < 0) onNext() else onPrevious()
                                swipe.snapTo(if (value < 0) width[0] * 0.5f else -width[0] * 0.5f)
                            }
                            swipe.animateTo(0f, spring(dampingRatio = 0.7f))
                        }
                    },
                ) { change, amount ->
                    change.consume()
                    scope.launch { swipe.snapTo(swipe.value + amount) }
                }
            },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .graphicsLayer {
                        translationX = swipe.value
                        alpha = 1f - (abs(swipe.value) / (width[0] * 0.8f)).coerceIn(0f, 0.8f)
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedContent(
                    targetState = song,
                    transitionSpec = {
                        (fadeIn(tween(300)) + slideInHorizontally { it / 3 }).togetherWith(fadeOut(tween(200)))
                    },
                    label = "miniArt",
                ) { s ->
                    Artwork(s.art, s.album, Modifier.size(42.dp), RoundedCornerShape(6.dp))
                }
                Spacer(Modifier.width(10.dp))
                AnimatedContent(
                    targetState = song,
                    transitionSpec = {
                        (fadeIn(tween(300)) + slideInHorizontally { it / 4 })
                            .togetherWith(fadeOut(tween(150)) + slideOutHorizontally { -it / 4 })
                    },
                    modifier = Modifier.weight(1f),
                    label = "miniText",
                ) { s ->
                    Column {
                        Text(
                            s.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2000),
                        )
                        Text(
                            s.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                        )
                    }
                }
            }
            LikeButton(liked, onToggleLike, size = 22.dp, inactiveTint = Color.White)
            PlayPauseButton(
                playing = isPlaying,
                onClick = onTogglePlay,
                size = 38.dp,
                container = Color.Transparent,
                content = Color.White,
                elevated = false,
            )
        }
        // Progress line
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.fraction)
                    .height(2.dp)
                    .background(Color.White)
            )
        }
    }
}
