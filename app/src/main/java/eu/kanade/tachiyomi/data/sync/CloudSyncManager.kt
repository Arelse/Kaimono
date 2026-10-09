package eu.kanade.tachiyomi.data.sync

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
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
 * BackupRestorer) into per-account Firebase Storage, at user_backups/{uid}.tachibk.
 *
 * This is push-on-background / pull-on-foreground, not live multi-device sync: two devices
 * signed into the same account at once only pick up each other's changes the next time one
 * backgrounds (push) and the other foregrounds or signs in fresh (pull) - there's no
 * continuous/real-time merge while both are open simultaneously.
 *
 * Only applies to non-anonymous accounts (email/password or Google). A guest/anonymous
 * FirebaseUser's uid is regenerated on every reinstall, so there's nothing stable to key a
 * cloud copy against for that case.
 *
 * Reuses the same BackupManga/history/categories format the in-app manual backup screens
 * already produce, rather than a bespoke Firestore schema - this is the one sync surface this
 * app already has well exercised (manga, chapters read/bookmarked, history timestamps and
 * durations, categories, favorites). There's deliberately no anime history/library here: per
 * computeProfileStats's own notes, this app's schema has no anime-watch backend yet, so there's
 * nothing to sync for that tab.
 */
object CloudSyncManager {

    private const val STORAGE_PATH_PREFIX = "user_backups"

    private fun currentUid(): String? {
        val user = FirebaseAuth.getInstance().currentUser
        return if (user != null && !user.isAnonymous) user.uid else null
    }

    private fun refFor(uid: String) =
        FirebaseStorage.getInstance().reference.child("$STORAGE_PATH_PREFIX/$uid.tachibk")

    /**
     * Downloads this account's cloud backup (if any) and restores it into the local library.
     * If nothing has been uploaded for this account yet (brand-new account, or first device to
     * ever sign into it), seeds the cloud with whatever's local instead, so a sync-down never
     * silently does nothing on a device that actually has data worth preserving.
     */
    suspend fun syncDown(context: Context) {
        val uid = currentUid() ?: return
        try {
            val tempFile = File.createTempFile("cloud_restore", ".tachibk", context.cacheDir)
            try {
                refFor(uid).getFile(tempFile).awaitTask()
                BackupRestorer(context, BackupNotifier(context), isSync = true)
                    .restore(Uri.fromFile(tempFile), RestoreOptions())
            } finally {
                tempFile.delete()
            }
        } catch (e: StorageException) {
            if (e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                syncUp(context)
            } else {
                logcat(LogPriority.ERROR, e) { "Cloud sync-down failed" }
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Cloud sync-down failed" }
        }
    }

    /** Pushes the current local library/history/favorites up to this account's cloud backup. */
    suspend fun syncUp(context: Context) {
        val uid = currentUid() ?: return
        try {
            val tempFile = File.createTempFile("cloud_backup", ".tachibk", context.cacheDir)
            try {
                BackupCreator(context, isAutoBackup = false).backup(Uri.fromFile(tempFile), BackupOptions())
                refFor(uid).putFile(Uri.fromFile(tempFile)).awaitTask()
            } finally {
                tempFile.delete()
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Cloud sync-up failed" }
        }
    }

    /**
     * Bridges a GMS/Firebase [Task] to a suspend call without adding the
     * kotlinx-coroutines-play-services dependency - same approach as LeaderboardScreen's
     * Task.awaitTask(), duplicated here since that one is private to its file.
     */
    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resume(result) }
        addOnFailureListener { exception -> cont.resumeWithException(exception) }
    }
}
