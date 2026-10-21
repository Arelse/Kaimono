package eu.kanade.tachiyomi.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import coil3.compose.AsyncImage
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.more.settings.screen.SettingsDataScreen
import eu.kanade.tachiyomi.data.auth.AuthManager
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.isNovelSource
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.profile.LeaderboardScreen
import eu.kanade.tachiyomi.ui.profile.ProfileScreen
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.domain.source.interactor.GetRemoteManga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/*
 * Media Hub: the alternate Home tab layout selected in Settings > UI Settings > Home screen layout.
 * It reads the same sources, covers and latest/popular data as the Default layout (DiscoverTab), and
 * re-skins it to the "Anime, Manga & Novel Hub" reference design: a compact header with four action
 * buttons, a search bar, an All / Anime / Manga / Novels filter, a hero carousel, a Recently Updated
 * row and one shelf per source. The palette, sync, trophy and account buttons open bottom sheets.
 *
 * Colors here are fixed to the reference design (plus the accent and AMOLED options from the palette
 * sheet), so this layout does not follow the app's Material theme.
 */

private val HubBackground = Color(0xFF111216)
private val HubSurface = Color(0xFF1A1B22)
private val HubSurfaceSoft = Color(0xFF121318)
private val HubElevated = Color(0xFF23252F)
private val HubBorder = Color(0x1AFFFFFF)
// Header action buttons, matched to the mockup's .top-action-btn look.
private val HubActionFill = Color.White.copy(alpha = 0.06f)
private val HubActionBorder = Color.White.copy(alpha = 0.07f)
private val HubBadgeRing = Color(0xFF111216)
private val HubSyncDot = Color(0xFF34D399)
private val HubZinc200 = Color(0xFFE4E4E7)
private val HubZinc300 = Color(0xFFD4D4D8)
private val HubZinc400 = Color(0xFFA1A1AA)
private val HubZinc500 = Color(0xFF71717A)
private val HubZinc800 = Color(0xFF27272A)
private val HubNovelPill = Color(0xFFFCD34D)
private val HubOnAccent = Color(0xFF09090B)

private data class HubSwatch(val label: String, val argb: Int)

private val hubSwatches = listOf(
    HubSwatch("Lavender", 0xFFC8B6FF.toInt()),
    HubSwatch("Sky Blue", 0xFF90CAF9.toInt()),
    HubSwatch("Matcha", 0xFFA5D6A7.toInt()),
    HubSwatch("Coral", 0xFFFFAB91.toInt()),
)

private enum class HubCategory(val label: String, val title: String) {
    ALL("All", "Manga"),
    ANIME("Anime", "Anime"),
    MANGA("Manga", "Manga"),
    NOVELS("Novels", "Novels"),
}

private enum class HubDrawer { PALETTE, SYNC, TROPHY, ACCOUNT }

private fun HubCategory.icon(): ImageVector = when (this) {
    HubCategory.ALL -> Icons.Outlined.GridView
    HubCategory.ANIME -> Icons.Outlined.Movie
    HubCategory.MANGA -> Icons.Outlined.Book
    HubCategory.NOVELS -> Icons.AutoMirrored.Outlined.MenuBook
}

// This app has no anime sources, so the Anime filter is always empty.
private fun CatalogueSource.belongsTo(category: HubCategory): Boolean = when (category) {
    HubCategory.ALL -> true
    HubCategory.ANIME -> false
    HubCategory.MANGA -> !isNovelSource()
    HubCategory.NOVELS -> isNovelSource()
}

/** Label on a card: NOVEL for novels only. Manga, manhwa and manhua cards get no label. */
private fun mangaKindLabel(manga: Manga): String = if (manga.isNovel) "NOVEL" else ""

/** Label on a source row: "Novels" for novel sources only. Others get no label. */
private fun sourceKindLabel(source: CatalogueSource): String = if (source.isNovelSource()) "Novels" else ""

private fun CatalogueSource.langLabel(): String {
    return if (lang == "all" || lang.isEmpty()) "Multi-language" else LocaleHelper.getLocalizedDisplayName(lang)
}

