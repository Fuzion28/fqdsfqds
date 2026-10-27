package com.sonora.music.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sonora.music.data.Song
import com.sonora.music.ui.components.AddToPlaylistSheet
import com.sonora.music.ui.components.NameDialog
import com.sonora.music.ui.components.SongActions
import com.sonora.music.ui.components.bounceClick
import com.sonora.music.ui.components.rememberPop
import com.sonora.music.ui.detail.CollectionScreen
import com.sonora.music.ui.detail.CollectionSource
import com.sonora.music.ui.home.HomeScreen
import com.sonora.music.ui.library.LibraryScreen
import com.sonora.music.ui.permission.PermissionScreen
import com.sonora.music.ui.player.FullPlayer
import com.sonora.music.ui.player.FullPlayerActions
import com.sonora.music.ui.player.MiniPlayer
import com.sonora.music.ui.player.QueueSheet
import com.sonora.music.ui.search.SearchScreen
import com.sonora.music.ui.theme.SonoraColors
import kotlinx.coroutines.launch

/** Bottom space screens must leave for the mini player and navigation bar. */
val LocalBottomInset = compositionLocalOf { 0.dp }

/** Navigation entry points shared by all screens. */
class AppNav(private val nav: NavHostController) {
    fun album(id: Long) = nav.navigate("album/$id")
    fun artist(id: Long) = nav.navigate("artist/$id")
    fun playlist(id: String) = nav.navigate("playlist/$id")
    fun liked() = nav.navigate("liked")
    fun recentlyAdded() = nav.navigate("recent")
    fun mix(artistId: Long) = nav.navigate("mix/$artistId")
    fun back() = nav.popBackStack()
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Home("home", "Accueil", Icons.Outlined.Home, Icons.Rounded.Home),
    Search("search", "Rechercher", Icons.Outlined.Search, Icons.Rounded.Search),
    Library("library", "Bibliothèque", Icons.Outlined.LibraryMusic, Icons.Rounded.LibraryMusic),
}

private fun audioPermission() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

@Composable
fun AppRoot(vm: MainViewModel) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, audioPermission()) == PackageManager.PERMISSION_GRANTED)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = ContextCompat.checkSelfPermission(context, audioPermission()) == PackageManager.PERMISSION_GRANTED
    }
    LaunchedEffect(granted) { if (granted) vm.loadLibrary() }

    Box(Modifier.fillMaxSize().background(SonoraColors.Background)) {
        if (granted) {
            MainScaffold(vm)
        } else {
            PermissionScreen(audioPermission(), onGranted = { granted = true })
        }
    }
}

