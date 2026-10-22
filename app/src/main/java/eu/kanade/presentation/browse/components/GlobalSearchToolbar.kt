package eu.kanade.presentation.browse.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource


/**
 * Soft violet glow along the bottom edge of a pill, as in the mockup.
 * [height] and [overhang] are in px: the glow box sits [overhang] px below the bottom edge.
 * [shift] (-1..1) slides the glow sideways. [intensity] brightens it (used while searching).
 */
internal fun DrawScope.drawSearchBottomGlow(
    widthFraction: Float,
    height: Float,
    overhang: Float,
    shift: Float = 0f,
    intensity: Float = 1f,
    primary: Color,
) {
    val glowWidth = size.width * widthFraction
    val top = size.height + overhang - height
    val left = (size.width - glowWidth) / 2f + shift * 0.18f * size.width
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                glowLight(primary).copy(alpha = (0.95f * intensity).coerceAtMost(1f)),
                glowMid(primary).copy(alpha = (0.84f * intensity).coerceAtMost(1f)),
                glowDeep(primary).copy(alpha = (0.48f * intensity).coerceAtMost(1f)),
                Color.Transparent,
            ),
            center = Offset(left + glowWidth / 2f, top + height * 0.6f),
            radius = glowWidth / 2f,
        ),
        topLeft = Offset(left, top),
        size = Size(glowWidth, height),
    )
}

/**
 * One glow layer from the mockup's search pill, drawn like its CSS: an elliptical radial gradient
 * (origin 50% 60%), scaled X and Y separately, translated, and faded.
 * [drift] moves it by a fraction of its own width; [overhang] (px) is how far its box sits below
 * the bottom edge; [translateY] is a vertical nudge in px; [alpha] is the mockup's opacity keyframe.
 */
private fun DrawScope.drawSearchGlowLayer(
    widthFraction: Float,
    height: Float,
    overhang: Float,
    drift: Float,
    scaleX: Float,
    scaleY: Float,
    translateY: Float,
    alpha: Float,
    intensity: Float,
    primary: Color,
) {
    val boxWidth = size.width * widthFraction
    val boxTop = size.height + overhang - height
    // The box is scaled about its own centre, like a CSS transform.
    val centerX = size.width / 2f + drift * boxWidth
    val centerY = boxTop + height / 2f + translateY
    val radiusX = boxWidth / 2f * scaleX
    val radiusY = height / 2f * scaleY
    val a = alpha * intensity
    // Gradient radius reaches past the box edge, so the falloff matches the mockup's farthest-corner ellipse.
    val gradientRadius = radiusY * 1.25f
    withTransform({
        translate(left = centerX, top = centerY)
        scale(scaleX = radiusX / radiusY, scaleY = 1f, pivot = Offset.Zero)
    }) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glowLight(primary).copy(alpha = (0.95f * a).coerceAtMost(1f)),
                    glowMid(primary).copy(alpha = (0.84f * a).coerceAtMost(1f)),
                    glowDeep(primary).copy(alpha = (0.48f * a).coerceAtMost(1f)),
                    Color.Transparent,
                ),
                center = Offset(0f, radiusY * 0.2f),
                radius = gradientRadius,
            ),
            radius = gradientRadius,
            center = Offset.Zero,
        )
    }
}

/**
 * [glowStyle] is true when a non-default home style is picked or the gradient theme is on.
 * Otherwise the original toolbar is shown unchanged.
 */
@Composable
fun GlobalSearchToolbar(
    searchQuery: String?,
    progress: Int,
    total: Int,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onCancelSearch: () -> Unit = {},
    hideSourceFilter: Boolean,
    sourceFilter: SourceFilter,
    pinGroups: List<String> = emptyList(),
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onlyShowHasResults: Boolean,
    onToggleResults: () -> Unit,
    showFilters: Boolean = true,
    glowStyle: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    if (glowStyle) {
        GlowGlobalSearchToolbar(
            searchQuery = searchQuery,
            progress = progress,
            total = total,
            navigateUp = navigateUp,
            onChangeSearchQuery = onChangeSearchQuery,
            onSearch = onSearch,
            onCancelSearch = onCancelSearch,
            hideSourceFilter = hideSourceFilter,
            sourceFilter = sourceFilter,
            pinGroups = pinGroups,
            onChangeSearchFilter = onChangeSearchFilter,
            onlyShowHasResults = onlyShowHasResults,
            onToggleResults = onToggleResults,
            showFilters = showFilters,
        )
    } else {
        ClassicGlobalSearchToolbar(
            searchQuery = searchQuery,
            progress = progress,
            total = total,
            navigateUp = navigateUp,
            onChangeSearchQuery = onChangeSearchQuery,
            onSearch = onSearch,
            hideSourceFilter = hideSourceFilter,
            sourceFilter = sourceFilter,
            pinGroups = pinGroups,
            onChangeSearchFilter = onChangeSearchFilter,
            onlyShowHasResults = onlyShowHasResults,
            onToggleResults = onToggleResults,
            showFilters = showFilters,
            scrollBehavior = scrollBehavior,
        )
    }
}

