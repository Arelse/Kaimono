package eu.kanade.tachiyomi.ui.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

// These replicate the exact SVG paths from the reference nav bar (Premium_Discover_UI.html)
// rather than using the app's default Material/Tabler icons for these five nav slots.
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

// Library tab: four vertical bars of varying height
val LibraryNavIcon: ImageVector by lazy {
    strokeIcon(
        "LibraryNavIcon",
        "m16 6 4 14",
        "M12 6v14",
        "M8 8v12",
        "M4 4v16",
    )
}

// History/Updates tab: circular refresh/restore arrow
val UpdatesNavIcon: ImageVector by lazy {
    strokeIcon(
        "UpdatesNavIcon",
        "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8",
        "M3 3v5h5",
    )
}

// Home: four-pointed sparkle, used for the raised center button
val SparkleNavIcon: ImageVector by lazy {
    strokeIcon(
        "SparkleNavIcon",
        "M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z",
    )
}

// Anime tab: circle with a play triangle
val AnimeNavIcon: ImageVector by lazy {
    strokeIcon(
        "AnimeNavIcon",
        "M22 12A10 10 0 1 1 2 12A10 10 0 1 1 22 12z",
        "M10 8L16 12L10 16Z",
    )
}

// Browse/Sources tab: 2x2 grid of squares (kept for other possible uses)
val SourcesNavIcon: ImageVector by lazy {
    strokeIcon(
        "SourcesNavIcon",
        "M3 3h7v7h-7z",
        "M14 3h7v7h-7z",
        "M14 14h7v7h-7z",
        "M3 14h7v7h-7z",
    )
}

// Browse tab: magnifying glass with a compass rose inside and a sparkle at the top-right,
// matching the provided reference icon. Built from simple primitives (full ring + handle +
// inner circle, stroked; needle and sparkle, filled) rather than one hand-traced path, since a
// composite of simple validated shapes is far more reliable than a single complex arc path
// authored blind.
val BrowseSearchNavIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "BrowseSearchNavIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Magnifying glass ring
        addPath(
            pathData = PathParser().parsePathString(
                "M17 10A7 7 0 1 1 3 10A7 7 0 1 1 17 10z",
            ).toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        // Handle
        addPath(
            pathData = PathParser().parsePathString("M15 15L21 21").toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        // Compass bezel
        addPath(
            pathData = PathParser().parsePathString(
                "M13.2 10A3.2 3.2 0 1 1 6.8 10A3.2 3.2 0 1 1 13.2 10z",
            ).toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.3f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        // Compass needle
        addPath(
            pathData = PathParser().parsePathString("M10 8.1L11.3 10L10 11.9L8.7 10Z").toNodes(),
            fill = SolidColor(Color.Black),
        )
        // Sparkle, top-right of the ring
        addPath(
            pathData = PathParser().parsePathString(
                "M16.5 3.3L17.3 5.6L19.6 6.4L17.3 7.2L16.5 9.5L15.7 7.2L13.4 6.4L15.7 5.6Z",
            ).toNodes(),
            fill = SolidColor(Color.Black),
        )
    }.build()
}

// More/Settings tab: gear
val SettingsNavIcon: ImageVector by lazy {
    strokeIcon(
        "SettingsNavIcon",
        "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z",
        "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
    )
}
