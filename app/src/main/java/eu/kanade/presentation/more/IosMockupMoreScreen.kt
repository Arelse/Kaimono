package eu.kanade.presentation.more

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import eu.kanade.domain.ui.UiPreferences
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.domain.ui.model.UiMode
import eu.kanade.tachiyomi.ui.home.NavTabKeys
import eu.kanade.tachiyomi.ui.more.DownloadQueueState

// Colors taken from the two reference mockups (Tailwind classes translated to hex/alpha).
private val IosBlue = Color(0xFF0A84FF)
private val IosCardBg = Color(0xFF1C1C1E).copy(alpha = 0.65f)
private val IosCardBorder = Color.White.copy(alpha = 0.08f)
private val IosDivider = Color.White.copy(alpha = 0.06f)
private val IosIconBoxBg = Color.White.copy(alpha = 0.06f)
private val IosNeutral300 = Color(0xFFD4D4D4)
private val IosNeutral400 = Color(0xFFA3A3A3)
private val IosNeutral500 = Color(0xFF737373)
private val IosSwitchOff = Color(0xFF787880).copy(alpha = 0.32f)

/** One item in the iOS-style bottom bar. */
data class IosTabItem(
    val key: String,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/**
 * Bottom navigation used by the Default and Vision Glass modes. Matches the mockups'
 * `<nav>`: 80dp tall, 80% black, 1dp top hairline, evenly spaced items, selected item shown
 * with a blue pill behind its icon.
 */
@Composable
fun IosMockupTabBar(
    items: List<IosTabItem>,
    uiMode: UiMode,
    modifier: Modifier = Modifier,
) {
    val vision = uiMode == UiMode.ALTERNATIVE
    // Follows the active theme's surface color; pure black only when Pure AMOLED dark mode is on.
    val uiPreferences = remember { Injekt.get<UiPreferences>() }
    val pureBlack by uiPreferences.themeDarkAmoled.collectAsState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh)
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.10f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            IosTabButton(item = item, vision = vision)
        }
    }
}

@Composable
private fun IosTabButton(item: IosTabItem, vision: Boolean) {
    val tint = if (item.selected) IosBlue else IosNeutral500
    val icon = iosTabIcon(item.key, vision)
    val iconHeight = if (vision) 20.dp else 18.dp
    Column(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = item.onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (item.selected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(IosBlue.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
            ) {
                IosIcon(icon = icon, height = iconHeight, tint = tint)
            }
        } else {
            IosIcon(icon = icon, height = iconHeight, tint = tint)
        }
        Text(
            text = item.label,
            fontSize = 10.sp,
            fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
            color = tint,
        )
    }
}

private fun iosTabIcon(key: String, vision: Boolean): ImageVector = when (key) {
    NavTabKeys.HOME -> if (vision) IosMockupIcons.heroHome else IosMockupIcons.faCompass
    NavTabKeys.NOVELS, NavTabKeys.LIBRARY -> if (vision) IosMockupIcons.heroBookOpen else IosMockupIcons.faBookBookmark
    NavTabKeys.HISTORY -> if (vision) IosMockupIcons.heroClock else IosMockupIcons.faClockRotateLeft
    NavTabKeys.BROWSE -> if (vision) IosMockupIcons.heroSearch else IosMockupIcons.faGlobe
    NavTabKeys.MORE -> if (vision) IosMockupIcons.heroDots else IosMockupIcons.faEllipsis
    else -> if (vision) IosMockupIcons.heroHome else IosMockupIcons.faCompass
}

/**
 * The More screen as drawn in the two reference mockups. [uiMode] must be DEFAULT (first
 * mockup, "VisionOS Reader") or ALTERNATIVE (second mockup, "Refined").
 */