@Composable
private fun ClassicGlobalSearchToolbar(
    searchQuery: String?,
    progress: Int,
    total: Int,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    hideSourceFilter: Boolean,
    sourceFilter: SourceFilter,
    pinGroups: List<String>,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onlyShowHasResults: Boolean,
    onToggleResults: () -> Unit,
    showFilters: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    var showGroupRow by remember { mutableStateOf(false) }

    val arrowRotation by animateFloatAsState(
        targetValue = if (showGroupRow) -90f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "ArrowRotation",
    )

    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        Box {
            SearchToolbar(
                searchQuery = searchQuery,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = onSearch,
                onClickCloseSearch = navigateUp,
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
            if (progress in 1..<total) {
                LinearProgressIndicator(
                    progress = { progress / total.toFloat() },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(),
                )
            }
        }

        if (showFilters) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.padding.small),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                if (!hideSourceFilter) {
                    val isPinnedOrGroup = sourceFilter == SourceFilter.PinnedOnly || sourceFilter is SourceFilter.Group
                    val pinnedLabel = when (sourceFilter) {
                        is SourceFilter.Group -> sourceFilter.name
                        else -> stringResource(MR.strings.pinned_sources)
                    }
                    val pinnedIcon = when (sourceFilter) {
                        is SourceFilter.Group -> Icons.AutoMirrored.Outlined.Label
                        else -> Icons.Outlined.PushPin
                    }

                    FilterChip(
                        selected = isPinnedOrGroup,
                        onClick = {
                            if (!isPinnedOrGroup) {
                                onChangeSearchFilter(SourceFilter.PinnedOnly)
                            } else if (pinGroups.isNotEmpty()) {
                                showGroupRow = !showGroupRow
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = pinnedIcon,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        },
                        trailingIcon = if (pinGroups.isNotEmpty()) {
                            {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(FilterChipDefaults.IconSize)
                                        .rotate(arrowRotation),
                                )
                            }
                        } else {
                            null
                        },
                        label = {
                            Text(text = pinnedLabel)
                        },
                    )
                    FilterChip(
                        selected = sourceFilter == SourceFilter.All,
                        onClick = {
                            onChangeSearchFilter(SourceFilter.All)
                            showGroupRow = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        },
                        label = {
                            Text(text = stringResource(MR.strings.all))
                        },
                    )

                    VerticalDivider()
                }

                FilterChip(
                    selected = onlyShowHasResults,
                    onClick = { onToggleResults() },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    },
                    label = {
                        Text(text = stringResource(MR.strings.has_results))
                    },
                )
            }

            AnimatedVisibility(visible = showGroupRow && pinGroups.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = MaterialTheme.padding.small, vertical = MaterialTheme.padding.extraSmall),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                ) {
                    pinGroups.forEach { group ->
                        val isGroupSelected = sourceFilter == SourceFilter.Group(group)
                        FilterChip(
                            selected = isGroupSelected,
                            onClick = {
                                if (isGroupSelected) {
                                    onChangeSearchFilter(SourceFilter.PinnedOnly)
                                } else {
                                    onChangeSearchFilter(SourceFilter.Group(group))
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Label,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            },
                            label = {
                                Text(text = group)
                            },
                        )
                    }
                }
            }
        }

        HorizontalDivider()
    }
}

