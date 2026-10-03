package com.attendo.android.domain.usecase

import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QRValidator @Inject constructor() {
    
    private val secretAppKey = "Attendo_Secure_2026_!@#"
    private val academicYear = "2025-2026" // Typically fetched from Settings DB

    fun validatePayload(payload: String): Result<String> {
        val parts = payload.split("|")
        if (parts.size != 2) {
            return Result.failure(Exception("Invalid payload format"))
        }

        val nationalId = parts[0]
        val providedHash = parts[1]

        val rawString = "$nationalId$secretAppKey$academicYear"
        val expectedHash = hashString(rawString)

        return if (providedHash == expectedHash) {
            Result.success(nationalId)
        } else {
            Result.failure(Exception("QR Signature Mismatch"))
        }
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
