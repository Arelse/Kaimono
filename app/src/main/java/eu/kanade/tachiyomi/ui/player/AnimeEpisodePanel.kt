package eu.kanade.tachiyomi.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Episode panel that slides in from the right over the player. It shows a list with number badges,
 * or a two-column grid of number tiles; the header button switches between the two.
 *
 * @param episodes each episode as its number label and its name.
 */
@Composable
fun AnimeEpisodePanel(
    episodes: List<Pair<String, String>>,
    currentIndex: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var asGrid by remember { mutableStateOf(false) }

    // Full-screen scrim: tapping outside the panel closes it.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(360.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                .background(Color(0xE6161620))
                // Taps on the panel itself must not reach the scrim and close it.
                .clickable(onClick = {})
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Episodes",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { asGrid = !asGrid }) {
                    Icon(
                        imageVector = if (asGrid) Icons.AutoMirrored.Filled.List else Icons.Filled.GridView,
                        contentDescription = if (asGrid) "Show as list" else "Show as grid",
                        tint = Color.White,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            Spacer(Modifier.height(12.dp))

            if (asGrid) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(episodes.indices.chunked(2)) { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { index ->
                                EpisodeTile(
                                    index = index,
                                    episode = episodes[index],
                                    selected = index == currentIndex,
                                    modifier = Modifier.weight(1f),
                                    onPick = onPick,
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(episodes.indices.toList()) { index ->
                        EpisodeRow(
                            index = index,
                            episode = episodes[index],
                            selected = index == currentIndex,
                            onPick = onPick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    index: Int,
    episode: Pair<String, String>,
    selected: Boolean,
    onPick: (Int) -> Unit,
) {
    val (number, name) = episode
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFF2E1B66) else Color(0x33FFFFFF))
            .clickable { onPick(index) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 58.dp, height = 40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) Color(0xFF9D80FF) else Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "EP $number",
                color = if (selected) Color(0xFF12002E) else Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = name.ifBlank { "Episode $number" },
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun EpisodeTile(
    index: Int,
    episode: Pair<String, String>,
    selected: Boolean,
    modifier: Modifier,
    onPick: (Int) -> Unit,
) {
    val (number, name) = episode
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFF2E1B66) else Color(0x33FFFFFF))
            .clickable { onPick(index) }
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) Color(0xFF9D80FF) else Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                color = if (selected) Color(0xFF12002E) else Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = name.ifBlank { "Episode $number" },
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
