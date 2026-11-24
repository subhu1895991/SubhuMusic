@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS",
    "TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
)

package com.dsann.music.ui.library

import android.app.Activity
import androidx.activity.compose.BackHandler
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.imageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.dsann.music.MainActivity
import com.dsann.music.R
import com.dsann.music.data.db.PlaylistEntity
import com.dsann.music.data.lyrics.MetadataRepair
import com.dsann.music.data.lyrics.engine.LyricsCacheManager
import com.dsann.music.data.innertube.AccountManager
import com.dsann.music.data.innertube.YouTubeMusic
import com.dsann.music.data.innertube.OnlineAlbum
import com.dsann.music.data.innertube.OnlineArtist
import com.dsann.music.data.innertube.OnlinePlaylist
import com.dsann.music.data.innertube.OnlineSong
import com.dsann.music.model.SongItem
import com.dsann.music.service.AudioPlaybackService
import com.dsann.music.service.CachedMediaMetadata
import com.dsann.music.util.SongDownloader
import com.dsann.music.model.toSongItem
import com.dsann.music.ui.CreateLocalPlaylistDialog
import com.dsann.music.ui.LocalPlaylistPickerDialog
import com.dsann.music.ui.SongActionsBottomSheet
import com.dsann.music.ui.explore.AccountScreen
import com.dsann.music.ui.explore.OnlineSongBottomSheet
import com.dsann.music.ui.explore.SongCreditsScreen
import com.dsann.music.viewmodel.AccountViewModel
import com.dsann.music.viewmodel.ExploreViewModel
import com.dsann.music.viewmodel.PlayerSharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

