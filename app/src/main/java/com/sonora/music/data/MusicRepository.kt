package com.sonora.music.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    suspend fun load(): Library = withContext(Dispatchers.IO) {
        val songs = querySongs()
        val albums = songs.groupBy { it.albumId }.map { (id, list) ->
            val sorted = list.sortedWith(compareBy({ it.track }, { it.title.lowercase() }))
            Album(
                id = id,
                title = sorted.first().album,
                artist = sorted.groupingBy { it.artist }.eachCount().maxBy { it.value }.key,
                artistId = sorted.first().artistId,
                year = sorted.maxOf { it.year },
                songs = sorted,
            )
        }.sortedBy { it.title.lowercase() }
        val artists = songs.groupBy { it.artistId }.map { (id, list) ->
            Artist(
                id = id,
                name = list.first().artist,
                songs = list.sortedBy { it.title.lowercase() },
                albums = albums.filter { album -> album.songs.any { it.artistId == id } },
            )
        }.sortedBy { it.name.lowercase() }
        Library(songs.sortedBy { it.title.lowercase() }, albums, artists, loaded = true)
    }

    private fun querySongs(): List<Song> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
        )
        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 15000"
        val result = mutableListOf<Song>()
        runCatching {
            context.contentResolver.query(collection, projection, selection, null, null)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val artistIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val artist = c.getString(artistCol)
                        ?.takeUnless { it.isBlank() || it == MediaStore.UNKNOWN_STRING }
                        ?: "Artiste inconnu"
                    val album = c.getString(albumCol)?.takeUnless { it.isBlank() } ?: "Album inconnu"
                    result += Song(
                        id = id,
                        title = c.getString(titleCol)?.takeUnless { it.isBlank() } ?: "Sans titre",
                        artist = artist,
                        artistId = c.getLong(artistIdCol),
                        album = album,
                        albumId = c.getLong(albumIdCol),
                        durationMs = c.getLong(durationCol),
                        dateAdded = c.getLong(dateCol),
                        // TRACK is encoded as disc * 1000 + track.
                        track = c.getInt(trackCol) % 1000,
                        year = c.getInt(yearCol),
                        uri = ContentUris.withAppendedId(collection, id),
                    )
                }
            }
        }
        return result
    }
}