/** Round-robins the lists so one source doesn't crowd out the others, de-duplicated by title. */
private fun interleave(lists: List<List<Manga>>, limit: Int): List<Manga> {
    val result = mutableListOf<Manga>()
    val longest = lists.maxOfOrNull { it.size } ?: 0
    for (i in 0 until longest) {
        for (list in lists) {
            if (i < list.size && result.size < limit && result.none { it.title == list[i].title }) {
                result.add(list[i])
            }
        }
    }
    return result
}

@Composable
fun MediaHubHome() {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    val tabNavigator = LocalTabNavigator.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel<DiscoverViewModel>()
    val state by viewModel.state.collectAsState()

    val uiPreferences = remember { Injekt.get<UiPreferences>() }
    val accentArgb by uiPreferences.homeHubAccent.collectAsState()
    val amoled by uiPreferences.homeHubAmoled.collectAsState()

    val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
    // Same "Items per row" value as the Library's Display settings (0 = auto).
    val shelfColumns by libraryPreferences.portraitColumns.collectAsState()
    val updatesCount by libraryPreferences.newUpdatesCount.collectAsState()

    val authManager: AuthManager = remember { Injekt.get() }
    val user by authManager.currentUser.collectAsState()

    val gradientOn by uiPreferences.accentGradientEnabled.collectAsState()
    val accent = Color(accentArgb)
    // With the accent gradient on, the background is transparent so the gradient covers the whole screen.
    val background = when {
        gradientOn -> Color.Transparent
        amoled -> Color.Black
        else -> HubBackground
    }
    var headerPx by remember { mutableIntStateOf(0) }
    val headerHeight = with(LocalDensity.current) { headerPx.toDp() }

    var category by remember { mutableStateOf(HubCategory.ALL) }
    var drawer by remember { mutableStateOf<HubDrawer?>(null) }
    var forceEmptyRecent by remember { mutableStateOf(false) }
    // Bumped when a dot is tapped, so the auto-slide timer starts again from zero.
    var heroTick by remember { mutableStateOf(0) }
    var syncStatus by remember { mutableStateOf("Tap Sync Now to check your sources for new chapters") }

    val categorySources = state.allSources.filter { it.belongsTo(category) }
    val sourceById = state.allSources.associateBy { it.id }

    LaunchedEffect(categorySources.map { it.id }) {
        categorySources.forEach { source ->
            viewModel.prefetchPopular(source)
            viewModel.prefetchLatest(source)
        }
    }

    // Read the caches as snapshots so this composable recomposes when a source finishes loading.
    val popularSnapshot = viewModel.popularCache.toMap()
    val latestSnapshot = viewModel.latestCache.toMap()

    val featured = interleave(categorySources.mapNotNull { popularSnapshot[it.id] }, limit = 5)
    val recent = interleave(categorySources.mapNotNull { latestSnapshot[it.id] }, limit = 12)
    val showRecent = recent.isNotEmpty() && !forceEmptyRecent
    // Featured cards: swipe by hand, or auto-advance every 5s with a smooth slide.
    val heroPagerState = rememberPagerState(pageCount = { featured.size })
    val heroScope = rememberCoroutineScope()
    val heroSlideSpec = tween<Float>(durationMillis = 700, easing = FastOutSlowInEasing)

    LaunchedEffect(heroTick, featured.size) {
        if (featured.size > 1) {
            while (true) {
                delay(5_000)
                // Skip this tick while the user is dragging the card.
                if (!heroPagerState.isScrollInProgress) {
                    heroPagerState.animateScrollToPage(
                        (heroPagerState.currentPage + 1) % featured.size,
                        animationSpec = heroSlideSpec,
                    )
                }
            }
        }
    }

    val openManga: (Manga) -> Unit = { manga ->
        scope.launch {
            val localManga = viewModel.getNetworkToLocalManga(manga)
            navigator.push(MangaScreen(localManga.id))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(background)) {
        // The top block floats above the list as glass, so scrolled content stays visible behind it.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(1f)
                .onSizeChanged { headerPx = it.height }
                .background(background),
        ) {
        HubHeader(
            title = category.title,
            accent = accent,
            showUpdateDot = updatesCount > 0,
            photoUrl = user?.photoUrl,
            onPalette = { drawer = HubDrawer.PALETTE },
            onSync = { navigator.push(eu.kanade.tachiyomi.ui.updates.UpdatesScheduleScreen) },
            onTrophy = { drawer = HubDrawer.TROPHY },
            onAccount = { drawer = HubDrawer.ACCOUNT },
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            HubSegmented(
                selected = category,
                accent = accent,
                onSelect = {
                    category = it
                    heroScope.launch { heroPagerState.scrollToPage(0) }
                },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        }

        // Search lives in its own screen now: this floating button opens Global Search directly.
        HubSearchFab(
            accent = accent,
            onClick = { navigator.push(GlobalSearchScreen()) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .zIndex(2f)
                .padding(end = 20.dp, bottom = 96.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = headerHeight, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = accent)
                    }
                }
            } else if (state.allSources.isEmpty() || categorySources.isEmpty()) {
                item {
                    HubNoSources(
                        accent = accent,
                        message = if (category == HubCategory.ANIME) {
                            "No anime sources are installed in this app."
                        } else if (state.allSources.isEmpty()) {
                            "No sources found. Install an extension to get started."
                        } else {
                            "No ${category.label.lowercase()} sources are enabled."
                        },
                        onBrowse = { tabNavigator.current = BrowseTab },
                    )
                }
            } else {
                if (featured.isNotEmpty()) {
                    item {
                        HorizontalPager(
                            state = heroPagerState,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            pageSpacing = 12.dp,
                            beyondViewportPageCount = 1,
                        ) { page ->
                            val manga = featured[page]
                            HubHero(
                                manga = manga,
                                sourceName = sourceById[manga.source]?.name.orEmpty(),
                                index = heroPagerState.currentPage,
                                count = featured.size,
                                accent = accent,
                                background = background,
                                onOpen = { openManga(manga) },
                                onBrowseSource = {
                                    sourceById[manga.source]?.let { source ->
                                        navigator.push(BrowseSourceScreen(source.id, GetRemoteManga.QUERY_POPULAR, isFeed = true))
                                    }
                                },
                                onSelectDot = { target ->
                                    heroTick++
                                    heroScope.launch {
                                        heroPagerState.animateScrollToPage(target, animationSpec = heroSlideSpec)
                                    }
                                },
                            )
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Recently Updated",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                HubPill(
                                    text = "${recent.size} new",
                                    textColor = HubZinc300,
                                    background = HubZinc800,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Toggle Empty",
                                    fontSize = 11.sp,
                                    color = HubZinc400,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable { forceEmptyRecent = !forceEmptyRecent },
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable { navigator.push(UpdatesTab) }
                                        .semantics { contentDescription = "View all updates" },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                        contentDescription = null,
                                        tint = HubZinc400,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (showRecent) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(recent, key = { "recent_${it.source}_${it.url}" }) { manga ->
                                    HubRecentCard(
                                        manga = manga,
                                        sourceName = sourceById[manga.source]?.name.orEmpty(),
                                        accent = accent,
                                        onClick = { openManga(manga) },
                                    )
                                }
                            }
                        } else {
                            HubRecentEmpty(
                                accent = accent,
                                onCheck = {
                                    val started = LibraryUpdateJob.startNow(context)
                                    syncStatus = if (started) {
                                        "Checking your sources for new chapters..."
                                    } else {
                                        "A check is already running, or nothing needs updating."
                                    }
                                },
                            )
                        }
                    }
                }

                items(categorySources, key = { "shelf_${it.id}" }) { source ->
                    HubShelf(
                        source = source,
                        mangas = popularSnapshot[source.id],
                        accent = accent,
                        columns = shelfColumns,
                        onOpen = openManga,
                        onSeeAll = {
                            navigator.push(BrowseSourceScreen(source.id, GetRemoteManga.QUERY_POPULAR, isFeed = true))
                        },
                    )
                }
            }
        }
    }

    if (drawer != null) {
        ModalBottomSheet(
            onDismissRequest = { drawer = null },
            containerColor = HubElevated,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
            ) {
                when (drawer) {
                    HubDrawer.PALETTE -> {
                        HubDrawerTitle(title = "Appearance & Palette", subtitle = "Accent color for Featured")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            hubSwatches.forEach { swatch ->
                                val selected = swatch.argb == accentArgb
                                val shape = RoundedCornerShape(16.dp)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(shape)
                                        .background(HubSurfaceSoft)
                                        .border(1.dp, if (selected) Color(swatch.argb).copy(alpha = 0.5f) else HubBorder, shape)
                                        .clickable { uiPreferences.homeHubAccent.set(swatch.argb) }
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(swatch.argb)),
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = swatch.label, fontSize = 10.5.sp, color = HubZinc300)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(HubSurfaceSoft)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Pure AMOLED reader mode",
                                fontSize = 14.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = amoled,
                                onCheckedChange = { uiPreferences.homeHubAmoled.set(it) },
                            )
                        }
                    }
                    HubDrawer.SYNC -> {
                        HubDrawerTitle(title = "Library & Source Sync", subtitle = syncStatus)
                        HubPillButton(
                            label = "Sync Now",
                            accent = accent,
                            onClick = {
                                val started = LibraryUpdateJob.startNow(context)
                                syncStatus = if (started) {
                                    "Syncing your library and sources..."
                                } else {
                                    "A sync is already running, or nothing needs updating."
                                }
                            },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (state.allSources.isEmpty()) {
                            Text(text = "No sources installed.", fontSize = 12.sp, color = HubZinc400)
                        }
                        state.allSources.take(8).forEach { source ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = source.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(text = source.langLabel(), fontSize = 11.sp, color = HubZinc400)
                                }
                                HubPill(
                                    text = sourceKindLabel(source),
                                    textColor = accent,
                                    background = accent.copy(alpha = 0.15f),
                                )
                            }
                        }
                    }
                    HubDrawer.TROPHY -> {
                        HubDrawerTitle(title = "Reading & Watching Milestones", subtitle = "Your sources and updates at a glance")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            HubStatTile(
                                label = "Manga",
                                value = state.allSources.count { !it.isNovelSource() }.toString(),
                                caption = "Sources",
                                accent = accent,
                                modifier = Modifier.weight(1f),
                            )
                            HubStatTile(
                                label = "Novels",
                                value = state.allSources.count { it.isNovelSource() }.toString(),
                                caption = "Sources",
                                accent = accent,
                                modifier = Modifier.weight(1f),
                            )
                            HubStatTile(
                                label = "Updates",
                                value = updatesCount.toString(),
                                caption = "New",
                                accent = accent,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HubSecondaryButton(
                            label = "Open leaderboard",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                drawer = null
                                navigator.push(LeaderboardScreen())
                            },
                        )
                    }
                    HubDrawer.ACCOUNT -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(accent.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (user?.photoUrl != null) {
                                    AsyncImage(
                                        model = user?.photoUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Text(
                                        text = (user?.displayName ?: "G").take(1).uppercase(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accent,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user?.displayName ?: "Guest",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = user?.email ?: "Not signed in",
                                    fontSize = 11.sp,
                                    color = HubZinc400,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            HubSecondaryButton(
                                label = "Profile",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    drawer = null
                                    navigator.push(ProfileScreen())
                                },
                            )
                            HubSecondaryButton(
                                label = "Backup",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    drawer = null
                                    navigator.push(SettingsDataScreen)
                                },
                            )
                            HubSecondaryButton(
                                label = "Global search",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    drawer = null
                                    navigator.push(GlobalSearchScreen())
                                },
                            )
                        }
                    }
                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun HubHeader(
    title: String,
    accent: Color,
    showUpdateDot: Boolean,
    photoUrl: Any?,
    onPalette: () -> Unit,
    onSync: () -> Unit,
    onTrophy: () -> Unit,
    onAccount: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HubActionButton(description = "Theme and accent color", onClick = onPalette) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(19.dp),
                )
            }
            HubActionButton(
                description = "Updates",
                onClick = onSync,
                badge = if (showUpdateDot) HubSyncDot else null,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Update,
                    contentDescription = null,
                    tint = HubZinc200,
                    modifier = Modifier.size(19.dp),
                )
            }
            HubActionButton(description = "Achievements and stats", onClick = onTrophy) {
                // Same trophy icon as the Default layout's header.
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp),
                )
            }
            HubActionButton(
                description = "Account",
                onClick = onAccount,
                badge = accent,
                badgeAlignment = Alignment.BottomEnd,
                badgeInset = PaddingValues(bottom = 4.dp, end = 4.dp),
            ) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(38.dp).clip(CircleShape),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HubActionButton(
    description: String,
    onClick: () -> Unit,
    badge: Color? = null,
    badgeAlignment: Alignment = Alignment.TopEnd,
    badgeInset: PaddingValues = PaddingValues(top = 4.dp, end = 4.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(HubActionFill)
            .border(1.dp, HubActionBorder, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        content()
        if (badge != null) {
            // Dot with a dark ring cut-out, same as the mockup's ring-2 ring-[#111216].
            Box(
                modifier = Modifier
                    .align(badgeAlignment)
                    .padding(badgeInset)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(HubBadgeRing),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(badge),
                )
            }
        }
    }
}

