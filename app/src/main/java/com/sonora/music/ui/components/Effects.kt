package com.sonora.music.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Click with a springy press-down scale. */
fun Modifier.bounceClick(
    scaleDown: Float = 0.94f,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bounce",
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
}

/** Fades and slides content in once, staggered by [index]. */
fun Modifier.enterAnimation(index: Int, visible: Boolean = true): Modifier = composed {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { if (visible) started = true }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 550, delayMillis = (index * 70).coerceAtMost(600), easing = FastOutSlowInEasing),
        label = "enter",
    )
    graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 60f
    }
}

/** Animated "now playing" equalizer bars. */
@Composable
fun Equalizer(
    playing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = com.sonora.music.ui.theme.SonoraColors.Green,
    size: Dp = 14.dp,
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val bars = listOf(380, 520, 300, 450).map { duration ->
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(duration, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "bar",
        )
    }
    val idle = listOf(0.35f, 0.6f, 0.25f, 0.45f)
    Canvas(modifier.size(size)) {
        val count = bars.size
        val gap = this.size.width * 0.12f
        val barWidth = (this.size.width - gap * (count - 1)) / count
        bars.forEachIndexed { i, anim ->
            val h = this.size.height * (if (playing) anim.value else idle[i])
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barWidth + gap), this.size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2),
            )
        }
    }
}

/** Animates a value from 0 to 1 every time [key] changes; handy for pop effects. */
@Composable
fun rememberPop(key: Any?): Animatable<Float, *> {
    val anim = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(key) {
        if (first) {
            first = false
            return@LaunchedEffect
        }
        anim.snapTo(0.7f)
        delay(16)
        anim.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
    }
    return anim
}

/** Animated loading shimmer. */
fun Modifier.shimmer(): Modifier = composed {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        -1f, 2f,
        infiniteRepeatable(tween(1300, easing = androidx.compose.animation.core.LinearEasing)),
        label = "shimmerX",
    )
    drawWithCache {
        val w = size.width
        val brush = androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(Color(0xFF1E1E24), Color(0xFF2E2E36), Color(0xFF1E1E24)),
            start = Offset(w * x - w * 0.6f, 0f),
            end = Offset(w * x, size.height),
        )
        onDrawBehind { drawRect(brush) }
    }
}
