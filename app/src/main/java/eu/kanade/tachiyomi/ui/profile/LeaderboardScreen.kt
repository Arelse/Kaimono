package eu.kanade.tachiyomi.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Bridges a GMS/Firebase [Task] to a suspend call. This project doesn't depend on
 * kotlinx-coroutines-play-services (no other file uses Task.await()), so rather than add a
 * second new Gradle dependency alongside firebase-firestore, this is the standard few-line
 * equivalent - com.google.android.gms.tasks.Task is already on the classpath transitively via
 * firebase-auth, which every existing sign-in call in ProfileScreen.kt already returns.
 */
private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { exception -> cont.resumeWithException(exception) }
}

/**
 * One row of the reading leaderboard. [points] is a distinct-chapters-read count (see
 * [computeProfileStats]'s totalChaptersRead) - the only reading-volume number this app's
 * schema can back honestly across manga, manhwa and manhua alike. There is no "country of
 * origin" / format field anywhere in the manga schema (confirmed when building the stats
 * screen), so points are NOT split by manga vs manhwa vs manhua - splitting them would mean
 * guessing format from genre tags, which isn't reliable enough to rank people on. Every format
 * your extensions return counts toward the same point total.
 */
data class LeaderboardEntry(
    val uid: String,
    val displayName: String,
    val photoUrl: String?,
    val points: Long,
    val currentStreak: Int,
    val isAnonymous: Boolean,
)

private const val LEADERBOARD_COLLECTION = "leaderboard"

/**
 * Writes the current user's point total to Firestore so they show up on others' leaderboards.
 * Called from ProfileScreen right after it computes [ProfileStats] for its own stats section,
 * so this is just a re-send of a number already computed - no extra reading of local data.
 *
 * Requires Firestore security rules on the "leaderboard" collection that let a signed-in user
 * write only the document whose id matches their own uid, and let any signed-in user read the
 * collection - this call will silently fail (caught below) until those rules are set in the
 * Firebase console; this app has no way to set them from client code.
 */
suspend fun syncLeaderboardEntry(points: Int, currentStreak: Int) {
    val user = FirebaseAuth.getInstance().currentUser ?: return
    val entry = hashMapOf(
        "displayName" to (user.displayName?.takeIf { it.isNotBlank() } ?: if (user.isAnonymous) "Guest" else "Reader"),
        "photoUrl" to user.photoUrl?.toString(),
        "points" to points,
        "currentStreak" to currentStreak,
        "isAnonymous" to user.isAnonymous,
    )
    try {
        FirebaseFirestore.getInstance()
            .collection(LEADERBOARD_COLLECTION)
            .document(user.uid)
            .set(entry)
            .awaitTask()
    } catch (e: Exception) {
        // Leaderboard sync is best-effort - never block or crash the profile screen over it.
    }
}

