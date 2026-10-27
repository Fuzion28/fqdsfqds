package com.sonora.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonora.music.data.Library
import com.sonora.music.data.Playlist
import com.sonora.music.ui.theme.SonoraColors

@Composable
fun NameDialog(
    title: String,
    initial: String = "",
    confirmLabel: String = "Créer",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SonoraColors.SurfaceHigh,
        shape = RoundedCornerShape(20.dp),
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text("Ma playlist n°1") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SonoraColors.Green,
                    cursorColor = SonoraColors.Green,
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(confirmLabel, color = SonoraColors.Green)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = SonoraColors.TextSecondary) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    songId: Long,
    playlists: List<Playlist>,
    library: Library,
    onDismiss: () -> Unit,
    onPick: (Playlist) -> Unit,
    onCreate: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SonoraColors.SurfaceHigh,
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                "Ajouter à une playlist",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCreate)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GradientArt(
                    listOf(SonoraColors.Green, SonoraColors.GreenDark),
                    Icons.Rounded.Add,
                    Modifier.size(52.dp),
                    RoundedCornerShape(6.dp),
                )
                Spacer(Modifier.width(14.dp))
                Text("Nouvelle playlist", style = MaterialTheme.typography.titleMedium)
            }
            LazyColumn {
                items(playlists, key = { it.id }) { playlist ->
                    val contains = songId in playlist.songIds
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(playlist) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlaylistCover(playlist, library, Modifier.size(52.dp), RoundedCornerShape(6.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${playlist.songIds.size} titres", style = MaterialTheme.typography.bodyMedium, color = SonoraColors.TextSecondary)
                        }
                        if (contains) Icon(Icons.Rounded.Check, null, tint = SonoraColors.Green)
                    }
                }
            }
            Spacer(Modifier.size(16.dp))
        }
    }
}

/** 2x2 mosaic of the first artworks of a playlist. */
@Composable
fun PlaylistCover(
    playlist: Playlist,
    library: Library,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
) {
    val songs = playlist.songIds.mapNotNull { library.songsById[it] }
    val distinct = songs.distinctBy { it.albumId }.take(4)
    if (distinct.size < 4) {
        Artwork(distinct.firstOrNull()?.art, playlist.name, modifier, shape, icon = Icons.Rounded.MusicNote)
    } else {
        Column(modifier.clip(shape)) {
            for (row in 0..1) {
                Row(Modifier.weight(1f)) {
                    for (col in 0..1) {
                        val song = distinct[row * 2 + col]
                        Artwork(song.art, song.album, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}
