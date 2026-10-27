package com.sonora.music.ui.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sonora.music.playback.Progress
import com.sonora.music.ui.components.formatDuration

/** Seek bar that thickens and shows a thumb while scrubbing. */
@Composable
fun SeekBar(progress: Progress, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val shown = if (dragging) dragFraction else progress.fraction
    val track by animateDpAsState(if (dragging) 7.dp else 4.dp, spring(dampingRatio = 0.6f), label = "track")
    val thumb by animateFloatAsState(if (dragging) 1.4f else 1f, spring(dampingRatio = 0.5f), label = "thumb")

    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(progress.durationMs) {
                    detectTapGestures { offset ->
                        val f = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((f * progress.durationMs).toLong())
                    }
                }
                .pointerInput(progress.durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            onSeek((dragFraction * progress.durationMs).toLong())
                            dragging = false
                        },
                        onDragCancel = { dragging = false },
                    ) { change, amount ->
                        change.consume()
                        dragFraction = (dragFraction + amount / size.width).coerceIn(0f, 1f)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxWidth().height(20.dp)) {
                val h = track.toPx()
                val y = (size.height - h) / 2
                val radius = CornerRadius(h / 2)
                drawRoundRect(Color.White.copy(alpha = 0.22f), Offset(0f, y), Size(size.width, h), radius)
                drawRoundRect(
                    if (dragging) Color(0xFF1ED760) else Color.White,
                    Offset(0f, y), Size(size.width * shown, h), radius,
                )
                drawCircle(Color.White, radius = 6.dp.toPx() * thumb, center = Offset(size.width * shown, size.height / 2))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            val position = if (dragging) (dragFraction * progress.durationMs).toLong() else progress.positionMs
            Text(formatDuration(position), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
            Box(Modifier.weight(1f))
            Text(
                "-" + formatDuration(progress.durationMs - position),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}
