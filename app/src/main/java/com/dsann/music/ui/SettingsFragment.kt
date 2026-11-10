@file:Suppress("DEPRECATION")
@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)

package com.dsann.music.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dsann.music.BuildConfig
import com.dsann.music.MainActivity
import com.dsann.music.model.PlayerGestureAction
import com.dsann.music.model.PlayerGestureSettings
import com.dsann.music.R
import com.dsann.music.ui.player.SleepTimerBottomSheet
import com.dsann.music.viewmodel.PlayerSharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLEncoder
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

private val SmoothSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

// ═══════════════════════════════════════════════════════════════════════════════
// SETTINGS VIEW MODEL
// ═══════════════════════════════════════════════════════════════════════════════

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context: Context = application.applicationContext
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Dark Mode ────────────────────────────────────────────────────────────
    private val _darkMode = MutableStateFlow(
        prefs.getString("theme_mode", "system").let {
            if (it == "system") prefs.getBoolean(KEY_DARK_MODE, true) else it == "dark"
        }
    )
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()
    private val _themeMode = MutableStateFlow(
        prefs.getString("theme_mode", "system")?.lowercase()?.takeIf { it in listOf("system", "light", "dark") } ?: "system"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        val safe = mode.lowercase().takeIf { it in listOf("system", "light", "dark") } ?: "system"
        _themeMode.value = safe
        prefs.edit { putString("theme_mode", safe) }
        when (safe) {
            "dark" -> {
                _darkMode.value = true
                prefs.edit { putBoolean(KEY_DARK_MODE, true) }
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                )
            }
            "light" -> {
                _darkMode.value = false
                prefs.edit { putBoolean(KEY_DARK_MODE, false) }
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                )
            }
            else -> {
                val systemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                _darkMode.value = systemDark
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                )
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        _darkMode.value = enabled
        prefs.edit {putBoolean(KEY_DARK_MODE, enabled)}
        prefs.edit {putString("theme_mode", if (enabled) "dark" else "light")}
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            if (enabled) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    // ── YouTube Music Cookies ──────────────────────────────────────────────────
    private val _ytCookies = MutableStateFlow(prefs.getString("yt_cookies", null))
    val ytCookies: StateFlow<String?> = _ytCookies.asStateFlow()

    fun setYtCookies(cookies: String?) {
        _ytCookies.value = cookies
        if (cookies != null) {
            prefs.edit {putString("yt_cookies", cookies)}
            com.dsann.music.data.innertube.InnerTubeClient.cookie = cookies
        } else {
            prefs.edit {remove("yt_cookies")}
            com.dsann.music.data.innertube.InnerTubeClient.cookie = null
        }
    }

    // ── Crossfade ────────────────────────────────────────────────────────────
    private val _crossfadeEnabled =
        MutableStateFlow(prefs.getBoolean(KEY_CROSSFADE_ENABLED, false))
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled.asStateFlow()

    private val _crossfadeDuration =
        MutableStateFlow(prefs.getFloat(KEY_CROSSFADE_DURATION, 3f))
    val crossfadeDuration: StateFlow<Float> = _crossfadeDuration.asStateFlow()

    fun setCrossfadeEnabled(enabled: Boolean) {
        _crossfadeEnabled.value = enabled
        prefs.edit {putBoolean(KEY_CROSSFADE_ENABLED, enabled)}
    }

    fun setCrossfadeDuration(duration: Float) {
        _crossfadeDuration.value = duration
        prefs.edit {putFloat(KEY_CROSSFADE_DURATION, duration)}
    }

    // ── Stream Audio Cache ───────────────────────────────────────────────────
    private val _mediaCacheEnabled = MutableStateFlow(prefs.getBoolean(KEY_MEDIA_CACHE_ENABLED, true))
    val mediaCacheEnabled: StateFlow<Boolean> = _mediaCacheEnabled.asStateFlow()
    private val _mediaCacheLimitGb = MutableStateFlow(prefs.getInt(KEY_MEDIA_CACHE_LIMIT_GB, 1).coerceIn(1, 10))
    val mediaCacheLimitGb: StateFlow<Int> = _mediaCacheLimitGb.asStateFlow()

    fun setMediaCacheEnabled(enabled: Boolean) {
        _mediaCacheEnabled.value = enabled
        prefs.edit { putBoolean(KEY_MEDIA_CACHE_ENABLED, enabled) }
        com.dsann.music.service.AudioPlaybackService.updateMediaCacheSettings(context)
    }
    fun setMediaCacheLimitGb(limitGb: Int) {
        val safeLimit = limitGb.coerceIn(1, 10)
        _mediaCacheLimitGb.value = safeLimit
        prefs.edit { putInt(KEY_MEDIA_CACHE_LIMIT_GB, safeLimit) }
        com.dsann.music.service.AudioPlaybackService.updateMediaCacheSettings(context)
    }

    // ── Audio Focus ──────────────────────────────────────────────────────────
    private val _audioFocus = MutableStateFlow(prefs.getBoolean(KEY_AUDIO_FOCUS, true))
    val audioFocus: StateFlow<Boolean> = _audioFocus.asStateFlow()

    fun setAudioFocus(enabled: Boolean) {
        _audioFocus.value = enabled
        prefs.edit {putBoolean(KEY_AUDIO_FOCUS, enabled)}
    }

    private val _autoplayEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTOPLAY_ENABLED, true))
    val autoplayEnabled: StateFlow<Boolean> = _autoplayEnabled.asStateFlow()

    fun setAutoplayEnabled(enabled: Boolean) {
        _autoplayEnabled.value = enabled
        prefs.edit { putBoolean(KEY_AUTOPLAY_ENABLED, enabled) }
    }

    // ── Pure AMOLED Black ──────────────────────────────────────────────────
    private val _amoledBlack = MutableStateFlow(prefs.getBoolean(KEY_AMOLED_BLACK, false))
    val amoledBlack: StateFlow<Boolean> = _amoledBlack.asStateFlow()

    fun setAmoledBlack(enabled: Boolean) {
        _amoledBlack.value = enabled
        prefs.edit {putBoolean(KEY_AMOLED_BLACK, enabled)}
    }

    // ── Hide Nav Labels ──────────────────────────────────────────────────
    private val _hideNavLabels = MutableStateFlow(prefs.getBoolean("hide_nav_labels", false))
    val hideNavLabels: StateFlow<Boolean> = _hideNavLabels.asStateFlow()

    // More precise navigation label modes, while retaining the old boolean for existing installs.
    private val _navLabelsMode = MutableStateFlow(
        prefs.getString("nav_label_mode", if (prefs.getBoolean("hide_nav_labels", false)) "Never" else "Always")
            ?.takeIf { it in listOf("Always", "Selected", "Never") } ?: "Always"
    )
    val navLabelsMode: StateFlow<String> = _navLabelsMode.asStateFlow()

    fun setNavLabelsMode(mode: String) {
        val safe = mode.takeIf { it in listOf("Always", "Selected", "Never") } ?: "Always"
        _navLabelsMode.value = safe
        val hide = safe == "Never"
        _hideNavLabels.value = hide
        prefs.edit {
            putString("nav_label_mode", safe)
            putBoolean("hide_nav_labels", hide)
        }
    }

    fun setHideNavLabels(hide: Boolean) {
        setNavLabelsMode(if (hide) "Never" else "Always")
    }

    // ── Dynamic Nav Style ──────────────────────────────────────────────────
    private val _dynamicNavStyle = MutableStateFlow(prefs.getBoolean("dynamic_nav_style", false))
    val dynamicNavStyle: StateFlow<Boolean> = _dynamicNavStyle.asStateFlow()

    fun setDynamicNavStyle(enabled: Boolean) {
        _dynamicNavStyle.value = enabled
        prefs.edit {putBoolean("dynamic_nav_style", enabled)}
    }

    // ── Dynamic Album Theme ────────────────────────────────────────────────
    private val _dynamicAlbumTheme = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_ALBUM_THEME, false))
    val dynamicAlbumTheme: StateFlow<Boolean> = _dynamicAlbumTheme.asStateFlow()

    fun setDynamicAlbumTheme(enabled: Boolean) {
        _dynamicAlbumTheme.value = enabled
        prefs.edit {putBoolean(KEY_DYNAMIC_ALBUM_THEME, enabled)}
    }

    private val _appearancePreset = MutableStateFlow(
        prefs.getString(KEY_APPEARANCE_PRESET, "Ocean") ?: "Ocean"
    )
    val appearancePreset: StateFlow<String> = _appearancePreset.asStateFlow()

    fun setAppearancePreset(preset: String) {
        val safePreset = preset.takeIf {
            it in listOf("Ocean", "Violet", "Emerald", "Amber", "Rose", "Monochrome", "Crimson", "Teal", "Cyan", "Indigo", "Lime", "Graphite")
        } ?: "Ocean"
        _appearancePreset.value = safePreset
        prefs.edit { putString(KEY_APPEARANCE_PRESET, safePreset) }
    }

    private val _playerArtworkShape = MutableStateFlow(
        prefs.getString(KEY_PLAYER_ARTWORK_SHAPE, "Rounded") ?: "Rounded"
    )
    val playerArtworkShape: StateFlow<String> = _playerArtworkShape.asStateFlow()

    fun setPlayerArtworkShape(shape: String) {
        val safeShape = shape.takeIf {
            it in listOf("Rounded", "Square", "Circle", "Squircle", "Flower", "Cookie", "Arch", "Slanted")
        } ?: "Rounded"
        _playerArtworkShape.value = safeShape
        prefs.edit { putString(KEY_PLAYER_ARTWORK_SHAPE, safeShape) }
    }

    private val _playerBackgroundStyle = MutableStateFlow(
        prefs.getString("player_background_style", "Artwork") ?: "Artwork"
    )
    val playerBackgroundStyle: StateFlow<String> = _playerBackgroundStyle.asStateFlow()
    fun setPlayerBackgroundStyle(value: String) {
        val safe = value.takeIf { it in listOf("Artwork", "Theme", "Black", "Gradient") } ?: "Artwork"
        _playerBackgroundStyle.value = safe
        prefs.edit { putString("player_background_style", safe) }
    }

    private val _miniPlayerStyle = MutableStateFlow(
        prefs.getString("mini_player_style", "Card") ?: "Card"
    )
    val miniPlayerStyle: StateFlow<String> = _miniPlayerStyle.asStateFlow()
    fun setMiniPlayerStyle(value: String) {
        val safe = value.takeIf { it in listOf("Card", "Flat", "Compact", "Pill") } ?: "Card"
        _miniPlayerStyle.value = safe
        prefs.edit { putString("mini_player_style", safe) }
    }

    private val _miniPlayerControlShape = MutableStateFlow(
        prefs.getString("mini_player_control_shape", "Circle") ?: "Circle"
    )
    val miniPlayerControlShape: StateFlow<String> = _miniPlayerControlShape.asStateFlow()
    fun setMiniPlayerControlShape(value: String) {
        val safe = value.takeIf { it in listOf("Circle", "Rounded", "Pill", "None") } ?: "Circle"
        _miniPlayerControlShape.value = safe
        prefs.edit { putString("mini_player_control_shape", safe) }
    }

    private val _miniPlayerControlIcons = MutableStateFlow(
        prefs.getString("mini_player_control_icons", "Classic") ?: "Classic"
    )
    val miniPlayerControlIcons: StateFlow<String> = _miniPlayerControlIcons.asStateFlow()
    fun setMiniPlayerControlIcons(value: String) {
        val safe = value.takeIf { it in listOf("Classic", "Chevron", "Seek") } ?: "Classic"
        _miniPlayerControlIcons.value = safe
        prefs.edit { putString("mini_player_control_icons", safe) }
    }

    private val _miniPlayerProgressStyle = MutableStateFlow(
        prefs.getString("mini_player_progress", "Ring") ?: "Ring"
    )
    val miniPlayerProgressStyle: StateFlow<String> = _miniPlayerProgressStyle.asStateFlow()
    fun setMiniPlayerProgressStyle(value: String) {
        val safe = value.takeIf { it in listOf("Ring", "Line", "Fill", "None") } ?: "Ring"
        _miniPlayerProgressStyle.value = safe
        prefs.edit { putString("mini_player_progress", safe) }
    }

    // ── Player, your way ────────────────────────────────────────────────────
    private val _playerLayout = MutableStateFlow(
        prefs.getString("player_layout", "Standard")?.takeIf {
            it in listOf("Standard", "Cover", "Minimal", "Vinyl", "Cassette")
        } ?: "Standard"
    )
    val playerLayout: StateFlow<String> = _playerLayout.asStateFlow()
    fun setPlayerLayout(value: String) {
        val safe = value.takeIf { it in listOf("Standard", "Cover", "Minimal", "Vinyl", "Cassette") } ?: "Standard"
        _playerLayout.value = safe
        prefs.edit { putString("player_layout", safe) }
    }

    private val availablePlayerButtons = listOf("lyrics", "video", "queue", "share", "download", "like", "dislike", "sleep")
    private val defaultPlayerButtons = listOf("lyrics", "video", "queue", "share", "download")
    private fun readPlayerButtons(): List<String> {
        val stored = prefs.getString("player_buttons", null)
        val parsed = stored?.split(",")?.map { it.trim().lowercase() }
            ?.filter { it in availablePlayerButtons }?.distinct().orEmpty()
        return (parsed + defaultPlayerButtons.filter { it !in parsed }).take(6)
            .let { safe -> if (safe.size >= 3) safe else defaultPlayerButtons }
    }
    private val _playerButtons = MutableStateFlow(readPlayerButtons())
    val playerButtons: StateFlow<List<String>> = _playerButtons.asStateFlow()
    fun setPlayerButtons(buttons: List<String>) {
        val safe = buttons.filter { it in availablePlayerButtons }.distinct().take(6)
        if (safe.size < 3) return
        _playerButtons.value = safe
        prefs.edit { putString("player_buttons", safe.joinToString(",")) }
    }
    fun movePlayerButton(from: Int, to: Int) {
        setPlayerButtons(com.dsann.music.util.OrderUtils.move(_playerButtons.value, from, to))
    }

    private val _playerTitleAlign = MutableStateFlow(prefs.getString("player_title_align", "Left") ?: "Left")
    val playerTitleAlign: StateFlow<String> = _playerTitleAlign.asStateFlow()
    fun setPlayerTitleAlign(value: String) {
        val safe = value.takeIf { it in listOf("Left", "Centered") } ?: "Left"
        _playerTitleAlign.value = safe
        prefs.edit { putString("player_title_align", safe) }
    }

    private val _playerTitleSize = MutableStateFlow(prefs.getString("player_title_size", "Medium") ?: "Medium")
    val playerTitleSize: StateFlow<String> = _playerTitleSize.asStateFlow()
    fun setPlayerTitleSize(value: String) {
        val safe = value.takeIf { it in listOf("Small", "Medium", "Large") } ?: "Medium"
        _playerTitleSize.value = safe
        prefs.edit { putString("player_title_size", safe) }
    }

    private val _playerTitleWeight = MutableStateFlow(prefs.getString("player_title_weight", "Bold") ?: "Bold")
    val playerTitleWeight: StateFlow<String> = _playerTitleWeight.asStateFlow()
    fun setPlayerTitleWeight(value: String) {
        val safe = value.takeIf { it in listOf("Light", "Regular", "Bold") } ?: "Bold"
        _playerTitleWeight.value = safe
        prefs.edit { putString("player_title_weight", safe) }
    }

    private val _playerTitleScroll = MutableStateFlow(prefs.getBoolean("player_title_scroll", true))
    val playerTitleScroll: StateFlow<Boolean> = _playerTitleScroll.asStateFlow()
    fun setPlayerTitleScroll(value: Boolean) {
        _playerTitleScroll.value = value
        prefs.edit { putBoolean("player_title_scroll", value) }
    }

    private val _playerTimes = MutableStateFlow(prefs.getString("player_times", "Both") ?: "Both")
    val playerTimes: StateFlow<String> = _playerTimes.asStateFlow()
    fun setPlayerTimes(value: String) {
        val safe = value.takeIf { it in listOf("Both", "Elapsed", "Time left", "None") } ?: "Both"
        _playerTimes.value = safe
        prefs.edit { putString("player_times", safe) }
    }

    private val _scrubberStyle = MutableStateFlow(
        prefs.getString("scrubber_style", "Waveform")
            ?.let { if (it == "Wave") "Waveform" else it }
            ?.takeIf { it in listOf("Hairline", "Bold", "Thumb", "Waveform") }
            ?: "Waveform"
    )
    val scrubberStyle: StateFlow<String> = _scrubberStyle.asStateFlow()
    fun setScrubberStyle(value: String) {
        val safe = value.takeIf { it in listOf("Hairline", "Bold", "Thumb", "Waveform") } ?: "Waveform"
        _scrubberStyle.value = safe
        prefs.edit { putString("scrubber_style", safe) }
    }

    private val _scrubberColor = MutableStateFlow(prefs.getString("scrubber_color", "Accent") ?: "Accent")
    val scrubberColor: StateFlow<String> = _scrubberColor.asStateFlow()
    fun setScrubberColor(value: String) {
        val safe = value.takeIf { it in listOf("Accent", "White", "Cover") } ?: "Accent"
        _scrubberColor.value = safe
        prefs.edit { putString("scrubber_color", safe) }
    }

    private val _visualizerStyle = MutableStateFlow(prefs.getString("player_visualizer", "Off") ?: "Off")
    val visualizerStyle: StateFlow<String> = _visualizerStyle.asStateFlow()
    fun setVisualizerStyle(value: String) {
        val safe = value.takeIf { it in listOf("Off", "Bars", "Wave", "Ring", "Pulse", "Glow") } ?: "Off"
        _visualizerStyle.value = safe
        prefs.edit { putString("player_visualizer", safe) }
    }

    // Additional Prism-style player customisation.
    private val _playerButtonStyle = MutableStateFlow(prefs.getString("player_button_style", "Pill") ?: "Pill")
    val playerButtonStyle: StateFlow<String> = _playerButtonStyle.asStateFlow()
    fun setPlayerButtonStyle(value: String) {
        val safe = value.takeIf { it in listOf("Pill", "Rounded", "Minimal") } ?: "Pill"
        _playerButtonStyle.value = safe
        prefs.edit { putString("player_button_style", safe) }
    }

    private val _scrubberThickness = MutableStateFlow(prefs.getFloat("scrubber_thickness", 1f).coerceIn(0.6f, 2f))
    val scrubberThickness: StateFlow<Float> = _scrubberThickness.asStateFlow()
    fun setScrubberThickness(value: Float) {
        val safe = value.coerceIn(0.6f, 2f)
        _scrubberThickness.value = safe
        prefs.edit { putFloat("scrubber_thickness", safe) }
    }

    private val _scrubberThumbStyle = MutableStateFlow(prefs.getString("scrubber_thumb_style", "Auto") ?: "Auto")
    val scrubberThumbStyle: StateFlow<String> = _scrubberThumbStyle.asStateFlow()
    fun setScrubberThumbStyle(value: String) {
        val safe = value.takeIf { it in listOf("Auto", "Dot", "Line", "Ring", "None") } ?: "Auto"
        _scrubberThumbStyle.value = safe
        prefs.edit { putString("scrubber_thumb_style", safe) }
    }

    private val _visualizerColor = MutableStateFlow(prefs.getString("visualizer_color", "Accent") ?: "Accent")
    val visualizerColor: StateFlow<String> = _visualizerColor.asStateFlow()
    fun setVisualizerColor(value: String) {
        val safe = value.takeIf { it in listOf("Accent", "White", "Cover", "Spectrum") } ?: "Accent"
        _visualizerColor.value = safe
        prefs.edit { putString("visualizer_color", safe) }
    }

    private val _lyricsTextSize = MutableStateFlow(
        prefs.getString("lyrics_text_size", "Standard") ?: "Standard"
    )
    val lyricsTextSize: StateFlow<String> = _lyricsTextSize.asStateFlow()
    fun setLyricsTextSize(value: String) {
        val safe = value.takeIf { it in listOf("Small", "Standard", "Large") } ?: "Standard"
        _lyricsTextSize.value = safe
        prefs.edit { putString("lyrics_text_size", safe) }
    }

    private val _lyricsAlignment = MutableStateFlow(
        prefs.getString("lyrics_alignment", "Original") ?: "Original"
    )
    val lyricsAlignment: StateFlow<String> = _lyricsAlignment.asStateFlow()
    fun setLyricsAlignment(value: String) {
        val safe = value.takeIf { it in listOf("Original", "Left", "Center") } ?: "Original"
        _lyricsAlignment.value = safe
        prefs.edit { putString("lyrics_alignment", safe) }
    }

    private val _lyricsFontFamily = MutableStateFlow(
        prefs.getString("lyrics_font_family", "Rounded") ?: "Rounded"
    )
    val lyricsFontFamily: StateFlow<String> = _lyricsFontFamily.asStateFlow()
    fun setLyricsFontFamily(value: String) {
        val safe = value.takeIf { it in listOf("Rounded", "Sans", "Serif", "Mono") } ?: "Rounded"
        _lyricsFontFamily.value = safe
        prefs.edit { putString("lyrics_font_family", safe) }
    }

    private val _uiDensity = MutableStateFlow(
        prefs.getString("ui_density", "Comfortable") ?: "Comfortable"
    )
    val uiDensity: StateFlow<String> = _uiDensity.asStateFlow()
    fun setUiDensity(value: String) {
        val safe = value.takeIf { it in listOf("Compact", "Comfortable", "Large") } ?: "Comfortable"
        _uiDensity.value = safe
        prefs.edit { putString("ui_density", safe) }
    }

    // ── Ignore Short Audio ──────────────────────────────────────────────────
    private val _ignoreShortAudio = MutableStateFlow(prefs.getBoolean(KEY_IGNORE_SHORT_AUDIO, false))
    val ignoreShortAudio: StateFlow<Boolean> = _ignoreShortAudio.asStateFlow()

    private val _ignoreShortAudioDuration = MutableStateFlow(prefs.getFloat(KEY_IGNORE_SHORT_AUDIO_DURATION, 30f))
    val ignoreShortAudioDuration: StateFlow<Float> = _ignoreShortAudioDuration.asStateFlow()

    fun setIgnoreShortAudio(enabled: Boolean) {
        _ignoreShortAudio.value = enabled
        prefs.edit {putBoolean(KEY_IGNORE_SHORT_AUDIO, enabled)}
    }

    fun setIgnoreShortAudioDuration(duration: Float) {
        _ignoreShortAudioDuration.value = duration
        prefs.edit {putFloat(KEY_IGNORE_SHORT_AUDIO_DURATION, duration)}
    }

    // ── Data Saver ──────────────────────────────────────────────────────────
    private val _dataSaver = MutableStateFlow(prefs.getBoolean(KEY_DATA_SAVER, false))
    val dataSaver: StateFlow<Boolean> = _dataSaver.asStateFlow()

    fun setDataSaver(enabled: Boolean) {
        _dataSaver.value = enabled
        prefs.edit {putBoolean(KEY_DATA_SAVER, enabled)}
        com.dsann.music.data.innertube.NewPipeStreamExtractor.clearCache()
    }

    // ── Playback bitrate and format ───────────────────────────────────────────
    private val _audioQuality = MutableStateFlow(
        com.dsann.music.data.innertube.NewPipeStreamExtractor.normalizeAudioQuality(
            prefs.getString(KEY_AUDIO_QUALITY, "256 kbps")
        )
    )
    val audioQuality: StateFlow<String> = _audioQuality.asStateFlow()

    private val _playbackAudioFormat = MutableStateFlow(
        com.dsann.music.data.innertube.NewPipeStreamExtractor.normalizeAudioFormat(
            prefs.getString(KEY_PLAYBACK_FORMAT, "auto")
        )
    )
    val playbackAudioFormat: StateFlow<String> = _playbackAudioFormat.asStateFlow()

    private val _downloadAudioFormat = MutableStateFlow(
        com.dsann.music.data.innertube.NewPipeStreamExtractor.normalizeAudioFormat(
            prefs.getString(KEY_DOWNLOAD_FORMAT, "auto")
        )
    )
    val downloadAudioFormat: StateFlow<String> = _downloadAudioFormat.asStateFlow()

    private val _downloadBitrate = MutableStateFlow(
        com.dsann.music.data.innertube.NewPipeStreamExtractor.normalizeAudioQuality(
            prefs.getString(KEY_DOWNLOAD_BITRATE, "256 kbps")
        )
    )
    val downloadBitrate: StateFlow<String> = _downloadBitrate.asStateFlow()

    fun setAudioQuality(quality: String) {
        val normalized = com.dsann.music.data.innertube.NewPipeStreamExtractor
            .normalizeAudioQuality(quality)
        _audioQuality.value = normalized
        prefs.edit { putString(KEY_AUDIO_QUALITY, normalized) }
        com.dsann.music.data.innertube.YouTubeMusic.invalidateAllStreamUrls()
    }

    fun setPlaybackAudioFormat(format: String) {
        val normalized = com.dsann.music.data.innertube.NewPipeStreamExtractor.normalizeAudioFormat(format)
        _playbackAudioFormat.value = normalized
        prefs.edit { putString(KEY_PLAYBACK_FORMAT, normalized) }
        com.dsann.music.data.innertube.YouTubeMusic.invalidateAllStreamUrls()
    }

    fun setDownloadAudioFormat(format: String) {
        val normalized = com.dsann.music.data.innertube.NewPipeStreamExtractor.normalizeAudioFormat(format)
        _downloadAudioFormat.value = normalized
        prefs.edit { putString(KEY_DOWNLOAD_FORMAT, normalized) }
        com.dsann.music.data.innertube.NewPipeStreamExtractor.clearCache()
    }

    fun setDownloadBitrate(quality: String) {
        val normalized = com.dsann.music.data.innertube.NewPipeStreamExtractor.normalizeAudioQuality(quality)
        _downloadBitrate.value = normalized
        prefs.edit { putString(KEY_DOWNLOAD_BITRATE, normalized) }
        com.dsann.music.data.innertube.NewPipeStreamExtractor.clearCache()
    }

    // ── Pause Listening History ────────────────────────────────────────────────
    private val _pauseHistory = MutableStateFlow(prefs.getBoolean("pause_history", false))
    val pauseHistory: StateFlow<Boolean> = _pauseHistory.asStateFlow()

    fun setPauseHistory(pause: Boolean) {
        _pauseHistory.value = pause
        prefs.edit {putBoolean("pause_history", pause)}
    }

    // ── Library Paths ────────────────────────────────────────────────────────
    private val _libraryPaths = MutableStateFlow(
        (prefs.getString(KEY_LIBRARY_PATHS, "") ?: "").split("||").filter { it.isNotEmpty() }
    )
    val libraryPaths: StateFlow<List<String>> = _libraryPaths.asStateFlow()

    fun addLibraryPath(path: String) {
        val current = _libraryPaths.value
        if (current.contains(path)) return
        val newList = current + path
        _libraryPaths.value = newList
        prefs.edit {putString(KEY_LIBRARY_PATHS, newList.joinToString("||"))}
    }

    fun removeLibraryPath(path: String) {
        val current = _libraryPaths.value
        val newList = current.filter { it != path }
        _libraryPaths.value = newList
        prefs.edit {putString(KEY_LIBRARY_PATHS, newList.joinToString("||"))}
    }

    // ── Hidden Folders ────────────────────────────────────────────────────────
    private val _hiddenFolders = MutableStateFlow(
        (prefs.getString(KEY_HIDDEN_FOLDERS, "") ?: "").split("||").filter { it.isNotEmpty() }
    )
    val hiddenFolders: StateFlow<List<String>> = _hiddenFolders.asStateFlow()

    fun addHiddenFolder(path: String) {
        val current = _hiddenFolders.value
        if (current.contains(path)) return
        val newList = current + path
        _hiddenFolders.value = newList
        prefs.edit {putString(KEY_HIDDEN_FOLDERS, newList.joinToString("||"))}
    }

    fun removeHiddenFolder(path: String) {
        val current = _hiddenFolders.value
        val newList = current.filter { it != path }
        _hiddenFolders.value = newList
        prefs.edit {putString(KEY_HIDDEN_FOLDERS, newList.joinToString("||"))}
    }

    // ── Cache & Downloads ───────────────────────────────────────────────────
    private val _downloadFolder = MutableStateFlow(prefs.getString("download_folder", null))
    val downloadFolder: StateFlow<String?> = _downloadFolder.asStateFlow()

    fun setDownloadFolder(uri: String?) {
        _downloadFolder.value = uri
        if (uri != null) {
            prefs.edit {putString("download_folder", uri)}
        } else {
            prefs.edit {remove("download_folder")}
        }
    }

    // Sizes in MB. -1 or 0 could mean unlimited, or we can use large numbers. We'll use Int (MB). 0 = unlimited.
    private val _songCacheMaxSize = MutableStateFlow(prefs.getInt("song_cache_max_size", 1024)) // Default 1GB
    val songCacheMaxSize: StateFlow<Int> = _songCacheMaxSize.asStateFlow()

    fun setSongCacheMaxSize(sizeMb: Int) {
        _songCacheMaxSize.value = sizeMb
        prefs.edit {putInt("song_cache_max_size", sizeMb)}
    }

    private val _imageCacheMaxSize = MutableStateFlow(prefs.getInt("image_cache_max_size", 512)) // Default 500MB
    val imageCacheMaxSize: StateFlow<Int> = _imageCacheMaxSize.asStateFlow()

    fun setImageCacheMaxSize(sizeMb: Int) {
        _imageCacheMaxSize.value = sizeMb
        prefs.edit {putInt("image_cache_max_size", sizeMb)}
    }

    // ── Backup & Restore ──────────────────────────────────────────────────────
    fun exportBackup(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settingsMap = prefs.all
                val favManager = com.dsann.music.util.FavoritesManager(context)
                val favoritesList = favManager.favoriteIds.toList()

                val db = com.dsann.music.data.db.MusicDatabase.getDatabase(context)
                val playlistDao = db.playlistDao()

                // Read playlists and songs using Flow.first() extension
                val playlists = playlistDao.getAllPlaylists().first()
                val playlistSongsList = mutableListOf<com.dsann.music.data.db.PlaylistSongEntity>()
                playlists.forEach { playlist: com.dsann.music.data.db.PlaylistEntity ->
                    val songs = playlistDao.getSongsForPlaylist(playlist.id).first()
                    playlistSongsList.addAll(songs)
                }

                val backupObject = com.google.gson.JsonObject().apply {
                    addProperty("version", 1)

                    val settingsObj = com.google.gson.JsonObject()
                    settingsMap.forEach { (key, value) ->
                        when (value) {
                            is Boolean -> settingsObj.addProperty(key, value)
                            is Float -> settingsObj.addProperty(key, value)
                            is Int -> settingsObj.addProperty(key, value)
                            is Long -> settingsObj.addProperty(key, value)
                            is String -> settingsObj.addProperty(key, value)
                        }
                    }
                    add("settings", settingsObj)

                    val favArray = com.google.gson.JsonArray()
                    favoritesList.forEach { favArray.add(it) }
                    add("favorites", favArray)

                    val gson = com.google.gson.Gson()
                    add("playlists", gson.toJsonTree(playlists))
                    add("playlist_songs", gson.toJsonTree(playlistSongsList))
                }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(backupObject.toString().toByteArray())
                }

                withContext(Dispatchers.Main) {
                    onResult(true, "Backup exported successfully")
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to export backup", e)
                withContext(Dispatchers.Main) {
                    onResult(false, "Failed to export: ${e.localizedMessage}")
                }
            }
        }
    }

    fun importBackup(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                } ?: throw Exception("Could not read backup file")

                val gson = com.google.gson.Gson()
                val backupObject = gson.fromJson(jsonString, com.google.gson.JsonObject::class.java)

                if (backupObject.has("settings")) {
                    val settingsObj = backupObject.getAsJsonObject("settings")
                    prefs.edit {
                        settingsObj.entrySet().forEach { (key, jsonElement) ->
                            if (jsonElement.isJsonPrimitive) {
                                val prim = jsonElement.asJsonPrimitive
                                if (prim.isBoolean) putBoolean(key, prim.asBoolean)
                                else if (prim.isNumber) {
                                    val num = prim.asNumber
                                    when (key) {
                                        KEY_CROSSFADE_DURATION, KEY_TARGET_LUFS -> putFloat(key, num.toFloat())
                                        else -> {
                                            try {
                                                putInt(key, num.toInt())
                                            } catch (_: Exception) {
                                                putFloat(key, num.toFloat())
                                            }
                                        }
                                    }
                                }
                                else if (prim.isString) putString(key, prim.asString)
                            }
                        }
                    }

                    _darkMode.value = prefs.getBoolean(KEY_DARK_MODE, true)
                    _amoledBlack.value = prefs.getBoolean(KEY_AMOLED_BLACK, false)
                    _dynamicAlbumTheme.value = prefs.getBoolean(KEY_DYNAMIC_ALBUM_THEME, false)
                    _appearancePreset.value = prefs.getString(KEY_APPEARANCE_PRESET, "Ocean") ?: "Ocean"
                    _playerArtworkShape.value = prefs.getString(KEY_PLAYER_ARTWORK_SHAPE, "Rounded") ?: "Rounded"
                    _autoplayEnabled.value = prefs.getBoolean(KEY_AUTOPLAY_ENABLED, true)
                    _crossfadeEnabled.value = prefs.getBoolean(KEY_CROSSFADE_ENABLED, false)
                    _crossfadeDuration.value = prefs.getFloat(KEY_CROSSFADE_DURATION, 3f)
                    _audioFocus.value = prefs.getBoolean(KEY_AUDIO_FOCUS, true)
                    _dataSaver.value = prefs.getBoolean(KEY_DATA_SAVER, false)
                    _audioQuality.value =
                        com.dsann.music.data.innertube.NewPipeStreamExtractor
                            .normalizeAudioQuality(prefs.getString(KEY_AUDIO_QUALITY, "256 kbps"))
                    _playbackAudioFormat.value =
                        com.dsann.music.data.innertube.NewPipeStreamExtractor
                            .normalizeAudioFormat(prefs.getString(KEY_PLAYBACK_FORMAT, "auto"))
                    _downloadAudioFormat.value =
                        com.dsann.music.data.innertube.NewPipeStreamExtractor
                            .normalizeAudioFormat(prefs.getString(KEY_DOWNLOAD_FORMAT, "auto"))
                    _downloadBitrate.value =
                        com.dsann.music.data.innertube.NewPipeStreamExtractor
                            .normalizeAudioQuality(prefs.getString(KEY_DOWNLOAD_BITRATE, "256 kbps"))
                    _pauseHistory.value = prefs.getBoolean("pause_history", false)
                    _downloadFolder.value = prefs.getString("download_folder", null)
                    _songCacheMaxSize.value = prefs.getInt("song_cache_max_size", 1024)
                    _imageCacheMaxSize.value = prefs.getInt("image_cache_max_size", 512)
                }

                if (backupObject.has("favorites")) {
                    val favArray = backupObject.getAsJsonArray("favorites")
                    val favManager = com.dsann.music.util.FavoritesManager(context)
                    favArray.forEach { element ->
                        val idStr = element.asString
                        idStr.toLongOrNull()?.let { idLong ->
                            favManager.setFavorite(idLong, true)
                        }
                    }
                }

                val db = com.dsann.music.data.db.MusicDatabase.getDatabase(context)
                val playlistDao = db.playlistDao()

                if (backupObject.has("playlists")) {
                    val typeTokenPlaylists = object :
                        com.google.gson.reflect.TypeToken<List<com.dsann.music.data.db.PlaylistEntity>>() {}.type
                    val playlistsList: List<com.dsann.music.data.db.PlaylistEntity> =
                        gson.fromJson(backupObject.get("playlists"), typeTokenPlaylists)

                    playlistsList.forEach { playlist ->
                        playlistDao.insertPlaylist(playlist)
                    }
                }

                if (backupObject.has("playlist_songs")) {
                    val typeTokenSongs = object : com.google.gson.reflect.TypeToken<List<com.dsann.music.data.db.PlaylistSongEntity>>() {}.type
                    val songsList: List<com.dsann.music.data.db.PlaylistSongEntity> = gson.fromJson(backupObject.get("playlist_songs"), typeTokenSongs)

                    songsList.forEach { song ->
                        playlistDao.insertPlaylistSong(song)
                    }
                }

                withContext(Dispatchers.Main) {
                    onResult(true, "Backup imported successfully")
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to import backup", e)
                withContext(Dispatchers.Main) {
                    onResult(false, "Import failed: ${e.localizedMessage}")
                }
            }
        }
    }

    // ── Cache ────────────────────────────────────────────────────────────────
    private val _songCacheSize = MutableStateFlow("Calculating...")
    val songCacheSize: StateFlow<String> = _songCacheSize.asStateFlow()

    private val _imageCacheSize = MutableStateFlow("Calculating...")
    val imageCacheSize: StateFlow<String> = _imageCacheSize.asStateFlow()

    fun calculateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val imageCacheDir = File(context.cacheDir, "image_cache")
            val imageSize = getDirSize(imageCacheDir)
            
            var totalCacheSize = getDirSize(context.cacheDir)
            totalCacheSize += getDirSize(context.codeCacheDir)
            val songSize = (totalCacheSize - imageSize).coerceAtLeast(0L)

            _imageCacheSize.value = formatFileSize(imageSize)
            _songCacheSize.value = formatFileSize(songSize)
        }
    }

    fun clearSongCache(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name != "image_cache") {
                    deleteDir(file)
                }
            }
            deleteDir(context.codeCacheDir)
            calculateCacheSize()
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    fun clearImageCache(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val imageCacheDir = File(context.cacheDir, "image_cache")
            deleteDir(imageCacheDir)
            calculateCacheSize()
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private fun getDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isFile) file.length() else getDirSize(file)
        }
        return size
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups =
            (log10(bytes.toDouble()) / log10(1024.0)).toInt()
                .coerceAtMost(units.size - 1)
        return DecimalFormat("#.##")
            .format(bytes / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    private fun deleteDir(dir: File?) {
        if (dir == null || !dir.exists()) return
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) deleteDir(file)
            file.delete()
        }
    }

    companion object {
        const val PREFS_NAME = "AppSettings"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_CROSSFADE_ENABLED = "crossfade_enabled"
        const val KEY_CROSSFADE_DURATION = "crossfade_duration"
        const val KEY_AUDIO_FOCUS = "audio_focus"
        const val KEY_MEDIA_CACHE_ENABLED = "media_cache_enabled"
        const val KEY_MEDIA_CACHE_LIMIT_GB = "media_cache_limit_gb"
        const val KEY_LIBRARY_PATHS = "library_paths"
        const val KEY_HIDDEN_FOLDERS = "hidden_folders"
        const val KEY_AMOLED_BLACK = "amoled_black"
        const val KEY_IGNORE_SHORT_AUDIO = "ignore_short_audio"
        const val KEY_IGNORE_SHORT_AUDIO_DURATION = "ignore_short_audio_duration"
        const val KEY_DATA_SAVER = "data_saver"
        const val KEY_AUDIO_QUALITY = "audio_quality"
        const val KEY_PLAYBACK_FORMAT = "audio_format"
        const val KEY_DOWNLOAD_FORMAT = "download_audio_format"
        const val KEY_DOWNLOAD_BITRATE = "download_bitrate"
        const val KEY_SYNC_FREQUENCY = "sync_frequency"
        const val KEY_DYNAMIC_ALBUM_THEME = "dynamic_album_theme"
        const val KEY_APPEARANCE_PRESET = "appearance_preset"
        const val KEY_PLAYER_ARTWORK_SHAPE = "player_artwork_shape"
        const val KEY_AUTOPLAY_ENABLED = "autoplay_enabled"
        const val KEY_VOLUME_NORMALIZATION_ENABLED = "volume_normalization_enabled"
        const val KEY_TARGET_LUFS = "target_lufs"
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SETTINGS FRAGMENT
// ═══════════════════════════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════════════════════════
// COMPOSE UI — ENTRY POINT
// ═══════════════════════════════════════════════════════════════════════════════

private fun getPathFromUri(uri: Uri): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && android.provider.DocumentsContract.isTreeUri(uri)) {
        val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
        val split = docId.split(":")
        if (split.size >= 2) {
            val type = split[0]
            val relativePath = split[1]
            return if ("primary" == type.lowercase()) {
                android.os.Environment.getExternalStorageDirectory().toString() + "/" + relativePath
            } else {
                "/storage/$type/$relativePath"
            }
        }
    }
    return uri.path ?: uri.toString()
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun SettingsScreenContent(
    viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(LocalContext.current.findActivity() as androidx.activity.ComponentActivity),
    playerSharedViewModel: PlayerSharedViewModel = androidx.lifecycle.viewmodel.compose.viewModel(LocalContext.current.findActivity() as androidx.activity.ComponentActivity)
) {
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isPlayerExpanded by playerSharedViewModel.isPlayerExpanded.collectAsStateWithLifecycle()
    val amoledBlack by viewModel.amoledBlack.collectAsStateWithLifecycle()
    val dynamicAlbumTheme by viewModel.dynamicAlbumTheme.collectAsStateWithLifecycle()
    val appearancePreset by viewModel.appearancePreset.collectAsStateWithLifecycle()
    val playerArtworkShape by viewModel.playerArtworkShape.collectAsStateWithLifecycle()
    val playerBackgroundStyle by viewModel.playerBackgroundStyle.collectAsStateWithLifecycle()
    val miniPlayerStyle by viewModel.miniPlayerStyle.collectAsStateWithLifecycle()
    val miniPlayerControlShape by viewModel.miniPlayerControlShape.collectAsStateWithLifecycle()
    val miniPlayerControlIcons by viewModel.miniPlayerControlIcons.collectAsStateWithLifecycle()
    val miniPlayerProgressStyle by viewModel.miniPlayerProgressStyle.collectAsStateWithLifecycle()
    val playerLayout by viewModel.playerLayout.collectAsStateWithLifecycle()
    val playerButtons by viewModel.playerButtons.collectAsStateWithLifecycle()
    val playerTitleAlign by viewModel.playerTitleAlign.collectAsStateWithLifecycle()
    val playerTitleSize by viewModel.playerTitleSize.collectAsStateWithLifecycle()
    val playerTitleWeight by viewModel.playerTitleWeight.collectAsStateWithLifecycle()
    val playerTitleScroll by viewModel.playerTitleScroll.collectAsStateWithLifecycle()
    val playerTimes by viewModel.playerTimes.collectAsStateWithLifecycle()
    val scrubberStyle by viewModel.scrubberStyle.collectAsStateWithLifecycle()
    val scrubberColor by viewModel.scrubberColor.collectAsStateWithLifecycle()
    val visualizerStyle by viewModel.visualizerStyle.collectAsStateWithLifecycle()
    val playerButtonStyle by viewModel.playerButtonStyle.collectAsStateWithLifecycle()
    val scrubberThickness by viewModel.scrubberThickness.collectAsStateWithLifecycle()
    val scrubberThumbStyle by viewModel.scrubberThumbStyle.collectAsStateWithLifecycle()
    val visualizerColor by viewModel.visualizerColor.collectAsStateWithLifecycle()
    val lyricsTextSize by viewModel.lyricsTextSize.collectAsStateWithLifecycle()
    val lyricsAlignment by viewModel.lyricsAlignment.collectAsStateWithLifecycle()
    val lyricsFontFamily by viewModel.lyricsFontFamily.collectAsStateWithLifecycle()
    val uiDensity by viewModel.uiDensity.collectAsStateWithLifecycle()
    val autoplayEnabled by viewModel.autoplayEnabled.collectAsStateWithLifecycle()
    val navLabelsMode by viewModel.navLabelsMode.collectAsStateWithLifecycle()
    val dynamicNavStyle by viewModel.dynamicNavStyle.collectAsStateWithLifecycle()
    val crossfadeEnabled by viewModel.crossfadeEnabled.collectAsStateWithLifecycle()
    val crossfadeDuration by viewModel.crossfadeDuration.collectAsStateWithLifecycle()
    val audioFocus by viewModel.audioFocus.collectAsStateWithLifecycle()
    val sleepTimerEndTime by playerSharedViewModel.sleepTimerEndTime.collectAsStateWithLifecycle()
    val sleepTimerMode by playerSharedViewModel.sleepTimerMode.collectAsStateWithLifecycle()
    val libraryPaths by viewModel.libraryPaths.collectAsStateWithLifecycle()
    val songCacheSize by viewModel.songCacheSize.collectAsStateWithLifecycle()
    val imageCacheSize by viewModel.imageCacheSize.collectAsStateWithLifecycle()

    val ignoreShortAudio by viewModel.ignoreShortAudio.collectAsStateWithLifecycle()
    val ignoreShortAudioDuration by viewModel.ignoreShortAudioDuration.collectAsStateWithLifecycle()
    val hiddenFolders by viewModel.hiddenFolders.collectAsStateWithLifecycle()

    val dataSaver by viewModel.dataSaver.collectAsStateWithLifecycle()
    val audioQuality by viewModel.audioQuality.collectAsStateWithLifecycle()
    val playbackAudioFormat by viewModel.playbackAudioFormat.collectAsStateWithLifecycle()
    val downloadAudioFormat by viewModel.downloadAudioFormat.collectAsStateWithLifecycle()
    val downloadBitrate by viewModel.downloadBitrate.collectAsStateWithLifecycle()
    val pauseHistory by viewModel.pauseHistory.collectAsStateWithLifecycle()
    val downloadFolder by viewModel.downloadFolder.collectAsStateWithLifecycle()
    val songCacheMaxSize by viewModel.songCacheMaxSize.collectAsStateWithLifecycle()
    val imageCacheMaxSize by viewModel.imageCacheMaxSize.collectAsStateWithLifecycle()
    val mediaCacheEnabled by viewModel.mediaCacheEnabled.collectAsStateWithLifecycle()
    val mediaCacheLimitGb by viewModel.mediaCacheLimitGb.collectAsStateWithLifecycle()


    val ytCookies by viewModel.ytCookies.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val directoryPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.addLibraryPath(it.toString())
        }
    }

    val downloadFolderPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val path = getPathFromUri(it)
            viewModel.setDownloadFolder(path)
        }
    }

    val exportBackupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportBackup(context, it) { _, msg ->
                (context as? MainActivity)?.showSnackbar(msg, 0)
            }
        }
    }

    val importBackupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importBackup(context, it) { _, msg ->
                (context as? MainActivity)?.showSnackbar(msg, 0)
            }
        }
    }

    val hiddenFolderPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val path = getPathFromUri(it)
                viewModel.addHiddenFolder(path)
            } catch (e: Exception) {
                android.util.Log.e("SettingsScreenContent", "Failed to resolve hidden folder path", e)
            }
        }
    }



    LaunchedEffect(Unit) { viewModel.calculateCacheSize() }

    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val canPop = currentBackStackEntry?.destination?.route != SettingsRoute.Main.route

    NavHost(
        navController = navController, 
        startDestination = SettingsRoute.Main.route,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
    ) {
        composable(SettingsRoute.Main.route) {
            SettingsMainScreen(navController = navController)
        }
        composable(SettingsRoute.MusicManagement.route) {
            MusicManagementScreen(
                navController = navController,
                libraryPaths = libraryPaths,
                onRemovePath = { viewModel.removeLibraryPath(it) },
                onAddPathClick = { directoryPickerLauncher.launch(null) },
                songCacheSize = songCacheSize,
                imageCacheSize = imageCacheSize,
                onClearSongCache = {
                    viewModel.clearSongCache {
                        (context as? MainActivity)?.showSnackbar("Song cache cleared", 0)
                    }
                },
                onClearImageCache = {
                    viewModel.clearImageCache {
                        (context as? MainActivity)?.showSnackbar("Image cache cleared", 0)
                    }
                },
                songCacheMaxSize = songCacheMaxSize,
                onSongCacheMaxSizeChange = { viewModel.setSongCacheMaxSize(it) },
                imageCacheMaxSize = imageCacheMaxSize,
                onImageCacheMaxSizeChange = { viewModel.setImageCacheMaxSize(it) },
                mediaCacheEnabled = mediaCacheEnabled,
                onMediaCacheToggle = { viewModel.setMediaCacheEnabled(it) },
                mediaCacheLimitGb = mediaCacheLimitGb,
                onMediaCacheLimitChange = { viewModel.setMediaCacheLimitGb(it) },
                downloadFolder = downloadFolder,
                onDownloadFolderClick = { downloadFolderPickerLauncher.launch(null) },
                ignoreShortAudio = ignoreShortAudio,
                onIgnoreShortAudioChange = { viewModel.setIgnoreShortAudio(it) },
                ignoreShortAudioDuration = ignoreShortAudioDuration,
                onIgnoreShortAudioDurationChange = { viewModel.setIgnoreShortAudioDuration(it) },
                hiddenFolders = hiddenFolders,
                onAddHiddenFolderClick = { hiddenFolderPickerLauncher.launch(null) },
                onRemoveHiddenFolder = { viewModel.removeHiddenFolder(it) }
            )
        }
        composable(SettingsRoute.BackupRestore.route) {
            BackupRestoreScreen(
                navController = navController,
                onBackupClick = { exportBackupLauncher.launch("SUMITX-backup.json") },
                onRestoreClick = { importBackupLauncher.launch(arrayOf("application/json")) }
            )
        }
        composable(SettingsRoute.Account.route) {
            AccountScreen(
                navController = navController,
                ytCookies = ytCookies,
                onYtCookiesChange = { viewModel.setYtCookies(it) },
                dataSaver = dataSaver,
                onDataSaverChange = { viewModel.setDataSaver(it) },
                pauseHistory = pauseHistory,
                onPauseHistoryChange = { viewModel.setPauseHistory(it) }
            )
        }
        composable(SettingsRoute.Appearance.route) {
            AppearanceScreen(
                navController = navController,
                darkMode = darkMode,
                themeMode = themeMode,
                onThemeModeChange = { viewModel.setThemeMode(it) },
                onDarkModeChange = { viewModel.setDarkMode(it) },
                amoledBlack = amoledBlack,
                onAmoledBlackChange = { viewModel.setAmoledBlack(it) },
                dynamicAlbumTheme = dynamicAlbumTheme,
                onDynamicAlbumThemeChange = { viewModel.setDynamicAlbumTheme(it) },
                appearancePreset = appearancePreset,
                onAppearancePresetChange = { viewModel.setAppearancePreset(it) },
                playerArtworkShape = playerArtworkShape,
                onPlayerArtworkShapeChange = { viewModel.setPlayerArtworkShape(it) },
                playerBackgroundStyle = playerBackgroundStyle,
                onPlayerBackgroundStyleChange = { viewModel.setPlayerBackgroundStyle(it) },
                miniPlayerStyle = miniPlayerStyle,
                onMiniPlayerStyleChange = { viewModel.setMiniPlayerStyle(it) },
                miniPlayerControlShape = miniPlayerControlShape,
                onMiniPlayerControlShapeChange = { viewModel.setMiniPlayerControlShape(it) },
                miniPlayerControlIcons = miniPlayerControlIcons,
                onMiniPlayerControlIconsChange = { viewModel.setMiniPlayerControlIcons(it) },
                miniPlayerProgressStyle = miniPlayerProgressStyle,
                onMiniPlayerProgressStyleChange = { viewModel.setMiniPlayerProgressStyle(it) },
                lyricsTextSize = lyricsTextSize,
                onLyricsTextSizeChange = { viewModel.setLyricsTextSize(it) },
                lyricsAlignment = lyricsAlignment,
                onLyricsAlignmentChange = { viewModel.setLyricsAlignment(it) },
                lyricsFontFamily = lyricsFontFamily,
                onLyricsFontFamilyChange = { viewModel.setLyricsFontFamily(it) },
                uiDensity = uiDensity,
                onUiDensityChange = { viewModel.setUiDensity(it) },
                navLabelsMode = navLabelsMode,
                onNavLabelsModeChange = { viewModel.setNavLabelsMode(it) },
                dynamicNavStyle = dynamicNavStyle,
                onDynamicNavStyleChange = { viewModel.setDynamicNavStyle(it) },
                playerLayout = playerLayout,
                onPlayerLayoutChange = { viewModel.setPlayerLayout(it) },
                playerButtons = playerButtons,
                onPlayerButtonsChange = { viewModel.setPlayerButtons(it) },
                onMovePlayerButton = { from, to -> viewModel.movePlayerButton(from, to) },
                playerTitleAlign = playerTitleAlign,
                onPlayerTitleAlignChange = { viewModel.setPlayerTitleAlign(it) },
                playerTitleSize = playerTitleSize,
                onPlayerTitleSizeChange = { viewModel.setPlayerTitleSize(it) },
                playerTitleWeight = playerTitleWeight,
                onPlayerTitleWeightChange = { viewModel.setPlayerTitleWeight(it) },
                playerTitleScroll = playerTitleScroll,
                onPlayerTitleScrollChange = { viewModel.setPlayerTitleScroll(it) },
                playerTimes = playerTimes,
                onPlayerTimesChange = { viewModel.setPlayerTimes(it) },
                scrubberStyle = scrubberStyle,
                onScrubberStyleChange = { viewModel.setScrubberStyle(it) },
                scrubberColor = scrubberColor,
                onScrubberColorChange = { viewModel.setScrubberColor(it) },
                visualizerStyle = visualizerStyle,
                onVisualizerStyleChange = { viewModel.setVisualizerStyle(it) },
                playerButtonStyle = playerButtonStyle,
                onPlayerButtonStyleChange = { viewModel.setPlayerButtonStyle(it) },
                scrubberThickness = scrubberThickness,
                onScrubberThicknessChange = { viewModel.setScrubberThickness(it) },
                scrubberThumbStyle = scrubberThumbStyle,
                onScrubberThumbStyleChange = { viewModel.setScrubberThumbStyle(it) },
                visualizerColor = visualizerColor,
                onVisualizerColorChange = { viewModel.setVisualizerColor(it) }
            )
        }
        composable(SettingsRoute.Controls.route) {
            ControlsAndGesturesScreen(
                navController = navController,
                playerViewModel = playerSharedViewModel
            )
        }
        composable(SettingsRoute.Playback.route) {
            PlaybackScreen(
                navController = navController,
                crossfadeEnabled = crossfadeEnabled,
                onCrossfadeToggle = { viewModel.setCrossfadeEnabled(it) },
                crossfadeDuration = crossfadeDuration,
                onCrossfadeDurationChange = { viewModel.setCrossfadeDuration(it) },
                audioFocus = audioFocus,
                onAudioFocusToggle = { viewModel.setAudioFocus(it) },
                autoplayEnabled = autoplayEnabled,
                onAutoplayToggle = { enabled ->
                    viewModel.setAutoplayEnabled(enabled)
                    if (!enabled) playerSharedViewModel.removeAutoplayRecommendationsFromQueue()
                },
                sleepTimerEndTime = sleepTimerEndTime,
                sleepTimerMode = sleepTimerMode,
                onStartSleepTimer = { playerSharedViewModel.startCustomSleepTimer(it) },
                onCancelSleepTimer = { playerSharedViewModel.cancelSleepTimer() },
                onSetEndOfSong = { enable ->
                    if (enable) playerSharedViewModel.setSleepTimerMode(PlayerSharedViewModel.SleepTimerMode.END_OF_SONG)
                    else playerSharedViewModel.cancelSleepTimer()
                },
                audioQuality = audioQuality,
                onAudioQualityChange = { viewModel.setAudioQuality(it) },
                playbackAudioFormat = playbackAudioFormat,
                onPlaybackAudioFormatChange = { viewModel.setPlaybackAudioFormat(it) },
                downloadAudioFormat = downloadAudioFormat,
                onDownloadAudioFormatChange = { viewModel.setDownloadAudioFormat(it) },
                downloadBitrate = downloadBitrate,
                onDownloadBitrateChange = { viewModel.setDownloadBitrate(it) },
            )
        }
        composable(SettingsRoute.About.route) {
            AboutScreen(
                navController = navController,
                onCheckUpdate = {
                    (context as? Activity)?.let { activity ->
                        (context as? MainActivity)?.updateManager?.checkForUpdate(
                            activity.findViewById(android.R.id.content),
                            BuildConfig.VERSION_NAME
                        )
                    }
                },
                onWhatsNew = { navController.navigate(SettingsRoute.WhatsNew.route) },
                onOpenUrl = { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                }
            )
        }
        composable(SettingsRoute.Feedback.route) {
            FeedbackScreen(navController = navController)
        }
        composable(SettingsRoute.WhatsNew.route) {
            WhatsNewScreen(navController = navController)
        }
    }

    androidx.activity.compose.BackHandler(enabled = canPop && !isPlayerExpanded) {
        navController.popBackStack()
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPOSE UI — MAIN SETTINGS BODY
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDetailTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

sealed class SettingsRoute(val route: String) {
    object Main : SettingsRoute("main")
    object MusicManagement : SettingsRoute("music_management")
    object Account : SettingsRoute("account")
    object Appearance : SettingsRoute("appearance")
    object Playback : SettingsRoute("playback")
    object Controls : SettingsRoute("controls")
    object About : SettingsRoute("about")
    object Feedback : SettingsRoute("feedback")
    object WhatsNew : SettingsRoute("whats_new")
    object BackupRestore : SettingsRoute("backup_restore")
}

@Composable
private fun ControlsAndGesturesScreen(
    navController: androidx.navigation.NavController,
    playerViewModel: PlayerSharedViewModel
) {
    val gestures by playerViewModel.playerGestureSettings.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { SettingsDetailTopBar("Controls & Gestures") { navController.popBackStack() } }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 120.dp)
        ) {
            SettingsHeader("Player cover")
            Text(
                "Choose what swipes, a double tap and a long press do on the full player's artwork. Swiping down still collapses the player.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SettingsGroupCard(listOf(
                {
                    GestureSettingRow(
                        "Swipe left",
                        gestures.coverSwipeLeft,
                        PlayerGestureAction.choices(),
                        onSelect = { playerViewModel.setPlayerGesture("cover_swipe_left", it) }
                    )
                },
                {
                    GestureSettingRow(
                        "Swipe right",
                        gestures.coverSwipeRight,
                        PlayerGestureAction.choices(),
                        onSelect = { playerViewModel.setPlayerGesture("cover_swipe_right", it) }
                    )
                },
                {
                    GestureSettingRow(
                        "Swipe up",
                        gestures.coverSwipeUp,
                        PlayerGestureAction.choices(),
                        onSelect = { playerViewModel.setPlayerGesture("cover_swipe_up", it) }
                    )
                },
                {
                    GestureSettingRow(
                        "Double tap",
                        gestures.coverDoubleTap,
                        PlayerGestureAction.choices(doubleTap = true),
                        onSelect = {
                            playerViewModel.setPlayerGesture("cover_double_tap", it)
                            if (it == PlayerGestureAction.SEEK_SIDES && gestures.doubleTapSeekSeconds < 3) {
                                playerViewModel.setDoubleTapSeekSeconds(10)
                            }
                        }
                    )
                },
                {
                    GestureSettingRow(
                        "Long press",
                        gestures.coverLongPress,
                        PlayerGestureAction.choices(),
                        onSelect = { playerViewModel.setPlayerGesture("cover_long_press", it) }
                    )
                }
            ))
            if (gestures.coverDoubleTap == PlayerGestureAction.SEEK_SIDES) {
                SettingsHeader("Double-tap seek")
                SettingsGroupCard(listOf {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text("Seconds to seek", style = MaterialTheme.typography.bodyLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(3, 5, 10, 15).forEach { seconds ->
                                val selected = gestures.doubleTapSeekSeconds == seconds
                                Surface(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { playerViewModel.setDoubleTapSeekSeconds(seconds) },
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        "$seconds s",
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                })
            }
            SettingsHeader("Mini player")
            Text(
                "These actions apply while the player is collapsed. A tap still opens the full player.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SettingsGroupCard(listOf(
                {
                    GestureSettingRow(
                        "Swipe left",
                        gestures.miniSwipeLeft,
                        PlayerGestureAction.choices(mini = true),
                        onSelect = { playerViewModel.setPlayerGesture("mini_swipe_left", it) }
                    )
                },
                {
                    GestureSettingRow(
                        "Swipe right",
                        gestures.miniSwipeRight,
                        PlayerGestureAction.choices(mini = true),
                        onSelect = { playerViewModel.setPlayerGesture("mini_swipe_right", it) }
                    )
                },
                {
                    GestureSettingRow(
                        "Swipe up",
                        gestures.miniSwipeUp,
                        PlayerGestureAction.choices(mini = true),
                        onSelect = { playerViewModel.setPlayerGesture("mini_swipe_up", it) }
                    )
                },
                {
                    GestureSettingRow(
                        "Pull down",
                        gestures.miniSwipeDown,
                        PlayerGestureAction.choices(mini = true),
                        onSelect = { playerViewModel.setPlayerGesture("mini_swipe_down", it) }
                    )
                }
            ))
            SettingsHeader("Default actions")
            SettingsGroupCard(listOf {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            playerViewModel.setPlayerGesture("cover_swipe_left", PlayerGestureAction.NEXT)
                            playerViewModel.setPlayerGesture("cover_swipe_right", PlayerGestureAction.PREVIOUS)
                            playerViewModel.setPlayerGesture("cover_swipe_up", PlayerGestureAction.QUEUE)
                            playerViewModel.setPlayerGesture("cover_double_tap", PlayerGestureAction.SEEK_SIDES)
                            playerViewModel.setPlayerGesture("cover_long_press", PlayerGestureAction.NONE)
                            playerViewModel.setPlayerGesture("mini_swipe_left", PlayerGestureAction.NEXT)
                            playerViewModel.setPlayerGesture("mini_swipe_right", PlayerGestureAction.PREVIOUS)
                            playerViewModel.setPlayerGesture("mini_swipe_up", PlayerGestureAction.OPEN)
                            playerViewModel.setPlayerGesture("mini_swipe_down", PlayerGestureAction.STOP)
                            playerViewModel.setDoubleTapSeekSeconds(10)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Restore Prism-style defaults", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            })
        }
    }
}

@Composable
private fun GestureSettingRow(
    title: String,
    selected: PlayerGestureAction,
    options: List<PlayerGestureAction>,
    onSelect: (PlayerGestureAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(selected.label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Default.ChevronRight, contentDescription = "Change $title", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp).size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    onClick = {
                        expanded = false
                        onSelect(action)
                    },
                    trailingIcon = if (action == selected) {
                        { Icon(Icons.Default.Refresh, contentDescription = "Selected") }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun SettingsMainScreen(navController: androidx.navigation.NavController) {
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val mainActivity = context as? MainActivity

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset, available: Offset, source: NestedScrollSource
            ): Offset {
                if (!isLandscape && mainActivity != null) {
                    val delta = consumed.y
                    if (delta < -30f) mainActivity.hideBottomNavWithAnimation()
                    else if (delta > 30f) mainActivity.showBottomNavWithAnimation()
                }
                return super.onPostScroll(consumed, available, source)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .then(if (isLandscape) Modifier.padding(start = 88.dp) else Modifier)
            .nestedScroll(nestedScrollConnection)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
            .padding(bottom = 120.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 24.dp)
        )

        SettingsGroupCard(
            items = listOf(
                {
                    SettingsCategoryItem(
                        title = "Account & Sync",
                        subtitle = "YouTube Music login, history sync",
                        icon = Icons.Rounded.AccountCircle,
                        onClick = { navController.navigate(SettingsRoute.Account.route) }
                    )
                },
                {
                    SettingsCategoryItem(
                        title = "Playback",
                        subtitle = "Audio behavior, crossfade, audio focus",
                        icon = Icons.Rounded.PlayCircle,
                        onClick = { navController.navigate(SettingsRoute.Playback.route) }
                    )
                },
                {
                    SettingsCategoryItem(
                        title = "Controls & Gestures",
                        subtitle = "Player swipes, double tap, long press and mini-player actions",
                        icon = Icons.Rounded.TouchApp,
                        onClick = { navController.navigate(SettingsRoute.Controls.route) }
                    )
                },
                {
                    SettingsCategoryItem(
                        title = "Music Management",
                        subtitle = "Manage folders, refresh library, storage",
                        icon = Icons.Rounded.LibraryMusic,
                        onClick = { navController.navigate(SettingsRoute.MusicManagement.route) }
                    )
                },
                {
                    SettingsCategoryItem(
                        title = "Customize",
                        subtitle = "Theme, colors, icons, player, lyrics, typography and navigation",
                        icon = Icons.Rounded.Palette,
                        onClick = { navController.navigate(SettingsRoute.Appearance.route) }
                    )
                },
                {
                    SettingsCategoryItem(
                        title = "Backup & Restore",
                        subtitle = "Export and import your library and settings",
                        icon = Icons.Rounded.SettingsBackupRestore,
                        onClick = { navController.navigate(SettingsRoute.BackupRestore.route) }
                    )
                },
                {
                    SettingsCategoryItem(
                        title = "Feedback & Bug Reports",
                        subtitle = "Report issues, request features, export logs",
                        icon = Icons.Rounded.BugReport,
                        onClick = { navController.navigate(SettingsRoute.Feedback.route) }
                    )
                },
                {
                    SettingsCategoryItem(
                        title = "About",
                        subtitle = "App version, credits, updates",
                        icon = Icons.Rounded.Info,
                        onClick = { navController.navigate(SettingsRoute.About.route) }
                    )
                }
            )
        )
        
        AppLogoSection(isLandscape = isLandscape)
    }
}

@Composable
private fun SettingsCategoryItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun MusicManagementScreen(
    navController: androidx.navigation.NavController,
    libraryPaths: List<String>,
    onRemovePath: (String) -> Unit,
    onAddPathClick: () -> Unit,
    songCacheSize: String,
    imageCacheSize: String,
    onClearSongCache: () -> Unit,
    onClearImageCache: () -> Unit,
    ignoreShortAudio: Boolean,
    onIgnoreShortAudioChange: (Boolean) -> Unit,
    ignoreShortAudioDuration: Float,
    onIgnoreShortAudioDurationChange: (Float) -> Unit,
    songCacheMaxSize: Int,
    onSongCacheMaxSizeChange: (Int) -> Unit,
    imageCacheMaxSize: Int,
    onImageCacheMaxSizeChange: (Int) -> Unit,
    mediaCacheEnabled: Boolean,
    onMediaCacheToggle: (Boolean) -> Unit,
    mediaCacheLimitGb: Int,
    onMediaCacheLimitChange: (Int) -> Unit,
    downloadFolder: String?,
    onDownloadFolderClick: () -> Unit,
    hiddenFolders: List<String>,
    onAddHiddenFolderClick: () -> Unit,
    onRemoveHiddenFolder: (String) -> Unit
) {
    Scaffold(
        topBar = { SettingsDetailTopBar("Music Management") { navController.popBackStack() } }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp)
        ) {
            SettingsHeader(stringResource(R.string.settings_header_library))
            SettingsGroupCard(buildList {
                add { LibrarySourceHeader() }
                libraryPaths.forEach { path ->
                    add { LibraryPathRow(path, onRemovePath) }
                }
                if (libraryPaths.isEmpty()) {
                    add { LibraryPathRow(stringResource(R.string.setting_music_source_default), null) }
                }
                add { AddMorePathRow(onAddPathClick) }
            })

            SettingsHeader("Audio Filtering")
            SettingsGroupCard(buildList {
                add {
                    IgnoreShortAudioRow(ignoreShortAudio, onIgnoreShortAudioChange)
                }
                if (ignoreShortAudio) {
                    add {
                        IgnoreShortAudioDurationRow(ignoreShortAudioDuration, onIgnoreShortAudioDurationChange)
                    }
                }
            })

            SettingsHeader("Hidden Folders")
            SettingsGroupCard(buildList {
                hiddenFolders.forEach { folder ->
                    add { HiddenFolderRow(folder, onRemoveHiddenFolder) }
                }
                if (hiddenFolders.isEmpty()) {
                    add {
                        ListItem(
                            headlineContent = { Text("No folders blacklisted", style = MaterialTheme.typography.bodyLarge) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
                add {
                    AddHiddenFolderRow(onAddHiddenFolderClick)
                }
            })

            SettingsHeader("Downloads")
            SettingsGroupCard(listOf(
                { DownloadFolderRow(downloadFolder, onDownloadFolderClick) }
            ))

            SettingsHeader(stringResource(R.string.settings_header_storage))
            SettingsGroupCard(buildList {
                add { SongCacheSizeRow(songCacheSize, songCacheMaxSize, onSongCacheMaxSizeChange, onClearSongCache) }
                add { ImageCacheSizeRow(imageCacheSize, imageCacheMaxSize, onImageCacheMaxSizeChange, onClearImageCache) }
                add {
                    ListItem(
                        modifier = Modifier.clickable { onMediaCacheToggle(!mediaCacheEnabled) },
                        headlineContent = { Text("Online audio cache", fontWeight = FontWeight.SemiBold) },
                        supportingContent = {
                            Text(if (mediaCacheEnabled) "Cache streamed audio for faster repeat playback."
                                else "Disabled. New stream data is not retained.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingContent = { Switch(checked = mediaCacheEnabled, onCheckedChange = onMediaCacheToggle) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                if (mediaCacheEnabled) {
                    add {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("Stream cache limit", style = MaterialTheme.typography.bodyLarge)
                                Text(mediaCacheLimitGb.toString() + " GB", style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(value = mediaCacheLimitGb.toFloat(),
                                onValueChange = { onMediaCacheLimitChange(it.toInt().coerceIn(1, 10)) },
                                valueRange = 1f..10f, steps = 8)
                            Text("Limit is applied immediately. Oldest cached audio is evicted first when exceeded.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            })
        }
    }
}

@Composable
private fun BackupRestoreScreen(
    navController: androidx.navigation.NavController,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    Scaffold(
        topBar = { SettingsDetailTopBar("Backup & Restore") { navController.popBackStack() } }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp)
        ) {
            SettingsHeader("Backup & Restore")
            SettingsGroupCard(buildList {
                add {
                    ListItem(
                        onClick = onBackupClick,
                        content = {
                            Column {
                                Text("Export Settings & Library", style = MaterialTheme.typography.bodyLarge)
                                Text("Export playlists, favorites, and preferences to JSON", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_download),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                add {
                    ListItem(
                        onClick = onRestoreClick,
                        content = {
                            Column {
                                Text("Import Settings & Library", style = MaterialTheme.typography.bodyLarge)
                                Text("Restore playlists, favorites, and preferences from JSON", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_download),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = 180f }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            })
        }
    }
}

@Composable
private fun IgnoreShortAudioRow(checked: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        onClick = { onToggle(!checked) },
        content = {
            Column {
                Text(
                    text = "Ignore Short Audio (Voice Notes)",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Hide voice notes and short recordings from library",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onToggle
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun IgnoreShortAudioDurationRow(
    value: Float, onValueChange: (Float) -> Unit
) {
    ListItem(
        onClick = { },
        content = {
            Column {
                Text(
                    text = "Minimum Duration Threshold",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Ignore tracks under ${value.toInt()} seconds",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        supportingContent = {
            Slider(
                value = value.coerceIn(10f, 120f),
                onValueChange = onValueChange,
                valueRange = 10f..120f,
                steps = 10,
                modifier = Modifier.fillMaxWidth()
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun HiddenFolderRow(path: String, onRemove: (String) -> Unit) {
    ListItem(
        onClick = { },
        content = {
            Text(
                text = path.substringAfterLast("/"),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = path,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_folder_open),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = {
            Text(
                text = "Remove",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onRemove(path) }
                    .padding(8.dp)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun AddHiddenFolderRow(onClick: () -> Unit) {
    ListItem(
        onClick = onClick,
        content = {
            Text(
                text = "Add Hidden Folder",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun AccountScreen(
    navController: androidx.navigation.NavController,
    ytCookies: String?,
    onYtCookiesChange: (String?) -> Unit,
    dataSaver: Boolean,
    onDataSaverChange: (Boolean) -> Unit,
    pauseHistory: Boolean,
    onPauseHistoryChange: (Boolean) -> Unit
) {
    Scaffold(
        topBar = { SettingsDetailTopBar("Account & Sync") { navController.popBackStack() } }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp)
        ) {
            SettingsHeader("YouTube Music Account")
            SettingsGroupCard(listOf(
                {
                    var showLoginDialog by remember { mutableStateOf(false) }

                    if (showLoginDialog) {
                        YouTubeMusicLoginDialog(
                            onDismissRequest = { showLoginDialog = false },
                            onLoginSuccess = { cookies ->
                                onYtCookiesChange(cookies)
                                showLoginDialog = false
                            }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (ytCookies != null) "Logged in to YouTube Music" else "Guest Mode (Anonymous)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (ytCookies != null) {
                                Text(
                                    text = "Profile: SUMITX User",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (ytCookies != null) "Accessing personalized playlists, library, and recommended interests." else "No account linked. Tap to sign in securely.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                if (ytCookies != null) {
                                    onYtCookiesChange(null)
                                } else {
                                    showLoginDialog = true
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (ytCookies != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (ytCookies != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text(if (ytCookies != null) "Log Out" else "Log In")
                        }
                    }
                }
            ))

            SettingsHeader("Data Saving")
            SettingsGroupCard(listOf(
                { DataSaverRow(dataSaver, onDataSaverChange) }
            ))

            SettingsHeader("Sync & Privacy")
            SettingsGroupCard(listOf(
                { PauseHistoryRow(pauseHistory, onPauseHistoryChange) },
                { ManualSyncRow() }
            ))
        }
    }
}

@Composable
private fun DataSaverRow(checked: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        onClick = { onToggle(!checked) },
        content = {
            Column {
                Text(
                    text = "Data Saver (Wi-Fi Only)",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Restrict streaming and high-res cover art downloads to Wi-Fi only",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onToggle
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun PauseHistoryRow(checked: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        onClick = { onToggle(!checked) },
        content = {
            Column {
                Text(
                    text = "Pause Listening History",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Stop tracking played songs for recommendations",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onToggle
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun ManualSyncRow() {
    var isSyncing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ListItem(
        onClick = {
            if (!isSyncing) {
                isSyncing = true
                scope.launch {
                    delay(2000) // Simulate sync
                    isSyncing = false
                }
            }
        },
        content = {
            Column {
                Text(text = "Manual Sync", style = MaterialTheme.typography.bodyLarge)
                Text(text = "Sync playlists and favorites with server", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        trailingContent = {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync Now",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun AppearanceScreen(
    navController: androidx.navigation.NavController,
    darkMode: Boolean,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    amoledBlack: Boolean,
    onAmoledBlackChange: (Boolean) -> Unit,
    dynamicAlbumTheme: Boolean,
    onDynamicAlbumThemeChange: (Boolean) -> Unit,
    appearancePreset: String,
    onAppearancePresetChange: (String) -> Unit,
    playerArtworkShape: String,
    onPlayerArtworkShapeChange: (String) -> Unit,
    playerBackgroundStyle: String,
    onPlayerBackgroundStyleChange: (String) -> Unit,
    miniPlayerStyle: String,
    onMiniPlayerStyleChange: (String) -> Unit,
    miniPlayerControlShape: String,
    onMiniPlayerControlShapeChange: (String) -> Unit,
    miniPlayerControlIcons: String,
    onMiniPlayerControlIconsChange: (String) -> Unit,
    miniPlayerProgressStyle: String,
    onMiniPlayerProgressStyleChange: (String) -> Unit,
    lyricsTextSize: String,
    onLyricsTextSizeChange: (String) -> Unit,
    lyricsAlignment: String,
    onLyricsAlignmentChange: (String) -> Unit,
    lyricsFontFamily: String,
    onLyricsFontFamilyChange: (String) -> Unit,
    uiDensity: String,
    onUiDensityChange: (String) -> Unit,
    navLabelsMode: String,
    onNavLabelsModeChange: (String) -> Unit,
    dynamicNavStyle: Boolean,
    onDynamicNavStyleChange: (Boolean) -> Unit,
    playerLayout: String,
    onPlayerLayoutChange: (String) -> Unit,
    playerButtons: List<String>,
    onPlayerButtonsChange: (List<String>) -> Unit,
    onMovePlayerButton: (Int, Int) -> Unit,
    playerTitleAlign: String,
    onPlayerTitleAlignChange: (String) -> Unit,
    playerTitleSize: String,
    onPlayerTitleSizeChange: (String) -> Unit,
    playerTitleWeight: String,
    onPlayerTitleWeightChange: (String) -> Unit,
    playerTitleScroll: Boolean,
    onPlayerTitleScrollChange: (Boolean) -> Unit,
    playerTimes: String,
    onPlayerTimesChange: (String) -> Unit,
    scrubberStyle: String,
    onScrubberStyleChange: (String) -> Unit,
    scrubberColor: String,
    onScrubberColorChange: (String) -> Unit,
    visualizerStyle: String,
    onVisualizerStyleChange: (String) -> Unit,
    playerButtonStyle: String,
    onPlayerButtonStyleChange: (String) -> Unit,
    scrubberThickness: Float,
    onScrubberThicknessChange: (Float) -> Unit,
    scrubberThumbStyle: String,
    onScrubberThumbStyleChange: (String) -> Unit,
    visualizerColor: String,
    onVisualizerColorChange: (String) -> Unit
) {
    Scaffold(
        topBar = { SettingsDetailTopBar("Customize") { navController.popBackStack() } }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp)
        ) {
            Text(
                "Make the player and interface your own: choose a layout, arrange its buttons, tune typography and progress, and set its background.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            SettingsHeader("Theme")
            SettingsGroupCard(buildList {
                add {
                    SettingsChoiceRow(
                        title = "Theme mode",
                        description = "System follows your device setting. Light and Dark stay fixed until you change them.",
                        selected = themeMode.replaceFirstChar { it.uppercase() },
                        options = listOf("System", "Light", "Dark"),
                        onSelected = { onThemeModeChange(it.lowercase()) }
                    )
                }
                if (darkMode) add { AmoledBlackRow(amoledBlack, onAmoledBlackChange) }
                add { DynamicAlbumThemeRow(dynamicAlbumTheme, onDynamicAlbumThemeChange) }
            })

            SettingsHeader("Colors")
            SettingsGroupCard(listOf {
                SettingsChoiceRow(
                    title = "Accent palette",
                    description = "Choose the app-wide accent. Dynamic colors can still tint the interface from the current artwork.",
                    selected = appearancePreset,
                    options = listOf("Ocean", "Violet", "Emerald", "Amber", "Rose", "Monochrome", "Crimson", "Teal", "Cyan", "Indigo", "Lime", "Graphite"),
                    onSelected = onAppearancePresetChange
                )
            })

            SettingsHeader("Player layout")
            SettingsGroupCard(listOf {
                SettingsChoiceRow(
                    title = "Layout preset",
                    description = "Standard keeps the balanced player; Cover enlarges artwork; Minimal reduces artwork; Vinyl and Cassette change the artwork presentation.",
                    selected = playerLayout,
                    options = listOf("Standard", "Cover", "Minimal", "Vinyl", "Cassette"),
                    onSelected = onPlayerLayoutChange
                )
            })

            SettingsHeader("Player buttons")
            SettingsGroupCard(listOf {
                SettingsChoiceRow(
                    title = "Button presentation",
                    description = "Choose capsule buttons, soft rounded buttons or a compact icon-first row.",
                    selected = playerButtonStyle,
                    options = listOf("Pill", "Rounded", "Minimal"),
                    onSelected = onPlayerButtonStyleChange
                )
            })
            PlayerButtonsSettings(
                selected = playerButtons,
                onSelectionChanged = onPlayerButtonsChange,
                onMove = onMovePlayerButton
            )

            SettingsHeader("Title and times")
            SettingsGroupCard(listOf(
                {
                    SettingsChoiceRow(
                        title = "Title alignment",
                        description = "Align the title to the artwork's left edge or center it.",
                        selected = playerTitleAlign,
                        options = listOf("Left", "Centered"),
                        onSelected = onPlayerTitleAlignChange
                    )
                },
                {
                    SettingsChoiceRow(
                        title = "Title size",
                        description = "Adjust the song title scale independently.",
                        selected = playerTitleSize,
                        options = listOf("Small", "Medium", "Large"),
                        onSelected = onPlayerTitleSizeChange
                    )
                },
                {
                    SettingsChoiceRow(
                        title = "Title weight",
                        description = "Choose light, regular or bold lettering.",
                        selected = playerTitleWeight,
                        options = listOf("Light", "Regular", "Bold"),
                        onSelected = onPlayerTitleWeightChange
                    )
                },
                {
                    ToggleSettingRow(
                        title = "Scrolling titles",
                        description = "Long titles scroll instead of ending in an ellipsis.",
                        checked = playerTitleScroll,
                        onCheckedChange = onPlayerTitleScrollChange
                    )
                },
                {
                    SettingsChoiceRow(
                        title = "Time labels",
                        description = "Display elapsed time, remaining time, both or neither.",
                        selected = playerTimes,
                        options = listOf("Both", "Elapsed", "Time left", "None"),
                        onSelected = onPlayerTimesChange
                    )
                }
            ))

            SettingsHeader("Progress bar")
            SettingsGroupCard(listOf(
                {
                    SettingsChoiceRow(
                        title = "Progress style",
                        description = "Choose a fine line, a thicker line, a thumb or the animated waveform.",
                        selected = scrubberStyle,
                        options = listOf("Hairline", "Bold", "Thumb", "Waveform"),
                        onSelected = onScrubberStyleChange
                    )
                },
                {
                    SettingsChoiceRow(
                        title = "Progress color",
                        description = "Use the accent color, white or the cover-derived color.",
                        selected = scrubberColor,
                        options = listOf("Accent", "White", "Cover"),
                        onSelected = onScrubberColorChange
                    )
                },
                {
                    SettingsChoiceRow(
                        title = "Thumb style",
                        description = "Choose a dot, line, ring, automatic thumb or no thumb.",
                        selected = scrubberThumbStyle,
                        options = listOf("Auto", "Dot", "Line", "Ring", "None"),
                        onSelected = onScrubberThumbStyleChange
                    )
                },
                {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text("Track thickness: " + scrubberThickness.toString() + "×")
                        Slider(
                            value = scrubberThickness,
                            onValueChange = onScrubberThicknessChange,
                            valueRange = 0.6f..2f
                        )
                    }
                }
            ))

            SettingsHeader("Visualizer")
            SettingsGroupCard(listOf {
                SettingsChoiceRow(
                    title = "Player animation",
                    description = "Playback-state animation only. It does not use the microphone or record audio. Off uses the least resources.",
                    selected = visualizerStyle,
                    options = listOf("Off", "Bars", "Wave", "Ring", "Pulse", "Glow"),
                    onSelected = onVisualizerStyleChange
                )
            })
            SettingsGroupCard(listOf {
                SettingsChoiceRow(
                    title = "Visualizer color",
                    description = "Match the accent, use white, follow the artwork or cycle through a spectrum.",
                    selected = visualizerColor,
                    options = listOf("Accent", "White", "Cover", "Spectrum"),
                    onSelected = onVisualizerColorChange
                )
            })

            SettingsHeader("Player appearance")
            SettingsGroupCard(listOf(
                {
                    SettingsChoiceRow(
                        title = "Background treatment",
                        description = "Artwork, theme, black or a soft gradient based on the current cover.",
                        selected = playerBackgroundStyle,
                        options = listOf("Artwork", "Theme", "Black", "Gradient"),
                        onSelected = onPlayerBackgroundStyleChange
                    )
                },
                {
                    SettingsChoiceRow(
                        title = "Mini player style",
                        description = "Card, flat or compact mini-player surfaces.",
                        selected = miniPlayerStyle,
                        options = listOf("Card", "Flat", "Compact", "Pill"),
                        onSelected = onMiniPlayerStyleChange
                    )
                },
                {
                    SettingsChoiceRow(
                        title = "Mini-player button shape",
                        description = "Change the play, pause, previous and next control backgrounds independently of the mini-player card.",
                        selected = miniPlayerControlShape,
                        options = listOf("Circle", "Rounded", "Pill", "None"),
                        onSelected = onMiniPlayerControlShapeChange
                    )
                },
                {
                    SettingsChoiceRow(
                        title = "Mini-player icon set",
                        description = "Choose classic, chevron or seek-style icons for previous, play/pause and next.",
                        selected = miniPlayerControlIcons,
                        options = listOf("Classic", "Chevron", "Seek"),
                        onSelected = onMiniPlayerControlIconsChange
                    )
                },
                {
                    SettingsChoiceRow(
                        title = "Mini-player progress",
                        description = "Use a circular ring, a slim line, a filling track, or hide progress around the mini-player artwork.",
                        selected = miniPlayerProgressStyle,
                        options = listOf("Ring", "Line", "Fill", "None"),
                        onSelected = onMiniPlayerProgressStyleChange
                    )
                },
                {
                    SettingsChoiceRow(
                        title = "Player artwork shape",
                        description = "Rounded, square, circular, squircle, flower, cookie, arch or slanted album artwork.",
                        selected = playerArtworkShape,
                        options = listOf("Rounded", "Square", "Circle", "Squircle", "Flower", "Cookie", "Arch", "Slanted"),
                        onSelected = onPlayerArtworkShapeChange
                    )
                }
            ))

            SettingsHeader("Lyrics")
            SettingsGroupCard(listOf(
                {
                    SettingsChoiceRow(
                        title = "Lyrics text size",
                        description = "Change the size of synced and plain lyric lines.",
                        selected = lyricsTextSize,
                        options = listOf("Small", "Standard", "Large"),
                        onSelected = onLyricsTextSizeChange
                    )
                },
                {
                    SettingsChoiceRow(
                        title = "Lyrics alignment",
                        description = "Keep each line's original alignment, or align all lyric lines to the left or center.",
                        selected = lyricsAlignment,
                        options = listOf("Original", "Left", "Center"),
                        onSelected = onLyricsAlignmentChange
                    )
                },
                {
                    SettingsChoiceRow(
                        title = "Lyrics font",
                        description = "Choose rounded, neutral sans-serif, serif or monospaced lyric lettering.",
                        selected = lyricsFontFamily,
                        options = listOf("Rounded", "Sans", "Serif", "Mono"),
                        onSelected = onLyricsFontFamilyChange
                    )
                }
            ))

            SettingsHeader("Typography & Layout")
            SettingsGroupCard(listOf {
                SettingsChoiceRow(
                    title = "Interface density",
                    description = "Compact fits more content. Large increases the app's text scale and touch-target spacing.",
                    selected = uiDensity,
                    options = listOf("Compact", "Comfortable", "Large"),
                    onSelected = onUiDensityChange
                )
            })

            SettingsHeader("Navigation")
            SettingsGroupCard(buildList {
                add {
                    SettingsChoiceRow(
                        title = "Navigation labels",
                        description = "Always show every label, show only the selected tab's label, or hide all labels.",
                        selected = navLabelsMode,
                        options = listOf("Always", "Selected", "Never"),
                        onSelected = onNavLabelsModeChange
                    )
                }
                add { DynamicNavStyleRow(dynamicNavStyle, onDynamicNavStyleChange) }
            })
        }
    }
}

private val playerButtonNames = linkedMapOf(
    "lyrics" to "Lyrics",
    "video" to "Video",
    "queue" to "Up next",
    "share" to "Share",
    "download" to "Download",
    "like" to "Like",
    "dislike" to "Not for me",
    "sleep" to "Sleep timer"
)

@Composable
private fun PlayerButtonsSettings(
    selected: List<String>,
    onSelectionChanged: (List<String>) -> Unit,
    onMove: (Int, Int) -> Unit
) {
    SettingsGroupCard(listOf {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Choose 3 to 6 actions and arrange their order in the player.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val orderedSelected = selected.filter { it in playerButtonNames.keys }
            Text(
                "Shown in player · in this order",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )
            // Render the selected actions from the saved list, not the master lookup map.
            // Iterating playerButtonNames here used to keep the original display order even
            // when the stored order changed, making the reorder controls appear broken.
            orderedSelected.forEachIndexed { index, key ->
                val label = playerButtonNames[key] ?: key
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(
                        checked = true,
                        onCheckedChange = { enabled ->
                            if (!enabled && orderedSelected.size > 3) {
                                onSelectionChanged(orderedSelected - key)
                            }
                        }
                    )
                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(
                        onClick = { onMove(index, index - 1) },
                        enabled = index > 0
                    ) { Text("↑") }
                    TextButton(
                        onClick = { onMove(index, index + 1) },
                        enabled = index < orderedSelected.lastIndex
                    ) { Text("↓") }
                }
            }

            val availableActions = playerButtonNames.filterKeys { it !in orderedSelected }
            if (availableActions.isNotEmpty()) {
                Text(
                    "Available actions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                )
                availableActions.forEach { (key, label) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = false,
                            enabled = orderedSelected.size < 6,
                            onCheckedChange = { enabled ->
                                if (enabled && orderedSelected.size < 6) {
                                    onSelectionChanged(orderedSelected + key)
                                }
                            }
                        )
                        Text(
                            label,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(
                "${selected.size}/6 selected",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall
            )
            TextButton(onClick = {
                onSelectionChanged(listOf("lyrics", "video", "queue", "share", "download"))
            }) { Text("Restore default buttons") }
        }
    })
}

@Composable
private fun ToggleSettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsChoiceRow(
    title: String,
    description: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    // Keep the selector on its own full-width row. Putting all six palette/shape
    // chips in ListItem.trailingContent leaves only a few pixels on phone layouts,
    // which clips every option down to its first letter.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val active = selected == option
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelected(option) },
                    shape = RoundedCornerShape(50),
                    color = if (active) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = option,
                        maxLines = 1,
                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DynamicNavStyleRow(checked: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        onClick = { onToggle(!checked) },
        content = {
            Column {
                Text(
                    text = "Dynamic Navbar Style",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Compact height with bold, elevated icons",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onToggle
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun HideNavLabelsRow(checked: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        onClick = { onToggle(!checked) },
        content = {
            Column {
                Text(
                    text = "Hide Navigation Labels",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Remove text labels from the bottom navigation bar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onToggle
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun DynamicAlbumThemeRow(checked: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        onClick = { onToggle(!checked) },
        content = {
            Column {
                Text(
                    text = "Dynamic colors",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Extract theme colors from the currently playing album art",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Rounded.Palette,
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onToggle
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun AmoledBlackRow(checked: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        onClick = { onToggle(!checked) },
        content = {
            Column {
                Text(
                    text = "Pure AMOLED Black",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Pitch black background in dark mode to save battery",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onToggle
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun PlaybackScreen(
    navController: androidx.navigation.NavController,
    crossfadeEnabled: Boolean,
    onCrossfadeToggle: (Boolean) -> Unit,
    crossfadeDuration: Float,
    onCrossfadeDurationChange: (Float) -> Unit,
    audioFocus: Boolean,
    onAudioFocusToggle: (Boolean) -> Unit,
    autoplayEnabled: Boolean,
    onAutoplayToggle: (Boolean) -> Unit,
    sleepTimerEndTime: Long,
    sleepTimerMode: PlayerSharedViewModel.SleepTimerMode,
    onStartSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onSetEndOfSong: (Boolean) -> Unit,
    audioQuality: String,
    onAudioQualityChange: (String) -> Unit,
    playbackAudioFormat: String,
    onPlaybackAudioFormatChange: (String) -> Unit,
    downloadAudioFormat: String,
    onDownloadAudioFormatChange: (String) -> Unit,
    downloadBitrate: String,
    onDownloadBitrateChange: (String) -> Unit,
) {
    Scaffold(
        topBar = { SettingsDetailTopBar("Playback") { navController.popBackStack() } }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp)
        ) {
            SettingsHeader("Autoplay")
            SettingsGroupCard(listOf(
                { AutoplayRow(autoplayEnabled, onAutoplayToggle) }
            ))

            SettingsHeader("Crossfade")
            SettingsGroupCard(listOf(
                { CrossfadeRow(crossfadeEnabled, onCrossfadeToggle, crossfadeDuration, onCrossfadeDurationChange) }
            ))

            SettingsHeader("Playback Format")
            SettingsGroupCard(listOf(
                { AudioFormatRow("Playback Format", playbackAudioFormat, onPlaybackAudioFormatChange) }
            ))

            SettingsHeader("Playback Bitrate")
            SettingsGroupCard(listOf(
                { AudioQualityRow("Playback Bitrate", audioQuality, onAudioQualityChange) }
            ))

            SettingsHeader("Download Format")
            SettingsGroupCard(listOf(
                { AudioFormatRow("Download Format", downloadAudioFormat, onDownloadAudioFormatChange) }
            ))

            SettingsHeader("Download Bitrate")
            SettingsGroupCard(listOf(
                { AudioQualityRow("Download Bitrate", downloadBitrate, onDownloadBitrateChange) }
            ))

            SettingsHeader("Audio Focus")
            SettingsGroupCard(listOf(
                { AudioFocusRow(audioFocus, onAudioFocusToggle) }
            ))

            SettingsHeader("Sleep Timer")
            SettingsGroupCard(listOf(
                { SleepTimerSection(sleepTimerEndTime, sleepTimerMode, onStartSleepTimer, onCancelSleepTimer, onSetEndOfSong) }
            ))
        }
    }
}

@Composable
private fun AutoplayRow(checked: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        onClick = { onToggle(!checked) },
        content = {
            Column {
                Text("Autoplay", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Continue with similar YouTube Music tracks when the current queue ends.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onToggle)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AudioQualityRow(
    title: String,
    currentQuality: String,
    onQualityChange: (String) -> Unit
) {
    val options = listOf("48 kbps", "64 kbps", "96 kbps", "128 kbps", "160 kbps", "192 kbps", "256 kbps", "320 kbps")

    ListItem(
        headlineContent = {
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Target bitrate; the closest matching source stream is selected. The source may not offer the exact rate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { option ->
                        val isSelected = option == currentQuality
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { onQualityChange(option) }
                        ) {
                            Text(
                                text = option,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AudioFormatRow(
    title: String,
    currentFormat: String,
    onFormatChange: (String) -> Unit
) {
    val options = listOf(
        "auto" to "Auto",
        "m4a" to "M4A / AAC",
        "webm" to "WebM",
        "opus" to "Opus",
        "mp3" to "MP3",
        "ogg" to "Ogg"
    )
    val selectedFormat = com.dsann.music.data.innertube.NewPipeStreamExtractor
        .normalizeAudioFormat(currentFormat)

    ListItem(
        headlineContent = {
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Requests this source format without transcoding. If a track has no matching stream, SUMITX asks you to choose from formats that track actually offers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { (value, label) ->
                        val isSelected = value == selectedFormat
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { onFormatChange(value) }
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
// ── Whats New ───────────────────────────────────────────────────────────────

@Composable
private fun WhatsNewScreen(navController: androidx.navigation.NavController) {
    val releases by androidx.compose.runtime.produceState<List<com.dsann.music.update.GitHubReleaseClient.ReleaseInfo>?>(initialValue = null) {
        withContext(Dispatchers.IO) {
            val client = com.dsann.music.update.GitHubReleaseClient("subhu1895991", "SubHuMusic")
            value = client.getAllReleases()
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            androidx.compose.material3.LargeTopAppBar(
                title = { Text("What's New") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        if (releases == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (releases!!.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No releases found.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp)
            ) {
                val groupedReleases = releases!!.groupBy { release ->
                    release.tagName.removePrefix("v").removePrefix("V").substringBefore('.')
                }

                groupedReleases.forEach { (majorVersion, majorReleases) ->
                    item(key = "header_$majorVersion") {
                        Text(
                            text = "Version $majorVersion Series",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }

                    itemsIndexed(majorReleases, key = { _, rel -> rel.tagName }) { _, release ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = getSettingsSegmentedShape(index = 0, count = 2),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = release.tagName,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = getSettingsSegmentedShape(index = 1, count = 2),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                val cleanedBody = release.changelog
                                    .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // bold
                                    .replace(Regex("__(.*?)__"), "$1") // bold
                                    .replace(Regex("### (.*)"), "$1") // headers
                                    .replace(Regex("## (.*)"), "$1")
                                    .replace(Regex("# (.*)"), "$1")
                                    .trim()

                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = cleanedBody,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutScreen(
    navController: androidx.navigation.NavController,
    onCheckUpdate: () -> Unit,
    onWhatsNew: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    Scaffold(
        topBar = { SettingsDetailTopBar("About") { navController.popBackStack() } }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Image(
                painter = painterResource(R.drawable.sumitx_logo),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            
            // Version Pills
            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
            
            // Action Buttons
            androidx.compose.material3.ButtonGroup(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
            ) {
                Button(
                    onClick = onCheckUpdate,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Rounded.Update, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Updates", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = onWhatsNew,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Rounded.Info, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("What's New", style = MaterialTheme.typography.labelLarge)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Credits Card
            CreditsCard(onOpenUrl)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPOSE UI — COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SettingsHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsGroupCard(items: List<@Composable () -> Unit>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        items.forEachIndexed { index, item ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = getSettingsSegmentedShape(index = index, count = items.size),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                item()
            }
        }
    }
}

private fun getSettingsSegmentedShape(index: Int, count: Int): androidx.compose.ui.graphics.Shape {
    val outer = 32.dp
    val inner = 4.dp
    return when {
        count <= 1 -> RoundedCornerShape(outer)
        index == 0 -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
        index == count - 1 -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
        else -> RoundedCornerShape(inner)
    }
}

// ── Theme ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemeRow(isDark: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        onClick = { onToggle(!isDark) },
        content = {
            Text(
                text = stringResource(R.string.setting_dark_mode),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        trailingContent = { ThemeSwitch(isDark, onToggle) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun ThemeSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        thumbContent = {
            AnimatedContent(
                targetState = checked,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) togetherWith
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                },
                label = "theme_icon"
            ) { dark ->
                Icon(
                    imageVector = if (!dark) Icons.Default.WbSunny else Icons.Default.NightsStay,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        }
    )
}



// ── Wavy Divider ────────────────────────────────────────────────────────────

// ── Crossfade ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CrossfadeRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    duration: Float,
    onDurationChange: (Float) -> Unit
) {
    ListItem(
        onClick = { onToggle(!enabled) },
        verticalAlignment = Alignment.CenterVertically,
        content = {
            Text(
                text = stringResource(R.string.setting_crossfade),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Column {
                AnimatedContent(
                    targetState = if (enabled) "${duration.toInt()}s" else "Off",
                    transitionSpec = {
                        fadeIn(spring(stiffness = Spring.StiffnessMedium)) togetherWith
                            fadeOut(spring(stiffness = Spring.StiffnessMedium))
                    },
                    label = "crossfade_label"
                ) { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                AnimatedVisibility(visible = enabled) {
                    Slider(
                        value = duration,
                        onValueChange = onDurationChange,
                        enabled = enabled,
                        valueRange = 0f..12f,
                        steps = 11,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        },
        trailingContent = { Switch(checked = enabled, onCheckedChange = onToggle) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

// ── Audio Focus ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AudioFocusRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        onClick = { onToggle(!enabled) },
        content = {
            Text(
                text = stringResource(R.string.setting_audio_focus),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_audio_focus),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = { Switch(checked = enabled, onCheckedChange = onToggle) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

// ── Sleep Timer ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SleepTimerSection(
    endTime: Long, 
    mode: PlayerSharedViewModel.SleepTimerMode,
    onStart: (Int) -> Unit, 
    onCancel: () -> Unit,
    onSetEndOfSong: (Boolean) -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    val remaining by remember(endTime) {
        derivedStateOf { endTime - System.currentTimeMillis() }
    }

    Column {
        ListItem(
            onClick = { showBottomSheet = true },
            content = {
                Text(
                    text = stringResource(R.string.setting_sleep_timer),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            supportingContent = {
                val supportingText = when (mode) {
                    PlayerSharedViewModel.SleepTimerMode.OFF -> stringResource(R.string.setting_sleep_timer_off)
                    PlayerSharedViewModel.SleepTimerMode.END_OF_SONG -> "Stop at end of current song"
                    PlayerSharedViewModel.SleepTimerMode.END_OF_QUEUE -> "Stop at end of queue"
                    PlayerSharedViewModel.SleepTimerMode.CUSTOM -> {
                        if (remaining > 0) "${remaining / 60000} min remaining"
                        else stringResource(R.string.setting_sleep_timer_off)
                    }
                }
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_timer),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        if (showBottomSheet) {
            SleepTimerBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sleepTimerEndTime = endTime,
                sleepTimerMode = mode,
                onStartTimer = { mins ->
                    onStart(mins)
                },
                onCancelTimer = {
                    onCancel()
                },
                onSetEndOfSong = { enable ->
                    onSetEndOfSong(enable)
                }
            )
        }
    }
}

// ── Library ─────────────────────────────────────────────────────────────────

@Composable
private fun LibrarySourceHeader() {
    ListItem(
        headlineContent = {
            Text(
                text = stringResource(R.string.setting_music_source),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_folder_open),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryPathRow(path: String, onRemove: ((String) -> Unit)?) {
    ListItem(
        onClick = { },
        content = {
            Text(
                text = path.substringAfterLast(":"),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(
                    if (onRemove != null) R.drawable.ic_folder_music else R.drawable.ic_folder_open
                ),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = if (onRemove != null) {
            {
                Text(
                    text = "Remove",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onRemove(path) }
                        .padding(8.dp)
                )
            }
        } else null,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddMorePathRow(onClick: () -> Unit) {
    ListItem(
        onClick = onClick,
        content = {
            Text(
                text = stringResource(R.string.setting_add_more_path),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

// ── Storage ─────────────────────────────────────────────────────────────────

@Composable
private fun DownloadFolderRow(path: String?, onClick: () -> Unit) {
    ListItem(
        onClick = onClick,
        content = {
            Column {
                Text(
                    text = "Download Location",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = path ?: "Not Set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_folder_open),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun CacheSizeRow(
    title: String,
    currentSize: String, // from calculation, e.g. "120 MB"
    maxSize: Int,
    onMaxSizeChange: (Int) -> Unit,
    onClear: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Max $title Size") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    listOf(100, 500, 1024, 2048, 0).forEach { sizeOption ->
                        val text = if (sizeOption == 0) "Unlimited" else if (sizeOption >= 1024) "${sizeOption / 1024} GB" else "$sizeOption MB"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onMaxSizeChange(sizeOption)
                                    showDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (maxSize == sizeOption),
                                onClick = {
                                    onMaxSizeChange(sizeOption)
                                    showDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Extract numerical value roughly to show progress
    val currentBytes = parseCacheSize(currentSize)
    val maxBytes = if (maxSize == 0) 0L else maxSize * 1024L * 1024L
    val progress = if (maxBytes > 0) (currentBytes.toFloat() / maxBytes).coerceIn(0f, 1f) else 0f

    ListItem(
        onClick = { showDialog = true },
        content = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = currentSize,
                        transitionSpec = {
                            fadeIn(spring(stiffness = Spring.StiffnessMedium)) togetherWith
                                fadeOut(spring(stiffness = Spring.StiffnessMedium))
                        },
                        label = "cache_size"
                    ) { size ->
                        Text(
                            text = size,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    val maxText = if (maxSize == 0) "Unlimited" else if (maxSize >= 1024) "${maxSize / 1024} GB" else "$maxSize MB"
                    Text(
                        text = "Max: $maxText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (maxBytes > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(50)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Cache",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

// Very basic string parsing for cache size
private fun parseCacheSize(sizeStr: String): Long {
    try {
        val split = sizeStr.trim().split(" ")
        if (split.size != 2) return 0L
        val value = split[0].toFloatOrNull() ?: return 0L
        val unit = split[1].uppercase()
        return when (unit) {
            "KB" -> (value * 1024).toLong()
            "MB" -> (value * 1024 * 1024).toLong()
            "GB" -> (value * 1024 * 1024 * 1024).toLong()
            "B" -> value.toLong()
            else -> 0L
        }
    } catch (e: Exception) {
        return 0L
    }
}

@Composable
private fun SongCacheSizeRow(
    cacheSize: String,
    maxSize: Int,
    onMaxSizeChange: (Int) -> Unit,
    onClear: () -> Unit
) {
    CacheSizeRow(
        title = "Song Cache",
        currentSize = cacheSize,
        maxSize = maxSize,
        onMaxSizeChange = onMaxSizeChange,
        onClear = onClear
    )
}

@Composable
private fun ImageCacheSizeRow(
    cacheSize: String,
    maxSize: Int,
    onMaxSizeChange: (Int) -> Unit,
    onClear: () -> Unit
) {
    CacheSizeRow(
        title = "Image Cache",
        currentSize = cacheSize,
        maxSize = maxSize,
        onMaxSizeChange = onMaxSizeChange,
        onClear = onClear
    )
}

// ── Credits ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CreditsCard(onOpenUrl: (String) -> Unit) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.sumit_debnath),
                    contentDescription = "Sumit Debnath",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                
                Text(
                    text = "Sumit Debnath",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = "Android Developer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Customized and maintained by Sumit Debnath",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                
                Row(
                    modifier = Modifier.padding(top = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SocialIconButton(icon = R.drawable.ic_github, onClick = { onOpenUrl("https://github.com/subhu1895991") })
                }
            }
        }
    }
}

@Composable
private fun SocialIconButton(icon: Int, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// SocialButton is replaced by ButtonGroup clickableItem

// ── App Logo ────────────────────────────────────────────────────────────────

@Composable
private fun AppLogoSection(isLandscape: Boolean) {
    val iconSize by animateDpAsState(
        targetValue = if (isLandscape) 160.dp else 250.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "logo_size"
    )
    val fontSize by animateFloatAsState(
        targetValue = if (isLandscape) 32f else 44f,
        animationSpec = SmoothSpring,
        label = "logo_text_size"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (isLandscape) 8.dp else 16.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.sumitx_logo),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(iconSize)
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            fontSize = fontSize.sp,
            letterSpacing = 0.sp
        )
        Text(
            text = "© 2026 Sumit Debnath",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeMusicLoginDialog(
    onDismissRequest: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
        title = {
            Text(
                text = "Log In to YouTube Music",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                Text(
                    text = "Sign in to access your real playlists, library, and personalized interests.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        val cookieManager = CookieManager.getInstance()
                                        val cookies = cookieManager.getCookie("https://music.youtube.com")
                                        if (cookies != null && cookies.contains("SAPISID") && cookies.contains("HSID")) {
                                            onLoginSuccess(cookies)
                                        }
                                    }
                                }
                                
                                // Load Google login continuing to YouTube Music
                                loadUrl("https://accounts.google.com/ServiceLogin?service=youtube&passive=true&continue=https://music.youtube.com/")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}

// ── Feedback & Bug Reports ──────────────────────────────────────────────────

@Composable
private fun FeedbackScreen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    val debugInfo = remember {
        """
        Device: ${Build.MANUFACTURER} ${Build.MODEL}
        Android: ${Build.VERSION.RELEASE}
        SDK: ${Build.VERSION.SDK_INT}
        App Version: ${BuildConfig.VERSION_NAME}
        """.trimIndent()
    }

    Scaffold(
        topBar = { SettingsDetailTopBar("Feedback & Bug Reports") { navController.popBackStack() } }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp)
        ) {
            SettingsHeader("Report & Request")
            SettingsGroupCard(
                items = listOf(
                    {
                        ListItem(
                            headlineContent = { Text("Report a Bug", style = MaterialTheme.typography.bodyLarge) },
                            supportingContent = { Text("Open GitHub with pre-filled device information", style = MaterialTheme.typography.bodyMedium) },
                            leadingContent = { Icon(Icons.Rounded.BugReport, null, tint = MaterialTheme.colorScheme.error) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                val title = URLEncoder.encode("[Bug] ", "UTF-8")
                                val body = URLEncoder.encode("## Description\n\n\n## Device Information\n$debugInfo\n\n## Steps to Reproduce\n1.\n2.\n3.\n", "UTF-8")
                                val url = "https://github.com/subhu1895991/SubhuMusic/issues/new?title=$title&body=$body"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        )
                    },
                    {
                        ListItem(
                            headlineContent = { Text("Request Feature", style = MaterialTheme.typography.bodyLarge) },
                            supportingContent = { Text("Suggest a new feature for SUMITX", style = MaterialTheme.typography.bodyMedium) },
                            leadingContent = { Icon(Icons.Rounded.OpenInNew, null, tint = MaterialTheme.colorScheme.primary) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                val title = URLEncoder.encode("[Feature] ", "UTF-8")
                                val body = URLEncoder.encode("## Feature Description\n\n\n## Why is this needed?\n\n", "UTF-8")
                                val url = "https://github.com/subhu1895991/SubhuMusic/issues/new?title=$title&body=$body"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        )
                    }
                )
            )

            SettingsHeader("Debug Information")
            SettingsGroupCard(
                items = listOf(
                    {
                        ListItem(
                            headlineContent = { Text("Copy Debug Information", style = MaterialTheme.typography.bodyLarge) },
                            supportingContent = { Text("Copy device & app version to clipboard", style = MaterialTheme.typography.bodyMedium) },
                            leadingContent = { Icon(Icons.Rounded.ContentCopy, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Debug Info", debugInfo)
                                clipboardManager.setPrimaryClip(clip)
                            }
                        )
                    },
                    {
                        ListItem(
                            headlineContent = { Text("Export Logs", style = MaterialTheme.typography.bodyLarge) },
                            supportingContent = { Text("Share raw debug logs to attach to an issue", style = MaterialTheme.typography.bodyMedium) },
                            leadingContent = { Icon(Icons.Rounded.Description, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, debugInfo)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Export Logs"))
                            }
                        )
                    }
                )
            )
        }
    }
}
