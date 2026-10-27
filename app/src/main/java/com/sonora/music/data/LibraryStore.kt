package com.sonora.music.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Persists likes, playlists and listening history. */
class LibraryStore(context: Context) {

    private val prefs = context.getSharedPreferences("sonora_library", Context.MODE_PRIVATE)

    private val _liked = MutableStateFlow(readIds(KEY_LIKED))
    val liked: StateFlow<List<Long>> = _liked.asStateFlow()

    private val _recent = MutableStateFlow(readIds(KEY_RECENT))
    val recent: StateFlow<List<Long>> = _recent.asStateFlow()

    private val _playlists = MutableStateFlow(readPlaylists())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    fun toggleLike(songId: Long) {
        _liked.update { if (songId in it) it - songId else listOf(songId) + it }
        writeIds(KEY_LIKED, _liked.value)
    }

    fun markPlayed(songId: Long) {
        _recent.update { (listOf(songId) + (it - songId)).take(MAX_RECENT) }
        writeIds(KEY_RECENT, _recent.value)
    }

    fun createPlaylist(name: String, initial: List<Long> = emptyList()): Playlist {
        val playlist = Playlist(UUID.randomUUID().toString(), name, initial, System.currentTimeMillis())
        _playlists.update { listOf(playlist) + it }
        writePlaylists()
        return playlist
    }

    fun renamePlaylist(id: String, name: String) = editPlaylist(id) { it.copy(name = name) }

    fun deletePlaylist(id: String) {
        _playlists.update { list -> list.filterNot { it.id == id } }
        writePlaylists()
    }

    fun addToPlaylist(id: String, songId: Long) = editPlaylist(id) {
        if (songId in it.songIds) it else it.copy(songIds = it.songIds + songId)
    }

    fun removeFromPlaylist(id: String, songId: Long) = editPlaylist(id) {
        it.copy(songIds = it.songIds - songId)
    }

    private fun editPlaylist(id: String, transform: (Playlist) -> Playlist) {
        _playlists.update { list -> list.map { if (it.id == id) transform(it) else it } }
        writePlaylists()
    }

    private fun readIds(key: String): List<Long> = runCatching {
        val array = JSONArray(prefs.getString(key, "[]"))
        List(array.length()) { array.getLong(it) }
    }.getOrDefault(emptyList())

    private fun writeIds(key: String, ids: List<Long>) {
        prefs.edit().putString(key, JSONArray(ids).toString()).apply()
    }

    private fun readPlaylists(): List<Playlist> = runCatching {
        val array = JSONArray(prefs.getString(KEY_PLAYLISTS, "[]"))
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            val ids = o.getJSONArray("songs")
            Playlist(
                id = o.getString("id"),
                name = o.getString("name"),
                songIds = List(ids.length()) { ids.getLong(it) },
                createdAt = o.optLong("createdAt"),
            )
        }
    }.getOrDefault(emptyList())

    private fun writePlaylists() {
        val array = JSONArray()
        _playlists.value.forEach { p ->
            array.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("songs", JSONArray(p.songIds))
                    .put("createdAt", p.createdAt)
            )
        }
        prefs.edit().putString(KEY_PLAYLISTS, array.toString()).apply()
    }

    private companion object {
        const val KEY_LIKED = "liked"
        const val KEY_RECENT = "recent"
        const val KEY_PLAYLISTS = "playlists"
        const val MAX_RECENT = 40
    }
}