@Composable
private fun MainScaffold(vm: MainViewModel) {
    val nav = rememberNavController()
    val appNav = remember(nav) { AppNav(nav) }
    val scope = rememberCoroutineScope()

    val library by vm.library.collectAsState()
    val playerState by vm.player.state.collectAsState()
    val progress by vm.player.progress.collectAsState()
    val liked by vm.store.liked.collectAsState()
    val playlists by vm.store.playlists.collectAsState()

    val current: Song? = playerState.currentId?.let { library.songsById[it] }
    val likedSet = remember(liked) { liked.toSet() }

    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var addToPlaylistFor by remember { mutableStateOf<Song?>(null) }
    var createPlaylist by remember { mutableStateOf(false) }
    var createWith by remember { mutableStateOf<Song?>(null) }
    var showQueue by remember { mutableStateOf(false) }

    val expand = remember { Animatable(0f).apply { updateBounds(0f, 1f) } }
    fun animateExpand(target: Float) = scope.launch {
        expand.animateTo(target, spring(dampingRatio = 0.92f, stiffness = Spring.StiffnessMediumLow))
    }
    LaunchedEffect(current == null) { if (current == null) expand.snapTo(0f) }
    BackHandler(enabled = expand.targetValue > 0.5f) { animateExpand(0f) }

    val songActions = remember(likedSet, appNav) {
        SongActions(
            isLiked = { it.id in likedSet },
            onToggleLike = vm::toggleLike,
            onAddToQueue = { vm.player.addToQueue(it) },
            onAddToPlaylist = { addToPlaylistFor = it },
            onGoToAlbum = { appNav.album(it.albumId) },
            onGoToArtist = { appNav.artist(it.artistId) },
        )
    }

    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset: Dp = 64.dp + navInset + (if (current != null) 70.dp else 0.dp) + 16.dp

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val heightPx = constraints.maxHeight.toFloat()

        CompositionLocalProvider(LocalBottomInset provides bottomInset) {
            NavHost(
                navController = nav,
                startDestination = Tab.Home.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween(320)) + slideInHorizontally(tween(320)) { it / 8 } },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(320)) },
                popExitTransition = { fadeOut(tween(220)) + slideOutHorizontally(tween(260)) { it / 8 } },
            ) {
                composable(Tab.Home.route, enterTransition = { fadeIn(tween(250)) }, exitTransition = { fadeOut(tween(200)) }) {
                    HomeScreen(vm, library, playerState, appNav, onSongActions = songActions)
                }
                composable(Tab.Search.route, enterTransition = { fadeIn(tween(250)) }, exitTransition = { fadeOut(tween(200)) }) {
                    SearchScreen(vm, library, playerState, appNav, songActions)
                }
                composable(Tab.Library.route, enterTransition = { fadeIn(tween(250)) }, exitTransition = { fadeOut(tween(200)) }) {
                    LibraryScreen(library, liked, playlists, appNav, onCreatePlaylist = { createWith = null; createPlaylist = true })
                }
                composable("album/{id}", listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    CollectionScreen(vm, CollectionSource.AlbumSource(entry.arguments!!.getLong("id")), appNav, songActions)
                }
                composable("artist/{id}", listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    CollectionScreen(vm, CollectionSource.ArtistSource(entry.arguments!!.getLong("id")), appNav, songActions)
                }
                composable("mix/{id}", listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    CollectionScreen(vm, CollectionSource.MixSource(entry.arguments!!.getLong("id")), appNav, songActions)
                }
                composable("playlist/{id}") { entry ->
                    CollectionScreen(vm, CollectionSource.PlaylistSource(entry.arguments!!.getString("id")!!), appNav, songActions)
                }
                composable("liked") { CollectionScreen(vm, CollectionSource.Liked, appNav, songActions) }
                composable("recent") { CollectionScreen(vm, CollectionSource.RecentlyAdded, appNav, songActions) }
            }
        }

        // Bottom chrome: mini player + navigation bar, slides away as the player expands.
        var chromeHeight by remember { mutableStateOf(0f) }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { chromeHeight = it.height.toFloat() }
                .graphicsLayer {
                    translationY = expand.value * chromeHeight
                    alpha = 1f - expand.value * 0.6f
                }
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.35f to SonoraColors.Background.copy(alpha = 0.85f),
                        1f to SonoraColors.Background,
                    )
                ),
        ) {
            Spacer(Modifier.height(18.dp))
            AnimatedVisibility(
                visible = current != null,
                enter = slideInVertically(spring(dampingRatio = 0.7f)) { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                current?.let { song ->
                    MiniPlayer(
                        song = song,
                        isPlaying = playerState.isPlaying,
                        progress = progress,
                        liked = song.id in likedSet,
                        onToggleLike = { vm.toggleLike(song) },
                        onTogglePlay = vm.player::togglePlay,
                        onNext = vm.player::nextItem,
                        onPrevious = vm.player::previousItem,
                        onExpand = { animateExpand(1f) },
                    )
                }
            }
            BottomNavBar(tab) { selected ->
                if (selected == tab) {
                    nav.popBackStack(selected.route, inclusive = false)
                } else {
                    tab = selected
                    nav.navigate(selected.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }

        // Full-screen player, dragged down to dismiss.
        if (current != null && expand.value > 0.001f) {
            val dragState = rememberDraggableState { delta ->
                scope.launch { expand.snapTo(expand.value - delta / heightPx) }
            }
            FullPlayer(
                song = current,
                previous = playerState.previousId?.let { library.songsById[it] },
                next = playerState.nextId?.let { library.songsById[it] },
                state = playerState,
                progress = vm.player.progress,
                liked = current.id in likedSet,
                actions = FullPlayerActions(
                    onCollapse = { animateExpand(0f) },
                    onTogglePlay = vm.player::togglePlay,
                    onNext = vm.player::next,
                    onPrevious = vm.player::previous,
                    onNextItem = vm.player::nextItem,
                    onPreviousItem = vm.player::previousItem,
                    onSeek = vm.player::seekTo,
                    onShuffle = vm.player::toggleShuffle,
                    onRepeat = vm.player::cycleRepeat,
                    onToggleLike = { vm.toggleLike(current) },
                    onOpenQueue = { showQueue = true },
                    onAddToPlaylist = { addToPlaylistFor = current },
                    onGoToAlbum = { animateExpand(0f); appNav.album(current.albumId) },
                    onGoToArtist = { animateExpand(0f); appNav.artist(current.artistId) },
                ),
                modifier = Modifier
                    .graphicsLayer {
                        translationY = (1f - expand.value) * heightPx
                        val corner = (1f - expand.value)
                        alpha = (expand.value * 3f).coerceAtMost(1f)
                        scaleX = 1f - corner * 0.05f
                    }
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            val target = if (velocity > 1800f || expand.value < 0.75f) 0f else 1f
                            animateExpand(target)
                        },
                    ),
            )
        }
    }

    addToPlaylistFor?.let { song ->
        AddToPlaylistSheet(
            songId = song.id,
            playlists = playlists,
            library = library,
            onDismiss = { addToPlaylistFor = null },
            onPick = { vm.store.addToPlaylist(it.id, song.id); addToPlaylistFor = null },
            onCreate = { createWith = song; addToPlaylistFor = null; createPlaylist = true },
        )
    }
    if (createPlaylist) {
        NameDialog(
            title = "Nommer votre playlist",
            onDismiss = { createPlaylist = false },
            onConfirm = { name ->
                val playlist = vm.createPlaylist(name, createWith)
                createPlaylist = false
                if (createWith == null) appNav.playlist(playlist.id)
                createWith = null
            },
        )
    }
    if (showQueue) {
        QueueSheet(playerState, library, onDismiss = { showQueue = false }, onSkipTo = vm.player::skipToQueueIndex)
    }
}

@Composable
private fun BottomNavBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { tab ->
            val isSelected = tab == selected
            val pop = rememberPop(if (isSelected) tab else null)
            val color by animateColorAsState(
                if (isSelected) Color.White else SonoraColors.TextTertiary, tween(250), label = "tab",
            )
            Column(
                Modifier
                    .weight(1f)
                    .bounceClick(0.85f) { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    if (isSelected) tab.selectedIcon else tab.icon,
                    contentDescription = tab.label,
                    tint = color,
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer {
                            val s = if (isSelected) pop.value else 1f
                            scaleX = s
                            scaleY = s
                        },
                )
                Text(tab.label, style = MaterialTheme.typography.labelSmall, color = color)
            }
        }
    }
}
