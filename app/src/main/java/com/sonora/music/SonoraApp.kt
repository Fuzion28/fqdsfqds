package com.sonora.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.sonora.music.data.ArtworkFetcher

class SonoraApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components {
            add(ArtworkFetcher.ArtKeyer())
            add(ArtworkFetcher.Factory())
        }
        .crossfade(250)
        .build()
}
