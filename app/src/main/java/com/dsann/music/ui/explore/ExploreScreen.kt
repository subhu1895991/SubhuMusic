package com.dsann.music.ui.explore

import android.R.attr.translationY
import android.content.res.Configuration
import android.graphics.Color.alpha
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.dsann.music.ui.LocalPlaylistPickerDialog
import com.dsann.music.ui.CreateLocalPlaylistDialog
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.dsann.music.MainActivity
import com.dsann.music.data.innertube.OnlineSong
import com.dsann.music.data.innertube.SearchFilter
import com.dsann.music.data.innertube.SearchItem
import com.dsann.music.data.innertube.UserProfile
import com.dsann.music.model.SongItem
import com.dsann.music.viewmodel.AccountViewModel
import com.dsann.music.viewmodel.ExploreViewModel
import com.dsann.music.viewmodel.PlayerSharedViewModel
import com.dsann.music.viewmodel.DetailType
import kotlin.time.Duration.Companion.milliseconds

// ===== Shared Transition Locals =====

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedElementIfAvailable(key: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedScope = LocalAnimatedVisibilityScope.current
    return if (sharedScope != null && animatedScope != null) {
        with(sharedScope) {
            this@sharedElementIfAvailable.sharedElement(
                rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedScope
            )
        }
    } else this
}

