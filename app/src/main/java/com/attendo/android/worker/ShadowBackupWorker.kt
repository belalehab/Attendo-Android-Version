package com.attendo.android.worker

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.attendo.android.utils.DatabaseManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ShadowBackupWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val databaseManager: DatabaseManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val backupUriStr = inputData.getString("backupUri")
        if (backupUriStr.isNullOrEmpty()) {
            return Result.failure()
        }
        
        return try {
            val uri = Uri.parse(backupUriStr)
            val result = databaseManager.exportDatabase(uri)
            if (result.isSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