@Composable
private fun GlowGlobalSearchToolbar(
    searchQuery: String?,
    progress: Int,
    total: Int,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onCancelSearch: () -> Unit,
    hideSourceFilter: Boolean,
    sourceFilter: SourceFilter,
    pinGroups: List<String>,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onlyShowHasResults: Boolean,
    onToggleResults: () -> Unit,
    showFilters: Boolean,
) {
    var showGroupRow by remember { mutableStateOf(false) }

    val arrowRotation by animateFloatAsState(
        targetValue = if (showGroupRow) -90f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "ArrowRotation",
    )

    val query = searchQuery.orEmpty()
    val isSearching = total > 0 && progress < total

    Column(modifier = Modifier.fillMaxWidth().background(searchHeaderBackground()).statusBarsPadding()) {
        Column(
            modifier = Modifier.padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 12.dp),
        ) {
            SearchGlowPill(
                query = query,
                isSearching = isSearching,
                onQueryChange = onChangeSearchQuery,
                onBack = {
                    if (query.isNotEmpty()) onChangeSearchQuery("") else navigateUp()
                },
                onClear = { onChangeSearchQuery("") },
                onSubmit = { if (query.isNotBlank()) onSearch(query) },
                onAction = {
                    if (isSearching) {
                        onCancelSearch()
                    } else if (query.isNotBlank()) {
                        onSearch(query)
                    }
                },
            )

            if (showFilters) {
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (!hideSourceFilter) {
                        val isPinnedOrGroup = sourceFilter == SourceFilter.PinnedOnly || sourceFilter is SourceFilter.Group
                        val pinnedLabel = when (sourceFilter) {
                            is SourceFilter.Group -> sourceFilter.name
                            else -> stringResource(MR.strings.pinned_sources)
                        }
                        val pinnedIcon: ImageVector = when (sourceFilter) {
                            is SourceFilter.Group -> Icons.AutoMirrored.Outlined.Label
                            else -> Icons.Outlined.PushPin
                        }

                        SearchFilterChip(
                            label = pinnedLabel,
                            icon = pinnedIcon,
                            selected = isPinnedOrGroup,
                            trailingRotation = if (pinGroups.isNotEmpty()) arrowRotation else null,
                            onClick = {
                                if (!isPinnedOrGroup) {
                                    onChangeSearchFilter(SourceFilter.PinnedOnly)
                                } else if (pinGroups.isNotEmpty()) {
                                    showGroupRow = !showGroupRow
                                }
                            },
                        )
                        SearchFilterChip(
                            label = stringResource(MR.strings.all),
                            icon = Icons.Outlined.DoneAll,
                            selected = sourceFilter == SourceFilter.All,
                            onClick = {
                                onChangeSearchFilter(SourceFilter.All)
                                showGroupRow = false
                            },
                        )
                    }

                    SearchFilterChip(
                        label = stringResource(MR.strings.has_results),
                        icon = Icons.Outlined.FilterList,
                        selected = onlyShowHasResults,
                        onClick = onToggleResults,
                    )
                }

                AnimatedVisibility(visible = showGroupRow && pinGroups.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        pinGroups.forEach { group ->
                            val isGroupSelected = sourceFilter == SourceFilter.Group(group)
                            SearchFilterChip(
                                label = group,
                                icon = Icons.AutoMirrored.Outlined.Label,
                                selected = isGroupSelected,
                                onClick = {
                                    if (isGroupSelected) {
                                        onChangeSearchFilter(SourceFilter.PinnedOnly)
                                    } else {
                                        onChangeSearchFilter(SourceFilter.Group(group))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        if (isSearching) {
            LinearProgressIndicator(
                progress = { progress / total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HorizontalDivider(color = searchDivider())
    }
}

@Composable
private fun SearchGlowPill(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String?) -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    onAction: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (query.isEmpty()) focusRequester.requestFocus()
    }

    // Glow loops follow the mockup's CSS keyframes: a full cycle of 5.2s and 6.8s (2.4s and 3.1s
    // while searching), eased in and out. Reverse repeat runs each half in half the cycle.
    val glowEase = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    val transition = rememberInfiniteTransition(label = "searchGlow")
    val primaryPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSearching) 1200 else 2600, easing = glowEase),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPrimary",
    )
    val secondaryPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSearching) 1550 else 3400, easing = glowEase),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowSecondary",
    )
    // The glow boxes grow a little while searching, over 0.3s like the mockup's transition.
    val primaryHeight by animateDpAsState(
        targetValue = if (isSearching) 58.dp else 46.dp,
        animationSpec = tween(durationMillis = 300),
        label = "glowPrimaryHeight",
    )
    val secondaryHeight by animateDpAsState(
        targetValue = if (isSearching) 52.dp else 42.dp,
        animationSpec = tween(durationMillis = 300),
        label = "glowSecondaryHeight",
    )

    val pillShape = RoundedCornerShape(50)
    val intensity = if (isSearching) 1.25f else 1f
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(pillShape)
            .background(searchPillFill())
            .border(1.dp, searchPillRim(), pillShape),
    ) {
        // Primary glow: 84% wide, 24px below the edge, blurred 11px, drifts -18%..18%, scales X 0.95..1.18
        // and Y 1..1.15, opacity 0.85..1, nudges up 3px at its far point.
        Canvas(modifier = Modifier.fillMaxSize().blur(11.dp)) {
            drawSearchGlowLayer(
                widthFraction = 0.84f,
                height = primaryHeight.toPx(),
                overhang = (if (isSearching) 20.dp else 24.dp).toPx(),
                drift = -0.18f + 0.36f * primaryPhase,
                scaleX = 0.95f + 0.23f * primaryPhase,
                scaleY = 1f + 0.15f * primaryPhase,
                translateY = -3.dp.toPx() * primaryPhase,
                alpha = 0.85f + 0.15f * primaryPhase,
                intensity = intensity,
                primary = primary,
            )
        }
        // Secondary glow: 68% wide, blurred 13px, drifts the opposite way (+22%..-22%), scales X 1.05..0.9,
        // opacity 0.7..0.98.
        Canvas(modifier = Modifier.fillMaxSize().blur(13.dp)) {
            drawSearchGlowLayer(
                widthFraction = 0.68f,
                height = secondaryHeight.toPx(),
                overhang = 26.dp.toPx(),
                drift = 0.22f - 0.44f * secondaryPhase,
                scaleX = 1.05f - 0.15f * secondaryPhase,
                scaleY = 1f,
                translateY = (2f - 4f * secondaryPhase).dp.toPx(),
                alpha = 0.7f + 0.28f * secondaryPhase,
                intensity = intensity,
                primary = primary,
            )
        }
        // Top vignette (the mockup's ::before): pure black at the top so only the bottom glows,
        // plus a faint violet inner shadow along the bottom edge.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Black,
                        0.44f to Color.Black.copy(alpha = 0.94f),
                        0.76f to Color.Black.copy(alpha = 0.24f),
                        1f to Color.Transparent,
                    ),
                ),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.6f to Color.Transparent,
                        1f to primary.copy(alpha = 0.34f),
                    ),
                ),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 6.dp, end = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
                .clickable {
                    // Tapping anywhere on the text area opens the keyboard.
                    focusRequester.requestFocus()
                    keyboardController?.show()
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            if (query.isEmpty()) {
                Text(
                    text = "Search...",
                    color = searchPlaceholder(),
                    fontSize = 17.sp,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = TextStyle(color = searchInputText(), fontSize = 17.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        onSubmit()
                    },
                ),
            )
        }

        if (query.isNotEmpty()) {
            IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = searchPlaceholder(),
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onAction),
            contentAlignment = Alignment.Center,
        ) {
            if (isSearching) {
                // Stop square while a search is running.
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .border(2.dp, MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(3.5.dp)),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        }
    }
}

