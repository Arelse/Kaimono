package eu.kanade.tachiyomi.ui.profile

import kotlinx.coroutines.flow.first
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * One manga's reading history, condensed to what the Profile screen's "Lately Alighted Upon"
 * row and favorite-title card need.
 */
data class RecentItem(
    val mangaId: Long,
    val title: String,
    val coverData: MangaCover,
    val isNovel: Boolean,
    val interactionCount: Int,
    val totalDuration: Long,
    val lastReadAt: Date?,
)

data class ProfileStats(
    val totalReadDuration: Long,
    val itemsConsumed: Int,
    val daysActive: Int,
    val avgChaptersPerDay: Double,
    val currentStreak: Int,
    val longestStreak: Int,
    val favoriteTitle: RecentItem?,
    val recentItems: List<RecentItem>,
    val topGenres: List<Pair<String, Int>>,
    val topSources: List<Pair<String, Int>>,
    val typeBreakdown: List<Pair<String, Int>>,
) {
    companion object {
        val EMPTY = ProfileStats(0, 0, 0, 0.0, 0, 0, null, emptyList(), emptyList(), emptyList(), emptyList())
    }
}

/**
 * Computes [ProfileStats] from the user's real reading history (tachiyomi.domain.history) and
 * manga metadata (tachiyomi.domain.manga). There is no anime-watch or studio data anywhere in
 * this app's schema yet (the Anime tab has no backend), so this deliberately does not include
 * an "episodes watched" stat or a "top studios" breakdown - both would have to be fabricated.
 * "Top Sources" (which extension a read title came from) is the honest substitute for studios,
 * and the type breakdown is Manga vs Novel (the real split this app tracks) rather than
 * TV vs Manhwa.
 *
 * A manga lookup (for genre/source) is only done for the N most recently read titles, capped by
 * [genreLookupLimit], since a profile screen is a nice-to-have summary, not something that needs
 * to resolve someone's entire history on every open.
 */
suspend fun computeProfileStats(genreLookupLimit: Int = 40): ProfileStats {
    val getHistory = Injekt.get<GetHistory>()
    val getManga = Injekt.get<GetManga>()
    val sourceManager = Injekt.get<SourceManager>()

    val rows: List<HistoryWithRelations> = try {
        getHistory.subscribe(query = "", limit = Long.MAX_VALUE).first()
    } catch (e: Exception) {
        emptyList()
    }

    if (rows.isEmpty()) return ProfileStats.EMPTY

    val totalReadDuration = rows.sumOf { it.readDuration }

    val byManga: Map<Long, List<HistoryWithRelations>> = rows.groupBy { it.mangaId }
    val recentItems = byManga.map { (mangaId, group) ->
        val latest = group.maxByOrNull { it.readAt ?: Date(0) }
        RecentItem(
            mangaId = mangaId,
            title = latest?.title ?: group.first().title,
            coverData = latest?.coverData ?: group.first().coverData,
            isNovel = latest?.isNovel ?: group.first().isNovel,
            interactionCount = group.size,
            totalDuration = group.sumOf { it.readDuration },
            lastReadAt = group.mapNotNull { it.readAt }.maxOrNull(),
        )
    }.sortedByDescending { it.lastReadAt ?: Date(0) }

    val favoriteTitle = recentItems.maxByOrNull { it.totalDuration }
        ?: recentItems.maxByOrNull { it.interactionCount }

    val distinctDays: List<LocalDate> = rows.mapNotNull { it.readAt }
        .map { it.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() }
        .distinct()
        .sorted()

    val daysActive = distinctDays.size
    val avgChaptersPerDay = if (daysActive > 0) rows.size.toDouble() / daysActive else 0.0

    var longestStreak = 0
    var runLength = 0
    var previousDay: LocalDate? = null
    for (day in distinctDays) {
        runLength = if (previousDay != null && previousDay.plusDays(1) == day) runLength + 1 else 1
        longestStreak = maxOf(longestStreak, runLength)
        previousDay = day
    }
    val today = LocalDate.now()
    val currentStreak = if (distinctDays.isEmpty()) {
        0
    } else {
        val lastDay = distinctDays.last()
        if (lastDay != today && lastDay != today.minusDays(1)) {
            0
        } else {
            var streak = 1
            var cursor = lastDay
            for (i in distinctDays.size - 2 downTo 0) {
                val day = distinctDays[i]
                if (cursor.minusDays(1) == day) {
                    streak++
                    cursor = day
                } else {
                    break
                }
            }
            streak
        }
    }

    val typeBreakdown = recentItems
        .groupingBy { if (it.isNovel) "Novel" else "Manga" }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }

    // Genre/source insights need the full Manga row (genre list, source id), which history
    // rows don't carry - only looked up for the most recently read titles, capped.
    val lookupTargets = recentItems.take(genreLookupLimit)
    val genreCounts = mutableMapOf<String, Int>()
    val sourceCounts = mutableMapOf<String, Int>()
    for (item in lookupTargets) {
        val manga = try {
            getManga.await(item.mangaId)
        } catch (e: Exception) {
            null
        } ?: continue
        manga.genre?.forEach { genre ->
            if (genre.isNotBlank()) genreCounts[genre] = (genreCounts[genre] ?: 0) + 1
        }
        val sourceName = try {
            sourceManager.getOrStub(manga.source).name
        } catch (e: Exception) {
            null
        }
        if (!sourceName.isNullOrBlank()) {
            sourceCounts[sourceName] = (sourceCounts[sourceName] ?: 0) + 1
        }
    }

    return ProfileStats(
        totalReadDuration = totalReadDuration,
        itemsConsumed = byManga.size,
        daysActive = daysActive,
        avgChaptersPerDay = avgChaptersPerDay,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        favoriteTitle = favoriteTitle,
        recentItems = recentItems,
        topGenres = genreCounts.toList().sortedByDescending { it.second }.take(5),
        topSources = sourceCounts.toList().sortedByDescending { it.second }.take(5),
        typeBreakdown = typeBreakdown,
    )
}

/** Formats milliseconds as "1h 15m" / "45m" / "<1m", matching the reference design. */
fun formatReadDuration(ms: Long): String {
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        totalMinutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}
