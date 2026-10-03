package eu.kanade.presentation.manga.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

// These replicate the exact SVG paths from the reference manga-info action row
// (Manga_Reader_App_Premium_UI.html) rather than using the app's default Material icons
// for these five action slots.
private fun strokeIcon(name: String, vararg pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        pathData.forEach { d ->
            addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

// In Library: open book (two pages)
val MangaBookOpenIcon: ImageVector by lazy {
    strokeIcon(
        "MangaBookOpenIcon",
        "M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z",
        "M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z",
    )
}

// Soon: calendar with a small clock badge
val MangaCalendarClockIcon: ImageVector by lazy {
    strokeIcon(
        "MangaCalendarClockIcon",
        "M21 7.5V6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h3.5",
        "M16 2v4",
        "M8 2v4",
        "M3 10h5",
        "M17.5 17.5 16 16.25V14",
        "M22 16A6 6 0 1 1 10 16A6 6 0 1 1 22 16z",
    )
}

// WebView: compass
val MangaCompassIcon: ImageVector by lazy {
    strokeIcon(
        "MangaCompassIcon",
        "M22 12A10 10 0 1 1 2 12A10 10 0 1 1 22 12z",
        "M16.24 7.76L14.12 14.12L7.76 16.24L9.88 9.88L16.24 7.76Z",
    )
}

// Merge: git-merge style, two nodes joined by a curve
val MangaGitMergeIcon: ImageVector by lazy {
    strokeIcon(
        "MangaGitMergeIcon",
        "M21 18A3 3 0 1 1 15 18A3 3 0 1 1 21 18z",
        "M9 6A3 3 0 1 1 3 6A3 3 0 1 1 9 6z",
        "M6 21V9a9 9 0 0 0 9 9",
    )
}
