package com.sonora.music.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.sonora.music.data.ArtworkFetcher
import com.sonora.music.data.Song
import android.content.ContentUris
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val currentId: Long? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<Long> = emptyList(),
    val currentIndex: Int = 0,
    val previousId: Long? = null,
    val nextId: Long? = null,
)

data class Progress(val positionMs: Long = 0, val durationMs: Long = 0) {
    val fraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

/** Connects the UI to [PlaybackService] through a [MediaController]. */
class PlayerConnection(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private var ticker: Job? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
        }
    }

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val c = runCatching { future.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(listener)
            publish(c)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun publish(player: Player) {
        _state.value = PlayerState(
            currentId = player.currentMediaItem?.mediaId?.toLongOrNull(),
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            queue = List(player.mediaItemCount) { player.getMediaItemAt(it).mediaId.toLongOrNull() ?: -1L },
            currentIndex = player.currentMediaItemIndex,
            previousId = player.idAt(player.previousMediaItemIndex),
            nextId = player.idAt(player.nextMediaItemIndex),
        )
        updateProgress(player)
        if (player.isPlaying) startTicker() else ticker?.cancel()
    }

    private fun Player.idAt(index: Int): Long? =
        if (index in 0 until mediaItemCount) getMediaItemAt(index).mediaId.toLongOrNull() else null

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                controller?.let { updateProgress(it) }
                delay(250)
            }
        }
    }

    private fun updateProgress(player: Player) {
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        _progress.value = Progress(player.currentPosition.coerceAtLeast(0), duration)
    }

    fun play(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        c.shuffleModeEnabled = shuffle
        val start = if (shuffle) songs.indices.random() else startIndex.coerceIn(songs.indices)
        c.setMediaItems(songs.map { it.toMediaItem() }, start, 0L)
        c.prepare()
        c.play()
    }

    fun addToQueue(song: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) {
            play(listOf(song))
        } else {
            c.addMediaItem(c.currentMediaItemIndex + 1, song.toMediaItem())
        }
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition(0)
            c.play()
        }
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    /** Skips to the adjacent item regardless of the current position (used by swipes). */
    fun nextItem() {
        controller?.seekToNextMediaItem()
    }

    fun previousItem() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.let {
            it.seekTo(positionMs)
            updateProgress(it)
        }
    }

    fun skipToQueueIndex(index: Int) {
        controller?.let {
            it.seekToDefaultPosition(index)
            it.play()
        }
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun release() {
        ticker?.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }

    private fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(ContentUris.withAppendedId(ArtworkFetcher.ALBUM_ART_URI, albumId))
                .build()
        )
        .build()
}
