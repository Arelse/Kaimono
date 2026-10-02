package eu.kanade.presentation.history.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.getValue
import coil3.compose.AsyncImage
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.HistoryCardStyle
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.util.lang.toTimestampString
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

private val HistoryItemHeight = 96.dp

@Composable
fun HistoryItem(
    history: HistoryWithRelations,
    onClickCover: () -> Unit,
    onClickResume: () -> Unit,
    onClickDelete: () -> Unit,
    onClickFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardStyle by remember { Injekt.get<UiPreferences>() }.historyCardStyle.collectAsState()
    val roundness by remember { Injekt.get<UiPreferences>() }.cardRoundness.collectAsState()
    val r = roundness / 100f

    val readAt = remember { history.readAt?.toTimestampString() ?: "" }
    val progress = history.lastPageRead
    val progressSuffix = when {
        history.chapterRead -> " - 100%"
        progress > 0 -> if (history.isNovel) {
            " - $progress%"
        } else {
            " - ${stringResource(MR.strings.chapter_progress, progress + 1)}"
        }
        else -> ""
    }
    val subtitle = if (history.chapterNumber > -1) {
        stringResource(
            MR.strings.recent_manga_time,
            formatChapterNumber(history.chapterNumber),
            readAt,
        ) + progressSuffix
    } else {
        readAt + progressSuffix
    }

    when (cardStyle) {
        HistoryCardStyle.REGULAR -> Row(
            modifier = modifier
                .padding(horizontal = 12.dp, vertical = 3.dp)
                .clip(RoundedCornerShape((16 * r).dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.28f))
                .clickable(onClick = onClickResume)
                .height(HistoryItemHeight)
                .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MangaCover.Book(
                modifier = Modifier.fillMaxHeight(),
                data = history.coverData,
                onClick = onClickCover,
            )
            HistoryTexts(
                title = history.title,
                subtitle = subtitle,
                textColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            HistoryActions(history, onClickFavorite, onClickDelete, MaterialTheme.colorScheme.onSurface)
        }

        HistoryCardStyle.FROSTED_GLASS -> Box(
            modifier = modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape((20 * r).dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape((20 * r).dp))
                .clickable(onClick = onClickResume)
                .height(HistoryItemHeight + 16.dp),
        ) {
            AsyncImage(
                model = history.coverData,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .blur(24.dp),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.55f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            ),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MangaCover.Book(
                    modifier = Modifier.fillMaxHeight(),
                    data = history.coverData,
                    onClick = onClickCover,
                )
                HistoryTexts(
                    title = history.title,
                    subtitle = subtitle,
                    textColor = Color.White,
                    modifier = Modifier.weight(1f),
                )
                HistoryActions(history, onClickFavorite, onClickDelete, Color.White)
            }
        }

        HistoryCardStyle.BOOTIFUL -> Column(
            modifier = modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape((22 * r).dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape((22 * r).dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
                .clickable(onClick = onClickResume),
        ) {
            AsyncImage(
                model = history.coverData,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clickable(onClick = onClickCover),
            )
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (history.chapterNumber > -1) {
                        Text(
                            text = "Chapter ${formatChapterNumber(history.chapterNumber)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    Text(
                        text = history.title,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        text = readAt + progressSuffix,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                HistoryActions(history, onClickFavorite, onClickDelete, MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun HistoryTexts(
    title: String,
    subtitle: String,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(start = MaterialTheme.padding.medium, end = MaterialTheme.padding.small),
    ) {
        val textStyle = MaterialTheme.typography.bodyMedium
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = textStyle,
            color = textColor,
        )
        Text(
            text = subtitle,
            modifier = Modifier.padding(top = 4.dp),
            style = textStyle,
            color = textColor.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun HistoryActions(
    history: HistoryWithRelations,
    onClickFavorite: () -> Unit,
    onClickDelete: () -> Unit,
    tint: Color,
) {
    if (!history.coverData.isMangaFavorite) {
        IconButton(onClick = onClickFavorite) {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = stringResource(MR.strings.add_to_library),
                tint = tint,
            )
        }
    }
    IconButton(onClick = onClickDelete) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = stringResource(MR.strings.action_delete),
            tint = tint,
        )
    }
}

@PreviewLightDark
@Composable
private fun HistoryItemPreviews(
    @PreviewParameter(HistoryWithRelationsProvider::class)
    historyWithRelations: HistoryWithRelations,
) {
    TachiyomiPreviewTheme {
        Surface {
            HistoryItem(
                history = historyWithRelations,
                onClickCover = {},
                onClickResume = {},
                onClickDelete = {},
                onClickFavorite = {},
            )
        }
    }
}
