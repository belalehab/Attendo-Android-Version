package com.attendo.android.utils

import android.content.Context
import android.net.Uri
import com.attendo.android.data.local.Student
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvParser @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun parseStudents(uri: Uri, grade: String): Result<List<Student>> = withContext(Dispatchers.IO) {
        try {
            val students = mutableListOf<Student>()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                var isFirstLine = true
                
                reader.forEachLine { line ->
                    if (isFirstLine) {
                        isFirstLine = false // Skip header
                        return@forEachLine
                    }
                    
                    val tokens = line.split(",")
                    if (tokens.size >= 2) {
                        val name = tokens[0].trim().removeSurrounding("\"")
                        val nationalId = tokens[1].trim().removeSurrounding("\"")
                        if (name.isNotEmpty() && nationalId.isNotEmpty()) {
                            students.add(
                                Student(
                                    name = name,
                                    nationalId = nationalId,
                                    grade = grade
                                )
                            )
                        }
                    }
                }
            }
            Result.success(students)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
