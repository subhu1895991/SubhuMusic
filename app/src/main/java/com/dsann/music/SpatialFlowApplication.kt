package com.dsann.music

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.dsann.music.data.innertube.InnerTubeClient
import com.dsann.music.data.innertube.NewPipeStreamExtractor
import com.dsann.music.util.CacheManager
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SpatialFlowApplication : Application(), ImageLoaderFactory {

    companion object {
        lateinit var instance: SpatialFlowApplication
            private set
    }


    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient { InnerTubeClient.httpClient }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15) // Lower bitmap-cache pressure on older devices
                    .strongReferencesEnabled(true)
                    .weakReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(250 * 1024 * 1024) // 250MB size quota for offline artwork persistent caching
                    .build()
            }
            .crossfade(120) // Shorter crossfade reduces animation work on older GPUs
            .allowHardware(true)
            .allowRgb565(true) // Halves decoded artwork bitmap memory versus ARGB_8888
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    @SuppressLint("VisibleForTests")
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Do not perform disk-cache/client initialization on the process main thread.
        // These components are used by streaming, but are not required to draw the first screen.
        val appContext = applicationContext
        Thread({
            try {
                InnerTubeClient.initialize(appContext)
                NewPipeStreamExtractor.init(appContext)
                CacheManager.clearOldCache(appContext)
            } catch (error: Exception) {
                Log.e("SpatialFlowApplication", "Background startup initialization failed", error)
            } catch (error: LinkageError) {
                // A missing optional runtime component must not terminate the whole app process.
                Log.e("SpatialFlowApplication", "Optional startup dependency could not be loaded; continuing without background initialization", error)
            }
        }, "SpatialFlow-StartupInit").start()

        // Theme reads are small, synchronous preference reads needed for the first activity.
        val prefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", true)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES 
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        // Apply dynamic colors to all activities
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Load saved YouTube Music cookies for logged-in features
        val savedCookie = prefs.getString("yt_cookies", null)
        if (savedCookie != null) {
            InnerTubeClient.cookie = savedCookie
        }
        // Suppress expected Glide warnings for missing album arts
        Glide.init(this, GlideBuilder().setLogLevel(Log.ERROR))

        // Cache cleanup is already scheduled on the startup worker above.
    }
}
