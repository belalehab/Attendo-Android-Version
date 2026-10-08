package com.attendo.android.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import javax.inject.Inject
import javax.inject.Singleton

sealed class LicenseResult {
    data class Valid(val plan: String, val daysLeft: Long?) : LicenseResult()
    data class ExpiringSoon(val plan: String, val daysLeft: Long) : LicenseResult()
    object Expired : LicenseResult()
    object Tampered : LicenseResult()
    object Invalid : LicenseResult()
}

@Singleton
class JwtValidator @Inject constructor() {

    private val secretKeyString = "Attendo_Secure_RSA_2026_!@#_Key"
    // Pad to 32 bytes for HS256 if needed, or use as is if it fits the library constraints
    private val key = Keys.hmacShaKeyFor(secretKeyString.toByteArray(Charsets.UTF_8).let {
        if (it.size < 32) it + ByteArray(32 - it.size) else it
    })

    fun validateLicense(token: String, expectedHwId: String): LicenseResult {
        if (token.isBlank()) return LicenseResult.Invalid
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token.trim())
                .payload

            val hwId = (claims["hwId"] as? String)?.trim()
            val plan = (claims["plan"] as? String)?.trim() ?: "Pro"
            
            if (hwId != expectedHwId.trim()) {
                return LicenseResult.Invalid
            }

            val expDate = claims.expiration
            if (expDate != null) {
                val diffMillis = expDate.time - System.currentTimeMillis()
                val daysLeft = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillis)
                
                if (daysLeft < 0) {
                    return LicenseResult.Expired
                } else if (daysLeft <= 14) { // Assuming 14 days is the warning threshold
                    return LicenseResult.ExpiringSoon(plan, daysLeft)
                } else {
                    return LicenseResult.Valid(plan, daysLeft)
                }
            }
            
            LicenseResult.Valid(plan, null)
        } catch (e: io.jsonwebtoken.ExpiredJwtException) {
            LicenseResult.Expired
        } catch (e: io.jsonwebtoken.security.SignatureException) {
            LicenseResult.Tampered
        } catch (e: Exception) {
            LicenseResult.Invalid
        }
    }
}
