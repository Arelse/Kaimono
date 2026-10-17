package eu.kanade.presentation.browse.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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

// Colours from the Gemini black glow search mockup (glow style only).
private val SearchHeaderBackground = Color(0xFF0B0D13)
private val SearchPillFill = Color(0xFF000000)
private val SearchPillRim = Color(0x21FFFFFF)
private val SearchDivider = Color(0x14FFFFFF)
private val SearchActionBlue = Color(0xFF0A4999)
private val SearchInputText = Color(0xFFF1F3F4)
private val SearchPlaceholder = Color(0xFF9AA0A6)
private val SearchChipFill = Color(0xFF0B0D13)
private val SearchChipActiveFill = Color(0xFF474866)
private val SearchChipBorder = Color(0x26FFFFFF)
private val SearchChipActiveBorder = Color(0x59A8A8D4)
private val SearchChipText = Color(0xFFC7C9D1)
private val SearchChipActiveText = Color(0xFFF2F3FF)

/** Soft violet glow along the bottom edge of a pill, as in the mockup. */
internal fun DrawScope.drawSearchBottomGlow(widthFraction: Float, glowHeight: Float) {
    val glowWidth = size.width * widthFraction
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xF2BAA8FF),
                Color(0xD68066EE),
                Color(0x7A4C41A8),
                Color.Transparent,
            ),
            center = Offset(size.width / 2f, size.height),
            radius = glowWidth / 2f,
        ),
        topLeft = Offset((size.width - glowWidth) / 2f, size.height - glowHeight / 2f),
        size = Size(glowWidth, glowHeight),
    )
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

    Column(modifier = Modifier.fillMaxWidth().background(SearchHeaderBackground)) {
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
        HorizontalDivider(color = SearchDivider)
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

    val pillShape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(pillShape)
            .background(SearchPillFill)
            .drawBehind { drawSearchBottomGlow(widthFraction = 0.84f, glowHeight = 56.dp.toPx()) }
            .border(1.dp, SearchPillRim, pillShape)
            .padding(start = 6.dp, end = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color(0xFFE3E3E6),
            )
        }

        Box(
            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (query.isEmpty()) {
                Text(
                    text = "Search...",
                    color = SearchPlaceholder,
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
                textStyle = TextStyle(color = SearchInputText, fontSize = 17.sp),
                cursorBrush = SolidColor(Color(0xFFA594F9)),
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
                    tint = SearchPlaceholder,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(SearchActionBlue)
                .clickable(onClick = onAction),
            contentAlignment = Alignment.Center,
        ) {
            if (isSearching) {
                // Stop square while a search is running.
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .border(2.dp, Color.White, RoundedCornerShape(3.5.dp)),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp),
                )
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
    val textColor = if (selected) SearchChipActiveText else SearchChipText
    Row(
        modifier = Modifier
            .height(35.dp)
            .clip(shape)
            .background(if (selected) SearchChipActiveFill else SearchChipFill)
            .border(1.dp, if (selected) SearchChipActiveBorder else SearchChipBorder, shape)
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
