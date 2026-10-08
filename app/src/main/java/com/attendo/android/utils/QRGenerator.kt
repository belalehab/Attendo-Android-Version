package com.attendo.android.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import com.attendo.android.data.local.Student
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object QRGenerator {

    /**
     * Generates the same QR payload hash as the desktop version.
     * Formula: SHA256("${nationalId}Attendo_Secure_2026_!@#2025-2026")
     */
    fun generateQrPayload(nationalId: String): String {
        val secret = "Attendo_Secure_2026_!@#"
        val year = "2025-2026"
        val input = "$nationalId$secret$year"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Renders a QR code card as a Bitmap (600x600, white background, student name below QR).
     */
    private fun renderQrCard(student: Student): Bitmap {
        val cardSize = 600
        val qrSize = 420
        val bitmap = Bitmap.createBitmap(cardSize, cardSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // White background
        canvas.drawColor(Color.WHITE)

        val nationalId = student.nationalId ?: return bitmap
        val payload = generateQrPayload(nationalId)

        // Generate QR bitmap
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(payload, BarcodeFormat.QR_CODE, qrSize, qrSize)
        val qrBitmap = Bitmap.createBitmap(qrSize, qrSize, Bitmap.Config.ARGB_8888)
        for (x in 0 until qrSize) {
            for (y in 0 until qrSize) {
                qrBitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }

        // Center QR horizontally, put near top
        val qrLeft = (cardSize - qrSize) / 2f
        val qrTop = 40f
        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)

        // Draw student name below QR
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val nameY = qrTop + qrSize + 60f
        canvas.drawText(student.name, cardSize / 2f, nameY, namePaint)

        return bitmap
    }

    /**
     * Generates a ZIP file containing one JPG per student, using the same hash as the desktop.
     */
    suspend fun generateQrsZip(context: Context, uri: Uri, students: List<Student>) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val zipOut = ZipOutputStream(outputStream.buffered())

                for (student in students) {
                    if (student.nationalId.isNullOrBlank()) continue

                    val cardBitmap = renderQrCard(student)
                    val baos = ByteArrayOutputStream()
                    cardBitmap.compress(Bitmap.CompressFormat.JPEG, 95, baos)
                    val jpgBytes = baos.toByteArray()
                    cardBitmap.recycle()

                    // Safe filename: strip invalid chars but keep Arabic (unicode)
                    val safeName = student.name
                        .replace(Regex("[\\\\/:*?\"<>|]"), "")
                        .trim()
                        .ifBlank { student.nationalId!! }

                    val entry = ZipEntry("${safeName}_QR.jpg")
                    zipOut.putNextEntry(entry)
                    zipOut.write(jpgBytes)
                    zipOut.closeEntry()
                }

                zipOut.finish()
                zipOut.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
