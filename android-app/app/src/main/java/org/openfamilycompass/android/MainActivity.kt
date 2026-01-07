package org.openfamilycompass.android

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.webkit.*
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var sessionManager: SessionManager
    private var sessionAlreadyDetected = false
    private lateinit var fabMain: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var fabSettings: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var fabReload: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var fabMenuLayout: android.widget.LinearLayout
    private lateinit var fabOverlay: View
    private var isFabMenuOpen = false
    private lateinit var prefs: SharedPreferences
    private lateinit var deviceId: String
    
    // Activity Result Launcher für Settings
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Server-URL wurde geändert, lade neue URL
            android.util.Log.d("MainActivity", "Server URL changed, reloading...")
            sessionAlreadyDetected = false
            loadWebApp()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize components
        sessionManager = SessionManager(this)
        prefs = getSharedPreferences("OpenFamilyCompass", MODE_PRIVATE)
        deviceId = getOrCreateDeviceId()
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        fabMain = findViewById(R.id.fabMain)
        fabSettings = findViewById(R.id.fabSettings)
        fabReload = findViewById(R.id.fabReload)
        fabMenuLayout = findViewById(R.id.fabMenuLayout)
        fabOverlay = findViewById(R.id.fabOverlay)
        
        // Setup FAB Menu
        setupFabMenu()

        // Configure WebView
        setupWebView()

        // Check if server is configured
        if (!sessionManager.isServerConfigured()) {
            showServerConfigDialog()
        } else {
            loadWebApp()
            // FCM token will be sent after successful login
        }
    }

    private fun setupFabMenu() {
        // Main FAB öffnet/schließt das Menü
        fabMain.setOnClickListener {
            toggleFabMenu()
        }
        
        // Overlay schließt das Menü
        fabOverlay.setOnClickListener {
            closeFabMenu()
        }
        
        // Reload FAB
        fabReload.setOnClickListener {
            webView.reload()
            closeFabMenu()
        }
        
        // Settings FAB
        fabSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            settingsLauncher.launch(intent)
            closeFabMenu()
        }
    }
    
    private fun toggleFabMenu() {
        if (isFabMenuOpen) {
            closeFabMenu()
        } else {
            openFabMenu()
        }
    }
    
    private fun openFabMenu() {
        isFabMenuOpen = true
        fabMenuLayout.visibility = View.VISIBLE
        fabOverlay.visibility = View.VISIBLE
        
        // Animationen
        fabMain.animate().rotation(45f).setDuration(200).start()
        fabMenuLayout.alpha = 0f
        fabMenuLayout.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(200)
            .start()
    }
    
    private fun closeFabMenu() {
        isFabMenuOpen = false
        
        // Animationen
        fabMain.animate().rotation(0f).setDuration(200).start()
        fabMenuLayout.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                fabMenuLayout.visibility = View.GONE
                fabOverlay.visibility = View.GONE
            }
            .start()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            setSupportZoom(false)
        }

        // Cookie Manager - Persistente Speicherung aktivieren
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        // Wichtig: Persistente Speicherung aktivieren
        cookieManager.flush()

        // WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                
                // Nur URLs vom konfigurierten Server laden
                return if (url.startsWith(sessionManager.getServerUrl())) {
                    false // In WebView laden
                } else {
                    // Externe URLs im Browser öffnen
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    true
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE

                // Auf der Login-Seite: "Angemeldet bleiben" automatisch aktivieren
                if (url?.contains("/login") == true) {
                    webView.postDelayed({
                        webView.evaluateJavascript(
                            """
                            (function() {
                                var rememberMeCheckbox = document.getElementById('remember-me');
                                if (rememberMeCheckbox && !rememberMeCheckbox.checked) {
                                    rememberMeCheckbox.checked = true;
                                }
                            })();
                            """.trimIndent()
                        , null)
                    }, 300)
                }

                // Nach erfolgreichem Login: Session speichern
                if (url?.contains("/dashboard") == true || url?.contains("/perform_login") == true) {
                    // Nur einmal pro App-Start Session detektieren
                    if (!sessionAlreadyDetected) {
                        sessionAlreadyDetected = true
                        // Warte länger, damit Remember-Me Cookie vom Server gesetzt wird
                        webView.postDelayed({
                            detectAndSaveSession()
                        }, 1500)
                    }
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    showError(getString(R.string.error_page_load, error?.description ?: ""))
                }
            }
        }

        // WebChromeClient für Fortschritt
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
            }
        }
    }

    private fun loadWebApp() {
        val serverUrl = sessionManager.getServerUrl()
        
        android.util.Log.d("MainActivity", "Loading web app from: $serverUrl")
        
        // Stelle Cookies wieder her, wenn Session vorhanden
        if (sessionManager.hasValidSession()) {
            android.util.Log.d("MainActivity", "Valid session found, restoring cookies and loading dashboard")
            sessionManager.restoreCookies()
            // Session aktualisieren
            sessionManager.refreshSession()
            // Send FCM token for existing session
            getFcmToken()
            // Direkt zum Dashboard
            webView.loadUrl("$serverUrl/dashboard")
        } else {
            android.util.Log.d("MainActivity", "No valid session, loading login page")
            // Zur Login-Seite
            webView.loadUrl("$serverUrl/login")
        }
    }

    private fun detectAndSaveSession() {
        android.util.Log.d("MainActivity", "detectAndSaveSession called")
        
        // Extrahiere Access Token aus LocalStorage
        webView.evaluateJavascript(
            """
            (function() {
                return localStorage.getItem('app_access_token');
            })();
            """.trimIndent()
        ) { tokenResult ->
            val token = tokenResult?.trim('"')
            android.util.Log.d("MainActivity", "Token from localStorage: $token")
            
            if (!token.isNullOrEmpty() && token != "null") {
                // Dekodiere Token und extrahiere Rolle & Username
                val username = JwtDecoder.extractUsername(token)
                val role = JwtDecoder.extractRole(token)
                
                android.util.Log.d("MainActivity", "Extracted username: $username, role: $role")
                
                if (!username.isNullOrEmpty()) {
                    sessionManager.saveSession(username, role)
                    runOnUiThread {
                        val message = if (role != null) {
                            getString(R.string.toast_logged_in_as_with_role, username, role)
                        } else {
                            getString(R.string.toast_logged_in_as, username)
                        }
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                    // Send FCM token after successful login
                    getFcmToken()
                } else {
                    android.util.Log.d("MainActivity", "Username is empty, trying fallback")
                    tryFallbackSession()
                }
            } else {
                android.util.Log.d("MainActivity", "No token found, trying fallback")
                tryFallbackSession()
            }
        }
    }
    
    private fun tryFallbackSession() {
        // Fallback 1: Versuche Username aus DOM zu extrahieren
        webView.evaluateJavascript(
            "(function() { " +
            "  var authElement = document.querySelector('[sec\\\\:authentication=\"name\"]');" +
            "  return authElement ? authElement.textContent.trim() : null;" +
            "})();"
        ) { result ->
            val username = result?.trim('"')
            android.util.Log.d("MainActivity", "Username from DOM: $username")
            
            if (!username.isNullOrEmpty() && username != "null") {
                sessionManager.saveSession(username, null)
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.toast_logged_in_as, username), Toast.LENGTH_SHORT).show()
                }
                // Send FCM token after successful login
                getFcmToken()
            } else {
                // Fallback 2: Einfach Cookies speichern mit Dummy-Username
                android.util.Log.d("MainActivity", "Using dummy session for cookie storage")
                val cookieManager = CookieManager.getInstance()
                cookieManager.flush()
                val cookies = cookieManager.getCookie(sessionManager.getServerUrl())
                
                if (!cookies.isNullOrEmpty()) {
                    android.util.Log.d("MainActivity", "Cookies found, saving session with dummy user")
                    sessionManager.saveSession("user", null)
                    runOnUiThread {
                        Toast.makeText(this, getString(R.string.toast_logged_in_as, "user"), Toast.LENGTH_SHORT).show()
                    }
                    // Send FCM token after successful login
                    getFcmToken()
                } else {
                    android.util.Log.d("MainActivity", "No cookies found at all")
                }
            }
        }
    }

    private fun showServerConfigDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_server_config_title))
            .setMessage(getString(R.string.dialog_server_config_message))
            .setPositiveButton(getString(R.string.dialog_button_settings)) { _, _ ->
                val intent = Intent(this, SettingsActivity::class.java)
                settingsLauncher.launch(intent)
            }
            .setCancelable(false)
            .show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_logout_title))
            .setMessage(getString(R.string.dialog_logout_message))
            .setPositiveButton(getString(R.string.dialog_button_logout)) { _, _ ->
                performLogout()
            }
            .setNegativeButton(getString(R.string.dialog_button_cancel), null)
            .show()
    }

    private fun performLogout() {
        // 1. Lösche LocalStorage in WebView (dort sind die JWT Tokens)
        webView.evaluateJavascript(
            """
            (function() {
                localStorage.clear();
                sessionStorage.clear();
                return 'cleared';
            })();
            """.trimIndent()
        ) { result ->
            // 2. Rufe Server-Logout auf
            webView.loadUrl("${sessionManager.getServerUrl()}/logout")
            
            // 3. Warte kurz, dann lokale Session löschen
            webView.postDelayed({
                sessionManager.clearSession()
                webView.clearCache(true)
                webView.clearHistory()
                
                // 4. Zurück zur Login-Seite
                loadWebApp()
                
                // 5. Reset session detection flag
                sessionAlreadyDetected = false
                
                Toast.makeText(this, getString(R.string.toast_logged_out), Toast.LENGTH_SHORT).show()
            }, 500)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        
        // Refresh session wenn App wieder im Vordergrund
        if (sessionManager.hasValidSession()) {
            sessionManager.refreshSession()
        }
    }

    private fun getFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                android.util.Log.w("MainActivity", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            android.util.Log.d("MainActivity", "FCM Token: $token")
            sendTokenToServer(token)
        }
    }

    private fun sendTokenToServer(token: String) {
        // Verwende WebView, um Token via JavaScript zu senden
        val serverUrl = sessionManager.getServerUrl()
        val jsCode = """
            // Send FCM token
            fetch('$serverUrl/notifications/fcm-token', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({deviceId: '$deviceId', token: '$token'}),
                credentials: 'include'
            }).then(response => {
                if (response.ok) {
                    console.log('FCM Token sent successfully');
                } else {
                    console.error('Failed to send FCM Token:', response.status);
                }
            }).catch(error => {
                console.error('Error sending FCM Token:', error);
            });
        """.trimIndent()

        webView.evaluateJavascript(jsCode, null)
    }

    private fun getOrCreateDeviceId(): String {
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        
        // Speichere aktuelle Cookies beim Verlassen der App
        val cookieManager = CookieManager.getInstance()
        cookieManager.flush()
        
        // Aktualisiere gespeicherte Cookies in SharedPreferences
        if (sessionManager.hasValidSession()) {
            sessionManager.refreshSession()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}
