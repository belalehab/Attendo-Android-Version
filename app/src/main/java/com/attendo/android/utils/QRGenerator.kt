package com.attendo.android.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.attendo.android.data.local.Student
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QRGenerator {
    suspend fun generateQRsPdf(context: Context, uri: Uri, students: List<Student>) = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val writer = QRCodeWriter()

        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points
        val margin = 50
        val qrSize = 150
        val spacing = 20

        var currentX = margin
        var currentY = margin
        var pageNum = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }

        students.forEach { student ->
            if (currentY + qrSize + 30 > pageHeight - margin) {
                document.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentX = margin
                currentY = margin
            }

            val id = student.nationalId ?: return@forEach
            try {
                val bitMatrix = writer.encode(id, BarcodeFormat.QR_CODE, qrSize, qrSize)
                val width = bitMatrix.width
                val height = bitMatrix.height
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                    }
                }
                canvas.drawBitmap(bmp, currentX.toFloat(), currentY.toFloat(), null)
                canvas.drawText(student.name, currentX + (qrSize / 2f), currentY + qrSize + 15f, textPaint)
                canvas.drawText(id, currentX + (qrSize / 2f), currentY + qrSize + 30f, textPaint)

                currentX += qrSize + spacing
                if (currentX + qrSize > pageWidth - margin) {
                    currentX = margin
                    currentY += qrSize + 50
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        document.finishPage(page)

        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                document.writeTo(outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            document.close()
        }
    }
}
