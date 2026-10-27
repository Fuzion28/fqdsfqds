package com.sonora.music.data

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val dateAdded: Long,
    val track: Int,
    val year: Int,
    val uri: Uri,
) {
    val art: ArtRequest get() = ArtRequest(uri, albumId)
}

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val year: Int,
    val songs: List<Song>,
) {
    val art: ArtRequest? get() = songs.firstOrNull()?.art
}

data class Artist(
    val id: Long,
    val name: String,
    val songs: List<Song>,
    val albums: List<Album>,
) {
    val art: ArtRequest? get() = songs.firstOrNull()?.art
}

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<Long>,
    val createdAt: Long,
)

/** Snapshot of the device's music library. */
data class Library(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val loaded: Boolean = false,
) {
    val songsById: Map<Long, Song> = songs.associateBy { it.id }
    val albumsById: Map<Long, Album> = albums.associateBy { it.id }
    val artistsById: Map<Long, Artist> = artists.associateBy { it.id }
}

/** Image request for a song's artwork, resolved by [ArtworkFetcher]. */
data class ArtRequest(val songUri: Uri, val albumId: Long)
