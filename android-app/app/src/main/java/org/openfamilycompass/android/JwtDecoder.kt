package org.openfamilycompass.android

import android.util.Base64
import org.json.JSONObject

/**
 * Einfacher JWT Token Decoder ohne externe Library
 */
object JwtDecoder {
    
    /**
     * Extrahiert die Rolle aus einem JWT Access Token
     * @return Rolle (z.B. "ROLE_ADMIN", "ROLE_PARENT", "ROLE_CHILD") oder null
     */
    fun extractRole(token: String?): String? {
        if (token.isNullOrEmpty()) return null
        
        return try {
            val payload = decodePayload(token)
            payload?.optString("role")?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extrahiert den Username aus einem JWT Token
     */
    fun extractUsername(token: String?): String? {
        if (token.isNullOrEmpty()) return null
        
        return try {
            val payload = decodePayload(token)
            payload?.optString("sub")?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Prüft ob Token abgelaufen ist
     */
    fun isExpired(token: String?): Boolean {
        if (token.isNullOrEmpty()) return true
        
        return try {
            val payload = decodePayload(token)
            val exp = payload?.optLong("exp") ?: return true
            val now = System.currentTimeMillis() / 1000
            exp < now
        } catch (e: Exception) {
            true
        }
    }
    
    /**
     * Dekodiert das Payload eines JWT Tokens
     */
    private fun decodePayload(token: String): JSONObject? {
        try {
            // JWT Format: header.payload.signature
            val parts = token.split(".")
            if (parts.size != 3) return null
            
            // Dekodiere Payload (zweiter Teil)
            val payloadBase64 = parts[1]
            val payloadJson = String(
                Base64.decode(payloadBase64, Base64.URL_SAFE or Base64.NO_WRAP)
            )
            
            return JSONObject(payloadJson)
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Gibt vollständiges Payload als JSON zurück (für Debugging)
     */
    fun getPayload(token: String?): JSONObject? {
        if (token.isNullOrEmpty()) return null
        return decodePayload(token)
    }
}
