package com.sonora.music.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.music.data.Library
import com.sonora.music.playback.PlayerState
import com.sonora.music.ui.components.Artwork
import com.sonora.music.ui.components.Equalizer
import com.sonora.music.ui.theme.SonoraColors

@Composable
fun QueueSheet(
    state: PlayerState,
    library: Library,
    onDismiss: () -> Unit,
    onSkipTo: (Int) -> Unit,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (state.currentIndex - 1).coerceAtLeast(0))
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SonoraColors.SurfaceHigh,
    ) {
        Column(Modifier.fillMaxHeight(0.85f)) {
            Text(
                "File d'attente",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            LazyColumn(state = listState) {
                itemsIndexed(state.queue) { index, id ->
                    val song = library.songsById[id] ?: return@itemsIndexed
                    val current = index == state.currentIndex
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSkipTo(index) }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Artwork(song.art, song.album, Modifier.size(46.dp), RoundedCornerShape(4.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                song.title,
                                color = if (current) SonoraColors.Green else SonoraColors.TextPrimary,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                song.artist,
                                color = SonoraColors.TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (current) Equalizer(state.isPlaying, size = 16.dp)
                    }
                }
            }
        }
    }
}
