package com.example.smartlist.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * SyncWorker is a WorkManager CoroutineWorker stub for pushing/pulling sync operations.
 * This simplified version avoids Hilt-assisted injection in the scaffold so the project
 * can build without additional Hilt WorkManager setup. Replace with a Hilt-enabled
 * implementation when adding `androidx.hilt:hilt-work` and the proper Hilt setup.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // TODO: Implement sync logic: read pending ops, push to remote, pull remote updates, merge
        return Result.success()
    }
}
