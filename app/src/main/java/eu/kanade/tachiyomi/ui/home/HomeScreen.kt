package eu.kanade.tachiyomi.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.library.NovelsTab
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.NavigationBar
import tachiyomi.presentation.core.components.material.NavigationRail
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object HomeScreen : Screen() {

    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()
    private val showBottomNavEvent = Channel<Boolean>()

    @Suppress("ConstPropertyName")
    private const val TabFadeDuration = 200

    @Suppress("ConstPropertyName")
    private const val TabNavigatorKey = "HomeTabs"

    private val TABS = listOf(
        eu.kanade.tachiyomi.ui.discover.DiscoverTab,
        NovelsTab,
        LibraryTab,
        HistoryTab,
        BrowseTab,
        MoreTab,
    )

    private val JOINED_TABS = listOf(
        eu.kanade.tachiyomi.ui.discover.DiscoverTab,
        NovelsTab,
        HistoryTab,
        BrowseTab,
        MoreTab,
    )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val libraryPreferences = remember { Injekt.get<tachiyomi.domain.library.service.LibraryPreferences>() }
        val basePreferences = remember { Injekt.get<BasePreferences>() }
        val isJoined by libraryPreferences.joinedLibrary.collectAsState()
        val hideMangaUi by basePreferences.hideMangaUi.collectAsState()
        val uiPreferences = remember { Injekt.get<eu.kanade.domain.ui.UiPreferences>() }
        val navTabOrder by uiPreferences.navTabOrder.collectAsState()
        val navHiddenTabs by uiPreferences.navHiddenTabs.collectAsState()
        val baseTabs = if (isJoined || hideMangaUi) JOINED_TABS else TABS
        val orderedKeys = NavTabKeys.ordered(baseTabs.map { navTabKey(it) }, navTabOrder)
        val tabs = orderedKeys
            .filterNot { it in navHiddenTabs && it != NavTabKeys.MORE }
            .mapNotNull { key -> baseTabs.firstOrNull { navTabKey(it) == key } }
        TabNavigator(
            tab = eu.kanade.tachiyomi.ui.discover.DiscoverTab,
            key = TabNavigatorKey,
        ) { tabNavigator ->
            // Provide usable navigator to content screen
            CompositionLocalProvider(LocalNavigator provides navigator) {
                Scaffold(
                    startBar = {
                        if (isTabletUi()) {
                            NavigationRail {
                                tabs.fastForEach {
                                    NavigationRailItem(it)
                                }
                            }
                        }
                    },
                    bottomBar = {
                        if (!isTabletUi()) {
                            val bottomNavVisible by produceState(initialValue = true) {
                                showBottomNavEvent.receiveAsFlow().collectLatest { value = it }
                            }
                            AnimatedVisibility(
                                visible = bottomNavVisible,
                                enter = expandVertically(),
                                exit = shrinkVertically(),
                            ) {
                                PillNavigationBar(tabs)
                            }
                        }
                    },
                    contentWindowInsets = WindowInsets(0),
                ) { contentPadding ->
                    Box(
                        modifier = Modifier
                            .padding(contentPadding)
                            .consumeWindowInsets(contentPadding),
                    ) {
                        AnimatedContent(
                            targetState = tabNavigator.current,
                            transitionSpec = {
                                materialFadeThroughIn(initialScale = 1f, durationMillis = TabFadeDuration) togetherWith
                                    materialFadeThroughOut(durationMillis = TabFadeDuration)
                            },
                            label = "tabContent",
                        ) {
                            tabNavigator.saveableState(key = "currentTab", it) {
                                it.Content()
                            }
                        }
                    }
                }
            }

            val goToDiscoverTab = { tabNavigator.current = eu.kanade.tachiyomi.ui.discover.DiscoverTab }

            BackHandler(enabled = tabNavigator.current != eu.kanade.tachiyomi.ui.discover.DiscoverTab, onBack = goToDiscoverTab)

            LaunchedEffect(Unit) {
                launch {
                    librarySearchEvent.receiveAsFlow().collectLatest {
                        goToDiscoverTab()
                        NovelsTab.search(it)
                    }
                }
                launch {
                    openTabEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = when (it) {
                            is Tab.Library -> if (isJoined || hideMangaUi) NovelsTab else LibraryTab
                            Tab.Updates -> UpdatesTab
                            Tab.History -> HistoryTab
                            is Tab.Browse -> {
                                if (it.toExtensions) {
                                    BrowseTab.showExtension()
                                }
                                BrowseTab
                            }
                            is Tab.More -> MoreTab
                        }

                        if (it is Tab.Library && it.mangaIdToOpen != null) {
                            navigator.push(MangaScreen(it.mangaIdToOpen))
                        }
                        if (it is Tab.More && it.toDownloads) {
                            navigator.push(DownloadQueueScreen())
                        }
                    }
                }
            }
        }
    }

    // Floating pill bottom bar. Home (DiscoverTab) renders as a raised circular button with a
    // soft glow halo, centered among the other tabs (which are split evenly left/right of it)
    // rather than wherever Home happens to fall in the tabs list order. Selected regular tabs
    // show a small pink-to-blue gradient underline instead of the default filled indicator pill.
    @Composable
    private fun PillNavigationBar(tabs: List<eu.kanade.presentation.util.Tab>) {
        val navBarStyle by remember { Injekt.get<eu.kanade.domain.ui.UiPreferences>() }.navBarStyle.collectAsState()
        val navPrefs = remember { Injekt.get<eu.kanade.domain.ui.UiPreferences>() }
        val translucentNav by navPrefs.translucentNav.collectAsState()
        val navBarMargin by navPrefs.navBarMargin.collectAsState()
        // All tabs render inline in list order. Home (DiscoverTab) is first in TABS, so it
        // sits leftmost and behaves exactly like the other tabs - no raised/floating button.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = navBarMargin.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (translucentNav) 0.3f else 1f))
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.fastForEach { PillTabItem(it, dynamicPill = navBarStyle == eu.kanade.domain.ui.model.NavBarStyle.DYNAMIC_PILL) }
        }
    }

    @Composable
    private fun PillTabItem(tab: eu.kanade.presentation.util.Tab, dynamicPill: Boolean = false) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        val scale = remember { androidx.compose.animation.core.Animatable(1f) }
        val animationMs by remember { Injekt.get<eu.kanade.domain.ui.UiPreferences>() }.cardAnimationMs.collectAsState()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                ) {
                    scope.launch {
                        scale.animateTo(0.78f, animationSpec = androidx.compose.animation.core.tween((animationMs * 0.4f).toInt()))
                        scale.animateTo(
                            1f,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                            ),
                        )
                    }
                    if (!selected) {
                        tabNavigator.current = tab
                    } else {
                        scope.launch { tab.onReselect(navigator) }
                    }
                }
                .padding(8.dp),
        ) {
            if (dynamicPill) {
                androidx.compose.animation.AnimatedContent(
                    targetState = selected,
                    label = "dynamicPill",
                ) { isSelected ->
                    if (isSelected) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            CompositionLocalProvider(
                                androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onPrimaryContainer,
                            ) {
                                NavigationIconItem(tab)
                            }
                            Text(
                                text = navLabelFor(tab),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    } else {
                        Box(modifier = Modifier.padding(8.dp)) {
                            NavigationIconItem(tab)
                        }
                    }
                }
            } else {
                NavigationIconItem(tab)
                Text(
                    text = navLabelFor(tab),
                    fontSize = 10.sp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(width = 20.dp, height = 3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (selected) {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        androidx.compose.ui.graphics.Color(0xFFE94584),
                                        androidx.compose.ui.graphics.Color(0xFF3EC6F0),
                                    ),
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        androidx.compose.ui.graphics.Color.Transparent,
                                    ),
                                )
                            },
                        ),
                )
            }
        }
    }

    @Composable
    private fun RowScope.NavigationBarItem(tab: eu.kanade.presentation.util.Tab) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        NavigationBarItem(
            selected = selected,
            onClick = {
                if (!selected) {
                    tabNavigator.current = tab
                } else {
                    scope.launch { tab.onReselect(navigator) }
                }
            },
            icon = { NavigationIconItem(tab) },
            label = {
                Text(
                    text = tab.options.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            alwaysShowLabel = true,
        )
    }

    @Composable
    fun NavigationRailItem(tab: eu.kanade.presentation.util.Tab) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        NavigationRailItem(
            selected = selected,
            onClick = {
                if (!selected) {
                    tabNavigator.current = tab
                } else {
                    scope.launch { tab.onReselect(navigator) }
                }
            },
            icon = { NavigationIconItem(tab) },
            label = {
                Text(
                    text = tab.options.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            alwaysShowLabel = true,
        )
    }

    @Composable
    private fun NavigationIconItem(tab: eu.kanade.presentation.util.Tab) {
        BadgedBox(
            badge = {
                when {
                    tab is UpdatesTab -> {
                        val count by produceState(initialValue = 0) {
                            val pref = Injekt.get<LibraryPreferences>()
                            combine(
                                pref.newShowUpdatesCount.changes(),
                                pref.newUpdatesCount.changes(),
                            ) { show, count -> if (show) count else 0 }
                                .collectLatest { value = it }
                        }
                        if (count > 0) {
                            Badge {
                                val desc = pluralStringResource(
                                    MR.plurals.notification_chapters_generic,
                                    count = count,
                                    count,
                                )
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.semantics { contentDescription = desc },
                                )
                            }
                        }
                    }
                    BrowseTab::class.isInstance(tab) -> {
                        val count by produceState(initialValue = 0) {
                            Injekt.get<SourcePreferences>().extensionUpdatesCount.changes()
                                .collectLatest { value = it }
                        }
                        if (count > 0) {
                            Badge {
                                val desc = pluralStringResource(
                                    MR.plurals.update_check_notification_ext_updates,
                                    count = count,
                                    count,
                                )
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.semantics { contentDescription = desc },
                                )
                            }
                        }
                    }
                }
            },
        ) {
            val customIcon = customNavIconFor(tab)
            if (customIcon != null) {
                Icon(
                    imageVector = customIcon,
                    contentDescription = tab.options.title,
                )
            } else {
                Icon(
                    painter = tab.options.icon!!,
                    contentDescription = tab.options.title,
                )
            }
        }
    }

    private fun navTabKey(tab: eu.kanade.presentation.util.Tab): String = when {
        tab::class == eu.kanade.tachiyomi.ui.discover.DiscoverTab::class -> NavTabKeys.HOME
        tab is NovelsTab -> NavTabKeys.NOVELS
        tab is LibraryTab -> NavTabKeys.LIBRARY
        tab is HistoryTab -> NavTabKeys.HISTORY
        tab is MoreTab -> NavTabKeys.MORE
        BrowseTab::class.isInstance(tab) -> NavTabKeys.BROWSE
        else -> tab::class.simpleName.orEmpty()
    }

    @Composable
    private fun navLabelFor(tab: eu.kanade.presentation.util.Tab): String = when {
        tab::class == eu.kanade.tachiyomi.ui.discover.DiscoverTab::class -> "Home"
        tab is LibraryTab || tab is NovelsTab -> "Library"
        tab is HistoryTab -> "History"
        BrowseTab::class.isInstance(tab) -> "Browse"
        tab is MoreTab -> "Settings"
        else -> tab.options.title
    }

    private fun customNavIconFor(tab: eu.kanade.presentation.util.Tab): ImageVector? = when {
        tab::class == eu.kanade.tachiyomi.ui.discover.DiscoverTab::class -> SparkleNavIcon
        tab is LibraryTab || tab is NovelsTab -> LibraryNavIcon
        tab is HistoryTab -> UpdatesNavIcon
        BrowseTab::class.isInstance(tab) -> BrowseSearchNavIcon
        tab is MoreTab -> SettingsNavIcon
        else -> null
    }

    suspend fun search(query: String) {
        librarySearchEvent.send(query)
    }

    suspend fun openTab(tab: Tab) {
        openTabEvent.send(tab)
    }

    suspend fun showBottomNav(show: Boolean) {
        showBottomNavEvent.send(show)
    }

    sealed interface Tab {
        data class Library(val mangaIdToOpen: Long? = null) : Tab
        data object Updates : Tab
        data object History : Tab
        data class Browse(val toExtensions: Boolean = false) : Tab
        data class More(val toDownloads: Boolean) : Tab
    }
}
