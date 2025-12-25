package com.family.kidschores

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager

class SessionManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val cookieManager: CookieManager = CookieManager.getInstance()
    
    companion object {
        private const val PREFS_NAME = "KidsChoresPrefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_SESSION_COOKIE = "session_cookie"
        private const val KEY_LAST_LOGIN = "last_login"
        const val DEFAULT_SERVER_URL = "http://192.168.178.100:8080"
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
        
        // Speichere Cookies
        val cookies = cookieManager.getCookie(getServerUrl())
        if (cookies != null) {
            prefs.edit().putString(KEY_SESSION_COOKIE, cookies).apply()
        }
    }
    
    /**
     * Stellt gespeicherte Cookies wieder her
     */
    fun restoreCookies() {
        val cookies = prefs.getString(KEY_SESSION_COOKIE, null)
        if (cookies != null) {
            val serverUrl = getServerUrl()
            // Parse und setze jedes Cookie einzeln
            cookies.split(";").forEach { cookie ->
                val trimmedCookie = cookie.trim()
                if (trimmedCookie.isNotEmpty()) {
                    cookieManager.setCookie(serverUrl, trimmedCookie)
                }
            }
            cookieManager.flush()
        }
    }
    
    /**
     * Prüft, ob eine gültige Session existiert
     */
    fun hasValidSession(): Boolean {
        val username = prefs.getString(KEY_USERNAME, null)
        val lastLogin = prefs.getLong(KEY_LAST_LOGIN, 0)
        val cookies = prefs.getString(KEY_SESSION_COOKIE, null)
        
        // Session ist gültig wenn:
        // - Username vorhanden
        // - Cookies vorhanden
        // - Login nicht älter als 30 Tage
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
        val isRecentLogin = (System.currentTimeMillis() - lastLogin) < thirtyDaysInMillis
        
        return !username.isNullOrEmpty() && !cookies.isNullOrEmpty() && isRecentLogin
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
            
            // Aktualisiere Cookies
            val cookies = cookieManager.getCookie(getServerUrl())
            if (cookies != null) {
                prefs.edit().putString(KEY_SESSION_COOKIE, cookies).apply()
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
        
        // Lösche alle Cookies
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
    }
    
    /**
     * Prüft, ob bereits eine Server-URL konfiguriert wurde
     */
    fun isServerConfigured(): Boolean {
        val url = getServerUrl()
        return url != DEFAULT_SERVER_URL || prefs.contains(KEY_SERVER_URL)
    }
}