/** Round search button that floats above the bottom bar and opens Global Search. */
@Composable
private fun HubSearchFab(
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(HubSurface)
            .border(1.dp, HubBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = "Search",
            tint = accent,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun HubSegmented(
    selected: HubCategory,
    accent: Color,
    onSelect: (HubCategory) -> Unit,
) {
    val outerShape = RoundedCornerShape(16.dp)
    val itemShape = RoundedCornerShape(12.dp)
    val selectedText = lerp(accent, Color.White, 0.5f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(outerShape)
            .background(HubSurface)
            .border(1.dp, HubBorder, outerShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HubCategory.entries.forEach { category ->
            val isSelected = category == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(itemShape)
                    .background(if (isSelected) accent.copy(alpha = 0.16f) else Color.Transparent)
                    .border(1.dp, if (isSelected) accent.copy(alpha = 0.36f) else Color.Transparent, itemShape)
                    .clickable { onSelect(category) }
                    .height(44.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = category.icon(),
                    contentDescription = null,
                    tint = if (isSelected) selectedText else HubZinc400,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = category.label,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) selectedText else HubZinc400,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun HubPill(
    text: String,
    textColor: Color,
    background: Color,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
) {
    // Empty text means "no label" (only novels get one), so draw nothing.
    if (text.isEmpty()) return
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HubPillButton(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = HubOnAccent,
            maxLines = 1,
        )
    }
}

@Composable
private fun HubSecondaryButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(HubSurfaceSoft)
            .border(1.dp, HubBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HubDrawerTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = subtitle, fontSize = 12.sp, color = HubZinc400)
    }
}

@Composable
private fun HubStatTile(
    label: String,
    value: String,
    caption: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(HubSurfaceSoft)
            .border(1.dp, HubBorder, shape)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accent)
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Text(text = caption, fontSize = 10.sp, color = HubZinc400)
    }
}

@Composable
private fun HubHero(
    manga: Manga,
    sourceName: String,
    index: Int,
    count: Int,
    accent: Color,
    background: Color,
    onOpen: () -> Unit,
    onBrowseSource: () -> Unit,
    onSelectDot: (Int) -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(222.dp)
            .clip(shape)
            .background(HubSurface)
            .border(1.dp, HubBorder, shape),
    ) {
        AsyncImage(
            model = manga.asMangaCover(),
            contentDescription = manga.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.45f to background.copy(alpha = 0.6f),
                            1f to background,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            background.copy(alpha = 0.9f),
                            background.copy(alpha = 0.4f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HubPill(
                text = "Featured",
                textColor = Color.White,
                background = Color.Black.copy(alpha = 0.55f),
                dotColor = accent,
            )
            HubPill(
                text = sourceName,
                textColor = HubZinc200,
                background = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.weight(1f, fill = false),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = manga.title,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = manga.author ?: sourceName,
                fontSize = 11.5.sp,
                color = HubZinc300,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val pillShape = CircleShape
                Row(
                    modifier = Modifier
                        .clip(pillShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), pillShape)
                        .clickable(onClick = onOpen)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Start Reading",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .clip(pillShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), pillShape)
                        .clickable(onClick = onBrowseSource)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(text = "Browse source", fontSize = 11.sp, color = HubZinc200)
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(pillShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(count) { i ->
                        val selected = i == index
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (selected) 16.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (selected) accent else HubZinc500)
                                .clickable { onSelectDot(i) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HubRecentCard(
    manga: Manga,
    sourceName: String,
    accent: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .width(236.dp)
            .clip(shape)
            .background(HubSurface)
            .border(1.dp, HubBorder, shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val coverShape = RoundedCornerShape(12.dp)
        AsyncImage(
            model = manga.asMangaCover(),
            contentDescription = manga.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 44.dp, height = 56.dp)
                .clip(coverShape)
                .border(1.dp, Color.White.copy(alpha = 0.1f), coverShape),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HubPill(
                    text = mangaKindLabel(manga),
                    textColor = if (manga.isNovel) HubNovelPill else accent,
                    background = if (manga.isNovel) HubNovelPill.copy(alpha = 0.15f) else HubZinc800,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = sourceName,
                    fontSize = 10.sp,
                    color = HubZinc400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = manga.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = manga.author ?: "Tap to open",
                fontSize = 11.sp,
                color = HubZinc400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun HubRecentEmpty(
    accent: Color,
    onCheck: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(shape)
            .background(HubSurface)
            .border(1.dp, HubBorder, shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "No recent updates", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HubZinc200)
                Text(text = "All chapters caught up", fontSize = 11.sp, color = HubZinc400)
            }
        }
        HubPillButton(label = "Check", accent = accent, onClick = onCheck)
    }
}

@Composable
private fun HubShelf(
    source: CatalogueSource,
    mangas: List<Manga>?,
    accent: Color,
    columns: Int,
    onOpen: (Manga) -> Unit,
    onSeeAll: () -> Unit,
) {
    // columns = the Library's Items per row. 0 (auto) keeps the default 116dp card.
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val shelfGap = 14.dp
    val cardWidth: androidx.compose.ui.unit.Dp = if (columns <= 0) {
        116.dp
    } else {
        ((screenWidth - 40.dp - shelfGap * (columns - 1)) / columns).coerceAtLeast(72.dp)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = source.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    HubPill(
                        text = sourceKindLabel(source),
                        textColor = accent,
                        background = accent.copy(alpha = 0.15f),
                    )
                }
                Text(text = source.langLabel(), fontSize = 12.sp, color = HubZinc400, maxLines = 1)
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSeeAll)
                    .semantics { contentDescription = "See all from ${source.name}" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = HubZinc400,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        when {
            mangas == null -> {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(shelfGap),
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(cardWidth)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(HubSurface),
                        )
                    }
                }
            }
            mangas.isEmpty() -> {
                Text(
                    text = "Nothing to show from this source yet",
                    fontSize = 12.sp,
                    color = HubZinc400,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            else -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(shelfGap),
                ) {
                    itemsIndexed(mangas.take(20)) { index, manga ->
                        HubShelfCard(
                            manga = manga,
                            rank = index + 1,
                            cardWidth = cardWidth,
                            onClick = { onOpen(manga) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HubShelfCard(
    manga: Manga,
    rank: Int,
    cardWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(shape)
                .background(HubSurface)
                .border(1.dp, HubBorder, shape),
        ) {
            AsyncImage(
                model = manga.asMangaCover(),
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))),
                    ),
            )
            HubPill(
                text = "#$rank",
                textColor = Color.White,
                background = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            )
            HubPill(
                text = mangaKindLabel(manga),
                textColor = HubOnAccent,
                background = if (manga.isNovel) HubNovelPill else HubZinc200,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = manga.title,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFF4F4F6),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = manga.author.orEmpty(),
            fontSize = 10.5.sp,
            color = HubZinc400,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HubNoSources(
    message: String,
    accent: Color,
    onBrowse: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, fontSize = 13.sp, color = HubZinc300)
        Spacer(modifier = Modifier.height(12.dp))
        HubPillButton(label = "Browse extensions", accent = accent, onClick = onBrowse)
    }
}
