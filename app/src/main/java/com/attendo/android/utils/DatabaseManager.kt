package com.attendo.android.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dbName = "attendo_core.db"

    suspend fun exportDatabase(destinationUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(dbName)
            if (!dbFile.exists()) return@withContext Result.failure(Exception("Database not found"))

            // 1. Copy live DB to a temp file
            val tempFile = File(context.cacheDir, "temp_export.attdb")
            dbFile.copyTo(tempFile, overwrite = true)

            // 2. Drop room_master_table to ensure strict desktop parity
            val sqlite = SQLiteDatabase.openOrCreateDatabase(tempFile, null)
            sqlite.execSQL("DROP TABLE IF EXISTS room_master_table")
            sqlite.close()

            // 3. Write modified temp file to the user's destination URI
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                tempFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            tempFile.delete()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun createTempImport(sourceUri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "temp_import.attdb")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                tempFile.outputStream().use { out ->
                    input.copyTo(out)
                }
            }
            
            // Note: We don't inject room_master_table here because we only read from this temp DB during merge.
            // We never pass it to Room. We manually query it via raw SQLite in MergeUseCase.
            
            Result.success(tempFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun factoryReset(dao: com.attendo.android.data.local.AppDatabase): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.clearAllTables()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
