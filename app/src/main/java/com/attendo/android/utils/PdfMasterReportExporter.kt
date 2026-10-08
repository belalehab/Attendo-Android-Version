package com.attendo.android.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.attendo.android.data.local.Attendance
import com.attendo.android.data.local.Student
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

object PdfMasterReportExporter {
    suspend fun export(
        context: Context,
        uri: Uri,
        workspace: String,
        students: List<Student>,
        attendances: List<Attendance>,
        filterType: String
    ) {
        withContext(Dispatchers.IO) {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            val titlePaint = Paint().apply {
                textSize = 18f
                color = Color.BLACK
                isFakeBoldText = true
            }
            
            val textPaint = Paint().apply {
                textSize = 12f
                color = Color.BLACK
            }

            var y = 50f
            canvas.drawText("Master Report - $workspace ($filterType)", 50f, y, titlePaint)
            y += 40f
            
            canvas.drawText("Student Name", 50f, y, titlePaint)
            canvas.drawText("Attended", 350f, y, titlePaint)
            canvas.drawText("Excused", 450f, y, titlePaint)
            y += 20f

            val attendanceByStudent = attendances.groupBy { it.nationalId }

            for (student in students) {
                if (y > 800f) {
                    document.finishPage(page)
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                }
                
                val records = attendanceByStudent[student.nationalId] ?: emptyList()
                val attended = records.size
                val excused = records.count { it.isExcused == 1 }

                canvas.drawText(student.name, 50f, y, textPaint)
                canvas.drawText(attended.toString(), 350f, y, textPaint)
                canvas.drawText(excused.toString(), 450f, y, textPaint)
                y += 20f
            }

            document.finishPage(page)

            context.contentResolver.openOutputStream(uri)?.use { os ->
                document.writeTo(os)
            }
            document.close()
        }
    }
}