enum class SortOrder {
    A_Z,
    DATE_ADDED,
    ARTIST,
    DURATION
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: PlayerSharedViewModel,
    onEditSong: (SongItem) -> Unit = {},
    onNavigateToExplore: () -> Unit = {}
) {
    val context = LocalContext.current
    val mainActivity = remember(context) { getActivityFromContext(context) as? MainActivity }
    val fragmentActivity = remember(context) { getActivityFromContext(context) as? androidx.fragment.app.FragmentActivity }
    val fragmentManager = remember(fragmentActivity) { fragmentActivity?.supportFragmentManager }

    val isPlayerExpanded by viewModel.isPlayerExpanded.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var activeTab by rememberSaveable { mutableStateOf("Playlists") }
    LaunchedEffect(activeTab) {
        // Migrate a previously saved selection of the removed Device Files destination.
        if (activeTab == "Device Files") activeTab = "Playlists"
    }
    var searchQuery by rememberSaveable { mutableStateOf("")}
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.A_Z) }
    var playlistsAsList by rememberSaveable { mutableStateOf(false) }
    var albumsAsList by rememberSaveable { mutableStateOf(false) }
    var artistsAsList by rememberSaveable { mutableStateOf(false) }
    val currentCollectionAsList = when (activeTab) {
        "Playlists" -> playlistsAsList
        "Albums" -> albumsAsList
        "Artists" -> artistsAsList
        else -> false
    }
    var isRefreshing by remember { mutableStateOf(false) }

    var showAccountScreen by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }

    val accountViewModel = remember(fragmentActivity) {
        fragmentActivity?.let { ViewModelProvider(it)[AccountViewModel::class.java] }
    }
    val exploreViewModel = remember(fragmentActivity) {
        fragmentActivity?.let { ViewModelProvider(it)[ExploreViewModel::class.java] }
    }

    val history by accountViewModel?.history?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val currentOnlineSong by exploreViewModel?.currentOnlineSong?.collectAsStateWithLifecycle(null) ?: remember { mutableStateOf(null) }
    val isLoadingStream by exploreViewModel?.isLoadingStream?.collectAsStateWithLifecycle(false) ?: remember { mutableStateOf(false) }

    val localSongs by viewModel.localSongs.collectAsStateWithLifecycle(emptyList())
    val favoriteIds by viewModel.favoriteSongIds.collectAsStateWithLifecycle(emptySet())
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val lyricsCache = remember(context) { LyricsCacheManager(context.applicationContext) }

    var selectedSongForMenu by remember { mutableStateOf<OnlineSong?>(null) }
    var selectedCachedSongForMenu by remember { mutableStateOf<CachedSongEntry?>(null) }
    var songToAddPlaylist by remember { mutableStateOf<SongItem?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showCreditsForSong by remember { mutableStateOf<OnlineSong?>(null) }
    val localPlaylists by viewModel.localPlaylistsFlow.collectAsStateWithLifecycle(emptyList())

    // Library sections are local UI state rather than separate NavController destinations.
    // Consume Back here so it returns to the Library landing page.
    BackHandler(
        enabled = activeTab != "Playlists" || showAccountScreen || showHistoryScreen || showCreditsForSong != null
    ) {
        when {
            showCreditsForSong != null -> showCreditsForSong = null
            showHistoryScreen -> showHistoryScreen = false
            showAccountScreen -> showAccountScreen = false
            activeTab != "Playlists" -> {
                activeTab = "Playlists"
                searchQuery = ""
                mainActivity?.showBottomNavWithAnimation()
            }
        }
    }

    // Migrate old saved Cache/blank destinations to Playlists; Cache now lives inside Playlists.
    LaunchedEffect(Unit) {
        if (activeTab.isBlank() || activeTab == "Cache") activeTab = "Playlists"
    }

    // Trigger local files scan on first composition
    LaunchedEffect(Unit) {
        if (localSongs.isEmpty()) {
            scanLocalFiles(context, viewModel)
        }
    }

    // Observe only download completion here. Collecting the complete progress map as Compose
    // state made every progress tick recompose the entire Library screen.
    LaunchedEffect(activeTab) {
        if (activeTab == "Downloaded songs") {
            SongDownloader.downloadProgress.collect { progress ->
                if (progress.isEmpty()) {
                    scanLocalFiles(context, viewModel)
                }
            }
        }
    }

    // Observe online playback trigger from ExploreViewModel for History/Account pages
    LaunchedEffect(exploreViewModel) {
        exploreViewModel?.playbackTriggerEvent?.collect {
            val song = exploreViewModel.currentOnlineSong.value
            if (song != null) {
                val videoId = song.videoId
                val durationMs = song.durationMs
                val onlineQueue = exploreViewModel.onlineQueue.value
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
                    viewModel.setSongList(songItems)
                    val currentIdx = exploreViewModel.currentOnlineIndex.value
                    viewModel.playSongAtIndex(currentIdx)
                } else {
                    val songItem = SongItem.createOnlineSong(
                        videoId, song.title, song.artist, "", durationMs, song.thumbnailUrl, song.artistId, song.albumName
                    )
                    viewModel.playSong(songItem)
                }
            }
        }
    }

    val subscriptionChanged by exploreViewModel?.subscriptionChanged?.collectAsStateWithLifecycle(false) ?: remember { mutableStateOf(false) }
    LaunchedEffect(subscriptionChanged) {
        if (subscriptionChanged) {
            accountViewModel?.loadLibrary()
            exploreViewModel?.consumeSubscriptionChanged()
        }
    }

    val openCachedSongMenu: (CachedSongEntry) -> Unit = { item ->
        if (exploreViewModel != null) {
            selectedCachedSongForMenu = item
            selectedSongForMenu = OnlineSong(
                videoId = item.videoId,
                title = item.title,
                artist = item.artist,
                durationMs = item.durationMs,
                thumbnailUrl = android.net.Uri.fromFile(
                    AudioPlaybackService.getCachedMediaArtworkFile(context, item.songId)
                ).toString()
            )
        } else {
            com.dsann.music.ui.SnackbarController.showMessage(
                "Song options are unavailable right now"
            )
        }
    }

    // Existing tab helper signatures still accept this connection, but it is no longer
    // attached to scrolling content because the bottom navigation remains persistent.
    val nestedScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {}
    }

    Scaffold(
        // snackbarHost = { SnackbarHost(hostState = snackbarHostState) }, // Replaced by global SnackbarController
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Library header remains visible in every Library destination.
                AnimatedVisibility(
                    visible = true,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Library",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activeTab == "Playlists" || activeTab == "Albums" || activeTab == "Artists") {
                                IconButton(
                                    onClick = {
                                        when (activeTab) {
                                            "Playlists" -> playlistsAsList = !playlistsAsList
                                            "Albums" -> albumsAsList = !albumsAsList
                                            "Artists" -> artistsAsList = !artistsAsList
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (currentCollectionAsList) Icons.Default.GridView else Icons.Default.ViewList,
                                        contentDescription = if (currentCollectionAsList) "Show as grid" else "Show as list",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            IconButton(onClick = { activeTab = "History" }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_history),
                                    contentDescription = "Listening History",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            val userProfile by accountViewModel?.userProfile?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
                            if (userProfile?.avatarUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(userProfile?.avatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Account Settings",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable { showAccountScreen = true },
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                IconButton(onClick = { showAccountScreen = true }) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Account Settings",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Keep Library navigation visible while browsing, like Prism's tab-first Library.
                val tabs = listOf(
                    "Playlists" to "Playlists",
                    "Songs" to "Songs",
                    "Albums" to "Albums",
                    "Artists" to "Artists",
                    "History" to "History",
                    "Downloads" to "Downloaded songs",
                    "Podcasts" to "Podcasts",
                    "Recap" to "Recap"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { (label, tabKey) ->
                        FilterChip(
                            selected = activeTab == tabKey,
                            onClick = {
                                activeTab = tabKey
                                if (tabKey == "History") accountViewModel?.loadLibrary()
                            },
                            label = { Text(label, fontWeight = FontWeight.SemiBold) },
                            shape = CircleShape,
                            border = null,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    
            ) {
                when (activeTab) {
                    "Playlists" -> PlaylistsTabContent(
                        nestedScrollConnection,
                        viewModel,
                        playlistsAsList,
                        onCachedSongLongClick = openCachedSongMenu
                    )
                    "Podcasts" -> PodcastsTabContent(nestedScrollConnection)
                    "Songs" -> SongsTabContent(nestedScrollConnection, viewModel)
                    "Albums" -> AlbumsTabContent(nestedScrollConnection, albumsAsList)
                    "Artists" -> ArtistsTabContent(nestedScrollConnection, artistsAsList)
                    "Recap" -> RecapTabContent(nestedScrollConnection, viewModel, onNavigateToExplore)
                    "Downloaded songs" -> {
                        // Scan Local Files helper
                        val onRefreshAction = {
                            scope.launch {
                                isRefreshing = true
                                scanLocalFiles(context, viewModel)
                                isRefreshing = false
                            }
                        }

                        val pullToRefreshState = rememberPullToRefreshState()

                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = { onRefreshAction() },
                            state = pullToRefreshState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Search Bar
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search local songs...") },
                                    leadingIcon = { Icon(Icons.Default.Search, null) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Clear, null)
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    shape = CircleShape,
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )

                                // Sorting chips
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SortOrder.entries.forEach { order ->
                                        val isSel = sortOrder == order
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { sortOrder = order },
                                            label = {
                                                Text(
                                                    text = order.name.replace("_", " "),
                                                    fontWeight = FontWeight.SemiBold
                                                )
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

                                Spacer(modifier = Modifier.height(6.dp))

                                // Keep this folder scoped to files saved by SpatialFlow's downloader.
                                // Build the download list away from the UI thread. Large libraries
                                // used to filter, sort and group synchronously during composition,
                                // which stalled both first display and every search/sort change.
                                val sourceSongs by produceState(
                                    initialValue = emptyList<SongItem>(),
                                    localSongs,
                                    activeTab
                                ) {
                                    value = withContext(Dispatchers.Default) {
                                        localSongs.filter(::isSpatialFlowDownloadedSong)
                                    }
                                }

                                val filteredSongs by produceState(
                                    initialValue = emptyList<SongItem>(),
                                    sourceSongs,
                                    searchQuery
                                ) {
                                    if (searchQuery.isNotBlank()) delay(120L)
                                    val query = searchQuery.trim()
                                    value = withContext(Dispatchers.Default) {
                                        if (query.isEmpty()) sourceSongs else sourceSongs.filter {
                                            it.title.contains(query, ignoreCase = true) ||
                                                it.artist.contains(query, ignoreCase = true)
                                        }
                                    }
                                }

                                val sortedAndGrouped by produceState<List<Any>>(
                                    initialValue = emptyList(),
                                    filteredSongs,
                                    sortOrder
                                ) {
                                    value = withContext(Dispatchers.Default) {
                                        if (filteredSongs.isEmpty()) {
                                            emptyList()
                                        } else {
                                            val sorted = when (sortOrder) {
                                                SortOrder.A_Z -> filteredSongs.sortedWith(compareBy { it.title.lowercase() })
                                                SortOrder.DATE_ADDED -> filteredSongs.sortedWith(compareByDescending { it.dateAdded })
                                                SortOrder.ARTIST -> filteredSongs.sortedWith(compareBy { it.artist.lowercase() })
                                                SortOrder.DURATION -> filteredSongs.sortedWith(compareByDescending { it.duration })
                                            }

                                            val groups = LinkedHashMap<String, MutableList<SongItem>>()
                                            when (sortOrder) {
                                                SortOrder.A_Z -> {
                                                    for (song in sorted) {
                                                        val header = if (song.title.isEmpty()) "#" else {
                                                            val char = song.title.uppercase().first()
                                                            if (char.isLetter()) char.toString() else "#"
                                                        }
                                                        groups.getOrPut(header) { mutableListOf() }.add(song)
                                                    }
                                                }
                                                SortOrder.DATE_ADDED -> {
                                                    val nowSeconds = System.currentTimeMillis() / 1000
                                                    for (song in sorted) {
                                                        val diff = (nowSeconds - song.dateAdded).coerceAtLeast(0L)
                                                        val days = diff / (60 * 60 * 24)
                                                        val header = when {
                                                            days < 1 -> "Today"
                                                            days < 2 -> "Yesterday"
                                                            days < 7 -> "This Week"
                                                            days < 30 -> "This Month"
                                                            days < 365 -> "This Year"
                                                            else -> "Older"
                                                        }
                                                        groups.getOrPut(header) { mutableListOf() }.add(song)
                                                    }
                                                }
                                                SortOrder.ARTIST -> {
                                                    for (song in sorted) {
                                                        val header = song.artist.ifBlank { "Unknown Artist" }
                                                        groups.getOrPut(header) { mutableListOf() }.add(song)
                                                    }
                                                }
                                                SortOrder.DURATION -> {
                                                    for (song in sorted) {
                                                        val minutes = song.duration / 60000
                                                        val header = when {
                                                            minutes < 2 -> "Under 2 min"
                                                            minutes < 4 -> "2-4 min"
                                                            minutes < 6 -> "4-6 min"
                                                            minutes < 10 -> "6-10 min"
                                                            else -> "Over 10 min"
                                                        }
                                                        groups.getOrPut(header) { mutableListOf() }.add(song)
                                                    }
                                                }
                                            }

                                            buildList<Any> {
                                                for ((header, groupSongs) in groups) {
                                                    add(header)
                                                    addAll(groupSongs)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Precompute each song's position within its heading group once.
                                // Calculating this by scanning the list inside every LazyColumn row
                                // made large date/artist groups increasingly expensive to scroll.
                                val groupedSongPositions = remember(sortedAndGrouped) {
                                    buildMap<Long, Pair<Int, Int>> {
                                        var cursor = 0
                                        while (cursor < sortedAndGrouped.size) {
                                            if (sortedAndGrouped[cursor] !is String) {
                                                cursor++
                                                continue
                                            }
                                            val sectionStart = cursor + 1
                                            var sectionEnd = sectionStart
                                            while (
                                                sectionEnd < sortedAndGrouped.size &&
                                                sortedAndGrouped[sectionEnd] is SongItem
                                            ) {
                                                sectionEnd++
                                            }
                                            val sectionCount = sectionEnd - sectionStart
                                            for (songIndex in sectionStart until sectionEnd) {
                                                val song = sortedAndGrouped[songIndex] as SongItem
                                                put(song.id, (songIndex - sectionStart) to sectionCount)
                                            }
                                            cursor = sectionEnd
                                        }
                                    }
                                }

                                if (sortedAndGrouped.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            when {
                                                searchQuery.isNotEmpty() -> "No matching songs found"
                                                activeTab == "Downloaded songs" -> "No downloaded songs yet. Downloaded tracks will appear here."
                                                else -> "Scan device files to load songs"
                                            },
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .weight(1f),
                                        contentPadding = PaddingValues(bottom = 160.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        itemsIndexed(
                                            items = sortedAndGrouped,
                                            key = { _, item ->
                                                if (item is SongItem) "downloaded-song:${item.id}"
                                                else "downloaded-section:$item"
                                            },
                                            contentType = { _, item ->
                                                if (item is SongItem) "downloaded-song" else "section-header"
                                            }
                                        ) { index, item ->
                                            if (item is String) {
                                                Text(
                                                    text = item,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                                )
                                            } else if (item is SongItem) {
                                                val isPlaying = currentSong?.id == item.id
                                                val isFav = favoriteIds.contains(item.id)

                                                val (localIndex, count) =
                                                    groupedSongPositions[item.id] ?: (0 to 1)

                                                // SwipeToDismissBox implementation
                                                val dismissState = rememberSwipeToDismissBoxState(
                                                    confirmValueChange = { dismissValue ->
                                                        when (dismissValue) {
                                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                                viewModel.addToQueueNext(item)
                                                                scope.launch {
                                                                    com.dsann.music.ui.SnackbarController.showMessage("Playing next: ${item.title}")
                                                                }
                                                                false
                                                            }
                                                            SwipeToDismissBoxValue.EndToStart -> {
                                                                viewModel.addToQueue(item)
                                                                scope.launch {
                                                                    com.dsann.music.ui.SnackbarController.showMessage("Added to queue: ${item.title}")
                                                                }
                                                                false
                                                            }
                                                            else -> false
                                                        }
                                                    }
                                                )

                                                SwipeToDismissBox(
                                                    state = dismissState,
                                                    backgroundContent = {
                                                        val color = when (dismissState.dismissDirection) {
                                                            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                                                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.secondaryContainer
                                                            else -> Color.Transparent
                                                        }
                                                        val icon = when (dismissState.dismissDirection) {
                                                            SwipeToDismissBoxValue.StartToEnd -> Icons.Default.SkipNext
                                                            SwipeToDismissBoxValue.EndToStart -> Icons.AutoMirrored.Filled.QueueMusic
                                                            else -> null
                                                        }
                                                        val alignment = when (dismissState.dismissDirection) {
                                                            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                                            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                                            else -> Alignment.Center
                                                        }
                                                        val topRadius = if (localIndex == 0) 24.dp else 4.dp
                                                        val bottomRadius = if (localIndex == count - 1) 24.dp else 4.dp
                                                        val backgroundShape = RoundedCornerShape(
                                                            topStart = topRadius, topEnd = topRadius,
                                                            bottomStart = bottomRadius, bottomEnd = bottomRadius
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(horizontal = 8.dp, vertical = 1.dp)
                                                                .clip(backgroundShape)
                                                                .background(color.copy(alpha = dismissState.progress.coerceIn(0f, 1f))),
                                                            contentAlignment = alignment
                                                        ) {
                                                            if (icon != null) {
                                                                Icon(
                                                                    imageVector = icon,
                                                                    contentDescription = null,
                                                                    modifier = Modifier
                                                                        .padding(horizontal = 24.dp)
                                                                        .graphicsLayer {
                                                                            val p = dismissState.progress
                                                                            scaleX = (p * 1.2f).coerceIn(0.6f, 1.1f)
                                                                            scaleY = (p * 1.2f).coerceIn(0.6f, 1.1f)
                                                                            alpha = p.coerceIn(0.3f, 1.0f)
                                                                        },
                                                                    tint = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                                    } else {
                                                                        MaterialTheme.colorScheme.onSecondaryContainer
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    },
                                                    content = {
                                                        SongListItem(
                                                            song = item,
                                                            isPlaying = isPlaying,
                                                            isFavorite = isFav,
                                                            localIndex = localIndex,
                                                            localCount = count,
                                                            onClick = {
                                                                // Play clicked song, set the entire filtered songs list as queue
                                                                viewModel.setSongList(filteredSongs)
                                                                viewModel.playSong(item)
                                                            },
                                                            onLongClick = {
                                                                val bs = SongActionsBottomSheet.newInstance(item)
                                                                bs.setActionListener(object : SongActionsBottomSheet.ActionListener {
                                                                    override fun onPlay(song: SongItem) {
                                                                        viewModel.playSong(song)
                                                                    }
                                                                    override fun onPlayNext(song: SongItem) {
                                                                        viewModel.addToQueueNext(song)
                                                                        scope.launch {
                                                                            com.dsann.music.ui.SnackbarController.showMessage("Playing next: ${song.title}")
                                                                        }
                                                                    }
                                                                    override fun onAddToQueue(song: SongItem) {
                                                                        viewModel.addToQueue(song)
                                                                        scope.launch {
                                                                            com.dsann.music.ui.SnackbarController.showMessage("Added to queue: ${song.title}")
                                                                        }
                                                                    }
                                                                    override fun onDelete(song: SongItem) {
                                                                        scope.launch {
                                                                            val deleted = withContext(Dispatchers.IO) {
                                                                                // Downloaded songs on Android 10+ live in MediaStore. Deleting only
                                                                                // song.path leaves the indexed MediaStore item behind.
                                                                                val rowsDeleted = runCatching {
                                                                                    context.contentResolver.delete(song.contentUri, null, null)
                                                                                }.getOrDefault(0)
                                                                                if (rowsDeleted > 0) {
                                                                                    true
                                                                                } else {
                                                                                    val rawPath = (song.path ?: song.data).orEmpty()
                                                                                    if (rawPath.isBlank()) {
                                                                                        false
                                                                                    } else {
                                                                                        val file = java.io.File(rawPath)
                                                                                        !file.exists() || file.delete()
                                                                                    }
                                                                                }
                                                                            }
                                                                            scanLocalFiles(context, viewModel)
                                                                            com.dsann.music.ui.SnackbarController.showMessage(
                                                                                if (deleted) "Deleted: ${song.title}"
                                                                                else "Could not delete ${song.title}. The file may require storage permission."
                                                                            )
                                                                        }
                                                                    }
                                                                    override fun onEdit(song: SongItem) {
                                                                        onEditSong(song)
                                                                    }
                                                                    override fun onFavorite(song: SongItem, isFav: Boolean) {
                                                                        viewModel.toggleFavorite(song.id)
                                                                    }
                                                                    override fun onShare(song: SongItem) {
                                                                        try {
                                                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                                                type = "audio/*"
                                                                                putExtra(Intent.EXTRA_STREAM, song.contentUri)
                                                                                putExtra(Intent.EXTRA_TEXT, "Listen to ${song.title}")
                                                                            }
                                                                            context.startActivity(Intent.createChooser(intent, "Share Song"))
                                                                        } catch (_: Exception) {}
                                                                    }
                                                                })
                                                                fragmentManager?.let { bs.show(it, "SongActionsBottomSheet") }
                                                            }
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "History" -> HistoryScreen(
                        history = history,
                        currentOnlineSong = currentOnlineSong,
                        isLoadingStream = isLoadingStream,
                        isRefreshing = accountViewModel?.isLoading?.collectAsStateWithLifecycle()?.value == true,
                        onRefresh = { accountViewModel?.loadLibrary() },
                        onBack = { activeTab = "Playlists" },
                        onSongClick = { song, queue, index ->
                            exploreViewModel?.playOnlineSongWithQueue(song, queue, index)
                        },
                        onSongMenuClick = { song -> selectedSongForMenu = song },
                        showTopBar = false
                    )
                    "" -> PlaylistsTabContent(
                        nestedScrollConnection,
                        viewModel,
                        playlistsAsList,
                        onCachedSongLongClick = openCachedSongMenu
                    )
                }
            }
        }

        // Overlay AccountScreen
        AnimatedVisibility(
            visible = showAccountScreen && accountViewModel != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            if (accountViewModel != null) {
                AccountScreen(
                    viewModel = accountViewModel,
                    onBack = { showAccountScreen = false },
                    onSongClick = { song, queue, index ->
                        exploreViewModel?.playOnlineSongWithQueue(song, queue, index)
                    },
                    onNavigateToSignIn = {
                        showAccountScreen = false
                        mainActivity?.navigateToGoogleSignIn()
                    }
                )
            }
        }

        // Overlay HistoryScreen
        AnimatedVisibility(
            visible = showHistoryScreen,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            HistoryScreen(
                history = history,
                currentOnlineSong = currentOnlineSong,
                isLoadingStream = isLoadingStream,
                isRefreshing = accountViewModel?.isLoading?.collectAsStateWithLifecycle()?.value == true,
                onRefresh = { accountViewModel?.loadLibrary() },
                onBack = { showHistoryScreen = false },
                onSongClick = { song, queue, index ->
                    exploreViewModel?.playOnlineSongWithQueue(song, queue, index)
                },
                onSongMenuClick = { song ->
                    selectedSongForMenu = song
                }
            )
        }

        // Song 3-Dot Menu Bottom Sheet
        selectedSongForMenu?.let { song ->
            val isPinned = exploreViewModel?.uiState?.collectAsState()?.value?.pinnedSpeedDialIds?.contains(song.videoId) == true
            if (exploreViewModel != null) {
                OnlineSongBottomSheet(
                    song = song,
                    isPinned = isPinned,
                    onDismissRequest = {
                        selectedSongForMenu = null
                        selectedCachedSongForMenu = null
                    },
                    playerSharedViewModel = viewModel,
                    exploreViewModel = exploreViewModel,
                    onPlaylistAddClick = { onlineSongItem ->
                        songToAddPlaylist = onlineSongItem
                        showAddToPlaylistDialog = true
                    },
                    onViewCreditsClick = { s ->
                        showCreditsForSong = s
                    },
                    onRemoveFromCache = selectedCachedSongForMenu?.let { cachedItem ->
                        {
                            val removedResources = AudioPlaybackService.removeMediaCacheForSong(
                                context = context.applicationContext,
                                songId = cachedItem.songId,
                                videoId = cachedItem.videoId,
                                cacheUri = cachedItem.cachedUri
                            )
                            val track = MetadataRepair().repair(
                                cachedItem.title,
                                cachedItem.artist,
                                null,
                                cachedItem.durationMs,
                                null,
                                null
                            )
                            lyricsCache.evict(track)
                            com.dsann.music.ui.SnackbarController.showMessage(
                                if (removedResources > 0) {
                                    "Removed from cache: ${cachedItem.title}"
                                } else {
                                    "Cache entry not found: ${cachedItem.title}"
                                }
                            )
                        }
                    }
                )
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
                    viewModel.addSongToLocalPlaylist(playlist.id, songToAddPlaylist!!)
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
                    viewModel.createLocalPlaylist(name)
                    showCreatePlaylistDialog = false
                    showAddToPlaylistDialog = true
                },
                onDismiss = {
                    showCreatePlaylistDialog = false
                    showAddToPlaylistDialog = true
                }
            )
        }

        // Overlay SongCreditsScreen
        AnimatedVisibility(
            visible = showCreditsForSong != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            showCreditsForSong?.let { song ->
                SongCreditsScreen(
                    song = song,
                    onBack = { showCreditsForSong = null }
                )
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SongListItem(
    song: SongItem,
    isPlaying: Boolean,
    isFavorite: Boolean,
    localIndex: Int,
    localCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val shapes = ListItemDefaults.segmentedShapes(index = localIndex, count = localCount)
    val defaultBg = MaterialTheme.colorScheme.surfaceContainer
    val playingBg = MaterialTheme.colorScheme.primaryContainer

    val contentColor = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val subTextColor = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary

    ListItem(
        onClick = onClick,
        onLongClick = onLongClick,
        shapes = shapes,
        colors = ListItemDefaults.colors(
            containerColor = if (isPlaying) playingBg else defaultBg,
            headlineColor = contentColor,
            supportingColor = subTextColor,
            leadingIconColor = accentColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp),
        leadingContent = {
            val context = LocalContext.current
            val artModel = remember(song) { song.getAlbumArtUri() ?: R.drawable.ic_music_note }
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artModel)
                    .crossfade(true)
                    .error(R.drawable.ic_music_note)
                    .placeholder(R.drawable.ic_music_note)
                    .fallback(R.drawable.ic_music_note)
                    .build(),
                contentDescription = "Album Art",
                modifier = Modifier
                    .size(54.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Now Playing",
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        content = {
            Column {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@Composable
private fun UnifiedLibraryContent(
    nestedScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    onNavigateToTab: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = remember { getActivityFromContext(context) as? androidx.fragment.app.FragmentActivity }
    val accountViewModel = remember(activity) {
        activity?.let { ViewModelProvider(it)[AccountViewModel::class.java] }
    }
    val exploreViewModel = remember(activity) {
        activity?.let { ViewModelProvider(it)[ExploreViewModel::class.java] }
    }

    val onlinePlaylists by accountViewModel?.playlists?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val onlineAlbums by accountViewModel?.albums?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val onlineArtists by accountViewModel?.artists?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val onlinePodcasts by accountViewModel?.podcasts?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val onlineSongs by accountViewModel?.songs?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }
    // Keep category callbacks current without rebuilding all library tiles on parent recomposition.
    val navigateToTab = rememberUpdatedState(onNavigateToTab)

    LaunchedEffect(Unit) {
        accountViewModel?.refreshAll()
    }

    val unifiedItems = remember(onlinePlaylists, onlineAlbums, onlineArtists, onlinePodcasts, onlineSongs) {
        val list = mutableListOf<UnifiedLibraryItem>()

        // Keep the key library actions first, in a stable Prism-style tile order.
        list.addAll(
            listOf(
                LibraryCategoryItem("Playlists", "Your playlists", Icons.AutoMirrored.Filled.PlaylistPlay) {
                    navigateToTab.value("Playlists")
                },
                LibraryCategoryItem("Liked songs", "Your YouTube Music likes", Icons.Default.Favorite) {
                    navigateToTab.value("Songs")
                },
                LibraryCategoryItem("Albums", "Browse saved albums", Icons.Default.Album) {
                    navigateToTab.value("Albums")
                },
                LibraryCategoryItem("Artists", "Browse artists", Icons.Default.Person) {
                    navigateToTab.value("Artists")
                },
                LibraryCategoryItem("Podcasts", "Podcast library", Icons.Default.Podcasts) {
                    navigateToTab.value("Podcasts")
                },
                LibraryCategoryItem("Recap", "Your listening recap", Icons.Default.BarChart) {
                    navigateToTab.value("Recap")
                },
                LibraryCategoryItem("Downloaded songs", "Music saved on this device", Icons.AutoMirrored.Filled.QueueMusic) {
                    navigateToTab.value("Downloaded songs")
                },
            )
        )

        onlinePlaylists.forEach { playlist ->
            list.add(object : UnifiedLibraryItem {
                override val title = playlist.title
                override val thumbnailUrl = playlist.thumbnailUrl
                override val subtitle = "Playlist" + (if (playlist.songCount?.isNotEmpty() == true) " • ${playlist.songCount}" else "")
                override val onClick = {
                    exploreViewModel?.cameFromLibrary = true
                    exploreViewModel?.loadPlaylist(playlist.playlistId)
                    navigateToExplore(activity)
                }
                override val isCircle = false
            })
        }

        onlineAlbums.forEach { album ->
            list.add(object : UnifiedLibraryItem {
                override val title = album.title
                override val thumbnailUrl = album.thumbnailUrl
                override val subtitle = "Album" + (if (album.artists.isNotEmpty()) " • ${album.artists.firstOrNull()?.name}" else "")
                override val onClick = {
                    exploreViewModel?.cameFromLibrary = true
                    exploreViewModel?.loadAlbum(album.browseId)
                    navigateToExplore(activity)
                }
                override val isCircle = false
            })
        }

        onlineArtists.forEach { artist ->
            list.add(object : UnifiedLibraryItem {
                override val title = artist.title
                override val thumbnailUrl = artist.thumbnailUrl
                override val subtitle = "Artist"
                override val onClick = {
                    exploreViewModel?.cameFromLibrary = true
                    exploreViewModel?.loadArtist(artist.browseId, artist.thumbnailUrl)
                    navigateToExplore(activity)
                }
                override val isCircle = true
            })
        }

        onlinePodcasts.forEach { podcast ->
            list.add(object : UnifiedLibraryItem {
                override val title = podcast.title
                override val thumbnailUrl = podcast.thumbnailUrl
                override val subtitle = "Podcast"
                override val onClick = {
                    exploreViewModel?.cameFromLibrary = true
                    exploreViewModel?.loadPlaylist(podcast.playlistId)
                    navigateToExplore(activity)
                }
                override val isCircle = false
            })
        }

        // Categories stay first; the saved items below retain their source order.
        list
    }

    if (unifiedItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No items found in your library", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 160.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                
        ) {
            items(unifiedItems) { item ->
                UnifiedLibraryCard(item)
            }
        }
    }
}

private interface UnifiedLibraryItem {
    val title: String
    val thumbnailUrl: String?
    val subtitle: String
    val onClick: () -> Unit
    val isCircle: Boolean
    val icon: ImageVector? get() = null
}

private data class LibraryCategoryItem(
    override val title: String,
    override val subtitle: String,
    override val icon: ImageVector,
    override val onClick: () -> Unit
) : UnifiedLibraryItem {
    override val thumbnailUrl: String? = null
    override val isCircle: Boolean = false
}

private fun libraryCardGradient(title: String): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFFFF5F6D), Color(0xFFFFC371)),
        listOf(Color(0xFF4776E6), Color(0xFF8E54E9)),
        listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
        listOf(Color(0xFFEE0979), Color(0xFFFF6A00)),
        listOf(Color(0xFF0575E6), Color(0xFF00F260)),
        listOf(Color(0xFF7F00FF), Color(0xFFE100FF)),
        listOf(Color(0xFFFC4A1A), Color(0xFFF7B733))
    )
    return palettes[Math.floorMod(title.hashCode(), palettes.size)]
}

@Composable
private fun UnifiedLibraryCard(item: UnifiedLibraryItem) {
    val accent = remember(item.title) { libraryCardGradient(item.title) }
    val hasArtwork = !item.thumbnailUrl.isNullOrBlank()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = accent.map { it.copy(alpha = if (hasArtwork) 0.13f else 0.20f) }
                )
            )
            .clickable { item.onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(if (item.isCircle) CircleShape else RoundedCornerShape(12.dp))
                .background(
                    if (hasArtwork) {
                        Brush.linearGradient(accent.map { it.copy(alpha = 0.16f) })
                    } else {
                        Brush.linearGradient(accent)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (hasArtwork) {
                androidx.compose.foundation.Image(
                    painter = coil.compose.rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.thumbnailUrl)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon ?: if (item.isCircle) Icons.Default.Person else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            item.title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            item.subtitle,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistsTabContent(
    nestedScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    viewModel: PlayerSharedViewModel,
    showAsList: Boolean = false,
    onCachedSongLongClick: (CachedSongEntry) -> Unit
) {
    val context = LocalContext.current
    val lyricsCache = remember(context) { LyricsCacheManager(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val activity = remember(context) { getActivityFromContext(context) as? androidx.fragment.app.FragmentActivity }
    val accountViewModel = remember(activity) {
        activity?.let { ViewModelProvider(it)[AccountViewModel::class.java] }
    }
    val exploreViewModel = remember(activity) {
        activity?.let { ViewModelProvider(it)[ExploreViewModel::class.java] }
    }
    val onlinePlaylists by accountViewModel?.playlists?.collectAsStateWithLifecycle(emptyList())
        ?: remember { mutableStateOf(emptyList()) }
    val localPlaylists by viewModel.localPlaylistsFlow.collectAsStateWithLifecycle(emptyList())

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var selectedLocalPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showCachePlaylist by remember { mutableStateOf(false) }
    val playlistGridState = rememberLazyGridState()

    BackHandler(enabled = showCachePlaylist) { showCachePlaylist = false }

    LaunchedEffect(Unit) { accountViewModel?.refreshAll() }

    if (showCreatePlaylistDialog) {
        var name by remember { mutableStateOf("") }
        var creating by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!creating) showCreatePlaylistDialog = false },
            title = { Text("Create playlist", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Playlist name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !creating
                    )
                    Text(
                        if (AccountManager.isLoggedIn(context)) {
                            "This playlist will be saved to your YouTube Music library."
                        } else {
                            "You are signed out. This playlist will be stored on this device."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = name.trim()
                        if (title.isNotEmpty() && !creating) {
                            if (!AccountManager.isLoggedIn(context)) {
                                viewModel.createLocalPlaylist(title)
                                showCreatePlaylistDialog = false
                            } else {
                                creating = true
                                scope.launch {
                                    YouTubeMusic.createPlaylist(
                                        title = title,
                                        description = "",
                                        privacyStatus = "PRIVATE"
                                    ).onSuccess { playlistId ->
                                        showCreatePlaylistDialog = false
                                        accountViewModel?.loadLibrary()
                                        exploreViewModel?.cameFromLibrary = true
                                        exploreViewModel?.loadPlaylist(playlistId)
                                        navigateToExplore(activity)
                                    }.onFailure { error ->
                                        com.dsann.music.ui.SnackbarController.showMessage(
                                            error.message ?: "Could not create the YouTube Music playlist."
                                        )
                                    }
                                    creating = false
                                }
                            }
                        }
                    },
                    enabled = name.isNotBlank() && !creating
                ) {
                    Text(if (creating) "Creating…" else "Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreatePlaylistDialog = false },
                    enabled = !creating
                ) { Text("Cancel") }
            }
        )
    }

    selectedLocalPlaylist?.let { playlist ->
        LocalPlaylistDetailsScreen(
            playlist = playlist,
            viewModel = viewModel,
            onDismiss = { selectedLocalPlaylist = null }
        )
    }

    if (showCachePlaylist) {
        Dialog(
            onDismissRequest = { showCachePlaylist = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showCachePlaylist = false }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to playlists")
                    }
                    Text("Cache", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                StreamCacheTabContent(
                    context = context,
                    viewModel = viewModel,
                    exploreViewModel = exploreViewModel,
                    lyricsCache = lyricsCache,
                    onSongLongClick = onCachedSongLongClick,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Playlists",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                IconButton(onClick = { showCreatePlaylistDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create playlist", tint = MaterialTheme.colorScheme.primary)
                }
            }

            LazyVerticalGrid(
                state = playlistGridState,
                columns = if (showAsList) GridCells.Fixed(1) else GridCells.Adaptive(minSize = 152.dp),
                contentPadding = PaddingValues(bottom = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    
            ) {
                item(key = "ds-cache-folder", contentType = "special-playlist-folder") {
                    if (showAsList) {
                        ListItem(
                            headlineContent = { Text("Cache", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Cached audio, artwork and lyrics") },
                            leadingContent = {
                                Box(
                                    Modifier.size(54.dp).clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .clickable { showCachePlaylist = true }
                        )
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth().height(190.dp).clickable {
                                showCachePlaylist = true
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier.fillMaxWidth().weight(1f)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        null,
                                        modifier = Modifier.size(42.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text("Cache", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item(key = "ds-create-playlist", contentType = "special-playlist-create") {
                    if (showAsList) {
                        ListItem(
                            headlineContent = { Text("Create playlist", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("New playlist") },
                            leadingContent = {
                                Box(
                                    Modifier.size(54.dp).clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .clickable { showCreatePlaylistDialog = true }
                        )
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth().height(190.dp).clickable {
                                showCreatePlaylistDialog = true
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Add,
                                        null,
                                        modifier = Modifier.size(42.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Text("Create playlist", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                items(
                    items = localPlaylists,
                    key = { "local-" + it.id },
                    contentType = { "local-playlist" }
                ) { playlist ->
                    if (showAsList) {
                        ListItem(
                            headlineContent = {
                                Text(playlist.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = { Text("On this device") },
                            leadingContent = {
                                Box(
                                    Modifier.size(54.dp).clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .clickable { selectedLocalPlaylist = playlist }
                        )
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth().height(190.dp).clickable {
                                selectedLocalPlaylist = playlist
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier.fillMaxWidth().weight(1f)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.PlaylistPlay,
                                        null,
                                        modifier = Modifier.size(42.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    playlist.title,
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                items(
                    items = onlinePlaylists,
                    key = { "youtube-" + it.playlistId },
                    contentType = { "online-playlist" }
                ) { playlist ->
                    OnlinePlaylistCard(
                        playlist = playlist,
                        asList = showAsList,
                        onClick = {
                            exploreViewModel?.cameFromLibrary = true
                            exploreViewModel?.loadPlaylist(playlist.playlistId)
                            navigateToExplore(activity)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalPlaylistDetailsScreen(
    playlist: PlaylistEntity,
    viewModel: PlayerSharedViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val songsEntity by viewModel.getSongsForLocalPlaylist(playlist.id).collectAsStateWithLifecycle(emptyList())
    val songs = remember(songsEntity) { songsEntity.map { it.toSongItem() } }
    val artworkUrl = playlist.thumbnailUrl?.takeIf { it.isNotBlank() }
        ?: songs.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl

    var artworkColor by remember(playlist.id) { mutableStateOf(Color(0xFF26364C)) }
    LaunchedEffect(playlist.id, artworkUrl) {
        if (!artworkUrl.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val request = ImageRequest.Builder(context)
                        .data(artworkUrl)
                        .allowHardware(false)
                        .build()
                    val result = context.imageLoader.execute(request)
                    if (result is SuccessResult) {
                        val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        if (bitmap != null) {
                            val palette = Palette.from(bitmap).generate()
                            val extracted = palette.getDarkVibrantColor(
                                palette.getDarkMutedColor(
                                    palette.getDominantColor(0xFF26364C.toInt())
                                )
                            )
                            withContext(Dispatchers.Main) {
                                artworkColor = Color(extracted)
                            }
                        }
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                artworkColor.copy(alpha = 0.72f),
                                artworkColor.copy(alpha = 0.32f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
            Column(modifier = Modifier.fillMaxSize()) {
                // Only the playlist name stays pinned. Artwork, controls and songs scroll below it.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Library",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        playlist.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = {
                            viewModel.deleteLocalPlaylist(playlist.id)
                            onDismiss()
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete playlist",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    item(key = "playlist-details-header", contentType = "playlist-header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(196.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!artworkUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(artworkUrl)
                                            .crossfade(false)
                                            .build(),
                                        contentDescription = "Playlist artwork",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.AutoMirrored.Filled.PlaylistPlay,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(72.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            Text(
                                "${songs.size} songs • On this device",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            if (songs.isNotEmpty()) {
                                Spacer(Modifier.height(18.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.setSongList(songs)
                                            viewModel.playSong(songs.first())
                                            onDismiss()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Play All")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val shuffled = songs.shuffled()
                                            viewModel.setSongList(shuffled)
                                            viewModel.playSong(shuffled.first())
                                            onDismiss()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Shuffle, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Shuffle")
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Songs",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (songs.isEmpty()) {
                        item(key = "playlist-empty", contentType = "empty") {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "This playlist is empty",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(
                            items = songs,
                            key = { it.id },
                            contentType = { "playlist-song" }
                        ) { song ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setSongList(songs)
                                            viewModel.playSong(song)
                                            onDismiss()
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!song.thumbnailUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = song.thumbnailUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.MusicNote,
                                                null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            song.title,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            song.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.removeSongFromLocalPlaylist(
                                                playlist.id,
                                                song.videoId ?: song.id.toString()
                                            )
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove song",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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

@Composable
private fun OnlinePlaylistCard(
    playlist: OnlinePlaylist,
    asList: Boolean = false,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val thumbnailRequest = remember(context, playlist.thumbnailUrl) {
        playlist.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(false)
                .build()
        }
    }

    if (asList) {
        ListItem(
            headlineContent = {
                Text(playlist.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text(playlist.songCount ?: "Playlist", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            },
            leadingContent = {
                if (!playlist.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbnailRequest,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
        )
        return
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (playlist.thumbnailUrl?.isNotEmpty() == true) {
                    AsyncImage(
                        model = thumbnailRequest,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    playlist.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    playlist.songCount ?: "Playlist",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PodcastsTabContent(nestedScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection) {
    val context = LocalContext.current
    val activity = remember { getActivityFromContext(context) as? androidx.fragment.app.FragmentActivity }

    val accountViewModel = remember(activity) {
        activity?.let { ViewModelProvider(it)[AccountViewModel::class.java] }
    }
    val exploreViewModel = remember(activity) {
        activity?.let { ViewModelProvider(it)[ExploreViewModel::class.java] }
    }

    val onlinePodcasts by accountViewModel?.podcasts?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }

    LaunchedEffect(Unit) {
        accountViewModel?.refreshAll()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Your Subscribed Podcasts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (onlinePodcasts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Podcasts,
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .alpha(0.6f),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No podcasts found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    
            ) {
                items(onlinePodcasts) { podcast ->
                    OnlinePlaylistCard(podcast, onClick = {
                        exploreViewModel?.cameFromLibrary = true
                        exploreViewModel?.loadPlaylist(podcast.playlistId)
                        navigateToExplore(activity)
                    })
                }
            }
        }
    }
}

@Composable
private fun SongsTabContent(nestedScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection, viewModel: PlayerSharedViewModel) {
    val context = LocalContext.current
    val activity = remember { getActivityFromContext(context) as? androidx.fragment.app.FragmentActivity }

    val accountViewModel = remember(activity) {
        activity?.let { ViewModelProvider(it)[AccountViewModel::class.java] }
    }

    val onlineSongs by accountViewModel?.songs?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }

    LaunchedEffect(Unit) {
        accountViewModel?.refreshAll()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Liked Songs (YT Music)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (onlineSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No online songs found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    ,
                contentPadding = PaddingValues(bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(onlineSongs) { song ->
                    ListItem(
                        headlineContent = { Text(song.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = {
                            if (song.thumbnailUrl?.isNotEmpty() == true) {
                                androidx.compose.foundation.Image(
                                    painter = coil.compose.rememberAsyncImagePainter(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(song.thumbnailUrl)
                                            .crossfade(true)
                                            .build()
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(48.dp))
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val list = onlineSongs.map { s ->
                                    SongItem.createOnlineSong(
                                        s.videoId,
                                        s.title,
                                        s.artist,
                                        "",
                                        s.durationMs,
                                        s.thumbnailUrl,
                                        s.artistId
                                    )
                                }
                                viewModel.setSongList(list)
                                val idx = list.indexOfFirst { it.videoId == song.videoId }
                                if (idx != -1) {
                                    viewModel.playSongAtIndex(idx)
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumsTabContent(
    nestedScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    showAsList: Boolean = false
) {
    val context = LocalContext.current
    val activity = remember { getActivityFromContext(context) as? androidx.fragment.app.FragmentActivity }

    val accountViewModel = remember(activity) {
        activity?.let { ViewModelProvider(it)[AccountViewModel::class.java] }
    }
    val exploreViewModel = remember(activity) {
        activity?.let { ViewModelProvider(it)[ExploreViewModel::class.java] }
    }

    val onlineAlbums by accountViewModel?.albums?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }

    LaunchedEffect(Unit) {
        accountViewModel?.refreshAll()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "YT Music Albums",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (onlineAlbums.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No online albums found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (showAsList) 1 else 2),
                contentPadding = PaddingValues(bottom = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    
            ) {
                items(onlineAlbums) { album ->
                    OnlineAlbumCard(album, asList = showAsList, onClick = {
                        exploreViewModel?.cameFromLibrary = true
                        exploreViewModel?.loadAlbum(album.browseId)
                        navigateToExplore(activity)
                    })
                }
            }
        }
    }
}

@Composable
private fun OnlineAlbumCard(
    album: OnlineAlbum,
    asList: Boolean = false,
    onClick: () -> Unit
) {
    if (asList) {
        ListItem(
            headlineContent = { Text(album.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = { Text(album.artists.firstOrNull()?.name ?: "Unknown Artist", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) },
            leadingContent = {
                if (!album.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(album.thumbnailUrl).crossfade(false).build(),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Album, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
        )
        return
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (album.thumbnailUrl?.isNotEmpty() == true) {
                    androidx.compose.foundation.Image(
                        painter = coil.compose.rememberAsyncImagePainter(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(album.thumbnailUrl)
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Album,
                            null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    album.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    album.artists.firstOrNull()?.name ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ArtistsTabContent(
    nestedScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    showAsList: Boolean = false
) {
    val context = LocalContext.current
    val activity = remember { getActivityFromContext(context) as? androidx.fragment.app.FragmentActivity }

    val accountViewModel = remember(activity) {
        activity?.let { ViewModelProvider(it)[AccountViewModel::class.java] }
    }
    val exploreViewModel = remember(activity) {
        activity?.let { ViewModelProvider(it)[ExploreViewModel::class.java] }
    }

    val onlineArtists by accountViewModel?.artists?.collectAsStateWithLifecycle(emptyList()) ?: remember { mutableStateOf(emptyList()) }

    LaunchedEffect(Unit) {
        accountViewModel?.refreshAll()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Followed Artists (YT Music)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (onlineArtists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No online artists found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (showAsList) 1 else 2),
                contentPadding = PaddingValues(bottom = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    
            ) {
                items(onlineArtists) { artist ->
                    OnlineArtistCard(artist, asList = showAsList, onClick = {
                        exploreViewModel?.cameFromLibrary = true
                        exploreViewModel?.loadArtist(artist.browseId, artist.thumbnailUrl)
                        navigateToExplore(activity)
                    })
                }
            }
        }
    }
}

@Composable
private fun OnlineArtistCard(
    artist: OnlineArtist,
    asList: Boolean = false,
    onClick: () -> Unit
) {
    if (asList) {
        ListItem(
            headlineContent = { Text(artist.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = { Text("Artist", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) },
            leadingContent = {
                if (!artist.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(artist.thumbnailUrl).crossfade(false).build(),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (artist.thumbnailUrl?.isNotEmpty() == true) {
                androidx.compose.foundation.Image(
                    painter = coil.compose.rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artist.thumbnailUrl)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            artist.title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            "Artist",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private data class ReplaySongSummary(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val totalMs: Long,
    val sessions: Int,
    val lastTimestamp: Long
)

private data class ReplayArtistSummary(
    val name: String,
    val totalMs: Long,
    val sessions: Int
)

private data class ReplayMonthSummary(
    val label: String,
    val totalMs: Long
)

@Composable
private fun RecapTabContent(
    nestedScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    viewModel: PlayerSharedViewModel,
    onNavigateToExplore: () -> Unit
) {
    val context = LocalContext.current
    val events by viewModel.listeningHistoryEventsFlow.collectAsStateWithLifecycle(emptyList())
    val years = remember(events) {
        events.map { event ->
            java.util.Calendar.getInstance().apply {
                timeInMillis = event.timestamp
            }.get(java.util.Calendar.YEAR).toString()
        }.distinct().sortedDescending()
    }
    val periods = remember(years) {
        listOf("This month", "Last 30 days") + years + "All time"
    }
    var selectedPeriod by rememberSaveable { mutableStateOf("This month") }
    LaunchedEffect(periods) {
        if (selectedPeriod !in periods) selectedPeriod = "All time"
    }

    val now = System.currentTimeMillis()
    val startTime = remember(selectedPeriod) {
        when (selectedPeriod) {
            "This month" -> java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.DAY_OF_MONTH, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            "Last 30 days" -> now - 30L * 24L * 60L * 60L * 1000L
            "All time" -> Long.MIN_VALUE
            else -> selectedPeriod.toIntOrNull()?.let { year ->
                java.util.Calendar.getInstance().apply {
                    clear()
                    set(year, java.util.Calendar.JANUARY, 1, 0, 0, 0)
                }.timeInMillis
            } ?: Long.MIN_VALUE
        }
    }
    val filteredEvents = remember(events, selectedPeriod, startTime) {
        events.filter { it.timestamp >= startTime && it.timestamp <= now }
    }
    val topSongs = remember(filteredEvents) {
        filteredEvents.groupBy { it.songId }
            .map { entry ->
                val id = entry.key
                val rows = entry.value
                val latest = rows.maxByOrNull { it.timestamp } ?: rows.first()
                ReplaySongSummary(
                    id = id,
                    title = latest.title,
                    artist = latest.artist,
                    thumbnailUrl = latest.thumbnailUrl,
                    totalMs = rows.sumOf { it.duration.coerceAtLeast(0L) },
                    sessions = rows.size,
                    lastTimestamp = rows.maxOfOrNull { it.timestamp } ?: latest.timestamp
                )
            }
            .sortedByDescending { it.totalMs }
            .take(100)
    }
    val topArtists = remember(filteredEvents) {
        filteredEvents.groupBy { it.artist.ifBlank { "Unknown Artist" } }
            .map { entry ->
                ReplayArtistSummary(
                    name = entry.key,
                    totalMs = entry.value.sumOf { it.duration.coerceAtLeast(0L) },
                    sessions = entry.value.size
                )
            }
            .sortedByDescending { it.totalMs }
            .take(8)
    }
    val monthly = remember(filteredEvents) {
        val monthFormatter = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
        filteredEvents.groupBy { event ->
            val calendar = java.util.Calendar.getInstance().apply { timeInMillis = event.timestamp }
            "%04d-%02d".format(
                java.util.Locale.ROOT,
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH) + 1
            )
        }.toList()
            .sortedBy { it.first }
            .takeLast(8)
            .map { entry ->
                val timestamp = entry.second.minOfOrNull { it.timestamp } ?: 0L
                ReplayMonthSummary(
                    label = monthFormatter.format(java.util.Date(timestamp)),
                    totalMs = entry.second.sumOf { it.duration.coerceAtLeast(0L) }
                )
            }
    }
    val totalMs = remember(filteredEvents) {
        filteredEvents.sumOf { it.duration.coerceAtLeast(0L) }
    }
    val activeDays = remember(filteredEvents) {
        filteredEvents.map { event ->
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT)
                .format(java.util.Date(event.timestamp))
        }.distinct().size
    }
    val peakHour = remember(filteredEvents) {
        filteredEvents.groupBy { it.hourOfDay.coerceIn(0, 23) }
            .maxByOrNull { it.value.size }?.key
    }
    val peakHabit = when (peakHour ?: -1) {
        in 5..11 -> "Morning Spark"
        in 12..16 -> "Afternoon Groove"
        in 17..21 -> "Evening Harmony"
        -1 -> "Waiting for data"
        else -> "Midnight Mystique"
    }
    val totalMinutes = totalMs / 60_000L
    val totalListenText = if (totalMinutes >= 60L) {
        (totalMinutes / 60L).toString() + "h " + (totalMinutes % 60L).toString() + "m"
    } else {
        totalMinutes.toString() + " min"
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(topArtists) {
        topArtists.forEach { artist -> viewModel.resolveArtistProfileImage(artist.name) }
    }

    fun toSongItem(summary: ReplaySongSummary): SongItem {
        val localId = summary.id.toLongOrNull()
        return if (localId != null) {
            SongItem(
                localId,
                summary.title,
                summary.artist,
                -1L,
                null,
                summary.totalMs,
                summary.lastTimestamp
            ).apply {
                thumbnailUrl = summary.thumbnailUrl
            }
        } else {
            SongItem.createOnlineSong(
                videoId = summary.id,
                title = summary.title,
                artist = summary.artist,
                streamUrl = null,
                durationMs = summary.totalMs,
                thumbnailUrl = summary.thumbnailUrl
            )
        }
    }

    fun playReplay(shuffle: Boolean) {
        val queue = topSongs.map { summary -> toSongItem(summary) }
        if (queue.isEmpty()) return
        viewModel.setShuffleEnabled(shuffle)
        viewModel.setSongList(queue)
        viewModel.playSongAtIndex(0)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            ,
        contentPadding = PaddingValues(bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "replay-header") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    "Replay",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Your listening, recapped on this device",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item(key = "replay-periods") {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                periods.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period) },
                        border = null
                    )
                }
            }
        }

        if (filteredEvents.isEmpty()) {
            item(key = "replay-empty") {
                Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Nothing to replay yet", fontWeight = FontWeight.Bold)
                        Text(
                            "Songs you play will appear here with listening time, top artists and your most-played tracks.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            item(key = "replay-hero") {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            selectedPeriod.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            totalListenText,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "listened",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                            Column {
                                Text(filteredEvents.size.toString(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                Text("plays", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column {
                                Text(topSongs.size.toString(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                Text("songs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column {
                                Text(activeDays.toString(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                Text("active days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            "Peak listening time · " + peakHabit,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        topSongs.firstOrNull()?.let { first ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.42f))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!first.thumbnailUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(first.thumbnailUrl).crossfade(false).build(),
                                        contentDescription = null,
                                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        Modifier.size(54.dp).clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) { Icon(Icons.Default.MusicNote, null) }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Your #1 song", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(first.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(first.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
            item(key = "replay-actions") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { playReplay(false) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        enabled = topSongs.isNotEmpty()
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Play")
                    }
                    OutlinedButton(
                        onClick = { playReplay(true) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        enabled = topSongs.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Shuffle, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Shuffle")
                    }
                    if (AccountManager.isLoggedIn(context)) {
                        IconButton(
                            onClick = {
                                val ids = topSongs.map { it.id }.filter { it.toLongOrNull() == null }.take(100)
                                if (ids.isNotEmpty()) {
                                    scope.launch {
                                        YouTubeMusic.createPlaylist(
                                            title = "Replay - " + selectedPeriod,
                                            description = "Made with SubhuMusic Replay",
                                            privacyStatus = "PRIVATE",
                                            videoIds = ids
                                        ).onSuccess {
                                            accountViewModelRefreshReplay(context)
                                            com.dsann.music.ui.SnackbarController.showMessage("Saved Replay to YouTube Music")
                                        }.onFailure { error ->
                                            com.dsann.music.ui.SnackbarController.showMessage(
                                                error.message ?: "Could not save Replay"
                                            )
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistPlay, "Save Replay playlist")
                        }
                    }
                }
            }
            if (monthly.size > 1) {
                item(key = "replay-monthly") {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Text("Listening by month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val maximum = monthly.maxOfOrNull { it.totalMs }?.coerceAtLeast(1L) ?: 1L
                        monthly.forEach { month ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(month.label, modifier = Modifier.width(48.dp), style = MaterialTheme.typography.labelSmall)
                                Box(
                                    Modifier.weight(1f).height(9.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        Modifier.fillMaxWidth((month.totalMs.toFloat() / maximum).coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (topArtists.isNotEmpty()) {
                item(key = "replay-top-artists") {
                    Column {
                        Text(
                            "Top artists",
                            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(topArtists, key = { it.name }) { artist ->
                                Column(
                                    Modifier.width(104.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val image = viewModel.artistProfileMap[artist.name]
                                    if (!image.isNullOrBlank()) {
                                        AsyncImage(
                                            model = image,
                                            contentDescription = null,
                                            modifier = Modifier.size(88.dp).clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            Modifier.size(88.dp).clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Person, null, modifier = Modifier.size(36.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(artist.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        artist.sessions.toString() + " plays",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item(key = "replay-top-songs-title") {
                Text(
                    "Top songs",
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            itemsIndexed(topSongs, key = { _, item -> item.id }) { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        val queue = topSongs.map { summary -> toSongItem(summary) }
                        viewModel.setShuffleEnabled(false)
                        viewModel.setSongList(queue)
                        viewModel.playSongAtIndex(index)
                    }.padding(horizontal = 16.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        (index + 1).toString(),
                        modifier = Modifier.width(32.dp),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (index < 3) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!item.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(item.thumbnailUrl).crossfade(false).build(),
                            contentDescription = null,
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.MusicNote, null) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text((item.totalMs / 60_000L).toString() + " min", style = MaterialTheme.typography.labelMedium)
                        Text(
                            item.sessions.toString() + " plays",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun accountViewModelRefreshReplay(context: Context) {
    val activity = getActivityFromContext(context) as? androidx.fragment.app.FragmentActivity ?: return
    ViewModelProvider(activity)[AccountViewModel::class.java].loadLibrary()
}


private fun isSpatialFlowDownloadedSong(song: SongItem): Boolean =
    isSpatialFlowDownloadedLocation(song.mediaStoreRelativePath, song.path ?: song.data)

private fun isSpatialFlowDownloadedLocation(relativePathValue: String?, rawPath: String?): Boolean {
    val relativePath = relativePathValue.orEmpty().trim('/').lowercase(java.util.Locale.ROOT)
    val appFolders = listOf(
        "music/subhumusic", "download/subhumusic", "downloads/subhumusic",
        // Keep recognising files downloaded by earlier D.S Music and SpatialFlow builds.
        "music/d.s music", "download/d.s music", "downloads/d.s music",
        "music/spatialflow", "download/spatialflow", "downloads/spatialflow"
    )
    if (appFolders.any { relativePath == it || relativePath.startsWith("$it/") }) return true

    val path = rawPath.orEmpty().replace('\\', '/').lowercase(java.util.Locale.ROOT)
    return path.contains("/music/subhumusic/") ||
        path.contains("/download/subhumusic/") ||
        path.contains("/downloads/subhumusic/") ||
        path.contains("/music/d.s music/") ||
        path.contains("/download/d.s music/") ||
        path.contains("/downloads/d.s music/") ||
        path.contains("/music/spatialflow/") ||
        path.contains("/download/spatialflow/") ||
        path.contains("/downloads/spatialflow/")
}

// Background scanner helper
internal suspend fun scanLocalFiles(context: Context, viewModel: PlayerSharedViewModel) {
    withContext(Dispatchers.IO) {
        val songs = ArrayList<SongItem>()
        try {
            val proj = mutableListOf(
                android.provider.MediaStore.Audio.Media._ID,
                android.provider.MediaStore.Audio.Media.TITLE,
                android.provider.MediaStore.Audio.Media.DISPLAY_NAME,
                android.provider.MediaStore.Audio.Media.ARTIST,
                android.provider.MediaStore.Audio.Media.IS_MUSIC,
                android.provider.MediaStore.Audio.Media.ALBUM_ID,
                android.provider.MediaStore.Audio.Media.ALBUM,
                android.provider.MediaStore.Audio.Media.DATA,
                android.provider.MediaStore.Audio.Media.DURATION,
                android.provider.MediaStore.Audio.Media.DATE_ADDED
            ).apply {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    add(android.provider.MediaStore.Audio.Media.RELATIVE_PATH)
                }
            }.toTypedArray()
            val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            val hiddenFolders = (prefs.getString("hidden_folders", "") ?: "").split("||").filter { it.isNotEmpty() }
            val ignoreShort = prefs.getBoolean("ignore_short_audio", false)
            val minDurationMs = if (ignoreShort) prefs.getFloat("ignore_short_audio_duration", 30f).toLong() * 1000L else 0L

            context.contentResolver.query(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                proj,
                null,
                null,
                "${android.provider.MediaStore.Audio.Media.TITLE} ASC"
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
                val displayNameCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DISPLAY_NAME)
                val artistCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
                val isMusicCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.IS_MUSIC)
                val albumCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM_ID)
                val albumNameCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM)
                val dataCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                val relativePathCol = c.getColumnIndex("relative_path")
                val durCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)
                val dateCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATE_ADDED)
                while (c.moveToNext()) {
                    val path = c.getString(dataCol)
                    val relativePath = if (relativePathCol >= 0) c.getString(relativePathCol) else null
                    val isDownloadedFile = isSpatialFlowDownloadedLocation(relativePath, path)
                    if (!isDownloadedFile && c.getInt(isMusicCol) == 0) continue
                    if (!isDownloadedFile && path != null && hiddenFolders.any { path.startsWith(it) }) continue

                    val duration = c.getLong(durCol)
                    if (!isDownloadedFile && ignoreShort && duration < minDurationMs) continue

                    // Some source containers cannot store tags. Recover basic display metadata
                    // from the downloaded filename instead of returning blank/unknown tracks.
                    val displayName = c.getString(displayNameCol).orEmpty().ifBlank {
                        path?.substringAfterLast('/') ?: ""
                    }
                    val baseName = displayName.substringBeforeLast('.', displayName)
                    val nameSeparator = baseName.lastIndexOf(" - ")
                    val fallbackTitle = if (nameSeparator > 0) baseName.substring(0, nameSeparator).trim() else baseName
                    val fallbackArtist = if (nameSeparator > 0) baseName.substring(nameSeparator + 3).trim() else ""
                    val storedTitle = c.getString(titleCol).orEmpty()
                    val storedArtist = c.getString(artistCol).orEmpty()
                    val title = if (
                        isDownloadedFile && (
                            storedTitle.isBlank() ||
                                storedTitle.equals("<unknown>", ignoreCase = true) ||
                                storedTitle.equals(baseName, ignoreCase = true)
                        )
                    ) fallbackTitle else storedTitle.ifBlank { fallbackTitle }
                    val artist = if (
                        isDownloadedFile &&
                            (storedArtist.isBlank() ||
                                storedArtist.equals("<unknown>", ignoreCase = true) ||
                                storedArtist.equals("unknown artist", ignoreCase = true)) &&
                            fallbackArtist.isNotBlank()
                    ) fallbackArtist else storedArtist.ifBlank { "Unknown Artist" }

                    songs.add(
                        SongItem(
                            c.getLong(idCol),
                            title,
                            artist,
                            c.getLong(albumCol),
                            c.getString(dataCol),
                            duration,
                            c.getLong(dateCol)
                        ).apply {
                            mediaStoreRelativePath = relativePath
                            albumName = c.getString(albumNameCol)?.takeIf {
                                it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true)
                            }
                        }
                    )
                }
            }
        } catch (_: Exception) {}
        withContext(Dispatchers.Main) {
            viewModel.setLocalSongs(songs)
        }
    }
}

private fun getActivityFromContext(context: Context): Activity? {
    var ctx = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun navigateToExplore(activity: Activity?) {
    (activity as? MainActivity)?.showArtistPage(null, null)
}


private data class CachedSongEntry(
    val songId: String,
    val title: String,
    val artist: String,
    val videoId: String,
    val durationMs: Long,
    val cachedUri: String
)

private data class CacheTabSnapshot(
    val entries: List<CachedSongEntry>
)

private suspend fun readCacheTabSnapshot(
    context: Context,
    lyricsCache: LyricsCacheManager
): CacheTabSnapshot = withContext(Dispatchers.IO) {
    val summary = try {
        AudioPlaybackService.getMediaCacheSnapshot(context)
    } catch (_: Exception) {
        0L to emptyList<Pair<String, Long>>()
    }
    val entriesBySong = LinkedHashMap<String, Pair<CachedSongEntry, Long>>()
    val metadataRepair = MetadataRepair()

    summary.second.forEach { (key, bytes) ->
        val metadata = AudioPlaybackService.getCachedMediaMetadata(context, key)
            ?: return@forEach
        if (metadata.songId.isBlank() || metadata.title.isBlank()) return@forEach

        val artwork = AudioPlaybackService.getCachedMediaArtworkFile(context, metadata.songId)
        if (!artwork.isFile || artwork.length() <= 0L) return@forEach

        val track = metadataRepair.repair(
            metadata.title,
            metadata.artist,
            null,
            metadata.durationMs,
            null,
            null
        )
        if (!lyricsCache.hasCachedLyrics(track)) return@forEach

        // Keep the Cache playlist genuinely offline: omit partial tracks that would have
        // to fall back to the network when tapped.
        val cachedUri = AudioPlaybackService.getCachedPlaybackUri(
            context,
            metadata.songId,
            metadata.videoId
        )?.toString() ?: return@forEach

        val entry = CachedSongEntry(
            songId = metadata.songId,
            title = metadata.title,
            artist = metadata.artist.ifBlank { "Unknown artist" },
            videoId = metadata.videoId,
            durationMs = metadata.durationMs,
            cachedUri = cachedUri
        )
        val existing = entriesBySong[metadata.songId]
        if (existing == null || bytes > existing.second) {
            entriesBySong[metadata.songId] = entry to bytes
        }
    }

    CacheTabSnapshot(
        entries = entriesBySong.values.map { it.first }.sortedBy { it.title.lowercase() }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun StreamCacheTabContent(
    context: Context,
    viewModel: PlayerSharedViewModel,
    exploreViewModel: ExploreViewModel?,
    lyricsCache: LyricsCacheManager,
    onSongLongClick: (CachedSongEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var cacheEntries by remember { mutableStateOf(emptyList<CachedSongEntry>()) }
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()

    LaunchedEffect(context, lyricsCache) {
        var snapshot = readCacheTabSnapshot(context, lyricsCache).entries
        cacheEntries = snapshot
        while (true) {
            delay(10_000L)
            val updated = readCacheTabSnapshot(context, lyricsCache).entries
            if (updated != snapshot) {
                snapshot = updated
                cacheEntries = updated
            }
        }
    }

    // Playlist entries use their exact Media3 cache URL; they never become a new online
    // innertube request, and the selected format/bitrate cannot override the cached stream.
    val cacheQueue = remember(cacheEntries, context) {
        cacheEntries.map { item ->
            SongItem(
                item.songId.toLongOrNull() ?: item.videoId.hashCode().toLong(),
                item.title,
                item.artist,
                -1L,
                item.cachedUri,
                item.durationMs,
                System.currentTimeMillis()
            ).apply {
                videoId = null
                isCachedTrack = true
                isDownloadedOffline = false
                contentUri = android.net.Uri.parse(item.cachedUri)
                thumbnailUrl = android.net.Uri.fromFile(
                    AudioPlaybackService.getCachedMediaArtworkFile(context, item.songId)
                ).toString()
            }
        }
    }

    fun startPlaylist(shuffle: Boolean) {
        if (cacheQueue.isEmpty()) return
        exploreViewModel?.clearOnlinePlaybackSelection()
        viewModel.setShuffleEnabled(shuffle)
        viewModel.setSongList(if (shuffle) cacheQueue.shuffled() else cacheQueue)
        viewModel.playSongAtIndex(0)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // YouTube-style playlist header: cover art, playlist identity, song count, and
        // dedicated Play all / Shuffle actions before the track list.
        if (cacheEntries.isNotEmpty()) {
            item(key = "cache-playlist-header") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AsyncImage(
                            model = AudioPlaybackService.getCachedMediaArtworkFile(context, cacheEntries.first().songId),
                            contentDescription = "Cache playlist artwork",
                            modifier = Modifier
                                .size(216.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            "Cache",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "SubhuMusic · ${cacheEntries.size} songs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Available offline",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { startPlaylist(false) },
                            enabled = cacheQueue.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Play all")
                        }
                        OutlinedButton(
                            onClick = { startPlaylist(true) },
                            enabled = cacheQueue.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Shuffle")
                        }
                    }
                    androidx.compose.material3.HorizontalDivider()
                }
            }
        } else {
            item(key = "cache-playlist-empty-header") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Cache", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Your offline playlist",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (cacheEntries.isEmpty()) {
            item(key = "cache-playlist-empty") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("No cached songs yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "A song appears here after its full audio, cover art, and lyrics are cached on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            itemsIndexed(cacheEntries, key = { _, item -> item.songId }) { index, item ->
                val artwork = AudioPlaybackService.getCachedMediaArtworkFile(context, item.songId)
                val queueSong = cacheQueue.getOrNull(index)
                val isCurrentSong = currentSong?.isCachedTrack == true &&
                    currentSong?.id == queueSong?.id &&
                    currentSong?.contentUri?.toString() == item.cachedUri

                ListItem(
                    leadingContent = {
                        AsyncImage(
                            model = artwork,
                            contentDescription = item.title + " cover art",
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    },
                    headlineContent = {
                        Text(
                            item.title,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isCurrentSong) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    supportingContent = {
                        Text(
                            if (isCurrentSong) "${item.artist} · Now playing" else item.artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Icon(
                            if (isCurrentSong) Icons.Filled.PlayArrow else Icons.AutoMirrored.Filled.PlaylistPlay,
                            contentDescription = if (isCurrentSong) "Now playing" else "Play cached song",
                            tint = if (isCurrentSong) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = {
                                if (index in cacheQueue.indices) {
                                    exploreViewModel?.clearOnlinePlaybackSelection()
                                    viewModel.setSongList(cacheQueue)
                                    viewModel.playSongAtIndex(index)
                                }
                            },
                            onLongClick = { onSongLongClick(item) }
                        )
                )
            }
        }
    }
}