@Composable
private fun SearchFilterChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    trailingRotation: Float? = null,
) {
    val shape = RoundedCornerShape(9.dp)
    val textColor = if (selected) searchChipActiveText() else searchChipText()
    Row(
        modifier = Modifier
            .height(35.dp)
            .clip(shape)
            .background(if (selected) searchChipActiveFill() else searchChipFill())
            .border(1.dp, if (selected) searchChipActiveBorder() else searchChipBorder(), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            maxLines = 1,
        )
        if (trailingRotation != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(trailingRotation),
            )
        }
    }
}

// Glow and chip colours follow the app theme instead of the fixed mockup violet and greys.
private fun glowLight(primary: Color): Color = lerp(primary, Color.White, 0.45f)
private fun glowMid(primary: Color): Color = primary
private fun glowDeep(primary: Color): Color = lerp(primary, Color.Black, 0.45f)

@Composable private fun searchHeaderBackground(): Color = MaterialTheme.colorScheme.background
@Composable private fun searchPillFill(): Color = MaterialTheme.colorScheme.surfaceContainerLowest
@Composable private fun searchPillRim(): Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
@Composable private fun searchDivider(): Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
@Composable private fun searchInputText(): Color = MaterialTheme.colorScheme.onSurface
@Composable private fun searchPlaceholder(): Color = MaterialTheme.colorScheme.onSurfaceVariant
@Composable private fun searchChipFill(): Color = MaterialTheme.colorScheme.surfaceContainerHigh
@Composable private fun searchChipActiveFill(): Color = MaterialTheme.colorScheme.primaryContainer
@Composable private fun searchChipBorder(): Color = MaterialTheme.colorScheme.outlineVariant
@Composable private fun searchChipActiveBorder(): Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
@Composable private fun searchChipText(): Color = MaterialTheme.colorScheme.onSurfaceVariant
@Composable private fun searchChipActiveText(): Color = MaterialTheme.colorScheme.onPrimaryContainer

          
