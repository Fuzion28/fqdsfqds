package com.sonora.music.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonora.music.data.Library
import com.sonora.music.data.LibraryStore
import com.sonora.music.data.MusicRepository
import com.sonora.music.data.Playlist
import com.sonora.music.data.Song
import com.sonora.music.playback.PlayerConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MusicRepository(app)
    val store = LibraryStore(app)
    val player = PlayerConnection(app)

    private val _library = MutableStateFlow(Library())
    val library: StateFlow<Library> = _library.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        viewModelScope.launch {
            player.state.map { it.currentId }.distinctUntilChanged().collect { id ->
                if (id != null) store.markPlayed(id)
            }
        }
    }

    fun loadLibrary() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            _library.value = repository.load()
            _refreshing.value = false
        }
    }

    fun play(songs: List<Song>, index: Int = 0) = player.play(songs, index)

    fun shuffle(songs: List<Song>) = player.play(songs, shuffle = true)

    fun toggleLike(song: Song) = store.toggleLike(song.id)

    fun createPlaylist(name: String, firstSong: Song? = null): Playlist =
        store.createPlaylist(name.trim().ifEmpty { "Ma playlist" }, listOfNotNull(firstSong?.id))

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
