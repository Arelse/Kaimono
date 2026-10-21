package eu.kanade.presentation.reader.appbars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.atsuGlass
import eu.kanade.presentation.components.flvetsuGlass
import eu.kanade.presentation.components.translucentReaderGlass
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import kotlin.math.roundToInt

private val iosBarsSlideSpec = tween<IntOffset>(200)
private val iosBarsFadeSpec = tween<Float>(150)

/** Glass material for each reader control theme. */
enum class ReaderGlassStyle { TRANSLUCENT, IOS26, ATSU }

/**
 * Glass surface for the reader chrome: ATSU reacts to [scrolling], IOS26 (Flvetsu) is the AnymeX
 * iOS 26 smoked glass, and TRANSLUCENT is a flat see-through tint.
 */
private fun Modifier.readerGlass(shape: Shape, tint: Color, style: ReaderGlassStyle, scrolling: Boolean): Modifier =
    when (style) {
        ReaderGlassStyle.ATSU -> atsuGlass(shape, tint, scrolling)
        ReaderGlassStyle.IOS26 -> flvetsuGlass(shape)
        ReaderGlassStyle.TRANSLUCENT -> translucentReaderGlass(shape, tint)
    }

/**
 * Translucent "iOS 26" skin for the reader's top/bottom chrome. Only used when
 * ReaderPreferences.controlTheme is not DEFAULT; the Default path (ReaderAppBars) is untouched.
 * Only supports the common horizontal chapter-navigator layout — for the vertical left/right
 * slider configuration (a rarely used setting), the caller should fall back to ReaderAppBars.
 */