suspend fun fetchLeaderboard(limit: Long = 50): List<LeaderboardEntry> {
    return try {
        val snapshot = FirebaseFirestore.getInstance()
            .collection(LEADERBOARD_COLLECTION)
            .orderBy("points", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .awaitTask()
        snapshot.documents.mapNotNull { doc ->
            val points = doc.getLong("points") ?: return@mapNotNull null
            LeaderboardEntry(
                uid = doc.id,
                displayName = doc.getString("displayName") ?: "Reader",
                photoUrl = doc.getString("photoUrl"),
                points = points,
                currentStreak = doc.getLong("currentStreak")?.toInt() ?: 0,
                isAnonymous = doc.getBoolean("isAnonymous") ?: false,
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

class LeaderboardScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val myUid = FirebaseAuth.getInstance().currentUser?.uid

        var entries by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
        var loading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            loading = true
            entries = fetchLeaderboard()
            loading = false
        }

        val myRankIndex = entries.indexOfFirst { it.uid == myUid }

        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("Leaderboard") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
            bottomBar = {
                // Keeps your own rank visible even when you've scrolled past it, or aren't in
                // the top 3. Hidden when you're already shown in the podium above.
                if (myRankIndex >= 3) {
                    YouBar(rank = myRankIndex + 1, entry = entries[myRankIndex])
                }
            },
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                when {
                    loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    entries.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No one's on the board yet - read a few chapters and open your profile to claim the top spot.",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    else -> {
                        val top3 = entries.take(3)
                        val rest = entries.drop(3)
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (top3.isNotEmpty()) {
                                Podium(top3 = top3, myUid = myUid)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                itemsIndexed(rest) { index, entry ->
                                    LeaderboardRow(rank = index + 4, entry = entry, isMe = entry.uid == myUid)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Podium(top3: List<LeaderboardEntry>, myUid: String?) {
    val gold = Color(0xFFFFC93C)
    val silver = Color(0xFFB6C0CC)
    val bronze = Color(0xFFC97A3D)
    // Display order is silver, gold, bronze (left-center-right), independent of list order.
    val second = top3.getOrNull(1)
    val first = top3.getOrNull(0)
    val third = top3.getOrNull(2)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        second?.let {
            PodiumSlot(entry = it, rank = 2, ringColor = silver, size = 56.dp, isMe = it.uid == myUid, modifier = Modifier.weight(1f))
        }
        first?.let {
            PodiumSlot(entry = it, rank = 1, ringColor = gold, size = 72.dp, isMe = it.uid == myUid, showCrown = true, modifier = Modifier.weight(1f))
        }
        third?.let {
            PodiumSlot(entry = it, rank = 3, ringColor = bronze, size = 56.dp, isMe = it.uid == myUid, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PodiumSlot(
    entry: LeaderboardEntry,
    rank: Int,
    ringColor: Color,
    size: androidx.compose.ui.unit.Dp,
    isMe: Boolean,
    modifier: Modifier = Modifier,
    showCrown: Boolean = false,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (showCrown) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = ringColor,
                modifier = Modifier.size(22.dp).padding(bottom = 2.dp),
            )
        } else {
            Spacer(modifier = Modifier.height(22.dp))
        }
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(size)
                    .border(2.5.dp, Brush.sweepGradient(listOf(ringColor, ringColor.copy(alpha = 0.4f), ringColor)), CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                if (entry.photoUrl != null) {
                    AsyncImage(
                        model = entry.photoUrl,
                        contentDescription = entry.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                } else {
                    Text(entry.displayName.take(1).uppercase(), fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(ringColor)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("$rank", color = Color.Black, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = entry.displayName + if (isMe) " (You)" else "",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text("${entry.points} pts", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ringColor)
        StreakBadge(days = entry.currentStreak, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun YouBar(rank: Int, entry: LeaderboardEntry) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.14f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("#$rank", color = accent, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(entry.displayName.take(1).uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text("You", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text("${entry.points} pts", fontWeight = FontWeight.Bold, color = accent)
            StreakBadge(days = entry.currentStreak)
        }
    }
}

/** Small flame + day-count badge, shown wherever an entry's streak is displayed. */
@Composable
private fun StreakBadge(days: Int, modifier: Modifier = Modifier) {
    if (days <= 0) return
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = "Streak",
            tint = Color(0xFFFF7A3C),
            modifier = Modifier.size(13.dp),
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = "${days}d",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF7A3C),
        )
    }
}

@Composable
private fun LeaderboardRow(rank: Int, entry: LeaderboardEntry, isMe: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isMe) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val rankColor = when (rank) {
            1 -> Color(0xFFFFD700)
            2 -> Color(0xFFC0C0C0)
            3 -> Color(0xFFCD7F32)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
            Text("#$rank", color = rankColor, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (entry.photoUrl != null) {
                AsyncImage(
                    model = entry.photoUrl,
                    contentDescription = entry.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(entry.displayName.take(1).uppercase(), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.displayName + if (isMe) " (You)" else "",
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (entry.isAnonymous) {
                Text("Guest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${entry.points} pts", fontWeight = FontWeight.Bold, color = accent)
            StreakBadge(days = entry.currentStreak)
        }
    }
}
