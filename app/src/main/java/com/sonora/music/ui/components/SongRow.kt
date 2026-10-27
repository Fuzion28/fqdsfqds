package com.sonora.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.music.data.Song
import com.sonora.music.ui.theme.SonoraColors

/** Actions available from a song's overflow menu. */
class SongActions(
    val isLiked: (Song) -> Boolean,
    val onToggleLike: (Song) -> Unit,
    val onAddToQueue: (Song) -> Unit,
    val onAddToPlaylist: (Song) -> Unit,
    val onGoToAlbum: (Song) -> Unit,
    val onGoToArtist: (Song) -> Unit,
    val onRemove: ((Song) -> Unit)? = null,
)

@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    actions: SongActions,
    modifier: Modifier = Modifier,
    index: Int? = null,
    showArtwork: Boolean = true,
) {
    val titleColor by animateColorAsState(
        if (isCurrent) SonoraColors.Green else SonoraColors.TextPrimary, label = "title"
    )
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (index != null && !showArtwork) {
            Box(Modifier.width(32.dp), contentAlignment = Alignment.CenterStart) {
                if (isCurrent) {
                    Equalizer(isPlaying, size = 14.dp)
                } else {
                    Text("${index + 1}", style = MaterialTheme.typography.bodyMedium, color = SonoraColors.TextSecondary)
                }
            }
        }
        if (showArtwork) {
            Box {
                Artwork(song.art, song.album, Modifier.size(50.dp), RoundedCornerShape(4.dp))
                AnimatedVisibility(
                    visible = isCurrent,
                    enter = fadeIn() + scaleIn(initialScale = 0.6f),
                    exit = fadeOut() + scaleOut(targetScale = 0.6f),
                    modifier = Modifier.matchParentSize(),
                ) {
                    Box(
                        Modifier.background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Equalizer(isPlaying, size = 18.dp) }
                }
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (actions.isLiked(song)) {
                    Icon(
                        Icons.Rounded.Favorite, null, tint = SonoraColors.Green,
                        modifier = Modifier.size(13.dp).padding(end = 2.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                }
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SonoraColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        SongMenuButton(song, actions)
    }
}

@Composable
fun SongMenuButton(song: Song, actions: SongActions, tint: Color = SonoraColors.TextSecondary) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, "Options", tint = tint)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = SonoraColors.SurfaceHighest,
            shape = RoundedCornerShape(12.dp),
        ) {
            val liked = actions.isLiked(song)
            MenuItem(if (liked) "Retirer des titres likés" else "Ajouter aux titres likés",
                if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder) {
                open = false; actions.onToggleLike(song)
            }
            MenuItem("Ajouter à une playlist", Icons.AutoMirrored.Rounded.PlaylistAdd) {
                open = false; actions.onAddToPlaylist(song)
            }
            MenuItem("Ajouter à la file d'attente", Icons.AutoMirrored.Rounded.QueueMusic) {
                open = false; actions.onAddToQueue(song)
            }
            MenuItem("Voir l'album", Icons.Rounded.Album) {
                open = false; actions.onGoToAlbum(song)
            }
            MenuItem("Voir l'artiste", Icons.Rounded.Person) {
                open = false; actions.onGoToArtist(song)
            }
            actions.onRemove?.let { remove ->
                MenuItem("Retirer de cette playlist", Icons.Rounded.RemoveCircleOutline) {
                    open = false; remove(song)
                }
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = { Icon(icon, null, tint = SonoraColors.TextSecondary) },
        onClick = onClick,
    )
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes >= 60) {
        "%d:%02d:%02d".format(minutes / 60, minutes % 60, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

fun formatTotalDuration(ms: Long): String {
    val minutes = ms / 60_000
    return if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
}
