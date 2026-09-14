package com.example.fitapp

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.fitapp.data.BodyMapping
import com.example.fitapp.data.ExerciseCatalog

class FitAppApplication : Application(), ImageLoaderFactory {

    companion object {
        @Volatile
        private var animatedLoader: ImageLoader? = null

        fun getAnimatedImageLoader(context: android.content.Context): ImageLoader {
            return animatedLoader ?: synchronized(this) {
                animatedLoader ?: ImageLoader.Builder(context.applicationContext)
                    .components {
                        if (Build.VERSION.SDK_INT >= 28) {
                            add(ImageDecoderDecoder.Factory())
                        } else {
                            add(GifDecoder.Factory())
                        }
                        add(SvgDecoder.Factory())
                    }
                    .memoryCache {
                        MemoryCache.Builder(context.applicationContext)
                            .maxSizePercent(0.25)
                            .build()
                    }
                    .crossfade(true)
                    .build().also { animatedLoader = it }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ExerciseCatalog.initialize(this)
        BodyMapping.initialize(this)
        coil.Coil.setImageLoader(this)
    }

    override fun newImageLoader(): ImageLoader {
        // Default ImageLoader for all preview cards, thumbnails and lists:
        // Decodes only static first frames (NO animated GifDecoder or ImageDecoderDecoder)
        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
}
