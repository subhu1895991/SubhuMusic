@file:Suppress("DEPRECATION")

package com.dsann.music

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dsann.music.model.SongItem
import com.dsann.music.service.AudioPlaybackService
import com.dsann.music.ui.AudioFormatChoiceDialogHost
import com.dsann.music.ui.PlayerBottomSheetCompose
import com.dsann.music.ui.SettingsScreenContent
import com.dsann.music.ui.TagEditorScreenEntryPoint
import com.dsann.music.ui.explore.ExploreScreen
import com.dsann.music.ui.explore.GoogleSignInScreen
import com.dsann.music.ui.library.LibraryScreen
import com.dsann.music.ui.library.LocalMusicScreen
import com.dsann.music.ui.theme.SpatialFlowTheme
import com.dsann.music.ui.theme.observeKey
import com.dsann.music.ui.onboarding.OnboardingScreen
import android.content.Context
import com.dsann.music.update.GitHubReleaseClient
import com.dsann.music.update.UpdateManager
import com.dsann.music.update.VersionUtils
import com.dsann.music.viewmodel.ExploreViewModel
import com.dsann.music.viewmodel.PlayerSharedViewModel
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val playerViewModel: PlayerSharedViewModel by viewModels()
    private val exploreViewModel: ExploreViewModel by viewModels()

    private var navController: NavController? = null
    private var previousDestination = "explore"
    private var isNavigating = false

    var audioService: AudioPlaybackService? = null
    var isServiceBound = false
    lateinit var updateManager: UpdateManager

    var isBottomNavVisible by mutableStateOf(true)
        private set

    fun hideBottomNavWithAnimation() {
        // Legacy nested-scroll callback: navigation is persistent now.
        isBottomNavVisible = true
    }

    fun showBottomNavWithAnimation() {
        isBottomNavVisible = true
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected")
            val binder = service as? AudioPlaybackService.LocalBinder
            audioService = binder?.getService()
            isServiceBound = true
            audioServiceStartupRequested = false
            audioService?.let {
                playerViewModel.audioService = it
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            isServiceBound = false
            audioServiceStartupRequested = false
            audioService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()

        val splashStartTime = System.currentTimeMillis()
        splashScreen.setKeepOnScreenCondition {
            System.currentTimeMillis() - splashStartTime < SPLASH_DURATION
        }

        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)

        setupSystemBars()

        // Keep the live ViewModel position during a configuration change. Only read the
        // persisted checkpoint when creating a fresh playback ViewModel after process death.
        if (savedInstanceState == null || playerViewModel.currentSong.value == null) {
            playerViewModel.loadLastPlaybackState()
        }

        // Media3 cache index loading can be slow on cold/large caches. Prepare it off
        // the UI thread before creating the playback service.
        prepareAndStartAudioService()
        // Permissions are now handled in Onboarding Screen

        setContent {
            val prefs = remember { getSharedPreferences("AppSettings", Context.MODE_PRIVATE) }
            val themeMode by prefs.observeKey("theme_mode", "system")
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }

            val currentSong by playerViewModel.currentSong.collectAsStateWithLifecycle()
            val playerBackgroundColor by playerViewModel.playerBackgroundColor.collectAsStateWithLifecycle()
            val dynamicAlbumColor = if (currentSong != null) playerBackgroundColor else null

            SpatialFlowTheme(darkTheme = isDarkTheme, dynamicAlbumColor = dynamicAlbumColor) {
                val navLabelsMode by prefs.observeKey(
                    "nav_label_mode",
                    if (prefs.getBoolean("hide_nav_labels", false)) "Never" else "Always"
                )
                val dynamicNavStyle by prefs.observeKey("dynamic_nav_style", false)
                val hasSeenOnboarding by prefs.observeKey("has_seen_onboarding_1_8", false)

                val currentNavController = rememberNavController()
                val rootTabRoutes = remember { setOf("explore", "library", "local_music", "settings") }
                val rootTabStateHolder = rememberSaveableStateHolder()
                var lastRootTab by remember { mutableStateOf("explore") }
                LaunchedEffect(currentNavController) {
                    navController = currentNavController
                }

                val isPlayerExpanded by playerViewModel.isPlayerExpanded.collectAsStateWithLifecycle()

                var editingSong by remember { mutableStateOf<SongItem?>(null) }
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                // Match Prism's wide-window breakpoint: phones in landscape and tablets use a side rail.
                val useNavigationRail = configuration.screenWidthDp >= 600

                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (!useNavigationRail) {
                        val navBackStackEntry by currentNavController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination

                        val showBottomBar = !isPlayerExpanded && currentDestination?.route in listOf("explore", "library", "local_music", "settings")

                        val density = LocalDensity.current
                        val navigationBarsHeightPx = WindowInsets.navigationBars.getBottom(density).toFloat()
                        val bottomNavHeight = 80.dp
                        val bottomNavHeightPx = with(density) { bottomNavHeight.toPx() }
                        val totalSlideDistPx = bottomNavHeightPx + navigationBarsHeightPx

                        // Keep navigation anchored while the player expands and while content scrolls.
                        val baseTargetTranslation = if (showBottomBar) 0f else totalSlideDistPx

                        val animatedBaseTranslationState = animateFloatAsState(
                            targetValue = baseTargetTranslation,
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "BottomNavTranslationY"
                        )

                        val navBarHeight by animateDpAsState(if (dynamicNavStyle) 64.dp else 80.dp, label = "navBarHeight")
                        val navIconSize by animateDpAsState(if (dynamicNavStyle) 34.dp else 26.dp, label = "navIconSize")
                        val navElevation by animateDpAsState(if (dynamicNavStyle) 8.dp else 3.dp, label = "navElevation")
                        val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

                        NavigationBar(
                            modifier = Modifier
                                .graphicsLayer {
                                    val translationY = animatedBaseTranslationState.value
                                        .coerceIn(0f, totalSlideDistPx)
                                    this.translationY = translationY
                                    this.alpha = if (translationY >= totalSlideDistPx - 1f) 0f else 1f
                                }
                                .height(navBarHeight + bottomPadding)
                                .onGloballyPositioned { coordinates ->
                                    val height = coordinates.size.height.toFloat()
                                    playerViewModel.setBottomNavHeight(height)
                                },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = navElevation,
                            windowInsets = WindowInsets(0, 0, 0, 0)
                        ) {
                            val items = listOf(
                                Triple("explore", "Explore", R.drawable.ic_explore),
                                Triple("library", "Library", R.drawable.ic_library_music),
                                Triple("local_music", "Local Music", R.drawable.ic_music_note),
                                Triple("settings", "Settings", R.drawable.ic_settings)
                            )
                             items.forEach { (route, label, iconResId) ->
                                val selected = currentDestination?.route == route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (!selected) {
                                            currentNavController.navigate(route) {
                                                popUpTo(currentNavController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { Icon(painter = painterResource(id = iconResId), contentDescription = label, modifier = Modifier.size(navIconSize)) },
                                    label = if (navLabelsMode == "Never") null else { { Text(label) } },
                                    alwaysShowLabel = navLabelsMode == "Always",
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    )
                                )
                            }
                        }
                        }
                    }
                ) { paddingValues ->
                    val navBackStackEntry by currentNavController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    val currentRoute = currentDestination?.route
                    LaunchedEffect(currentRoute) {
                        if (currentRoute != null && currentRoute in rootTabRoutes) {
                            lastRootTab = currentRoute
                        }
                    }
                    val activeCachedRootTab = when {
                        currentRoute != null && currentRoute in rootTabRoutes -> currentRoute
                        currentRoute == "tag_editor" || currentRoute == "google_signin" -> lastRootTab
                        else -> null
                    }
                    LaunchedEffect(currentDestination?.route) {
                        exploreViewModel.setExplorePageActive(currentDestination?.route == "explore")
                    }
                    val showBottomBar = !isPlayerExpanded && currentDestination?.route in listOf("explore", "library", "local_music", "settings")
                    // Keep the rail persistent in wide layouts; scroll-driven bottom-bar hiding is phone-only.
                    val showNavigationRail = useNavigationRail && showBottomBar
                    val layoutDirection = LocalLayoutDirection.current
                    // Navigation-bar start inset also covers landscape side-mounted system bars.
                    val railSystemInset = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateStartPadding(layoutDirection)
                    val railStartPadding = railSystemInset + 12.dp
                    val railInsetState = animateDpAsState(
                        targetValue = if (showNavigationRail) railSystemInset + 88.dp else 0.dp,
                        animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow),
                        label = "NavigationRailInset"
                    )
                    // Springs can overshoot their zero target slightly. Never feed a
                    // negative animated inset into Compose's non-negative padding modifier.
                    val railInset = railInsetState.value.coerceAtLeast(0.dp)

                    // Read the high-frequency expansion state during measurement rather than
                    // composition. This keeps each animation frame from recomposing the entire
                    // root navigation tree and all retained tab hosts.
                    val playerExpansionFractionState = playerViewModel.playerExpansionFraction.collectAsState()

                    // Base padding target not considering expansion
                    val baseTargetPadding = if (!useNavigationRail && showBottomBar) {
                        paddingValues.calculateBottomPadding()
                    } else {
                        0.dp
                    }

                    val animatedBasePaddingState = animateDpAsState(
                        targetValue = baseTargetPadding,
                        animationSpec = spring(
                            dampingRatio = 0.85f,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "BottomNavPadding"
                    )

                    // Final padding does not scale down during expansion to prevent massive Layout invalidation
                    // The player sheet covers the NavHost anyway, so keeping padding static is optimal
                    val finalPadding = animatedBasePaddingState.value

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = railInset.coerceAtLeast(0.dp),
                                    bottom = finalPadding.coerceAtLeast(0.dp)
                                )
                        ) {
                            // Compose only the foreground tab. Inactive screens used to remain
                            // alive at 1dp/alpha=0, so their effects, animations and non-lifecycle
                            // work continued consuming CPU while the user was elsewhere.
                            if (activeCachedRootTab != null) {
                                rootTabStateHolder.SaveableStateProvider(activeCachedRootTab) {
                                    when (activeCachedRootTab) {
                                        "explore" -> ExploreScreen(
                                            viewModel = exploreViewModel,
                                            playerSharedViewModel = playerViewModel,
                                            onNavigateToLibrary = {
                                                currentNavController.navigate("library") {
                                                    popUpTo(currentNavController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                        "library" -> LibraryScreen(
                                            viewModel = playerViewModel,
                                            onEditSong = { song ->
                                                editingSong = song
                                                currentNavController.navigate("tag_editor")
                                            },
                                            onNavigateToExplore = {
                                                currentNavController.navigate("explore") {
                                                    popUpTo(currentNavController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                        "local_music" -> LocalMusicScreen(viewModel = playerViewModel)
                                        "settings" -> SettingsScreenContent()
                                    }
                                }
                            }

                            val routeIndices = remember {
                                mapOf(
                                    "explore" to 0,
                                    "library" to 1,
                                    "local_music" to 2,
                                    "settings" to 3
                                )
                            }

                            NavHost(
                                navController = currentNavController,
                                startDestination = if (hasSeenOnboarding) "explore" else "onboarding",
                                modifier = Modifier.fillMaxSize(),
                                enterTransition = {
                                    val initialRoute = initialState.destination.route ?: ""
                                    val targetRoute = targetState.destination.route ?: ""
                                    val initialIndex = routeIndices[initialRoute] ?: 0
                                    val targetIndex = routeIndices[targetRoute] ?: 0
                                    
                                    if (targetIndex > initialIndex) {
                                        slideInHorizontally(
                                            initialOffsetX = { it },
                                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                                        ) + fadeIn(animationSpec = tween(220))
                                    } else {
                                        slideInHorizontally(
                                            initialOffsetX = { -it },
                                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                                        ) + fadeIn(animationSpec = tween(220))
                                    }
                                },
                                exitTransition = {
                                    val initialRoute = initialState.destination.route ?: ""
                                    val targetRoute = targetState.destination.route ?: ""
                                    val initialIndex = routeIndices[initialRoute] ?: 0
                                    val targetIndex = routeIndices[targetRoute] ?: 0
                                    
                                    if (targetIndex > initialIndex) {
                                        slideOutHorizontally(
                                            targetOffsetX = { -it / 3 },
                                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                                        ) + fadeOut(animationSpec = tween(220))
                                    } else {
                                        slideOutHorizontally(
                                            targetOffsetX = { it / 3 },
                                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                                        ) + fadeOut(animationSpec = tween(220))
                                    }
                                },
                                popEnterTransition = {
                                    val initialRoute = initialState.destination.route ?: ""
                                    val targetRoute = targetState.destination.route ?: ""
                                    val initialIndex = routeIndices[initialRoute] ?: 0
                                    val targetIndex = routeIndices[targetRoute] ?: 0
                                    
                                    if (targetIndex > initialIndex) {
                                        slideInHorizontally(
                                            initialOffsetX = { it },
                                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                                        ) + fadeIn(animationSpec = tween(220))
                                    } else {
                                        slideInHorizontally(
                                            initialOffsetX = { -it },
                                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                                        ) + fadeIn(animationSpec = tween(220))
                                    }
                                },
                                popExitTransition = {
                                    val initialRoute = initialState.destination.route ?: ""
                                    val targetRoute = targetState.destination.route ?: ""
                                    val initialIndex = routeIndices[initialRoute] ?: 0
                                    val targetIndex = routeIndices[targetRoute] ?: 0
                                    
                                    if (targetIndex > initialIndex) {
                                        slideOutHorizontally(
                                            targetOffsetX = { -it / 3 },
                                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                                        ) + fadeOut(animationSpec = tween(220))
                                    } else {
                                        slideOutHorizontally(
                                            targetOffsetX = { it / 3 },
                                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                                        ) + fadeOut(animationSpec = tween(220))
                                    }
                                }
                            ) {
                                composable("onboarding") {
                                    OnboardingScreen(
                                        onComplete = {
                                            currentNavController.navigate("explore") {
                                                popUpTo("onboarding") { inclusive = true }
                                            }
                                        }
                                    )
                                }
                                composable("explore") { }
                                composable("library") { }
                                composable("local_music") { }
                                composable("settings") { }
                                composable("tag_editor") {
                                    editingSong?.let { song ->
                                        TagEditorScreenEntryPoint(
                                            song = song,
                                            onNavigateUp = {
                                                currentNavController.navigateUp()
                                            }
                                        )
                                    }
                                }
                                composable("google_signin") {
                                    val accountViewModel: com.dsann.music.viewmodel.AccountViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                                    GoogleSignInScreen(
                                        accountViewModel = accountViewModel,
                                        onSignInSuccess = {
                                            currentNavController.navigate("explore") {
                                                popUpTo("explore") { inclusive = true }
                                            }
                                        },
                                        onNavigateUp = {
                                            currentNavController.navigateUp()
                                        }
                                    )
                                }
                            }
                        }

                        if (showNavigationRail) {
                            val railIconSize by animateDpAsState(
                                if (dynamicNavStyle) 34.dp else 26.dp,
                                label = "RailIconSize"
                            )
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = railStartPadding)
                                    .width(76.dp),
                                shape = RoundedCornerShape(38.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                tonalElevation = 6.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val items = listOf(
                                        Triple("explore", "Explore", R.drawable.ic_explore),
                                        Triple("library", "Library", R.drawable.ic_library_music),
                                        Triple("local_music", "Local Music", R.drawable.ic_music_note),
                                        Triple("settings", "Settings", R.drawable.ic_settings)
                                    )
                                    items.forEach { (route, label, iconResId) ->
                                        val selected = currentDestination?.route == route
                                        val tint = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(
                                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else androidx.compose.ui.graphics.Color.Transparent
                                                )
                                                .clickable {
                                                    if (!selected) {
                                                        currentNavController.navigate(route) {
                                                            popUpTo(currentNavController.graph.findStartDestination().id) {
                                                                saveState = true
                                                            }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                }
                                                .padding(vertical = if (navLabelsMode == "Never") 12.dp else 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                painter = painterResource(id = iconResId),
                                                contentDescription = label,
                                                tint = tint,
                                                modifier = Modifier.size(railIconSize)
                                            )
                                            if (navLabelsMode == "Always" || (navLabelsMode == "Selected" && selected)) {
                                                Text(
                                                    text = label,
                                                    color = tint,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        PlayerBottomSheetCompose(
                            activity = this@MainActivity,
                            viewModel = playerViewModel,
                            modifier = Modifier.layout { measurable, constraints ->
                                // Phone layouts have no navigation rail, so their player sheet
                                // must not subscribe to the 60 Hz expansion value or remeasure every frame.
                                val startInsetPx = if (useNavigationRail && railInset > 0.dp) {
                                    val expansion = playerExpansionFractionState.value.coerceIn(0f, 1f)
                                    (railInset.toPx() * (1f - expansion)).roundToInt().coerceAtLeast(0)
                                } else {
                                    0
                                }
                                val childMaxWidth = (constraints.maxWidth - startInsetPx).coerceAtLeast(0)
                                val childMinWidth = (constraints.minWidth - startInsetPx)
                                    .coerceAtLeast(0)
                                    .coerceAtMost(childMaxWidth)
                                val placeable = measurable.measure(
                                    constraints.copy(
                                        minWidth = childMinWidth,
                                        maxWidth = childMaxWidth
                                    )
                                )
                                val width = constraints.constrainWidth(placeable.width + startInsetPx)
                                val height = constraints.constrainHeight(placeable.height)
                                layout(width, height) {
                                    placeable.placeRelative(startInsetPx, 0)
                                }
                            }.fillMaxSize()
                        )
                        AudioFormatChoiceDialogHost()
                    }
                }
            }
        }

        handleIntent(intent)

        updateManager = UpdateManager(this)
        checkForUpdateOnLaunch()
    }

    fun navigateToSettings() {
        val controller = navController ?: return
        if (controller.currentDestination?.route != "settings") {
            controller.navigate("settings") {
                popUpTo(controller.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            previousDestination = "settings"
        }
    }

    fun navigateToGoogleSignIn() {
        val controller = navController ?: return
        if (controller.currentDestination?.route != "google_signin") {
            controller.navigate("google_signin")
        }
    }

    fun showArtistPage(artistId: String?, artistName: String?) {
        val controller = navController ?: return
        val cameFromOutside = controller.currentDestination?.route != "explore"
        
        if (cameFromOutside) {
            isNavigating = true
            if (controller.currentDestination?.route == "library") {
                // Keep Library below the Explore detail page, so Back returns to Library.
                controller.navigate("explore") {
                    launchSingleTop = true
                }
            } else {
                controller.navigate("explore") {
                    popUpTo(controller.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            previousDestination = "explore"
            isNavigating = false
        }

        if (!artistId.isNullOrBlank()) {
            if (cameFromOutside) {
                exploreViewModel.cameFromLibrary = true
            }
            exploreViewModel.loadArtist(artistId)
        } else if (!artistName.isNullOrBlank() && artistName != "Unknown Artist") {
            if (cameFromOutside) {
                exploreViewModel.cameFromLibrary = true
            }
            exploreViewModel.setSearchFilter(com.dsann.music.data.innertube.SearchFilter.ARTISTS)
            exploreViewModel.search(artistName)
        }
    }

    private fun checkForUpdateOnLaunch() {
        if (!shouldCheckForUpdate()) {
            Log.d(TAG, "Update check skipped (checked recently)")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = GitHubReleaseClient("subhu1895991", "SubhuMusic")
                val release = client.latestRelease ?: return@launch

                val currentVersion = BuildConfig.VERSION_NAME
                if (release.hasApk() && VersionUtils.isNewer(release.tagName, currentVersion)) {
                    withContext(Dispatchers.Main) {
                        showUpdateDialog(release)
                    }
                } else {
                    Log.d(TAG, "App is up to date (current: $currentVersion, latest: ${release.tagName})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed", e)
            }
        }
    }

    private fun showUpdateDialog(release: GitHubReleaseClient.ReleaseInfo) {
        val message = StringBuilder("Version ${release.tagName} is now available!\n\n")
        release.changelog?.takeIf { it.isNotEmpty() }?.let { changelog ->
            val displayLog = if (changelog.length > 350) "${changelog.substring(0, 350)}..." else changelog
            message.append("What's New:\n").append(displayLog)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Update Available")
            .setMessage(message.toString())
            .setCancelable(true)
            .setPositiveButton("Update Now") { _, _ ->
                val rootView = findViewById<View>(android.R.id.content)
                updateManager.downloadAndInstall(rootView, release)
            }
            .setNegativeButton("Later") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Don't Show Again") { dialog, _ ->
                disableUpdateCheck()
                dialog.dismiss()
            }
            .show()
    }

    private fun shouldCheckForUpdate(): Boolean {
        val prefs = getSharedPreferences("update_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("update_check_disabled", false)) return false

        val lastCheck = prefs.getLong("last_update_check", 0)
        val currentTime = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L

        return if (currentTime - lastCheck > oneDayMillis) {
            prefs.edit { putLong("last_update_check", currentTime) }
            true
        } else false
    }

    private fun disableUpdateCheck() {
        getSharedPreferences("update_prefs", MODE_PRIVATE).edit {
            putBoolean("update_check_disabled", true)
        }
        Log.d(TAG, "Auto update check disabled by user")
    }

    private var audioServiceStartupRequested = false

    private fun prepareAndStartAudioService() {
        if (audioServiceStartupRequested || isServiceBound) return
        audioServiceStartupRequested = true

        lifecycleScope.launch {
            val cacheReady = withContext(Dispatchers.IO) {
                try {
                    AudioPlaybackService.getCache(applicationContext)
                    true
                } catch (firstError: Exception) {
                    Log.e(TAG, "Media cache initialization failed; retrying after clearing the damaged cache", firstError)
                    try {
                        AudioPlaybackService.clearMediaCache(applicationContext)
                        AudioPlaybackService.getCache(applicationContext)
                        true
                    } catch (retryError: Exception) {
                        Log.e(TAG, "Playback service start postponed because the media cache could not be opened", retryError)
                        false
                    }
                }
            }

            if (cacheReady && !isFinishing && !isDestroyed) {
                startAudioService()
            } else {
                audioServiceStartupRequested = false
            }
        }
    }

    private fun startAudioService() {
        val serviceIntent = Intent(this, AudioPlaybackService::class.java)
        try {
            startService(serviceIntent)
        } catch (e: Exception) {
            Log.w(TAG, "startService failed, relying on bind: ${e.message}")
        }
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE)
        Log.d(TAG, "Audio service started and bound")
    }

    private fun ensureServiceRunning() {
        val manager = getSystemService(ACTIVITY_SERVICE) as? ActivityManager ?: return
        val isRunning = manager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == AudioPlaybackService::class.java.name
        }
        if (!isRunning) prepareAndStartAudioService()
    }

    private fun checkAudioPermission() {
        val requiredPermissions = getRequiredRuntimePermissions()
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), AUDIO_PERMISSION_REQUEST)
        }
    }

    private fun getRequiredRuntimePermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.RECORD_AUDIO
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            )
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    override fun onResume() {
        super.onResume()
        setupSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) setupSystemBars()
    }

    fun setupSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDarkMode
            isAppearanceLightNavigationBars = !isDarkMode
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PREVIOUS_DESTINATION, previousDestination)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController?.navigateUp() == true || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        ensureServiceRunning()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            try {
                unbindService(serviceConnection)
            } catch (_: Exception) {
                Log.d(TAG, "Service not bound, skipping unbind")
            }
            isServiceBound = false
        }
    }
    fun showSnackbar(message: String, duration: Int) {
        com.dsann.music.ui.SnackbarController.showMessage(message)
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        if (Intent.ACTION_VIEW == intent.action || "android.intent.action.MUSIC_PLAYER" == intent.action) {
            intent.data?.let {
                playExternalUri(it)
                return
            }
        }

        if (navController != null && intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)) {
            try {
                navController?.navigate("library")
            } catch (e: Exception) {
                Log.e(TAG, "Navigation request failed in handleIntent", e)
            }
        }
    }

    private fun playExternalUri(uri: Uri) {
        var displayName = "External Track"
        if ("content" == uri.scheme) {
            try {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            cursor.getString(nameIndex)?.takeIf { it.isNotEmpty() }?.let {
                                displayName = it
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to query metadata for external uri", e)
            }
        } else if ("file" == uri.scheme) {
            displayName = uri.lastPathSegment ?: displayName
        }

        if (displayName.contains(".")) {
            val dotIdx = displayName.lastIndexOf('.')
            if (dotIdx > 0) displayName = displayName.substring(0, dotIdx)
        }

        val externalId = -System.currentTimeMillis()
        val externalSong = SongItem(
            externalId,
            displayName,
            "External Source",
            -1L,
            uri.toString(),
            0L,
            System.currentTimeMillis() / 1000
        ).apply { contentUri = uri }

        val triggerPlayTask = Runnable {
            playerViewModel.playSong(externalSong)
            navController?.let { controller ->
                try {
                    controller.navigate("library")
                } catch (e: Exception) {
                    Log.e(TAG, "Navigation route to PlayerFragment failed in playExternalUri", e)
                }
            }
        }

        if (!isServiceBound) {
            lifecycleScope.launch {
                playerViewModel.audioServiceState.collect { service ->
                    if (service != null) {
                        triggerPlayTask.run()
                        this@launch.coroutineContext[kotlinx.coroutines.Job]?.cancel()
                    }
                }
            }
        } else {
            triggerPlayTask.run()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val AUDIO_PERMISSION_REQUEST = 100
        private const val SPLASH_DURATION = 800L
        private const val KEY_PREVIOUS_DESTINATION = "key_previous_destination"
        const val EXTRA_OPEN_PLAYER = "open_player"
    }
}