@Composable
fun IosMockupMoreScreen(
    uiMode: UiMode,
    downloadQueueState: DownloadQueueState,
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    onClickDownloadQueue: () -> Unit,
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    onClickDataAndStorage: () -> Unit,
    onClickUiSwitcher: () -> Unit,
    onClickSettings: () -> Unit,
    onClickAbout: () -> Unit,
    onClickHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vision = uiMode == UiMode.ALTERNATIVE
    val queueCount = when (downloadQueueState) {
        DownloadQueueState.Stopped -> 0
        is DownloadQueueState.Paused -> downloadQueueState.pending
        is DownloadQueueState.Downloading -> downloadQueueState.pending
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState()),
    ) {
        IosHeroIcon(vision = vision)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            IosSection(title = "Mode") {
                IosCard {
                    IosSwitchRow(
                        vision = vision,
                        icon = if (vision) IosMockupIcons.heroCloudDown else IosMockupIcons.faCloudArrowDown,
                        title = "Downloaded only",
                        subtitle = "Filters all entries in your library",
                        checked = downloadedOnly,
                        onCheckedChange = onDownloadedOnlyChange,
                    )
                    IosDividerLine()
                    IosSwitchRow(
                        vision = vision,
                        icon = if (vision) IosMockupIcons.heroEye else IosMockupIcons.faGlasses,
                        title = "Incognito mode",
                        subtitle = "Pauses reading history",
                        checked = incognitoMode,
                        onCheckedChange = onIncognitoModeChange,
                    )
                }
            }

            IosSection(title = "Content") {
                IosCard {
                    IosRow(
                        vision = vision,
                        icon = if (vision) IosMockupIcons.heroTrayDown else IosMockupIcons.faDownload,
                        title = "Download queue",
                        onClick = onClickDownloadQueue,
                        trailing = {
                            IosBadge(count = queueCount)
                            IosChevron(vision = vision)
                        },
                    )
                    IosDividerLine()
                    IosRow(
                        vision = vision,
                        icon = if (vision) IosMockupIcons.heroFolder else IosMockupIcons.faTags,
                        title = "Categories",
                        onClick = onClickCategories,
                    )
                    IosDividerLine()
                    IosRow(
                        vision = vision,
                        icon = if (vision) IosMockupIcons.heroChart else IosMockupIcons.faChartSimple,
                        title = "Statistics",
                        onClick = onClickStats,
                    )
                    IosDividerLine()
                    IosRow(
                        vision = vision,
                        icon = if (vision) IosMockupIcons.heroDatabase else IosMockupIcons.faDatabase,
                        title = "Data and storage",
                        onClick = onClickDataAndStorage,
                    )
                }
            }

            IosSection(title = "App") {
                if (vision) {
                    IosCard {
                        Column(modifier = Modifier.padding(8.dp)) {
                            IosAppRow(vision = true, icon = IosMockupIcons.heroGear, title = "Settings", onClick = onClickSettings)
                            IosDividerLine()
                            IosAppRow(vision = true, icon = IosMockupIcons.heroInfo, title = "About", onClick = onClickAbout)
                            IosDividerLine()
                            IosAppRow(vision = true, icon = IosMockupIcons.heroQuestion, title = "Help", onClick = onClickHelp)
                            IosDividerLine()
                            IosAppRow(vision = true, icon = IosMockupIcons.faPalette, title = "UI Switcher", onClick = onClickUiSwitcher)
                        }
                    }
                } else {
                    IosCard {
                        Column {
                            IosAppRow(vision = false, icon = IosMockupIcons.faGear, title = "Settings", onClick = onClickSettings)
                            IosDividerLine()
                            IosAppRow(vision = false, icon = IosMockupIcons.faCircleInfo, title = "About", onClick = onClickAbout)
                            IosDividerLine()
                            IosAppRow(vision = false, icon = IosMockupIcons.faCircleQuestion, title = "Help", onClick = onClickHelp)
                            IosDividerLine()
                            IosAppRow(vision = false, icon = IosMockupIcons.faPalette, title = "UI Switcher", onClick = onClickUiSwitcher)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IosHeroIcon(vision: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1C1C1E))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (vision) {
                IosIcon(icon = IosMockupIcons.heroBookOpenLarge, height = 32.dp, tint = Color.White)
            } else {
                IosIcon(icon = IosMockupIcons.faFeatherPointed, height = 24.dp, tint = Color.White)
            }
        }
    }
}

@Composable
private fun IosSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = IosNeutral400,
            letterSpacing = 0.3.sp,
            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
        )
        content()
    }
}

@Composable
private fun IosCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(IosCardBg)
            .border(1.dp, IosCardBorder, RoundedCornerShape(16.dp)),
        content = content,
    )
}

@Composable
private fun IosDividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(IosDivider),
    )
}

@Composable
private fun IosIcon(icon: ImageVector, height: Dp, tint: Color, modifier: Modifier = Modifier) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier.size(width = height * (icon.defaultWidth / icon.defaultHeight), height = height),
        tint = tint,
    )
}

@Composable
private fun IosChevron(vision: Boolean) {
    if (vision) {
        IosIcon(icon = IosMockupIcons.heroChevronRight, height = 14.dp, tint = IosNeutral500)
    } else {
        IosIcon(icon = IosMockupIcons.faChevronRight, height = 11.dp, tint = IosNeutral500)
    }
}

@Composable
private fun IosBadge(count: Int) {
    Text(
        text = count.toString(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White.copy(alpha = 0.8f),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun IosRow(
    vision: Boolean,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = { IosChevron(vision = vision) },
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IosIconBoxBg),
                contentAlignment = Alignment.Center,
            ) {
                IosIcon(
                    icon = icon,
                    height = if (vision) 16.dp else 14.dp,
                    tint = if (vision) Color.White else Color.White.copy(alpha = 0.9f),
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = IosNeutral400,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            trailing()
        }
    }
}

@Composable
private fun IosSwitchRow(
    vision: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    IosRow(
        vision = vision,
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = { onCheckedChange(!checked) },
        trailing = { IosSwitch(checked = checked, onCheckedChange = onCheckedChange) },
    )
}

@Composable
private fun IosAppRow(vision: Boolean, icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = if (vision) 14.dp else 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (vision) 14.dp else 12.dp),
        ) {
            IosIcon(
                icon = icon,
                height = if (vision) 16.dp else 14.dp,
                tint = if (vision) IosNeutral300 else IosNeutral400,
            )
            Text(
                text = title,
                fontSize = if (vision) 15.sp else 14.sp,
                fontWeight = if (vision) FontWeight.Medium else FontWeight.Normal,
                color = Color.White,
            )
        }
        IosChevron(vision = vision)
    }
}

@Composable
private fun IosSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = tween(300),
        label = "iosSwitchThumb",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) IosBlue else IosSwitchOff,
        animationSpec = tween(300),
        label = "iosSwitchTrack",
    )
    Box(
        modifier = Modifier
            .size(width = 51.dp, height = 31.dp)
            .then(
                if (checked) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = IosBlue.copy(alpha = 0.35f),
                        spotColor = IosBlue.copy(alpha = 0.35f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .offset(x = 2.dp + thumbOffset, y = 2.dp)
                .size(27.dp)
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}