// ===== Explore Screen =====

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
    playerSharedViewModel: PlayerSharedViewModel,
    onNavigateToLibrary: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val mainActivity = context as? MainActivity
    val scope = rememberCoroutineScope()

    val accountVM: AccountViewModel = viewModel()
    val userProfile by accountVM.userProfile.collectAsStateWithLifecycle()
    val accountHistory by accountVM.history.collectAsStateWithLifecycle()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val searchQuery = uiState.searchQuery
    val searchResults = uiState.searchResults
    val searchTopResult = uiState.searchTopResult
    val searchSections = uiState.searchSections
    val suggestions = uiState.suggestions
    val suggestionItems = uiState.suggestionItems
    val isSearching = uiState.isSearching
    val homeSections = uiState.homeSections
    val isLoadingHome = uiState.isLoadingHome
    val isLoadingStream = uiState.isLoadingStream
    val currentMood = uiState.currentMood
    val currentOnlineSong = uiState.currentOnlineSong
    val error = uiState.error
    val albumDetail = uiState.albumDetail
    val artistDetail = uiState.artistDetail
    val playlistDetail = uiState.playlistDetail
    val sectionDetail = uiState.sectionDetail
    val isLoadingDetail = uiState.isLoadingDetail
    val isRefreshing = uiState.isRefreshing
    val searchHistory = uiState.searchHistory
    val homeMoods = uiState.homeMoods
    val detailStack = uiState.detailStack

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var selectedSongForMenu by remember { mutableStateOf<OnlineSong?>(null) }
    var showCreditsForSong by remember { mutableStateOf<OnlineSong?>(null) }

    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var songToAddPlaylist by remember { mutableStateOf<SongItem?>(null) }
    val localPlaylists by playerSharedViewModel.localPlaylistsFlow.collectAsStateWithLifecycle(
        emptyList()
    )

    fun activateSearchItem(item: SearchItem, songQueue: List<OnlineSong>? = null) {
        when (item) {
            is SearchItem.Song -> {
                val queue = songQueue.orEmpty().ifEmpty { listOf(item.song) }
                val index = queue.indexOfFirst { it.videoId == item.song.videoId }.coerceAtLeast(0)
                viewModel.playOnlineSongWithQueue(item.song, queue, index)
                isSearchActive = false
            }
            is SearchItem.Album -> {
                isSearchActive = false
                viewModel.loadAlbum(item.album.browseId)
            }
            is SearchItem.Artist -> {
                isSearchActive = false
                viewModel.loadArtist(item.artist.browseId, item.artist.thumbnailUrl)
            }
            is SearchItem.Playlist -> {
                isSearchActive = false
                viewModel.loadPlaylist(item.playlist.playlistId)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.playbackTriggerEvent.collect {
            val song = viewModel.currentOnlineSong.value
            if (song != null) {
                val videoId = song.videoId
                val durationMs = song.durationMs
                val onlineQueue = viewModel.onlineQueue.value
                if (onlineQueue.isNotEmpty()) {
                    val songItems = onlineQueue.map { os ->
                        SongItem.createOnlineSong(
                            os.videoId,
                            os.title,
                            os.artist,
                            "", // Empty streamUrl permits automatic resolving pipeline promotion
                            os.durationMs,
                            os.thumbnailUrl,
                            os.artistId,
                            os.albumName
                        )
                    }
                    playerSharedViewModel.setSongList(songItems)
                    val currentIdx = viewModel.currentOnlineIndex.value
                    playerSharedViewModel.playSongAtIndex(currentIdx)
                } else {
                    val songItem = SongItem.createOnlineSong(
                        videoId,
                        song.title,
                        song.artist,
                        "",
                        durationMs,
                        song.thumbnailUrl,
                        song.artistId,
                        song.albumName
                    )
                    playerSharedViewModel.playSong(songItem)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        playerSharedViewModel.currentSongIndex.collect { idx ->
            if (idx >= 0) {
                val queue = viewModel.onlineQueue.value
                val activeVideoId = playerSharedViewModel.currentSong.value?.videoId
                // The shared player index also changes for Library/Cache queues. Do not use
                // that unrelated index to restore a stale Explore selection/highlight.
                if (idx < queue.size &&
                    !activeVideoId.isNullOrBlank() &&
                    queue[idx].videoId == activeVideoId
                ) {
                    viewModel.updateActiveSongAndIndex(idx, queue[idx])
                }
            }
        }
    }

    val homeListState = rememberLazyListState()
    val searchListState = rememberLazyListState()

    val isPlayerExpanded by playerSharedViewModel.isPlayerExpanded.collectAsStateWithLifecycle()

    androidx.activity.compose.BackHandler(enabled = showCreditsForSong != null && !isPlayerExpanded) {
        showCreditsForSong = null
    }

    val prefs = remember { context.getSharedPreferences("explore_prefs", 0) }
    var lastPlayedArtist by rememberSaveable { mutableStateOf(prefs.getString("last_played_artist", null)) }

    DisposableEffect(context) {
        val listener =
            android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                if (key == "last_played_artist") lastPlayedArtist =
                    p.getString("last_played_artist", null)
            }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val appPrefs = remember { context.getSharedPreferences("AppSettings", 0) }
    DisposableEffect(context) {
        val listener =
            android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "yt_cookies") viewModel.forceReloadHomeFeed()
            }
        appPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { appPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    var isAccountVisible by rememberSaveable { mutableStateOf(false) }

    val detailScreenActive = detailStack.isNotEmpty() || isLoadingDetail

    val handleDetailBack = remember(viewModel, mainActivity) {
        {
            val currentStackSize = viewModel.uiState.value.detailStack.size
            val isLastDetail = currentStackSize <= 1
            val wasFromLibrary = viewModel.cameFromLibrary && isLastDetail

            viewModel.popDetailStack()

            if (isLastDetail) {
                viewModel.cameFromLibrary = false
            }

            if (wasFromLibrary) {
                onNavigateToLibrary()
            }
        }
    }

    androidx.activity.compose.BackHandler(enabled = detailScreenActive && !isPlayerExpanded) {
        handleDetailBack()
    }

    // Active query entry must take priority over the non-active results back handler.
    // Otherwise the first back press only clears the query while leaving the query page open.
    androidx.activity.compose.BackHandler(enabled = isSearchActive && !isPlayerExpanded) {
        isSearchActive = false
        viewModel.clearSearch()
    }

    androidx.activity.compose.BackHandler(
        enabled = !isSearchActive && searchQuery.isNotBlank() && !isPlayerExpanded
    ) {
        viewModel.clearSearch()
    }

    androidx.activity.compose.BackHandler(enabled = isAccountVisible && !isPlayerExpanded) {
        isAccountVisible = false
    }

    LaunchedEffect(Unit) { viewModel.loadHomeFeed() }

    val screenPriority = remember {
        mapOf(
            "home" to 0,
            "account" to 1,
            "loading" to 2,
            "album" to 3,
            "playlist" to 3,
            "artist" to 3,
            "section" to 3,
            "credits" to 3
        )
    }

    val currentScreen =
        remember(detailStack, isLoadingDetail, isAccountVisible, showCreditsForSong) {
            when {
                isLoadingDetail -> "loading"
                showCreditsForSong != null -> "credits"
                isAccountVisible -> "account"
                detailStack.isNotEmpty() -> {
                    when (detailStack.last()) {
                        DetailType.ALBUM -> "album"
                        DetailType.PLAYLIST -> "playlist"
                        DetailType.ARTIST -> "artist"
                        DetailType.SECTION -> "section"
                    }
                }

                else -> "home"
            }
        }

    val currentFilter by viewModel.searchFilter.collectAsStateWithLifecycle()

    val onSearchHeaderQueryChange = remember(viewModel) {
        { query: String -> viewModel.setSearchQuery(query) }
    }
    val onSearchHeaderSearch = remember(viewModel) {
        { query: String ->
            val trimmedQuery = query.trim()
            // Close the editor before starting the request; search results and loading
            // state then render in the results container instead of the keyboard page.
            isSearchActive = false
            if (trimmedQuery.isNotEmpty()) {
                viewModel.search(trimmedQuery)
            }
        }
    }
    val onSearchHeaderActiveChange = remember {
        { active: Boolean -> isSearchActive = active }
    }
    val onSearchHeaderClearHistory = remember(viewModel) {
        { viewModel.clearSearchHistory() }
    }
    val onSearchHeaderRemoveHistoryItem = remember(viewModel) {
        { item: String -> viewModel.removeFromSearchHistory(item) }
    }
    val onSearchHeaderAccountVisibleChange = remember {
        { visible: Boolean -> isAccountVisible = visible }
    }
    val onSearchHeaderMoodClick = remember(viewModel) {
        { mood: String? -> viewModel.setMood(mood) }
    }
    val onSearchHeaderFilterClick = remember(viewModel) {
        { filter: SearchFilter? -> viewModel.setSearchFilter(filter) }
    }
    val onSearchHeaderClearSearch = remember(viewModel) {
        { viewModel.clearSearch() }
    }

    val color1 = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val color2 = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
    val color3 = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
    val backgroundColor = MaterialTheme.colorScheme.background

    val gradientAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isLoadingHome || homeSections.isEmpty()) 0f else 1f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1200, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
        label = "GradientFadeIn"
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .graphicsLayer {
                    val scrollY = when {
                        isSearchActive || searchQuery.isNotBlank() || searchResults.isNotEmpty() -> {
                            if (searchListState.firstVisibleItemIndex == 0) searchListState.firstVisibleItemScrollOffset else 2000
                        }
                        else -> {
                            if (homeListState.firstVisibleItemIndex == 0) homeListState.firstVisibleItemScrollOffset else 2000
                        }
                    }
                    // Smoothly fade out as you scroll down
                    val scrollAlpha = (1f - (scrollY / 600f)).coerceIn(0f, 1f)
                    alpha = scrollAlpha * gradientAlpha
                    // Slight parallax scroll effect
                    translationY = -scrollY * 0.1f
                }
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            color1,
                            color2,
                            color3,
                            backgroundColor
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isLandscape) Modifier.padding(start = 88.dp) else Modifier)
        ) {
            // ===== Main Content (Now encapsulates scrolling headers) =====
            Box(modifier = Modifier.fillMaxSize()) {
                SharedTransitionLayout {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                        // Keep full-page transitions responsive; the previous default motion
                        // scheme made heavy list/detail swaps feel unnecessarily slow.
                        val spatialSpec = androidx.compose.animation.core.tween<androidx.compose.ui.unit.IntOffset>(
                            durationMillis = 190,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                        val effectsSpec = androidx.compose.animation.core.tween<Float>(
                            durationMillis = 150,
                            easing = androidx.compose.animation.core.LinearEasing
                        )

                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                val initialWeight = screenPriority[initialState] ?: 0
                                val targetWeight = screenPriority[targetState] ?: 0

                                if (targetWeight > initialWeight) {
                                    // SLIDE PUSH: Entering from right, existing sliding out to left
                                    (slideInHorizontally(animationSpec = spatialSpec) { it } + fadeIn(
                                        effectsSpec
                                    ))
                                        .togetherWith(slideOutHorizontally(animationSpec = spatialSpec) { -it / 3 } + fadeOut(
                                            effectsSpec
                                        ))
                                } else {
                                    // SLIDE POP: Entering from left, existing sliding out to right
                                    (slideInHorizontally(animationSpec = spatialSpec) { -it / 3 } + fadeIn(
                                        effectsSpec
                                    ))
                                        .togetherWith(slideOutHorizontally(animationSpec = spatialSpec) { it } + fadeOut(
                                            effectsSpec
                                        ))
                                }
                            },
                            label = "Explore Slide Transitions"
                        ) { screen ->
                            CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                                when (screen) {
                                    "account" -> {
                                        AccountScreen(
                                            viewModel = accountVM,
                                            onBack = { isAccountVisible = false },
                                            onSongClick = { song, queue, index ->
                                                viewModel.playOnlineSongWithQueue(
                                                    song,
                                                    queue,
                                                    index
                                                )
                                            },
                                            onNavigateToSignIn = {
                                                mainActivity?.navigateToGoogleSignIn()
                                            }
                                        )
                                    }

                                    "loading" -> DetailScreenSkeleton()

                                    "album" -> albumDetail?.let { detail ->
                                        AlbumDetailView(
                                            albumPage = detail,
                                            currentOnlineSong = currentOnlineSong,
                                            isLoadingStream = isLoadingStream,
                                            onBack = { handleDetailBack() },
                                            onSongClick = { song, queue, index ->
                                                viewModel.playOnlineSongWithQueue(
                                                    song,
                                                    queue,
                                                    index
                                                )
                                            },
                                            onSongMenuClick = { selectedSongForMenu = it },
                                            onStartRadioClick = { videoId ->
                                                viewModel.startRadio(
                                                    videoId
                                                )
                                            }
                                        )
                                    }

                                    "playlist" -> playlistDetail?.let { detail ->
                                        PlaylistDetailView(
                                            playlistPage = detail,
                                            currentOnlineSong = currentOnlineSong,
                                            isLoadingStream = isLoadingStream,
                                            onBack = { handleDetailBack() },
                                            onSongClick = { song, queue, index ->
                                                viewModel.playOnlineSongWithQueue(
                                                    song,
                                                    queue,
                                                    index
                                                )
                                            },
                                            onSongMenuClick = { selectedSongForMenu = it },
                                            onStartRadioClick = { videoId ->
                                                viewModel.startRadio(
                                                    videoId
                                                )
                                            },
                                            onPlaylistSaved = { title, description, privacy ->
                                                viewModel.updatePlaylistDetails(
                                                    detail.playlist.playlistId,
                                                    title,
                                                    description,
                                                    privacy
                                                )
                                            },
                                            onPlaylistDeleted = {
                                                accountVM.loadLibrary()
                                                handleDetailBack()
                                            }
                                        )
                                    }

                                    "artist" -> artistDetail?.let { detail ->
                                        ArtistDetailView(
                                            artistPage = detail,
                                            currentOnlineSong = currentOnlineSong,
                                            isSubscribed = detail.artist.isSubscribed,
                                            onBack = { handleDetailBack() },
                                            onSongClick = { song, queue, index ->
                                                viewModel.playOnlineSongWithQueue(
                                                    song,
                                                    queue,
                                                    index
                                                )
                                            },
                                            onAlbumClick = { viewModel.loadAlbum(it.browseId) },
                                            onPlaylistClick = { viewModel.loadPlaylist(it.playlistId) },
                                            onArtistClick = {
                                                viewModel.loadArtist(
                                                    it.browseId,
                                                    it.thumbnailUrl
                                                )
                                            },
                                            onSongMenuClick = { selectedSongForMenu = it },
                                            onSubscribeClick = { channelId ->
                                                if (detail.artist.isSubscribed) {
                                                    viewModel.unsubscribeFromArtist(channelId)
                                                } else {
                                                    viewModel.subscribeToArtist(channelId)
                                                }
                                            },
                                            onStartRadioClick = { videoId ->
                                                viewModel.startRadio(
                                                    videoId
                                                )
                                            },
                                            onSectionClick = { browseId, params, title ->
                                                viewModel.loadSectionDetails(
                                                    browseId,
                                                    params,
                                                    title
                                                )
                                            }
                                        )
                                    }

                                    "section" -> sectionDetail?.let { detail ->
                                        SectionDetailView(
                                            section = detail,
                                            currentOnlineSong = currentOnlineSong,
                                            isLoadingStream = isLoadingStream,
                                            onBack = { handleDetailBack() },
                                            onSongClick = { song, queue, index ->
                                                viewModel.playOnlineSongWithQueue(
                                                    song,
                                                    queue,
                                                    index
                                                )
                                            },
                                            onAlbumClick = { viewModel.loadAlbum(it.browseId) },
                                            onPlaylistClick = { viewModel.loadPlaylist(it.playlistId) },
                                            onArtistClick = {
                                                viewModel.loadArtist(
                                                    it.browseId,
                                                    it.thumbnailUrl
                                                )
                                            },
                                            onSongMenuClick = { selectedSongForMenu = it },
                                            onStartRadioClick = { videoId ->
                                                viewModel.startRadio(
                                                    videoId
                                                )
                                            }
                                        )
                                    }

                                    "credits" -> showCreditsForSong?.let { s ->
                                        SongCreditsScreen(
                                            song = s,
                                            onBack = { showCreditsForSong = null }
                                        )
                                    }

                                    "home" -> Column(modifier = Modifier.fillMaxSize()) {
                                        SearchHeader(
                                            searchQuery = searchQuery,
                                            onQueryChange = onSearchHeaderQueryChange,
                                            onSearch = onSearchHeaderSearch,
                                            isSearchActive = isSearchActive,
                                            accountHistory = accountHistory,
                                            onAccountHistorySongClick = { song, queue, index ->
                                                viewModel.playOnlineSongWithQueue(
                                                    song,
                                                    queue,
                                                    index
                                                )
                                            },
                                            onSearchActiveChange = onSearchHeaderActiveChange,
                                            searchResults = searchResults,
                                            searchHistory = searchHistory,
                                            onClearSearchHistory = onSearchHeaderClearHistory,
                                            onRemoveFromSearchHistory = onSearchHeaderRemoveHistoryItem,
                                            onHistoryItemClick = { historyItem ->
                                                scope.launch {
                                                    isSearchActive = false
                                                    delay(50.milliseconds)
                                                    viewModel.search(historyItem)
                                                }
                                            },
                                            suggestions = suggestions,
                                            suggestionItems = suggestionItems,
                                            onSuggestionClick = { suggestion ->
                                                scope.launch {
                                                    isSearchActive = false
                                                    delay(50.milliseconds)
                                                    viewModel.search(suggestion)
                                                }
                                            },
                                            onSuggestionItemClick = { item ->
                                                activateSearchItem(item)
                                            },
                                            onAccountVisibleChange = onSearchHeaderAccountVisibleChange,
                                            userProfile = userProfile,
                                            homeMoods = homeMoods,
                                            currentMood = currentMood,
                                            onMoodClick = onSearchHeaderMoodClick,
                                            currentFilter = currentFilter,
                                            onFilterClick = onSearchHeaderFilterClick,
                                            isLandscape = isLandscape,
                                            onClearSearch = onSearchHeaderClearSearch
                                        )

                                        Box(modifier = Modifier.weight(1f)) {
                                            when {
                                                isSearching && searchResults.isEmpty() && searchTopResult == null -> {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                    ) { LoadingIndicator() }
                                                }

                                                searchQuery.isNotBlank() && (searchResults.isNotEmpty() || searchTopResult != null) -> {
                                                    val topSongQueue = remember(searchSections, searchTopResult) {
                                                        (searchSections.flatMap { section ->
                                                            section.items.filterIsInstance<SearchItem.Song>().map { it.song }
                                                        } + listOfNotNull((searchTopResult as? SearchItem.Song)?.song))
                                                            .distinctBy { it.videoId }
                                                    }
                                                    LazyColumn(
                                                        state = searchListState,
                                                        modifier = Modifier.fillMaxSize().background(Color.Transparent),
                                                        contentPadding = PaddingValues(bottom = 120.dp)
                                                    ) {
                                                        if (currentFilter == null) {
                                                            searchTopResult?.let { top ->
                                                                item(key = "search-top-result") {
                                                                    SearchTopResultCard(
                                                                        item = top,
                                                                        isCurrentlyPlaying = top is SearchItem.Song &&
                                                                            top.song.videoId == currentOnlineSong?.videoId,
                                                                        isLoading = top is SearchItem.Song && isLoadingStream &&
                                                                            top.song.videoId == currentOnlineSong?.videoId,
                                                                        onSongMenuClick = { selectedSongForMenu = it },
                                                                        onTouchDown = {
                                                                            if (top is SearchItem.Song) {
                                                                                playerSharedViewModel.specPrefetch(
                                                                                    SongItem.createOnlineSong(
                                                                                        top.song.videoId, top.song.title, top.song.artist, "",
                                                                                        top.song.durationMs, top.song.thumbnailUrl, top.song.artistId,
                                                                                        top.song.albumName
                                                                                    )
                                                                                )
                                                                            }
                                                                        },
                                                                        onClick = { activateSearchItem(top, topSongQueue) }
                                                                    )
                                                                }
                                                            }

                                                            searchSections.forEach { section ->
                                                                item(key = "search-section:${section.filter.name}") {
                                                                    Row(
                                                                        modifier = Modifier.fillMaxWidth()
                                                                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        Text(
                                                                            section.filter.label,
                                                                            modifier = Modifier.weight(1f),
                                                                            style = MaterialTheme.typography.titleMedium,
                                                                            fontWeight = FontWeight.SemiBold
                                                                        )
                                                                        TextButton(onClick = { viewModel.setSearchFilter(section.filter) }) {
                                                                            Text("See all")
                                                                        }
                                                                    }
                                                                }

                                                                val songRows = section.items.filterIsInstance<SearchItem.Song>()
                                                                val useRows = section.filter in setOf(
                                                                    SearchFilter.SONGS, SearchFilter.VIDEOS,
                                                                    SearchFilter.EPISODES, SearchFilter.LIVE
                                                                )
                                                                if (useRows) {
                                                                    items(
                                                                        section.items,
                                                                        key = { item ->
                                                                            when (item) {
                                                                                is SearchItem.Song -> "song:${item.song.videoId}"
                                                                                is SearchItem.Album -> "album:${item.album.browseId}"
                                                                                is SearchItem.Artist -> "artist:${item.artist.browseId}"
                                                                                is SearchItem.Playlist -> "playlist:${item.playlist.playlistId}"
                                                                            }
                                                                        }
                                                                    ) { item ->
                                                                        SearchResultItem(
                                                                            item = item,
                                                                            isCurrentlyPlaying = item is SearchItem.Song &&
                                                                                item.song.videoId == currentOnlineSong?.videoId,
                                                                            isLoading = item is SearchItem.Song && isLoadingStream &&
                                                                                item.song.videoId == currentOnlineSong?.videoId,
                                                                            onSongMenuClick = { selectedSongForMenu = it },
                                                                            onTouchDown = {
                                                                                if (item is SearchItem.Song) {
                                                                                    playerSharedViewModel.specPrefetch(
                                                                                        SongItem.createOnlineSong(
                                                                                            item.song.videoId, item.song.title, item.song.artist, "",
                                                                                            item.song.durationMs, item.song.thumbnailUrl, item.song.artistId,
                                                                                            item.song.albumName
                                                                                        )
                                                                                    )
                                                                                }
                                                                            },
                                                                            onClick = { activateSearchItem(item, songRows.map { it.song }) }
                                                                        )
                                                                    }
                                                                } else {
                                                                    item(key = "search-shelf:${section.filter.name}") {
                                                                        LazyRow(
                                                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                                        ) {
                                                                            items(
                                                                                section.items,
                                                                                key = { item ->
                                                                                    when (item) {
                                                                                        is SearchItem.Song -> "song:${item.song.videoId}"
                                                                                        is SearchItem.Album -> "album:${item.album.browseId}"
                                                                                        is SearchItem.Artist -> "artist:${item.artist.browseId}"
                                                                                        is SearchItem.Playlist -> "playlist:${item.playlist.playlistId}"
                                                                                    }
                                                                                }
                                                                            ) { item ->
                                                                                SearchBrowseCard(item) { activateSearchItem(item) }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            if (searchSections.isEmpty() && searchResults.isNotEmpty()) {
                                                                item(key = "search-fallback-heading") {
                                                                    Text(
                                                                        "More results",
                                                                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp),
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.SemiBold
                                                                    )
                                                                }
                                                                items(searchResults, key = { item ->
                                                                    when (item) {
                                                                        is SearchItem.Song -> "song:${item.song.videoId}"
                                                                        is SearchItem.Album -> "album:${item.album.browseId}"
                                                                        is SearchItem.Artist -> "artist:${item.artist.browseId}"
                                                                        is SearchItem.Playlist -> "playlist:${item.playlist.playlistId}"
                                                                    }
                                                                }) { item ->
                                                                    SearchResultItem(
                                                                        item = item,
                                                                        isCurrentlyPlaying = item is SearchItem.Song && item.song.videoId == currentOnlineSong?.videoId,
                                                                        isLoading = item is SearchItem.Song && isLoadingStream && item.song.videoId == currentOnlineSong?.videoId,
                                                                        onSongMenuClick = { selectedSongForMenu = it },
                                                                        onClick = { activateSearchItem(item) }
                                                                    )
                                                                }
                                                            }
                                                        } else {
                                                            items(searchResults, key = { item ->
                                                                when (item) {
                                                                    is SearchItem.Song -> "song:${item.song.videoId}"
                                                                    is SearchItem.Album -> "album:${item.album.browseId}"
                                                                    is SearchItem.Artist -> "artist:${item.artist.browseId}"
                                                                    is SearchItem.Playlist -> "playlist:${item.playlist.playlistId}"
                                                                }
                                                            }) { item ->
                                                                SearchResultItem(
                                                                    item = item,
                                                                    isCurrentlyPlaying = item is SearchItem.Song && item.song.videoId == currentOnlineSong?.videoId,
                                                                    isLoading = item is SearchItem.Song && isLoadingStream && item.song.videoId == currentOnlineSong?.videoId,
                                                                    onSongMenuClick = { selectedSongForMenu = it },
                                                                    onTouchDown = {
                                                                        if (item is SearchItem.Song) {
                                                                            playerSharedViewModel.specPrefetch(
                                                                                SongItem.createOnlineSong(
                                                                                    item.song.videoId, item.song.title, item.song.artist, "",
                                                                                    item.song.durationMs, item.song.thumbnailUrl, item.song.artistId,
                                                                                    item.song.albumName
                                                                                )
                                                                            )
                                                                        }
                                                                    },
                                                                    onClick = {
                                                                        activateSearchItem(
                                                                            item,
                                                                            searchResults.filterIsInstance<SearchItem.Song>().map { it.song }
                                                                        )
                                                                    }
                                                                )
                                                            }
                                                            if (isSearching) item {
                                                                Box(
                                                                    Modifier.fillMaxWidth().padding(16.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) { LoadingIndicator(Modifier.size(28.dp)) }
                                                            }
                                                        }
                                                    }

                                                    LaunchedEffect(searchListState, currentFilter, searchResults.size, searchSections.size) {
                                                        if (currentFilter != null) {
                                                            snapshotFlow { searchListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                                                                .collect { lastVisibleIndex ->
                                                                    val threshold = searchResults.size - 3
                                                                    if (lastVisibleIndex != null && lastVisibleIndex >= threshold) {
                                                                        viewModel.loadMoreResults()
                                                                    }
                                                                }
                                                        }
                                                    }
                                                }

                                                searchQuery.isNotBlank() -> {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Search,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(48.dp),
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Text(
                                                                text = "No results found for \"$searchQuery\"",
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }

                                                else -> {
                                                    val pullToRefreshState =
                                                        rememberPullToRefreshState()
                                                    PullToRefreshBox(
                                                        isRefreshing = isRefreshing,
                                                        onRefresh = { viewModel.refreshHomeFeed() },
                                                        state = pullToRefreshState,
                                                        modifier = Modifier.fillMaxSize(),
                                                        indicator = {
                                                            PullToRefreshDefaults.LoadingIndicator(
                                                                state = pullToRefreshState,
                                                                isRefreshing = isRefreshing,
                                                                modifier = Modifier.align(Alignment.TopCenter)
                                                            )
                                                        }
                                                    ) {
                                                        when {
                                                            homeSections.isNotEmpty() -> {
                                                                LazyColumn(
                                                                    state = homeListState,
                                                                    modifier = Modifier.fillMaxSize()
                                                                        .background(Color.Transparent),
                                                                    contentPadding = PaddingValues(
                                                                        bottom = 120.dp
                                                                    )
                                                                ) {

                                                                    item(key = "home-welcome-banner", contentType = "welcome-banner") {
                                                                        Box(
                                                                            modifier = Modifier.fillMaxWidth(),
                                                                            contentAlignment = Alignment.Center
                                                                        ) {
                                                                            Box(
                                                                                modifier = if (isLandscape) Modifier.widthIn(
                                                                                    max = 600.dp
                                                                                ) else Modifier.fillMaxWidth()
                                                                            ) {
                                                                                WelcomeGreetingBanner(
                                                                                    userName = userProfile?.name
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                    items(
                                                                        items = homeSections,
                                                                        key = { section -> "section-${section.title}" },
                                                                        contentType = { "home-section" }
                                                                    ) { section ->
                                                                        HomeSectionRow(
                                                                            section = section,
                                                                            currentOnlineSong = currentOnlineSong,
                                                                            onSongTouchDown = { song ->
                                                                                playerSharedViewModel.specPrefetch(
                                                                                    SongItem.createOnlineSong(
                                                                                        song.videoId,
                                                                                        song.title,
                                                                                        song.artist,
                                                                                        "",
                                                                                        song.durationMs,
                                                                                        song.thumbnailUrl,
                                                                                        song.artistId
                                                                                    )
                                                                                )
                                                                            },
                                                                            onSongClick = { song ->
                                                                                val sectionSongs =
                                                                                    section.items.filterIsInstance<SearchItem.Song>()
                                                                                        .map { it.song }
                                                                                val idx =
                                                                                    sectionSongs.indexOfFirst { it.videoId == song.videoId }
                                                                                viewModel.playOnlineSongWithQueue(
                                                                                    song,
                                                                                    sectionSongs,
                                                                                    idx
                                                                                )
                                                                            },
                                                                            onAlbumClick = {
                                                                                viewModel.loadAlbum(
                                                                                    it.browseId
                                                                                )
                                                                            },
                                                                            onArtistClick = {
                                                                                viewModel.loadArtist(
                                                                                    it.browseId,
                                                                                    it.thumbnailUrl
                                                                                )
                                                                            },
                                                                            onPlaylistClick = {
                                                                                viewModel.loadPlaylist(
                                                                                    it.playlistId
                                                                                )
                                                                            },
                                                                            onSongMenuClick = {
                                                                                selectedSongForMenu =
                                                                                    it
                                                                            },
                                                                            onSectionClick = { browseId, params, title ->
                                                                                viewModel.loadSectionDetails(
                                                                                    browseId,
                                                                                    params,
                                                                                    title
                                                                                )
                                                                            }
                                                                        )
                                                                    }
                                                                    item(key = "home-loading-footer", contentType = "loading-footer") {
                                                                        val isLoadingMore by viewModel.isLoadingMoreHome.collectAsStateWithLifecycle()
                                                                        if (isLoadingMore) {
                                                                            Box(
                                                                                modifier = Modifier.fillMaxWidth()
                                                                                    .padding(24.dp),
                                                                                contentAlignment = Alignment.Center
                                                                            ) {
                                                                                LoadingIndicator(
                                                                                    modifier = Modifier.size(
                                                                                        32.dp
                                                                                    )
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                // Restart when the feed grows so the paging threshold never captures an old size.
                                                                LaunchedEffect(homeListState, homeSections.size) {
                                                                    snapshotFlow { homeListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                                                                        .collect { lastVisibleIndex ->
                                                                            if (lastVisibleIndex != null && lastVisibleIndex >= homeSections.size) {
                                                                                viewModel.loadMoreHomeSections()
                                                                            }
                                                                        }
                                                                }
                                                            }

                                                            isLoadingHome -> HomeFeedSkeleton()

                                                            else -> {
                                                                Box(
                                                                    modifier = Modifier.fillMaxSize()
                                                                        .verticalScroll(
                                                                            rememberScrollState()
                                                                        ),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                        Icon(
                                                                            Icons.Default.MusicNote,
                                                                            null,
                                                                            modifier = Modifier.size(
                                                                                64.dp
                                                                            ),
                                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                                                alpha = 0.5f
                                                                            )
                                                                        )
                                                                        Spacer(
                                                                            modifier = Modifier.height(
                                                                                16.dp
                                                                            )
                                                                        )
                                                                        Text(
                                                                            "Search for music",
                                                                            style = MaterialTheme.typography.titleMedium,
                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                        )
                                                                        Spacer(
                                                                            modifier = Modifier.height(
                                                                                8.dp
                                                                            )
                                                                        )
                                                                        OutlinedButton(onClick = { viewModel.refreshHomeFeed() }) {
                                                                            Text("Load Home Feed")
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Error Snackbar
                error?.let { errorMsg ->
                    Snackbar(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                        action = { TextButton(onClick = { viewModel.clearError() }) { Text("OK") } }
                    ) { Text(errorMsg) }
                }

                // Song 3-Dot Menu Bottom Sheet (Premium Overhaul like YTM)
                // Song 3-Dot Menu Bottom Sheet (Premium Overhaul like YTM)
                selectedSongForMenu?.let { song ->
                    val isPinned = uiState.pinnedSpeedDialIds.contains(song.videoId)
                    OnlineSongBottomSheet(
                        song = song,
                        isPinned = isPinned,
                        onDismissRequest = { selectedSongForMenu = null },
                        playerSharedViewModel = playerSharedViewModel,
                        exploreViewModel = viewModel,
                        onPlaylistAddClick = { onlineSongItem ->
                            songToAddPlaylist = onlineSongItem
                            showAddToPlaylistDialog = true
                        },
                        onViewCreditsClick = { s ->
                            showCreditsForSong = s
                        }
                    )
                }


            }
        }

        if (showAddToPlaylistDialog && songToAddPlaylist != null) {
            LocalPlaylistPickerDialog(
                playlists = localPlaylists,
                onCreateNew = {
                    showCreatePlaylistDialog = true
                    showAddToPlaylistDialog = false
                },
                onPlaylistSelected = { playlist ->
                    playerSharedViewModel.addSongToLocalPlaylist(playlist.id, songToAddPlaylist!!)
                    showAddToPlaylistDialog = false
                    songToAddPlaylist = null
                    com.dsann.music.ui.SnackbarController.showMessage("Added to playlist: ${playlist.title}")
                },
                onDismiss = {
                    showAddToPlaylistDialog = false
                    songToAddPlaylist = null
                }
            )
        }

        if (showCreatePlaylistDialog) {
            CreateLocalPlaylistDialog(
                onConfirm = { name ->
                    playerSharedViewModel.createLocalPlaylist(name)
                    showCreatePlaylistDialog = false
                    showAddToPlaylistDialog = true
                },
                onDismiss = {
                    showCreatePlaylistDialog = false
                    showAddToPlaylistDialog = true
                }
            )
        }
    }
}

    @Composable
    fun SongCreditsScreen(song: OnlineSong, onBack: () -> Unit) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Mini Top Header: Circular Avatar + Artist + Info
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(song.thumbnailUrl)
                                    .size(96, 96).precision(Precision.INEXACT).crossfade(false).build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = "Song • ${
                                java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Placeholder to center the middle content perfectly
                    Spacer(modifier = Modifier.size(48.dp))
                }

                val contributors = remember(song.artist) {
                    song.artist.split(Regex("[,&]| and "), 0)
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                }

                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LEFT: Sticky Cover Art
                        Box(
                            modifier = Modifier.weight(0.45f).fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(song.thumbnailUrl)
                                    .size(512, 512).precision(Precision.INEXACT).crossfade(false).build(),
                                contentDescription = song.title,
                                modifier = Modifier
                                    .size(240.dp)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // RIGHT: Scrollable Credits
                        Column(
                            modifier = Modifier
                                .weight(0.55f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            CreditsTextSection(title = "Performed by", content = contributors)

                            CreditsTextSection(
                                title = "Written by",
                                content = listOf(contributors.firstOrNull() ?: song.artist)
                            )

                            CreditsTextSection(
                                title = "Produced by",
                                content = listOf(if (contributors.size > 1) contributors.last() else "SubhuMusic Engine")
                            )

                            CreditsTextSection(
                                title = "Music metadata provided by",
                                content = listOf(
                                    contributors.firstOrNull() ?: "Online Stream Analytics"
                                )
                            )

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                } else {
                    // Scrollable Credits Body (PORTRAIT)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))

                        // Center Cover Art
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(song.thumbnailUrl)
                                .size(640, 640).precision(Precision.INEXACT).crossfade(false).build(),
                            contentDescription = song.title,
                            modifier = Modifier
                                .size(320.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Large Bold Title
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 34.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        // Left-aligned dynamic section lists
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(28.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            CreditsTextSection(title = "Performed by", content = contributors)

                            CreditsTextSection(
                                title = "Written by",
                                content = listOf(contributors.firstOrNull() ?: song.artist)
                            )

                            CreditsTextSection(
                                title = "Produced by",
                                content = listOf(if (contributors.size > 1) contributors.last() else "SubhuMusic Engine")
                            )

                            CreditsTextSection(
                                title = "Music metadata provided by",
                                content = listOf(
                                    contributors.firstOrNull() ?: "Online Stream Analytics"
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(64.dp))
                    }
                }
            }
        }
    }

    @Composable
    fun CreditsTextSection(title: String, content: List<String>) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            content.forEach { item ->
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
    @Composable
    fun SearchHeader(
        searchQuery: String,
        onQueryChange: (String) -> Unit,
        onSearch: (String) -> Unit,
        isSearchActive: Boolean,
        accountHistory: List<OnlineSong>,
        onAccountHistorySongClick: (OnlineSong, List<OnlineSong>, Int) -> Unit,
        onSearchActiveChange: (Boolean) -> Unit,
        searchResults: List<SearchItem>,
        searchHistory: List<String>,
        onClearSearchHistory: () -> Unit,
        onRemoveFromSearchHistory: (String) -> Unit,
        onHistoryItemClick: (String) -> Unit,
        suggestions: List<String>,
        suggestionItems: List<SearchItem>,
        onSuggestionClick: (String) -> Unit,
        onSuggestionItemClick: (SearchItem) -> Unit,
        onAccountVisibleChange: (Boolean) -> Unit,
        userProfile: UserProfile?,
        homeMoods: List<String>,
        currentMood: String?,
        onMoodClick: (String?) -> Unit,
        currentFilter: SearchFilter?,
        onFilterClick: (SearchFilter?) -> Unit,
        isLandscape: Boolean,
        onClearSearch: () -> Unit
    ) {
        // Avoid Material3 SearchBar's expanded full-window implementation here. On
        // legacy Android runtimes it can open an empty-looking overlay. Render the
        // active query-entry page in the normal Compose tree instead.
        if (isSearchActive) {
            val searchFocusRequester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current
            val submitSearch: () -> Unit = {
                if (searchQuery.isNotBlank()) {
                    keyboardController?.hide()
                    onSearch(searchQuery.trim())
                }
            }
            LaunchedEffect(searchFocusRequester) {
                delay(100.milliseconds)
                searchFocusRequester.requestFocus()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            onSearchActiveChange(false)
                            onClearSearch()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(searchFocusRequester),
                        singleLine = true,
                        placeholder = {
                            Text(
                                "Search music, artists, albums...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear query"
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { submitSearch() }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    IconButton(
                        enabled = searchQuery.isNotBlank(),
                        onClick = { submitSearch() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (searchQuery.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (searchQuery.isBlank()) {
                        if (searchHistory.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recent Searches",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(onClick = onClearSearchHistory) {
                                        Text("Clear all")
                                    }
                                }
                            }
                            items(searchHistory) { historyItem ->
                                ListItem(
                                    headlineContent = { Text(historyItem) },
                                    leadingContent = { Icon(Icons.Default.History, null) },
                                    trailingContent = {
                                        IconButton(
                                            onClick = { onRemoveFromSearchHistory(historyItem) }
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove search",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.clickable {
                                        onHistoryItemClick(historyItem)
                                    }
                                )
                            }
                        }

                        if (userProfile != null && accountHistory.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Recently Played (YouTube Music)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            itemsIndexed(accountHistory) { idx, song ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            song.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            song.artist,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingContent = {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(song.thumbnailUrl)
                                                .size(128, 128).precision(Precision.INEXACT).crossfade(false).build(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.clickable {
                                        onSearchActiveChange(false)
                                        onAccountHistorySongClick(song, accountHistory, idx)
                                    }
                                )
                            }
                        }

                        if (searchHistory.isEmpty() &&
                            (userProfile == null || accountHistory.isEmpty())
                        ) {
                            item {
                                Text(
                                    text = "Search for a song, artist, album, or playlist.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(24.dp)
                                )
                            }
                        }
                    } else if (suggestions.isNotEmpty() || suggestionItems.isNotEmpty()) {
                        item(key = "typed-query") {
                            ListItem(
                                headlineContent = { Text("Search for \"$searchQuery\"") },
                                leadingContent = { Icon(Icons.Default.Search, null) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { onSearch(searchQuery.trim()) }
                            )
                        }
                        items(suggestions, key = { "suggestion:$it" }) { suggestion ->
                            ListItem(
                                headlineContent = { Text(suggestion) },
                                leadingContent = { Icon(Icons.Default.Search, null) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { onSuggestionClick(suggestion) }
                            )
                        }
                        items(
                            suggestionItems,
                            key = { item ->
                                when (item) {
                                    is SearchItem.Song -> "suggestion-song:" + item.song.videoId
                                    is SearchItem.Album -> "suggestion-album:" + item.album.browseId
                                    is SearchItem.Artist -> "suggestion-artist:" + item.artist.browseId
                                    is SearchItem.Playlist -> "suggestion-playlist:" + item.playlist.playlistId
                                }
                            }
                        ) { item ->
                            SearchResultItem(
                                item = item,
                                isCurrentlyPlaying = false,
                                isLoading = false,
                                onClick = { onSuggestionItemClick(item) }
                            )
                        }
                    } else {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "No suggestions yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Press Search to find results for \"$searchQuery\".",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            return
        }

        // Keep the collapsed Explore header simple. We intentionally avoid switching back
        // into Material3's expandable SearchBar after submitting, which could re-expand
        // over the results and make the screen appear to open and immediately close.
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .clickable { onSearchActiveChange(true) },
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = searchQuery.ifBlank { "Search Music" },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (searchQuery.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    IconButton(onClick = { onAccountVisibleChange(true) }) {
                        if (userProfile?.avatarUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(userProfile.avatarUrl)
                                    .size(96, 96)
                                    .precision(Precision.INEXACT)
                                    .crossfade(false)
                                    .build(),
                                contentDescription = "Account",
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Account",
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Category Chips — fetched from YouTube Music (hidden during active search inputs, without animations)
            if (!isSearchActive && searchQuery.isBlank() && searchResults.isEmpty() && homeMoods.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(homeMoods) { label ->
                        val selected = currentMood == label
                        FilterChip(
                            selected = selected,
                            onClick = { onMoodClick(if (selected) null else label) },
                            label = { Text(label, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else {
                                null
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = null,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Search Result Filter Chips (hidden during active search typing inputs, without animations)
            if (!isSearchActive && searchQuery.isNotBlank()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters: List<Pair<SearchFilter?, String>> =
                        listOf(null to "Top results") + SearchFilter.values().map { it to it.label }
                    items(filters) { (filter, label) ->
                        val selected = currentFilter == filter
                        FilterChip(
                            selected = selected,
                            onClick = { onFilterClick(filter) },
                            label = { Text(label, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else {
                                null
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = null,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
