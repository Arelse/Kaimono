package eu.kanade.tachiyomi.data.sync

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import eu.kanade.tachiyomi.data.backup.BackupNotifier
import eu.kanade.tachiyomi.data.backup.create.BackupCreator
import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.data.backup.restore.BackupRestorer
import eu.kanade.tachiyomi.data.backup.restore.RestoreOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Keeps library, reading history, and favorites available across an uninstall/reinstall by
 * shadowing the app's existing local .tachibk backup/restore system (BackupCreator /
 * BackupRestorer) into Firestore, under user_backups/{uid}.
 *
 * Firestore (not Firebase Storage) is used deliberately: Storage requires the paid Blaze plan
 * even for near-zero usage, while Firestore works on the free Spark plan. The tradeoff is that
 * Firestore caps each document at ~1 MiB, so the backup is base64-encoded and split across
 * user_backups/{uid}/chunks/{index} documents (each carrying an explicit "index" field, since
 * document IDs sort lexicographically, not numerically), with a user_backups/{uid} parent doc
 * tracking how many chunks make up the current backup.
 *
 * All chunk writes plus the parent doc update happen in a single Firestore batch, so a sync-up
 * either fully replaces the cloud copy or doesn't touch it at all - no partial/corrupt state if
 * something fails mid-upload. A batch tops out at 500 operations; a library would need to
 * produce a backup of several hundred MB to hit that, which isn't a realistic scale here.
 *
 * This is push-on-background / pull-on-foreground, not live multi-device sync: two devices
 * signed into the same account at once only pick up each other's changes the next time one
 * backgrounds (push) and the other foregrounds or signs in fresh (pull).
 *
 * Only applies to non-anonymous accounts (email/password or Google) - a guest/anonymous uid is
 * regenerated on every reinstall, so there's nothing stable to key a cloud copy against.
 *
 * No anime history/library sync: per computeProfileStats's own notes, this app's schema has no
 * anime-watch backend yet, so there's nothing there to sync.
 */
object CloudSyncManager {

    private const val BACKUPS_COLLECTION = "user_backups"
    private const val CHUNKS_SUBCOLLECTION = "chunks"

    // Comfortably under Firestore's ~1 MiB per-document limit, leaving room for field-name and
    // document overhead. Base64 inflates raw bytes by ~4/3, so this holds ~675 KB of raw backup
    // data per chunk.
    private const val CHUNK_SIZE_CHARS = 900_000

    private fun currentUid(): String? {
        val user = FirebaseAuth.getInstance().currentUser
        return if (user != null && !user.isAnonymous) user.uid else null
    }

    private fun firestore() = FirebaseFirestore.getInstance()

    private fun metaDoc(uid: String) = firestore().collection(BACKUPS_COLLECTION).document(uid)

    private fun chunksCollection(uid: String) = metaDoc(uid).collection(CHUNKS_SUBCOLLECTION)

    suspend fun syncDown(context: Context) {
        val uid = currentUid() ?: return
        try {
            val metaSnapshot = metaDoc(uid).get().awaitTask()
            val totalChunks = metaSnapshot.getLong("totalChunks")?.toInt()
            if (metaSnapshot.exists() && totalChunks != null && totalChunks > 0) {
                val chunksSnapshot = chunksCollection(uid).get().awaitTask()
                val base64 = chunksSnapshot.documents
                    .sortedBy { it.getLong("index") ?: 0L }
                    .joinToString("") { it.getString("data").orEmpty() }

                val bytes = Base64.decode(base64, Base64.NO_WRAP)
                val tempFile = File.createTempFile("cloud_restore", ".tachibk", context.cacheDir)
                try {
                    tempFile.writeBytes(bytes)
                    BackupRestorer(context, BackupNotifier(context), isSync = true)
                        .restore(Uri.fromFile(tempFile), RestoreOptions())
                } finally {
                    tempFile.delete()
                }
            } else {
                syncUp(context)
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Cloud sync-down failed" }
        }
    }

    suspend fun syncUp(context: Context) {
        val uid = currentUid() ?: return
        try {
            val tempFile = File.createTempFile("cloud_backup", ".tachibk", context.cacheDir)
            val base64: String
            try {
                BackupCreator(context, isAutoBackup = false).backup(Uri.fromFile(tempFile), BackupOptions())
                base64 = Base64.encodeToString(tempFile.readBytes(), Base64.NO_WRAP)
            } finally {
                tempFile.delete()
            }

            val newChunks = base64.chunked(CHUNK_SIZE_CHARS)
            val previousTotal = metaDoc(uid).get().awaitTask().getLong("totalChunks")?.toInt() ?: 0

            val batch = firestore().batch()
            newChunks.forEachIndexed { index, chunk ->
                batch.set(chunksCollection(uid).document(index.toString()), mapOf("data" to chunk, "index" to index))
            }
            for (i in newChunks.size until previousTotal) {
                batch.delete(chunksCollection(uid).document(i.toString()))
            }
            batch.set(
                metaDoc(uid),
                mapOf(
                    "totalChunks" to newChunks.size,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().awaitTask()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Cloud sync-up failed" }
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resume(result) }
        addOnFailureListener { exception -> cont.resumeWithException(exception) }
    }
}
