package com.jbcbros.qbitremote.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jbcbros.qbitremote.data.repository.QbRepository

/**
 * Periodically (>=15 min) polls qBittorrent and posts a notification when a torrent:
 *  - transitions into a completed (seeding) state, or
 *  - enters the error state.
 *
 * Uses its own [QbRepository] instance (a plain constructor is fine — config is shared via DataStore).
 * First run only seeds the known-state sets so we don't flood notifications for every already-complete
 * torrent; subsequent runs notify on real transitions.
 */
class TorrentCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val completedStates = setOf(
        "uploading", "pausedUP", "queuedUP", "stalledUP", "checkingUP", "forcedUP"
    )

    override suspend fun doWork(): Result {
        val context = applicationContext
        val repo = QbRepository(context)
        val config = repo.loadConfig()
        if (config.host.isBlank()) return Result.success()

        val torrents = try {
            repo.getTorrents()
        } catch (_: Exception) {
            return Result.success()
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prevCompleted = prefs.getStringSet(KEY_COMPLETED, emptySet()).orEmpty()
        val prevErrored = prefs.getStringSet(KEY_ERRORED, emptySet()).orEmpty()
        val seededBefore = prevCompleted.isNotEmpty() || prevErrored.isNotEmpty()

        val nowCompleted = mutableSetOf<String>()
        val nowErrored = mutableSetOf<String>()

        torrents.forEach { t ->
            val isCompleted = t.state in completedStates
            val isError = t.state == "error"

            if (isCompleted) {
                if (seededBefore && t.hash !in prevCompleted) {
                    NotificationHelper.notifyCompleted(context, t.name, t.hash.hashCode())
                }
                nowCompleted += t.hash
            }
            if (isError) {
                if (seededBefore && t.hash !in prevErrored) {
                    NotificationHelper.notifyError(context, t.name, t.hash.hashCode())
                }
                nowErrored += t.hash
            }
        }

        prefs.edit()
            .putStringSet(KEY_COMPLETED, nowCompleted)
            .putStringSet(KEY_ERRORED, nowErrored)
            .apply()

        return Result.success()
    }

    companion object {
        private const val PREFS_NAME = "torrent_check_states"
        private const val KEY_COMPLETED = "completed_hashes"
        private const val KEY_ERRORED = "errored_hashes"
        const val WORK_NAME = "torrent_check_work"
    }
}
