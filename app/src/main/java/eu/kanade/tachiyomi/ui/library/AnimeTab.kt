package eu.kanade.tachiyomi.ui.library

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen

/**
 * Anime library tab. There is no anime content model yet (that's the next phase: anime
 * extensions/sources under Browse), so this intentionally shows an honest empty state rather
 * than faking data that would just be thrown away once real anime sources exist.
 */
data object AnimeTab : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 6u,
            title = "Anime",
            icon = rememberVectorPainter(Icons.Outlined.Movie),
        )

    @Composable
    override fun Content() {
        Scaffold(
            topBar = { scrollBehavior ->
                CenterAlignedTopAppBar(
                    title = { Text("Anime") },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
                )
            },
        ) { contentPadding ->
            EmptyScreen(
                message = "Your anime library is empty",
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}
