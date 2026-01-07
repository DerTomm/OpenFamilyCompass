package org.openfamilycompass.android

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager

class SessionManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val cookieManager: CookieManager = CookieManager.getInstance()
    
    init {
        // Aktiviere persistente Cookie-Speicherung
        cookieManager.setAcceptCookie(true)
    }
    
    companion object {
        private const val PREFS_NAME = "KidsChoresPrefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_SESSION_COOKIE = "session_cookie"
        private const val KEY_LAST_LOGIN = "last_login"
        const val DEFAULT_SERVER_URL = "https://my.openfamilycompass.url"
    }
    
    /**
     * Speichert die Server-URL
     */
    fun saveServerUrl(url: String) {
        prefs.edit().putString(KEY_SERVER_URL, url).apply()
    }
    
    /**
     * Gibt die gespeicherte Server-URL zurück
     */
    fun getServerUrl(): String {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }
    
    /**
     * Speichert Session-Informationen nach erfolgreichem Login
     */
    fun saveSession(username: String, role: String? = null) {
        prefs.edit().apply {
            putString(KEY_USERNAME, username)
            if (role != null) {
                putString(KEY_USER_ROLE, role)
            }
            putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
            apply()
        }
        
        // Speichere Cookies (inkl. Remember-Me Cookie)
        // Flush zuerst, um sicherzustellen dass alle Cookies geschrieben sind
        cookieManager.flush()
        
        val serverUrl = getServerUrl()
        val cookies = cookieManager.getCookie(serverUrl)
        if (cookies != null) {
            prefs.edit().putString(KEY_SESSION_COOKIE, cookies).apply()
            android.util.Log.d("SessionManager", "Saved cookies for $serverUrl: $cookies")
        } else {
            android.util.Log.d("SessionManager", "No cookies found for $serverUrl")
        }
    }
    
    /**
     * Stellt gespeicherte Cookies wieder her
     */
    fun restoreCookies() {
        val cookies = prefs.getString(KEY_SESSION_COOKIE, null)
        if (cookies != null) {
            val serverUrl = getServerUrl()
            android.util.Log.d("SessionManager", "Restoring cookies for: $serverUrl")
            android.util.Log.d("SessionManager", "Cookies to restore: $cookies")
            
            // Lösche alte Cookies zuerst
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
            
            // Parse und setze jedes Cookie einzeln
            cookies.split(";").forEach { cookie ->
                val trimmedCookie = cookie.trim()
                if (trimmedCookie.isNotEmpty()) {
                    cookieManager.setCookie(serverUrl, trimmedCookie)
                    android.util.Log.d("SessionManager", "Set cookie: $trimmedCookie")
                }
            }
            
            // Wichtig: Warte auf Completion
            cookieManager.flush()
            android.util.Log.d("SessionManager", "Cookies restored successfully")
        } else {
            android.util.Log.d("SessionManager", "No cookies to restore")
        }
    }
    
    /**
     * Prüft, ob eine gültige Session existiert
     */
    fun hasValidSession(): Boolean {
        val username = prefs.getString(KEY_USERNAME, null)
        val lastLogin = prefs.getLong(KEY_LAST_LOGIN, 0)
        val cookies = prefs.getString(KEY_SESSION_COOKIE, null)
        
        android.util.Log.d("SessionManager", "Checking session validity:")
        android.util.Log.d("SessionManager", "  Username: $username")
        android.util.Log.d("SessionManager", "  Last Login: $lastLogin")
        android.util.Log.d("SessionManager", "  Cookies present: ${!cookies.isNullOrEmpty()}")
        
        // Session ist gültig wenn:
        // - Username vorhanden
        // - Cookies vorhanden
        // - Login nicht älter als 30 Tage
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
        val isRecentLogin = (System.currentTimeMillis() - lastLogin) < thirtyDaysInMillis
        
        val isValid = !username.isNullOrEmpty() && !cookies.isNullOrEmpty() && isRecentLogin
        android.util.Log.d("SessionManager", "  Session valid: $isValid")
        
        return isValid
    }
    
    /**
     * Gibt den gespeicherten Benutzernamen zurück
     */
    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }
    
    /**
     * Gibt die gespeicherte Benutzer-Rolle zurück
     */
    fun getUserRole(): String? {
        return prefs.getString(KEY_USER_ROLE, null)
    }
    
    /**
     * Aktualisiert die Session (für Refresh)
     */
    fun refreshSession() {
        val username = getUsername()
        if (!username.isNullOrEmpty()) {
            prefs.edit().putLong(KEY_LAST_LOGIN, System.currentTimeMillis()).apply()
            
            // Aktualisiere Cookies (inkl. Remember-Me Cookie falls vorhanden)
            val cookies = cookieManager.getCookie(getServerUrl())
            if (cookies != null) {
                prefs.edit().putString(KEY_SESSION_COOKIE, cookies).apply()
                android.util.Log.d("SessionManager", "Refreshed cookies: $cookies")
            }
        }
    }
    
    /**
     * Löscht die Session (Logout)
     */
    fun clearSession() {
        prefs.edit().apply {
            remove(KEY_USERNAME)
            remove(KEY_USER_ROLE)
            remove(KEY_SESSION_COOKIE)
            remove(KEY_LAST_LOGIN)
            apply()
        }
        
        // Lösche alle Cookies (inkl. Remember-Me Cookie)
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
        
        android.util.Log.d("SessionManager", "Session and cookies cleared")
    }
    
    /**
     * Prüft, ob bereits eine Server-URL konfiguriert wurde
     */
    fun isServerConfigured(): Boolean {
        val url = getServerUrl()
        return url != DEFAULT_SERVER_URL || prefs.contains(KEY_SERVER_URL)
    }
}
