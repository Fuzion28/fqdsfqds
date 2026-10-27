package com.sonora.music.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonora.music.ui.theme.SonoraColors
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/** Round play/pause button with a morphing icon. */
@Composable
fun PlayPauseButton(
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    container: Color = SonoraColors.Green,
    content: Color = Color.Black,
    elevated: Boolean = true,
) {
    Box(
        modifier
            .size(size)
            .then(if (elevated) Modifier.shadow(12.dp, CircleShape, ambientColor = container, spotColor = container) else Modifier)
            .background(container, CircleShape)
            .bounceClick(0.88f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = playing,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), initialScale = 0.4f) + fadeIn(tween(150)))
                    .togetherWith(scaleOut(tween(150), targetScale = 0.4f) + fadeOut(tween(120)))
            },
            label = "playPause",
        ) { isPlaying ->
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Lecture",
                tint = content,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

/** Heart button with a bouncy pop and a particle burst when liking. */
@Composable
fun LikeButton(
    liked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    inactiveTint: Color = SonoraColors.TextSecondary,
) {
    val scale = remember { Animatable(1f) }
    val burst = remember { Animatable(1f) }
    var initialized by remember { mutableStateOf(false) }
    val tint by animateColorAsState(if (liked) SonoraColors.Green else inactiveTint, tween(250), label = "likeTint")

    LaunchedEffect(liked) {
        if (!initialized) {
            initialized = true
            return@LaunchedEffect
        }
        if (liked) {
            launch {
                burst.snapTo(0f)
                burst.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
            }
            scale.snapTo(0.5f)
            scale.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium))
        } else {
            scale.snapTo(0.8f)
            scale.animateTo(1f, spring(dampingRatio = 0.5f))
        }
    }

    Box(modifier.size(size + 18.dp).bounceClick(0.85f, onClick = onToggle), contentAlignment = Alignment.Center) {
        val b = burst.value
        if (b < 1f) {
            Canvas(Modifier.fillMaxSize()) {
                val center = this.center
                val maxR = this.size.minDimension / 2
                drawCircle(
                    color = SonoraColors.Green.copy(alpha = (1f - b) * 0.6f),
                    radius = maxR * (0.4f + 0.6f * b),
                    style = Stroke(width = 3.dp.toPx() * (1f - b)),
                )
                repeat(8) { i ->
                    val angle = Math.toRadians(i * 45.0 + 22.5)
                    val r = maxR * (0.45f + 0.55f * b)
                    drawCircle(
                        color = (if (i % 2 == 0) SonoraColors.Green else Color.White).copy(alpha = 1f - b),
                        radius = 2.5.dp.toPx() * (1f - b * 0.6f),
                        center = center.copy(
                            x = center.x + (cos(angle) * r).toFloat(),
                            y = center.y + (sin(angle) * r).toFloat(),
                        ),
                    )
                }
            }
        }
        Icon(
            if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (liked) "Retirer des titres likés" else "Ajouter aux titres likés",
            tint = tint,
            modifier = Modifier
                .size(size)
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        )
    }
}

/** Icon toggle with a small active dot underneath (shuffle / repeat). */
@Composable
fun ToggleIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
) {
    val tint by animateColorAsState(if (active) SonoraColors.Green else Color.White.copy(alpha = 0.85f), label = "toggle")
    val dot by animateFloatAsState(if (active) 1f else 0f, spring(dampingRatio = 0.5f), label = "dot")
    Box(modifier.size(size + 22.dp).bounceClick(0.85f, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(size))
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .size(5.dp)
                .graphicsLayer { scaleX = dot; scaleY = dot; alpha = dot }
                .background(SonoraColors.Green, CircleShape)
        )
    }
}
