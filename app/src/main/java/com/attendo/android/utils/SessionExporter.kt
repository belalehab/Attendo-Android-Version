package com.attendo.android.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AttendeeEntry(val name: String, val nationalId: String, val timestamp: String)

object SessionExporter {

    suspend fun exportPdf(context: Context, uri: Uri, sessionTitle: String, attendees: List<AttendeeEntry>) =
        withContext(Dispatchers.IO) {
            val document = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 50

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 18f
                isFakeBoldText = true
            }
            val headerPaint = Paint().apply {
                color = Color.parseColor("#14B8A6")
                textSize = 12f
                isFakeBoldText = true
            }
            val bodyPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 11f
            }
            val grayPaint = Paint().apply {
                color = Color.GRAY
                textSize = 10f
            }

            var y = margin.toFloat() + 20f

            // Title
            canvas.drawText("Attendo - Session Report", margin.toFloat(), y, titlePaint)
            y += 30f
            canvas.drawText(sessionTitle, margin.toFloat(), y, headerPaint)
            y += 20f
            canvas.drawText("Total Attendees: ${attendees.size}", margin.toFloat(), y, grayPaint)
            y += 30f

            // Divider
            val divPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
            canvas.drawLine(margin.toFloat(), y, (pageWidth - margin).toFloat(), y, divPaint)
            y += 15f

            // Table header
            canvas.drawText("#", margin.toFloat(), y, headerPaint)
            canvas.drawText("Name", (margin + 30).toFloat(), y, headerPaint)
            canvas.drawText("National ID", (margin + 240).toFloat(), y, headerPaint)
            canvas.drawText("Time", (margin + 380).toFloat(), y, headerPaint)
            y += 18f

            // Rows
            attendees.forEachIndexed { index, entry ->
                if (y > pageHeight - margin - 20) {
                    // Simple truncation: in a future version, add new pages
                    canvas.drawText("... and ${attendees.size - index} more", margin.toFloat(), y, grayPaint)
                    return@forEachIndexed
                }
                val rowBg = if (index % 2 == 0) Color.parseColor("#F8FAFC") else Color.WHITE
                val bgPaint = Paint().apply { color = rowBg }
                canvas.drawRect(margin.toFloat(), y - 14f, (pageWidth - margin).toFloat(), y + 4f, bgPaint)

                canvas.drawText("${index + 1}", margin.toFloat(), y, bodyPaint)
                canvas.drawText(entry.name.take(28), (margin + 30).toFloat(), y, bodyPaint)
                canvas.drawText(entry.nationalId, (margin + 240).toFloat(), y, bodyPaint)
                canvas.drawText(entry.timestamp.take(19), (margin + 380).toFloat(), y, bodyPaint)
                y += 18f
            }

            document.finishPage(page)

            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    document.writeTo(outputStream)
                }
            } finally {
                document.close()
            }
        }

    suspend fun exportCsv(context: Context, uri: Uri, sessionTitle: String, attendees: List<AttendeeEntry>) =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val sb = StringBuilder()
                    sb.appendLine("Session,\"$sessionTitle\"")
                    sb.appendLine("Total Attendees,${attendees.size}")
                    sb.appendLine()
                    sb.appendLine("#,Name,National ID,Timestamp")
                    attendees.forEachIndexed { index, entry ->
                        sb.appendLine("${index + 1},\"${entry.name}\",${entry.nationalId},${entry.timestamp}")
                    }
                    outputStream.write(sb.toString().toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    suspend fun exportXlsx(context: Context, uri: Uri, sessionTitle: String, attendees: List<AttendeeEntry>) =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    org.dhatim.fastexcel.Workbook(outputStream, "AttendoApp", "1.0").use { wb ->
                        val ws = wb.newWorksheet("Attendance")
                        ws.value(0, 0, "Session")
                        ws.value(0, 1, sessionTitle)
                        ws.value(1, 0, "Total Attendees")
                        ws.value(1, 1, attendees.size)
                        
                        ws.value(3, 0, "#")
                        ws.value(3, 1, "Name")
                        ws.value(3, 2, "National ID")
                        ws.value(3, 3, "Timestamp")
                        
                        attendees.forEachIndexed { index, entry ->
                            val r = index + 4
                            ws.value(r, 0, index + 1)
                            ws.value(r, 1, entry.name)
                            ws.value(r, 2, entry.nationalId)
                            ws.value(r, 3, entry.timestamp)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
}
