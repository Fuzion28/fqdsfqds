package com.sonora.music.data

import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads embedded/album artwork for a song from the MediaStore. */
class ArtworkFetcher(
    private val data: ArtRequest,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val bitmap = loadThumbnail() ?: loadAlbumArt() ?: loadEmbedded()
            ?: error("No artwork for ${data.songUri}")
        DrawableResult(
            drawable = bitmap.toDrawable(options.context.resources),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    private fun loadThumbnail(): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            options.context.contentResolver.loadThumbnail(data.songUri, Size(640, 640), null)
        }.getOrNull()
    }

    private fun loadAlbumArt(): Bitmap? = runCatching {
        val uri = ContentUris.withAppendedId(ALBUM_ART_URI, data.albumId)
        options.context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }.getOrNull()

    private fun loadEmbedded(): Bitmap? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(options.context, data.songUri)
            retriever.embeddedPicture?.let { bytes ->
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            }
        } finally {
            retriever.release()
        }
    }.getOrNull()

    class Factory : Fetcher.Factory<ArtRequest> {
        override fun create(data: ArtRequest, options: Options, imageLoader: ImageLoader): Fetcher =
            ArtworkFetcher(data, options)
    }

    class ArtKeyer : Keyer<ArtRequest> {
        override fun key(data: ArtRequest, options: Options): String = "art:${data.songUri}"
    }

    companion object {
        val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}
