package com.dsann.music.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContentUris
import android.net.Uri
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log

import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.dsann.music.R
import com.dsann.music.data.innertube.NewPipeStreamExtractor
import com.dsann.music.model.SongItem
import com.dsann.music.service.AudioPlaybackService
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.datasource.cache.ContentMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object SongDownloader {
    private const val TAG = "SongDownloader"
    private const val CHANNEL_ID = "spatialflow_downloads"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    fun cleanTitle(title: String): String {
        var clean = title
        val regexes = listOf(
            "(?i)\\[official music video\\]",
            "(?i)\\(official music video\\)",
            "(?i)\\[official video\\]",
            "(?i)\\(official video\\)",
            "(?i)\\[lyrics\\]",
            "(?i)\\(lyrics\\)",
            "(?i)\\[lyric video\\]",
            "(?i)\\(lyric video\\)",
            "(?i)\\[official audio\\]",
            "(?i)\\(official audio\\)",
            "(?i)official music video",
            "(?i)official video",
            "(?i)lyric video",
            "(?i)\\[video\\]",
            "(?i)\\(video\\)",
            "(?i)\\[official release\\]",
            "(?i)\\(official release\\)"
        )
        for (regex in regexes) {
            clean = clean.replace(Regex(regex), "")
        }
        // Remove trailing dashes, slashes, and spaces
        return clean.trim().replace(Regex("\\s+"), " ").removeSuffix("-").removeSuffix("/").trim()
    }

    fun cleanArtist(artist: String): String {
        return artist.replace(Regex("(?i)\\s*-\\s*Topic"), "").trim().replace(Regex("\\s+"), " ")
    }

    fun cleanAlbum(title: String): String {
        var clean = cleanTitle(title)
        val albumRegexes = listOf(
            "(?i)\\s*\\((slowed|reverb|speed up|sped up|remix|slowed\\s*\\+\\s*reverb|slowed\\s*and\\s*reverb)\\)",
            "(?i)\\s*\\[(slowed|reverb|speed up|sped up|remix|slowed\\s*\\+\\s*reverb|slowed\\s*and\\s*reverb)\\]"
        )
        for (regex in albumRegexes) {
            clean = clean.replace(Regex(regex), "")
        }
        return clean.trim().replace(Regex("\\s+"), " ")
    }

    private class DownloadTask {
        @Volatile var job: Job? = null
        @Volatile var connection: HttpURLConnection? = null
        @Volatile var call: okhttp3.Call? = null
        @Volatile var outputFile: File? = null
        @Volatile var cancelled: Boolean = false
    }

    private data class CachedStreamCandidate(
        val cacheKey: String,
        val spans: List<CacheSpan>,
        val contentLength: Long,
        val mimeType: String?,
        val cachedBytes: Long,
        val isComplete: Boolean
    )

    private val activeDownloads = ConcurrentHashMap<String, DownloadTask>()

    fun isDownloading(videoId: String?): Boolean =
        !videoId.isNullOrBlank() && activeDownloads.containsKey(videoId)

    private const val PREF_DOWNLOADED_TRACK_URIS = "spatialflow_downloaded_track_uris"

    /**
     * Resolve an app-downloaded audio file before trying Media3's online stream resolver.
     * Video-ID mappings are preferred; the SUMITX folder and legacy folder names from earlier installs
     * provide a migration path for downloads created before mappings were recorded.
     */
    fun findDownloadedUri(
        context: Context,
        videoId: String?,
        title: String,
        artist: String
    ): Uri? {
        if (videoId.isNullOrBlank()) return null
        val prefs = context.getSharedPreferences(PREF_DOWNLOADED_TRACK_URIS, Context.MODE_PRIVATE)
        val mappingKey = "uri_$videoId"
        val mapped = prefs.getString(mappingKey, null)?.let { raw ->
            runCatching { Uri.parse(raw) }.getOrNull()
        }
        if (mapped != null && isUriReadable(context, mapped)) return mapped
        if (mapped != null) prefs.edit().remove(mappingKey).apply()

        val baseName = "${cleanTitle(title)} - ${cleanArtist(artist)}"
            .replace(Regex("[^a-zA-Z0-9._\\s()\\[\\]-]"), "_")
        val extensions = listOf("m4a", "mp3", "webm", "opus", "ogg", "flac", "aac", "wav")
        for (extension in extensions) {
            val filename = "$baseName.$extension"
            val found = findSumitXMediaStoreUri(context, filename)
                ?: findLegacyDownloadedFile(filename)
            if (found != null && isUriReadable(context, found)) {
                prefs.edit().putString(mappingKey, found.toString()).apply()
                return found
            }
        }
        return null
    }

    /** Record the permanent MediaStore URI immediately after the file is published. */
    private fun rememberDownloadedSong(context: Context, videoId: String, outputFile: File) {
        if (videoId.isBlank()) return
        val uri = findSumitXMediaStoreUri(context, outputFile.name)
            ?: if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && outputFile.isFile) {
                Uri.fromFile(outputFile)
            } else null
        if (uri != null) {
            context.getSharedPreferences(PREF_DOWNLOADED_TRACK_URIS, Context.MODE_PRIVATE)
                .edit()
                .putString("uri_$videoId", uri.toString())
                .putString("name_$videoId", outputFile.name)
                .apply()
        } else {
            Log.w(TAG, "Download completed but permanent MediaStore URI was not found for $videoId")
        }
    }

    private fun findSumitXMediaStoreUri(context: Context, filename: String): Uri? {
        val projection = mutableListOf(MediaStore.Audio.Media._ID).apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.DATA)
            } else {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            }
        }.toTypedArray()

        return try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.DISPLAY_NAME} = ?",
                arrayOf(filename),
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val locationColumn = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                } else {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                }
                while (cursor.moveToNext()) {
                    val location = if (locationColumn >= 0) cursor.getString(locationColumn).orEmpty() else ""
                    val normalizedLocation = location.replace('\\', '/').lowercase()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (!normalizedLocation.contains("sumitx/") &&
                            !normalizedLocation.contains("subhumusic/") &&
                            !normalizedLocation.contains("spatialflow/") &&
                            !normalizedLocation.contains("d.s music/")
                        ) continue
                    } else {
                        if (!normalizedLocation.contains("/sumitx/") &&
                            !normalizedLocation.contains("/subhumusic/") &&
                            !normalizedLocation.contains("/spatialflow/") &&
                            !normalizedLocation.contains("/d.s music/")
                        ) continue
                    }
                    return@use ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        cursor.getLong(idColumn)
                    )
                }
                null
            }
        } catch (error: Exception) {
            Log.w(TAG, "Could not resolve published download $filename", error)
            null
        }
    }

    private fun findLegacyDownloadedFile(filename: String): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return null
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = listOf("SUMITX", "SubhuMusic", "D.S Music", "SpatialFlow")
            .asSequence()
            .map { folder -> File(File(downloads, folder), filename) }
            .firstOrNull { it.isFile }
        return file?.let(Uri::fromFile)
    }

    private fun isUriReadable(context: Context, uri: Uri): Boolean = try {
        when (uri.scheme) {
            "file" -> uri.path?.let { File(it).isFile } == true
            "content" -> context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                it.length != 0L
            } == true
            else -> false
        }
    } catch (_: Exception) {
        false
    }

    /**
     * Tapping Download twice for the same track toggles it off. We keep the
     * connection handle so cancellation also interrupts a blocked HTTP read.
     */
    fun downloadSong(context: Context, song: SongItem) {
        val videoId = song.videoId
        if (videoId.isNullOrBlank()) {
            com.dsann.music.ui.SnackbarController.showMessage("Local songs are already offline")
            return
        }

        if (activeDownloads.containsKey(videoId)) {
            cancelDownload(context, song)
            return
        }

        val task = DownloadTask()
        val job = scope.launch(start = CoroutineStart.LAZY) {
            performDownload(context.applicationContext, song, videoId, task)
        }
        task.job = job

        val alreadyActive = activeDownloads.putIfAbsent(videoId, task)
        if (alreadyActive != null) {
            job.cancel()
            // The second tap is a cancellation request for the active task.
            cancelDownload(context, song)
            return
        }

        _downloadProgress.value += (videoId to 0)
        job.invokeOnCompletion {
            if (task.cancelled) task.outputFile?.delete()
            activeDownloads.remove(videoId, task)
            _downloadProgress.value -= videoId
        }
        job.start()
    }

    fun cancelDownload(context: Context, song: SongItem) {
        val videoId = song.videoId ?: return
        val task = activeDownloads[videoId] ?: return
        if (task.cancelled) return
        task.cancelled = true
        // HttpURLConnection.disconnect() can perform TLS socket shutdown synchronously.
        // On Android 6 this throws NetworkOnMainThreadException when cancellation is
        // triggered from a UI click, so always disconnect from an IO coroutine.
        // Both disconnect and Job.cancel can run synchronous HTTP/TLS cancellation handlers.
        // Schedule both off the UI thread so Android 6 StrictMode cannot crash the app.
        scope.launch(Dispatchers.IO) {
            runCatching { task.connection?.disconnect() }
            runCatching { task.call?.cancel() }
        }
        scope.launch(Dispatchers.IO) {
            task.job?.cancel(CancellationException("Download cancelled by user"))
        }
        cancelNotification(context.applicationContext, song.id.toInt().coerceAtLeast(1))
        com.dsann.music.ui.SnackbarController.showMessage("Download cancelled: ${cleanTitle(song.title)}")
    }

    private suspend fun performDownload(
        context: Context,
        song: SongItem,
        videoId: String,
        task: DownloadTask
    ) {
        val notificationId = song.id.toInt().coerceAtLeast(1)
        val cleanTitleStr = cleanTitle(song.title)
        val cleanArtistStr = cleanArtist(song.artist)
        val coroutineContext = currentCoroutineContext()

        try {
            // Do not let a stale player UI state launch a duplicate download. This check runs
            // inside the IO download job, never on the main thread.
            if (isSongDownloaded(context, song)) {
                cancelNotification(context, notificationId)
                withContext(Dispatchers.Main) {
                    com.dsann.music.ui.SnackbarController.showMessage(
                        "Already downloaded: $cleanTitleStr",
                        iconResId = R.drawable.ic_downloaded
                    )
                }
                return
            }

            updateNotification(context, cleanTitleStr, 0, notificationId)

            // Reuse playback cache only when playback and download are both in Auto mode
            // with the same target bitrate. Otherwise resolve a fresh stream so the download
            // format and bitrate selectors are actually applied (cached spans have no MIME
            // metadata, so they cannot safely satisfy a different format choice).
            val settings = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            val playbackFormat = NewPipeStreamExtractor.normalizeAudioFormat(
                settings.getString("audio_format", "auto")
            )
            val downloadFormat = NewPipeStreamExtractor.normalizeAudioFormat(
                settings.getString("download_audio_format", "auto")
            )
            val playbackBitrate = NewPipeStreamExtractor.normalizeAudioQuality(
                settings.getString("audio_quality", "256 kbps")
            )
            val downloadBitrate = NewPipeStreamExtractor.normalizeAudioQuality(
                settings.getString("download_bitrate", "256 kbps")
            )
            val cacheMatchesDownloadSelection =
                playbackFormat == "auto" &&
                downloadFormat == "auto" &&
                playbackBitrate == downloadBitrate
            val cachedStream = if (cacheMatchesDownloadSelection) {
                findCachedStream(context, song.id.toString())
            } else {
                null
            }
            coroutineContext.ensureActive()

            val outputFile: File
            if (cachedStream != null) {
                if (!cachedStream.isComplete) {
                    cancelNotification(context, notificationId)
                    val cacheSize = android.text.format.Formatter.formatFileSize(context, cachedStream.cachedBytes)
                    val totalSize = if (cachedStream.contentLength > 0L) {
                        android.text.format.Formatter.formatFileSize(context, cachedStream.contentLength)
                    } else {
                        "unknown total size"
                    }
                    withContext(Dispatchers.Main) {
                        com.dsann.music.ui.SnackbarController.showMessage(
                            "Only $cacheSize of this song is cached ($totalSize). No network download was started; play it through to cache the full track."
                        )
                    }
                    return
                }

                val extension = extensionForMimeType(cachedStream.mimeType)
                outputFile = AudioFileManager.createOutputFile(
                    context,
                    "$cleanTitleStr - $cleanArtistStr.$extension"
                )
                task.outputFile = outputFile
                copyCachedStream(cachedStream, outputFile, coroutineContext)
                _downloadProgress.value += (videoId to 90)
                updateNotification(context, cleanTitleStr, 90, notificationId)
            } else {
                // No cache entry for this track: use the standard online download path.
                val downloadStream = NewPipeStreamExtractor.getDownloadStreamData(
                    videoId,
                    allowFormatChoice = true
                )
                coroutineContext.ensureActive()
                val streamUrl = downloadStream?.url
                if (streamUrl.isNullOrBlank()) {
                    throw IOException("Failed to extract download URL")
                }

                val extension = when {
                    downloadStream?.mimeType?.startsWith("audio/mp4", ignoreCase = true) == true -> "m4a"
                    downloadStream?.mimeType?.startsWith("audio/mpeg", ignoreCase = true) == true -> "mp3"
                    downloadStream?.mimeType?.contains("webm", ignoreCase = true) == true -> "webm"
                    downloadStream?.mimeType?.contains("opus", ignoreCase = true) == true -> "opus"
                    downloadStream?.mimeType?.contains("ogg", ignoreCase = true) == true -> "ogg"
                    downloadStream?.mimeType?.contains("flac", ignoreCase = true) == true -> "flac"
                    downloadStream?.mimeType?.contains("aac", ignoreCase = true) == true -> "aac"
                    downloadStream?.mimeType?.contains("wav", ignoreCase = true) == true -> "wav"
                    else -> "m4a"
                }
                outputFile = AudioFileManager.createOutputFile(
                    context,
                    "$cleanTitleStr - $cleanArtistStr.$extension"
                )
                task.outputFile = outputFile

                downloadNetworkStream(
                    context = context,
                    title = cleanTitleStr,
                    streamUrl = streamUrl,
                    expectedBytes = downloadStream?.contentLength,
                    outputFile = outputFile,
                    task = task,
                    videoId = videoId,
                    notificationId = notificationId,
                    coroutineContext = coroutineContext
                )
            }

            coroutineContext.ensureActive()

            var artworkBytes: ByteArray? = null
            if (cachedStream != null) {
                // Cached downloads stay fully offline; only use artwork already saved locally.
                val cachedArtwork = AudioPlaybackService.getCachedMediaArtworkFile(context, song.id.toString())
                if (cachedArtwork.isFile && cachedArtwork.length() > 0L) {
                    artworkBytes = runCatching { cachedArtwork.readBytes() }.getOrNull()
                }
            } else {
                val thumbnailUrl = song.thumbnailUrl
                if (!thumbnailUrl.isNullOrEmpty()) {
                    try {
                        val future = Glide.with(context)
                            .asBitmap()
                            .load(thumbnailUrl)
                            .submit()
                        val originalBitmap = future.get()
                        if (originalBitmap != null) {
                            val size = originalBitmap.width.coerceAtMost(originalBitmap.height)
                            val x = (originalBitmap.width - size) / 2
                            val y = (originalBitmap.height - size) / 2
                            val croppedBitmap = Bitmap.createBitmap(originalBitmap, x, y, size, size)
                            val stream = ByteArrayOutputStream()
                            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                            artworkBytes = stream.toByteArray()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to fetch and crop thumbnail", e)
                    }
                }
            }

            coroutineContext.ensureActive()

            val metadataEmbedded = embedMetadataSafely(
                outputFile = outputFile,
                title = cleanTitleStr,
                artist = cleanArtistStr,
                album = song.albumName,
                videoId = videoId,
                fromCache = cachedStream != null,
                artworkBytes = artworkBytes
            )

            coroutineContext.ensureActive()
            if (!AudioFileManager.scanFile(context, outputFile, cleanTitleStr, cleanArtistStr, song.albumName)) {
                throw IOException("Could not publish the downloaded audio to the music library")
            }
            rememberDownloadedSong(context, videoId, outputFile)
            showFinishedNotification(context, cleanTitleStr, cleanArtistStr, notificationId)
            _downloadProgress.value += (videoId to 100)

            withContext(Dispatchers.Main) {
                com.dsann.music.ui.SnackbarController.showMessage(
                    when {
                        cachedStream != null && metadataEmbedded -> "Saved cached audio offline: $cleanTitleStr"
                        cachedStream != null -> "Saved cached audio offline: $cleanTitleStr (metadata tags unavailable)"
                        metadataEmbedded -> "Downloaded with embedded metadata: $cleanTitleStr"
                        else -> "Downloaded: $cleanTitleStr (this audio format could not store embedded tags)"
                    },
                    iconResId = R.drawable.ic_downloaded
                )
            }
        } catch (e: com.dsann.music.data.innertube.AudioFormatSelectionCancelledException) {
            task.outputFile?.delete()
            cancelNotification(context, notificationId)
            withContext(Dispatchers.Main) {
                com.dsann.music.ui.SnackbarController.showMessage(
                    "Download cancelled: no format selected"
                )
            }
        } catch (e: CancellationException) {
            task.cancelled = true
            task.outputFile?.delete()
            cancelNotification(context, notificationId)
            throw e
        } catch (e: Exception) {
            task.outputFile?.delete()
            Log.e(TAG, "Download failed for ${song.title}", e)
            cancelNotification(context, notificationId)
            if (!task.cancelled) {
                withContext(Dispatchers.Main) {
                    com.dsann.music.ui.SnackbarController.showMessage("Download failed: ${e.message}")
                }
            }
        } finally {
            task.connection?.disconnect()
            task.connection = null
            task.call?.cancel()
            task.call = null
            if (task.cancelled) task.outputFile?.delete()
            _downloadProgress.value -= videoId
        }
    }


    /**
     * Downloads with browser-like headers, buffered IO, exact response-length validation,
     * and bounded retries for transient read/network failures.
     */
    private suspend fun downloadNetworkStream(
        context: Context,
        title: String,
        streamUrl: String,
        expectedBytes: Long?,
        outputFile: File,
        task: DownloadTask,
        videoId: String,
        notificationId: Int,
        coroutineContext: CoroutineContext
    ) {
        var lastError: Exception? = null
        for (attempt in 1..3) {
            var call: okhttp3.Call? = null
            var response: okhttp3.Response? = null
            try {
                coroutineContext.ensureActive()
                if (outputFile.exists() && !outputFile.delete()) {
                    throw IOException("Could not clear an incomplete audio download")
                }
                outputFile.parentFile?.let { parent ->
                    if (!parent.exists() && !parent.mkdirs()) {
                        throw IOException("Could not create the download directory")
                    }
                }

                // Google video streams are fetched as 1 MiB range requests, pipelining the next
                // range while the current range is read. This avoids YouTube's playback-speed
                // throttling on a single long, open-ended GET without changing audio quality.
                call = YouTubeRangeDownloadClient.newCall(streamUrl)
                task.call = call
                response = call.execute()
                if (response.code != HttpURLConnection.HTTP_OK) {
                    throw IOException("Download server returned HTTP ${response.code}")
                }

                val declaredLength = response.body.contentLength().takeIf { it > 0L }
                val progressLength = declaredLength ?: expectedBytes?.takeIf { it > 0L }
                var downloadedBytes = 0L
                var lastProgressUpdate = 0L
                response.body.byteStream().use { input ->
                    BufferedOutputStream(FileOutputStream(outputFile), 64 * 1024).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val bytesRead = input.read(buffer)
                            if (bytesRead < 0) break
                            if (bytesRead == 0) continue
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead.toLong()

                            if (progressLength != null && progressLength > 0L) {
                                val progress = ((downloadedBytes.toDouble() / progressLength.toDouble()) * 88.0)
                                    .toInt().coerceIn(1, 88)
                                if (_downloadProgress.value[videoId] != progress) {
                                    _downloadProgress.value += (videoId to progress)
                                }
                                val now = System.currentTimeMillis()
                                if (now - lastProgressUpdate > 400L) {
                                    lastProgressUpdate = now
                                    updateNotification(context, title, progress, notificationId)
                                }
                            }
                        }
                    }
                }

                if (downloadedBytes <= 0L) {
                    throw IOException("The server returned an empty audio stream")
                }
                if (declaredLength != null && downloadedBytes != declaredLength) {
                    throw IOException(
                        "Incomplete audio download: received $downloadedBytes of $declaredLength bytes"
                    )
                }
                Log.i(TAG, "Downloaded $downloadedBytes bytes using range-enabled mode on attempt $attempt")
                return
            } catch (cancelled: CancellationException) {
                outputFile.delete()
                throw cancelled
            } catch (error: Exception) {
                lastError = error
                outputFile.delete()
                coroutineContext.ensureActive()
                Log.w(TAG, "Download attempt $attempt/3 failed for $title", error)
                if (attempt < 3) {
                    _downloadProgress.value += (videoId to 0)
                    delay(attempt * 500L)
                }
            } finally {
                response?.close()
                call?.cancel()
                if (task.call === call) task.call = null
                if (task.connection != null) {
                    runCatching { task.connection?.disconnect() }
                    task.connection = null
                }
            }
        }
        throw IOException("Audio download failed after 3 attempts: ${lastError?.message ?: "unknown network error"}", lastError)
    }

    /**
     * Tag a temporary copy and replace the original only after metadata commit succeeds.
     * Unsupported taggers therefore cannot corrupt the downloaded audio stream.
     */
    private fun embedMetadataSafely(
        outputFile: File,
        title: String,
        artist: String,
        album: String?,
        videoId: String,
        fromCache: Boolean,
        artworkBytes: ByteArray?
    ): Boolean {
        val extension = outputFile.extension
        if (extension.equals("webm", ignoreCase = true)) {
            return embedWebmMetadataSafely(outputFile, title, artist, album, videoId, fromCache)
        }
        val taggedFile = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}.tagging.$extension")
        var backupFile: File? = null
        try {
            if (taggedFile.exists() && !taggedFile.delete()) {
                throw IOException("Could not clear the metadata staging file")
            }
            outputFile.copyTo(taggedFile, overwrite = true)
            val audioFile = org.jaudiotagger.audio.AudioFileIO.read(taggedFile)
            val isMp4Container = extension.equals("m4a", ignoreCase = true) ||
                extension.equals("mp4", ignoreCase = true) ||
                extension.equals("m4p", ignoreCase = true) ||
                extension.equals("m4b", ignoreCase = true)

            val tag = if (isMp4Container) {
                (audioFile.tag as? org.jaudiotagger.tag.mp4.Mp4Tag)
                    ?: org.jaudiotagger.tag.mp4.Mp4Tag().also { audioFile.setTag(it) }
            } else {
                audioFile.tagOrCreateAndSetDefault
            }

            val sourceComment = if (fromCache) {
                "Exported from SUMITX cached audio (video ID: $videoId)"
            } else {
                "Downloaded from YouTube Music (video ID: $videoId)"
            }

            if (tag is org.jaudiotagger.tag.mp4.Mp4Tag) {
                // Use native MP4 atoms for M4A/AAC. This is more reliable across Android
                // media scanners than relying only on generic cross-format field mappings.
                tag.setField(tag.createField(org.jaudiotagger.tag.mp4.Mp4FieldKey.TITLE, title))
                tag.setField(tag.createField(org.jaudiotagger.tag.mp4.Mp4FieldKey.ARTIST, artist))
                runCatching {
                    tag.setField(tag.createField(org.jaudiotagger.tag.mp4.Mp4FieldKey.ALBUM_ARTIST, artist))
                }.onFailure { Log.d(TAG, "MP4 album artist tag not supported: ${it.message}") }
                album?.takeIf { it.isNotBlank() }?.let { albumTitle ->
                    runCatching {
                        tag.setField(tag.createField(org.jaudiotagger.tag.mp4.Mp4FieldKey.ALBUM, albumTitle.trim()))
                    }.onFailure { Log.d(TAG, "MP4 album tag not supported: ${it.message}") }
                }
                runCatching {
                    tag.setField(tag.createField(org.jaudiotagger.tag.mp4.Mp4FieldKey.COMMENT, sourceComment))
                }.onFailure { Log.d(TAG, "MP4 comment tag not supported: ${it.message}") }
            } else {
                // TITLE and ARTIST are required. Optional fields are isolated because some formats
                // don't support every generic jaudiotagger key.
                tag.setField(org.jaudiotagger.tag.FieldKey.TITLE, title)
                tag.setField(org.jaudiotagger.tag.FieldKey.ARTIST, artist)
                runCatching { tag.setField(org.jaudiotagger.tag.FieldKey.ALBUM_ARTIST, artist) }
                    .onFailure { Log.d(TAG, "Album artist tag not supported by ${extension}: ${it.message}") }
                album?.takeIf { it.isNotBlank() }?.let { albumTitle ->
                    runCatching { tag.setField(org.jaudiotagger.tag.FieldKey.ALBUM, albumTitle.trim()) }
                        .onFailure { Log.d(TAG, "Album tag not supported by ${extension}: ${it.message}") }
                }
                runCatching { tag.setField(org.jaudiotagger.tag.FieldKey.COMMENT, sourceComment) }
                    .onFailure { Log.d(TAG, "Comment tag not supported by ${extension}: ${it.message}") }
            }

            if (artworkBytes != null && artworkBytes.isNotEmpty()) {
                runCatching {
                    if (tag is org.jaudiotagger.tag.mp4.Mp4Tag) {
                        tag.deleteArtworkField()
                        tag.setField(tag.createArtworkField(artworkBytes))
                    } else {
                        val artwork = org.jaudiotagger.tag.images.StandardArtwork()
                        artwork.binaryData = artworkBytes
                        artwork.mimeType = "image/jpeg"
                        tag.deleteArtworkField()
                        tag.setField(artwork)
                    }
                }.onFailure { Log.w(TAG, "Could not embed cover art in ${extension}: ${it.message}") }
            }

            audioFile.commit()
            if (!taggedFile.isFile || taggedFile.length() <= 0L) {
                throw IOException("Metadata write produced an empty file")
            }

            // Verify the written tags by reading the staged audio back.
            val writtenTag = org.jaudiotagger.audio.AudioFileIO.read(taggedFile).tag
            val writtenTitle = if (writtenTag is org.jaudiotagger.tag.mp4.Mp4Tag) {
                writtenTag.getFirst(org.jaudiotagger.tag.mp4.Mp4FieldKey.TITLE)
            } else {
                writtenTag.getFirst(org.jaudiotagger.tag.FieldKey.TITLE)
            }
            val writtenArtist = if (writtenTag is org.jaudiotagger.tag.mp4.Mp4Tag) {
                writtenTag.getFirst(org.jaudiotagger.tag.mp4.Mp4FieldKey.ARTIST)
            } else {
                writtenTag.getFirst(org.jaudiotagger.tag.FieldKey.ARTIST)
            }
            if (writtenTitle.isNullOrBlank() || writtenArtist.isNullOrBlank()) {
                throw IOException("Metadata verification failed: title or artist was not persisted")
            }

            backupFile = File(outputFile.parentFile, "${outputFile.name}.raw-backup")
            if (backupFile.exists() && !backupFile.delete()) {
                throw IOException("Could not clear the audio backup file")
            }
            if (!outputFile.renameTo(backupFile)) {
                throw IOException("Could not preserve original audio before metadata writing")
            }
            if (!taggedFile.renameTo(outputFile)) {
                backupFile.renameTo(outputFile)
                throw IOException("Could not install the tagged audio file")
            }
            backupFile.delete()
            return true
        } catch (error: Exception) {
            Log.w(TAG, "Metadata embedding failed safely for ${outputFile.name}", error)
            taggedFile.delete()
            if (backupFile?.exists() == true && !outputFile.exists()) {
                backupFile.renameTo(outputFile)
            }
            return false
        }
    }

    /**
     * WebM/Opus is not handled by the usual audio-tag writer. Write standard Matroska
     * SimpleTag elements into a WebM Segment so that tags are embedded in the media file,
     * rather than relying only on MediaStore's display fields.
     */
    private fun embedWebmMetadataSafely(
        outputFile: File,
        title: String,
        artist: String,
        album: String?,
        videoId: String,
        fromCache: Boolean
    ): Boolean {
        val stagedFile = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}.tagging.webm")
        var backupFile: File? = null
        try {
            if (stagedFile.exists() && !stagedFile.delete()) {
                throw IOException("Could not clear the WebM metadata staging file")
            }
            outputFile.copyTo(stagedFile, overwrite = true)
            val sourceComment = if (fromCache) {
                "Exported from SUMITX cached audio (video ID: $videoId)"
            } else {
                "Downloaded from YouTube Music (video ID: $videoId)"
            }
            val tags = buildWebmMetadataTags(title, artist, album, sourceComment)
            appendWebmMetadataTags(stagedFile, tags)

            backupFile = File(outputFile.parentFile, "${outputFile.name}.raw-backup")
            if (backupFile.exists() && !backupFile.delete()) {
                throw IOException("Could not clear the WebM audio backup file")
            }
            if (!outputFile.renameTo(backupFile)) {
                throw IOException("Could not preserve original WebM audio before metadata writing")
            }
            if (!stagedFile.renameTo(outputFile)) {
                backupFile.renameTo(outputFile)
                throw IOException("Could not install the tagged WebM audio file")
            }
            backupFile.delete()
            return true
        } catch (error: Exception) {
            Log.w(TAG, "WebM metadata embedding failed safely for ${outputFile.name}", error)
            stagedFile.delete()
            if (backupFile?.exists() == true && !outputFile.exists()) {
                backupFile.renameTo(outputFile)
            }
            return false
        }
    }

    private fun buildWebmMetadataTags(
        title: String,
        artist: String,
        album: String?,
        comment: String
    ): ByteArray {
        val entries = ArrayList<ByteArray>()

        fun addTag(name: String, value: String?) {
            if (value.isNullOrBlank()) return
            val nameElement = webmElement(byteArrayOf(0x45, 0xA3.toByte()), name.toByteArray(Charsets.UTF_8))
            val valueElement = webmElement(byteArrayOf(0x44, 0x87.toByte()), value.toByteArray(Charsets.UTF_8))
            val simpleTag = webmElement(byteArrayOf(0x67, 0xC8.toByte()), nameElement + valueElement)
            // TargetTypeValue 50 is the default track-level tag target; omitting a TrackUID
            // makes the descriptive tags apply to the audio track.
            val targets = webmElement(
                byteArrayOf(0x63, 0xC0.toByte()),
                webmElement(byteArrayOf(0x68, 0xCA.toByte()), byteArrayOf(50))
            )
            entries.add(webmElement(byteArrayOf(0x73, 0x73.toByte()), targets + simpleTag))
        }

        addTag("TITLE", title)
        addTag("ARTIST", artist)
        addTag("ALBUM", album)
        addTag("COMMENT", comment)
        return webmElement(
            byteArrayOf(0x12, 0x54, 0xC3.toByte(), 0x67),
            entries.fold(ByteArray(0)) { output, entry -> output + entry }
        )
    }

    private fun webmElement(id: ByteArray, payload: ByteArray): ByteArray =
        id + encodeWebmSize(payload.size.toLong()) + payload

    private fun encodeWebmSize(value: Long, fixedWidth: Int? = null): ByteArray {
        if (value < 0L) throw IOException("Invalid EBML element size")
        val width = fixedWidth ?: (1..8).firstOrNull { candidate ->
            value <= (1L shl (7 * candidate)) - 2L
        } ?: throw IOException("EBML element is too large")
        if (width !in 1..8 || value > (1L shl (7 * width)) - 2L) {
            throw IOException("EBML size does not fit its existing field")
        }
        val encoded = ByteArray(width)
        var remaining = value
        for (index in width - 1 downTo 0) {
            encoded[index] = (remaining and 0xFFL).toByte()
            remaining = remaining ushr 8
        }
        encoded[0] = (encoded[0].toInt() or (1 shl (8 - width))).toByte()
        return encoded
    }

    private fun appendWebmMetadataTags(file: File, tags: ByteArray) {
        java.io.RandomAccessFile(file, "rw").use { randomFile ->
            val fileLength = randomFile.length()
            if (fileLength < 8L) throw IOException("Downloaded WebM file is too small to tag")

            // The Segment header occurs near the beginning of a WebM file. Do not scan the
            // audio clusters indefinitely or risk matching arbitrary payload bytes later on.
            val scanLimit = minOf(fileLength - 4L, 1024L * 1024L)
            var segmentOffset = -1L
            var offset = 0L
            while (offset <= scanLimit) {
                randomFile.seek(offset)
                if (randomFile.readUnsignedByte() == 0x18 &&
                    randomFile.readUnsignedByte() == 0x53 &&
                    randomFile.readUnsignedByte() == 0x80 &&
                    randomFile.readUnsignedByte() == 0x67
                ) {
                    segmentOffset = offset
                    break
                }
                offset++
            }
            if (segmentOffset < 0L) throw IOException("WebM Segment header was not found")

            val sizeOffset = segmentOffset + 4L
            randomFile.seek(sizeOffset)
            val first = randomFile.readUnsignedByte()
            var width = 1
            var marker = 0x80
            while (width <= 8 && (first and marker) == 0) {
                width++
                marker = marker ushr 1
            }
            if (width > 8) throw IOException("Invalid WebM Segment size")

            var segmentSize = (first and (marker - 1)).toLong()
            repeat(width - 1) {
                segmentSize = (segmentSize shl 8) or randomFile.readUnsignedByte().toLong()
            }
            val unknownSize = segmentSize == (1L shl (7 * width)) - 1L
            if (unknownSize) {
                // Unknown-size Segments extend to EOF, so adding another Segment child is safe.
                randomFile.seek(fileLength)
                randomFile.write(tags)
                return
            }

            val payloadStart = sizeOffset + width
            val expectedEnd = payloadStart + segmentSize
            if (expectedEnd != fileLength) {
                throw IOException("Unexpected trailing bytes after the WebM Segment")
            }
            val newSize = encodeWebmSize(segmentSize + tags.size.toLong(), width)
            randomFile.seek(sizeOffset)
            randomFile.write(newSize)
            randomFile.seek(fileLength)
            randomFile.write(tags)
        }
    }

    private fun findCachedStream(context: Context, songId: String): CachedStreamCandidate? {
        val cache = try {
            AudioPlaybackService.getCache(context.applicationContext)
        } catch (e: Exception) {
            throw IOException("Unable to inspect the local audio cache; refusing to start a network download", e)
        }

        val candidates = cache.keys.mapNotNull { cacheKey ->
            val metadata = AudioPlaybackService.getCachedMediaMetadata(context, cacheKey)
                ?: return@mapNotNull null
            if (metadata.songId != songId) return@mapNotNull null

            val spans = cache.getCachedSpans(cacheKey).toList().sortedBy { it.position }
            if (spans.isEmpty()) return@mapNotNull null
            val contentMetadata = cache.getContentMetadata(cacheKey)
            val contentLength = ContentMetadata.getContentLength(contentMetadata)
            val cachedBytes = spans.sumOf { it.length }
            // Media3 does not preserve MIME in cache metadata. Use the MIME recorded after
            // playback, then the URL's mime parameter or a container signature for old entries.
            val mimeType = metadata.mimeType.takeIf { it.isNotBlank() }
                ?: inferCachedMimeType(cacheKey, spans)
            if (mimeType.isNullOrBlank()) return@mapNotNull null

            var coveredUntil = 0L
            var hasGap = false
            spans.forEach { span ->
                if (span.position > coveredUntil) hasGap = true
                coveredUntil = maxOf(coveredUntil, span.position + span.length)
            }
            val complete = contentLength > 0L &&
                !hasGap &&
                coveredUntil >= contentLength &&
                cache.isCached(cacheKey, 0L, contentLength)

            CachedStreamCandidate(
                cacheKey = cacheKey,
                spans = spans,
                contentLength = contentLength,
                mimeType = mimeType,
                cachedBytes = cachedBytes,
                isComplete = complete
            )
        }
        return candidates.firstOrNull { it.isComplete } ?: candidates.maxByOrNull { it.cachedBytes }
    }

    private fun inferCachedMimeType(cacheKey: String, spans: List<CacheSpan>): String? {
        val urlMime = runCatching {
            android.net.Uri.parse(cacheKey).getQueryParameter("mime")
        }.getOrNull()?.substringBefore(';')?.trim()
        if (!urlMime.isNullOrBlank()) return urlMime

        val firstSpan = spans.firstOrNull { it.position == 0L } ?: return null
        val spanFile = firstSpan.file ?: return null
        val header = ByteArray(16)
        val read = runCatching {
            FileInputStream(spanFile).use { input -> input.read(header) }
        }.getOrDefault(-1)
        if (read < 4) return null

        fun ascii(start: Int, length: Int): String =
            String(header, start, length, Charsets.US_ASCII)

        return when {
            (header[0].toInt() and 0xFF) == 0x1A &&
                (header[1].toInt() and 0xFF) == 0x45 &&
                (header[2].toInt() and 0xFF) == 0xDF &&
                (header[3].toInt() and 0xFF) == 0xA3 -> "audio/webm"
            read >= 8 && ascii(4, 4) == "ftyp" -> "audio/mp4"
            ascii(0, 4) == "OggS" -> "audio/ogg"
            ascii(0, 4) == "fLaC" -> "audio/flac"
            ascii(0, 4) == "RIFF" -> "audio/wav"
            ascii(0, 3) == "ID3" ||
                ((header[0].toInt() and 0xFF) == 0xFF &&
                    ((header[1].toInt() and 0xE0) == 0xE0)) -> "audio/mpeg"
            else -> null
        }
    }

    private fun copyCachedStream(
        cached: CachedStreamCandidate,
        outputFile: File,
        coroutineContext: CoroutineContext
    ) {
        outputFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
        var expectedPosition = 0L
        FileOutputStream(outputFile).use { output ->
            val buffer = ByteArray(64 * 1024)
            for (span in cached.spans) {
                if (span.position != expectedPosition) {
                    throw IOException("Cached audio has a gap; refusing to use the network")
                }
                val spanFile = span.file ?: throw IOException("A cached audio segment is no longer available")
                FileInputStream(spanFile).use { input ->
                    var remaining = span.length
                    while (remaining > 0L) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                        if (read < 0) throw IOException("Cached audio segment ended unexpectedly")
                        output.write(buffer, 0, read)
                        remaining -= read.toLong()
                    }
                }
                expectedPosition += span.length
            }
            if (expectedPosition < cached.contentLength) {
                throw IOException("Cached audio is incomplete; refusing to use the network")
            }
        }
        if (outputFile.length() != cached.contentLength) {
            outputFile.delete()
            throw IOException("Cached audio size does not match its content length")
        }
    }

    private fun extensionForMimeType(mimeType: String?): String = when {
        mimeType?.contains("mp4", ignoreCase = true) == true -> "m4a"
        mimeType?.contains("mpeg", ignoreCase = true) == true -> "mp3"
        mimeType?.contains("webm", ignoreCase = true) == true -> "webm"
        mimeType?.contains("opus", ignoreCase = true) == true -> "opus"
        mimeType?.contains("ogg", ignoreCase = true) == true -> "ogg"
        mimeType?.contains("flac", ignoreCase = true) == true -> "flac"
        mimeType?.contains("aac", ignoreCase = true) == true -> "aac"
        mimeType?.contains("wav", ignoreCase = true) == true -> "wav"
        else -> "m4a"
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of active song downloads"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(context: Context, title: String, progress: Int, notificationId: Int) {
        createNotificationChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.dsann.music.R.drawable.ic_music_note)
            .setContentTitle("Downloading Track")
            .setContentText("$title ($progress%)")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    private fun showFinishedNotification(context: Context, title: String, artist: String, notificationId: Int) {
        createNotificationChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.dsann.music.R.drawable.ic_music_note)
            .setContentTitle("Download Complete")
            .setContentText("$title — $artist")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    private fun cancelNotification(context: Context, notificationId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
    }

    fun isSongDownloaded(context: Context, song: SongItem): Boolean {
        if (song.videoId.isNullOrEmpty()) return true // Local songs are already available offline

        val cleanTitleStr = cleanTitle(song.title)
        val cleanArtistStr = cleanArtist(song.artist)
        val extensions = listOf("m4a", "mp3", "webm", "ogg", "flac", "aac", "wav", "opus")
        val cleanNames = extensions.map { extension ->
            "$cleanTitleStr - $cleanArtistStr.$extension"
                .replace(Regex("[^a-zA-Z0-9._\\s()\\[\\]-]"), "_")
        }

        // 1. Check the current SUMITX folder and legacy folders before scoped storage.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appFolders = listOf("SUMITX", "SubhuMusic", "D.S Music", "SpatialFlow").map { File(downloads, it) }
            if (cleanNames.any { name -> appFolders.any { folder -> File(folder, name).isFile } }) return true
        }

        // 2. Match every container the downloader can create. Previously only .mp3 was
        // checked, so valid .m4a/.webm exports appeared undownloaded and the player state
        // could stay stale or offer a duplicate download.
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        for (cleanName in cleanNames) {
            try {
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    "${MediaStore.Audio.Media.DISPLAY_NAME} = ?",
                    arrayOf(cleanName),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking if song is downloaded as $cleanName", e)
            }
        }
        return false
    }
}