@Composable
fun IosReaderAppBars(
    visible: Boolean,

    mangaTitle: String?,
    chapterTitle: String?,
    navigateUp: () -> Unit,
    onClickTopAppBar: () -> Unit,
    onClickChapterList: () -> Unit,
    bookmarked: Boolean,
    onToggleBookmarked: () -> Unit,

    currentPage: Int,
    totalPages: Int,
    onPageIndexChange: (Int) -> Unit,
    onPageIndexChangeFinished: () -> Unit,

    readingMode: ReadingMode,
    onClickReadingMode: () -> Unit,
    orientation: ReaderOrientation,
    onClickOrientation: () -> Unit,
    cropEnabled: Boolean,
    onClickCropBorder: () -> Unit,
    onClickSettings: () -> Unit,
    style: ReaderGlassStyle = ReaderGlassStyle.IOS26,
    scrolling: Boolean = false,
) {
    // Glass surfaces: tint only, the glass modifier adds the sheen and rim.
    val pillColor = MaterialTheme.colorScheme.surfaceContainerHigh
    // Flvetsu uses pure white text and icons on the smoked glass, like AnymeX's iOS 26 player.
    val onPillColor = if (style == ReaderGlassStyle.IOS26) Color.White else MaterialTheme.colorScheme.onSurface

    Box(modifier = Modifier.fillMaxHeight()) {
        // Top bar: back circle, title/subtitle pill, bookmark + settings circles.
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopCenter),
            visible = visible,
            enter = slideInVertically(iosBarsSlideSpec) { -it } + fadeIn(iosBarsFadeSpec),
            exit = slideOutVertically(iosBarsSlideSpec) { -it } + fadeOut(iosBarsFadeSpec),
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IosCircleIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    pillColor = pillColor,
                    style = style,
                    scrolling = scrolling,
                    onClick = navigateUp,
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .readerGlass(RoundedCornerShape(24.dp), pillColor, style, scrolling)
                        .clickable(onClick = onClickChapterList)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mangaTitle.orEmpty(),
                            color = onPillColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (chapterTitle != null) {
                            Text(
                                text = chapterTitle,
                                color = onPillColor.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = onPillColor.copy(alpha = 0.7f),
                    )
                }

                IosCircleIconButton(
                    icon = if (bookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    pillColor = pillColor,
                    style = style,
                    scrolling = scrolling,
                    onClick = onToggleBookmarked,
                )

                IosCircleIconButton(
                    icon = Icons.Outlined.Settings,
                    contentDescription = null,
                    pillColor = pillColor,
                    style = style,
                    scrolling = scrolling,
                    onClick = onClickSettings,
                )
            }
        }

        // Bottom: page slider pill with prev/next circles, then a translucent toolbar row.
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = visible,
            enter = slideInVertically(iosBarsSlideSpec) { it } + fadeIn(iosBarsFadeSpec),
            exit = slideOutVertically(iosBarsSlideSpec) { it } + fadeOut(iosBarsFadeSpec),
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IosCircleIconButton(
                        icon = Icons.Outlined.SkipPrevious,
                        contentDescription = null,
                        pillColor = pillColor,
                        enabled = currentPage > 0,
                        style = style,
                        scrolling = scrolling,
                        onClick = { onPageIndexChange((currentPage - 1).coerceAtLeast(0)); onPageIndexChangeFinished() },
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .readerGlass(RoundedCornerShape(24.dp), pillColor, style, scrolling)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(text = (currentPage + 1).toString(), color = onPillColor.copy(alpha = 0.7f))
                        var sliderPosition by remember(currentPage) {
                            mutableFloatStateOf(currentPage.toFloat())
                        }
                        Slider(
                            modifier = Modifier.weight(1f),
                            value = sliderPosition,
                            valueRange = 0f..(totalPages - 1).coerceAtLeast(0).toFloat(),
                            onValueChange = {
                                sliderPosition = it
                                onPageIndexChange(it.roundToInt())
                            },
                            onValueChangeFinished = onPageIndexChangeFinished,
                            colors = SliderDefaults.colors(
                                thumbColor = if (style == ReaderGlassStyle.IOS26) Color.White else MaterialTheme.colorScheme.primary,
                                activeTrackColor = if (style == ReaderGlassStyle.IOS26) Color.White else MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = onPillColor.copy(alpha = 0.2f),
                            ),
                        )
                        Text(text = totalPages.toString(), color = onPillColor.copy(alpha = 0.7f))
                    }

                    IosCircleIconButton(
                        icon = Icons.Outlined.SkipNext,
                        contentDescription = null,
                        pillColor = pillColor,
                        enabled = currentPage < totalPages - 1,
                        style = style,
                        scrolling = scrolling,
                        onClick = { onPageIndexChange((currentPage + 1).coerceAtMost(totalPages - 1)); onPageIndexChangeFinished() },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .readerGlass(RoundedCornerShape(24.dp), pillColor, style, scrolling)
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClickReadingMode) {
                        Icon(
                            painter = painterResource(readingMode.iconRes),
                            contentDescription = null,
                            tint = onPillColor,
                        )
                    }
                    IconButton(onClick = onClickOrientation) {
                        Icon(imageVector = orientation.icon, contentDescription = null, tint = onPillColor)
                    }
                    IconButton(onClick = onClickCropBorder) {
                        Icon(
                            painter = painterResource(
                                if (cropEnabled) R.drawable.ic_crop_24dp else R.drawable.ic_crop_off_24dp,
                            ),
                            contentDescription = null,
                            tint = onPillColor,
                        )
                    }
                    IconButton(onClick = onClickSettings) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = null, tint = onPillColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun IosCircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    pillColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    style: ReaderGlassStyle = ReaderGlassStyle.IOS26,
    scrolling: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .readerGlass(CircleShape, pillColor, style, scrolling)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (style == ReaderGlassStyle.IOS26) {
                Color.White.copy(alpha = if (enabled) 0.95f else 0.35f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.3f)
            },
        )
    }
}

/**
 * Floating vertical pill on the right edge. The middle button starts/stops Auto Scroll; + and −
 * raise/lower its speed (Auto Scroll Speed), so every control on this pill is wired to a real,
 * working feature. Shown independent of menu visibility, matching the reference design.
 */
@Composable
fun IosZoomControlPill(
    autoScrollActive: Boolean,
    onToggleAutoScroll: () -> Unit,
    onSpeedIncrease: () -> Unit,
    onSpeedDecrease: () -> Unit,
    style: ReaderGlassStyle = ReaderGlassStyle.IOS26,
    scrolling: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val pillColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        modifier = modifier
            .widthIn(min = 52.dp)
            .readerGlass(RoundedCornerShape(26.dp), pillColor, style, scrolling)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onSpeedIncrease, modifier = Modifier.size(52.dp)) {
            Text(
                text = "+",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
            )
        }
        IconButton(onClick = onToggleAutoScroll) {
            Icon(
                imageVector = if (autoScrollActive) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onSpeedDecrease, modifier = Modifier.size(52.dp)) {
            Text(
                text = "−",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
            )
        }
    }
}
