package com.attendo.android.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JwtValidator @Inject constructor() {

    private val secretKeyString = "Attendo_Secure_RSA_2026_!@#_Key"
    // Pad to 32 bytes for HS256 if needed, or use as is if it fits the library constraints
    private val key = Keys.hmacShaKeyFor(secretKeyString.toByteArray(Charsets.UTF_8).let {
        if (it.size < 32) it + ByteArray(32 - it.size) else it
    })

    fun validateLicense(token: String, expectedHwId: String): Result<Boolean> {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload

            val hwId = claims["hwId"] as? String
            
            if (hwId == expectedHwId) {
                Result.success(true)
            } else {
                Result.failure(Exception("License hardware mismatch"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
