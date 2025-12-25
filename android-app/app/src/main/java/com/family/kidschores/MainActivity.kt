package com.family.kidschores

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var sessionManager: SessionManager
    private var sessionAlreadyDetected = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize components
        sessionManager = SessionManager(this)
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Setup Toolbar as ActionBar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(false)
        }
        
        // Zeige Username wenn Session existiert
        updateToolbarWithUsername()

        // Configure WebView
        setupWebView()

        // Setup SwipeRefresh
        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        // Check if server is configured
        if (!sessionManager.isServerConfigured()) {
            showServerConfigDialog()
        } else {
            loadWebApp()
        }
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

        // Cookie Manager
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

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
                swipeRefreshLayout.isRefreshing = false

                // Verstecke Web-Navbar in der App
                hideWebNavbar()

                // Nach erfolgreichem Login: Session speichern
                if (url?.contains("/dashboard") == true || url?.contains("/perform_login") == true) {
                    // Nur einmal pro App-Start Session detektieren
                    if (!sessionAlreadyDetected) {
                        sessionAlreadyDetected = true
                        // Warte kurz, damit Cookies gesetzt werden
                        webView.postDelayed({
                            detectAndSaveSession()
                        }, 500)
                    }
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    showError("Fehler beim Laden der Seite: ${error?.description}")
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
        
        // Stelle Cookies wieder her, wenn Session vorhanden
        if (sessionManager.hasValidSession()) {
            sessionManager.restoreCookies()
            // Session aktualisieren
            sessionManager.refreshSession()
            // Direkt zum Dashboard
            webView.loadUrl("$serverUrl/dashboard")
        } else {
            // Zur Login-Seite
            webView.loadUrl("$serverUrl/login")
        }
    }

    private fun detectAndSaveSession() {
        // Extrahiere Access Token aus LocalStorage
        webView.evaluateJavascript(
            """
            (function() {
                return localStorage.getItem('app_access_token');
            })();
            """.trimIndent()
        ) { tokenResult ->
            val token = tokenResult?.trim('"')
            
            if (!token.isNullOrEmpty() && token != "null") {
                // Dekodiere Token und extrahiere Rolle & Username
                val username = JwtDecoder.extractUsername(token)
                val role = JwtDecoder.extractRole(token)
                
                if (!username.isNullOrEmpty()) {
                    sessionManager.saveSession(username, role)
                    runOnUiThread {
                        Toast.makeText(
                            this, 
                            "Angemeldet als $username${if (role != null) " ($role)" else ""}", 
                            Toast.LENGTH_SHORT
                        ).show()
                        // Menü aktualisieren nach Login
                        invalidateOptionsMenu()
                        // Username in Toolbar anzeigen
                        updateToolbarWithUsername()
                    }
                }
            } else {
                // Fallback: Versuche Username aus DOM zu extrahieren
                webView.evaluateJavascript(
                    "(function() { " +
                    "  var authElement = document.querySelector('[sec\\\\:authentication=\"name\"]');" +
                    "  return authElement ? authElement.textContent.trim() : null;" +
                    "})();"
                ) { result ->
                    val username = result?.trim('"')
                    if (!username.isNullOrEmpty() && username != "null") {
                        sessionManager.saveSession(username, null)
                        runOnUiThread {
                            Toast.makeText(this, "Angemeldet als $username", Toast.LENGTH_SHORT).show()
                            invalidateOptionsMenu()
                            updateToolbarWithUsername()
                        }
                    }
                }
            }
        }
    }

    private fun updateToolbarWithUsername() {
        val username = sessionManager.getUsername()
        // Update Menu Item Title mit Username
        invalidateOptionsMenu()
    }

    private fun showProfilePopupMenu(item: MenuItem) {
        // Verwende Toolbar als Anchor für das Popup
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        val popup = PopupMenu(this, toolbar)
        popup.menuInflater.inflate(R.menu.menu_profile_popup, popup.menu)
        
        // Verstecke "Benutzereinstellungen" und "Abmelden" wenn nicht eingeloggt
        val hasSession = sessionManager.hasValidSession()
        popup.menu.findItem(R.id.popup_profile_settings)?.isVisible = hasSession
        popup.menu.findItem(R.id.popup_logout)?.isVisible = hasSession
        
        popup.setOnMenuItemClickListener { menuItem ->
            val serverUrl = sessionManager.getServerUrl()
            when (menuItem.itemId) {
                R.id.popup_profile_settings -> {
                    val profileUrl = "$serverUrl/profile/settings"
                    android.util.Log.d("MainActivity", "Loading profile: $profileUrl")
                    webView.loadUrl(profileUrl)
                    true
                }
                R.id.popup_app_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.popup_logout -> {
                    showLogoutConfirmation()
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }

    private fun hideWebNavbar() {
        webView.evaluateJavascript(
            """
            (function() {
                // Verstecke Bootstrap Navbar
                var navbar = document.querySelector('.navbar');
                if (navbar) {
                    navbar.style.display = 'none';
                }
                
                // Verstecke alle nav-Elemente
                var navElements = document.querySelectorAll('nav');
                navElements.forEach(function(nav) {
                    nav.style.display = 'none';
                });
                
                // Adjustiere Body Padding (falls Navbar fixed war)
                document.body.style.paddingTop = '0';
                
                return 'navbar hidden';
            })();
            """.trimIndent(), null
        )
    }

    private fun showServerConfigDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_server_config_title))
            .setMessage(getString(R.string.dialog_server_config_message))
            .setPositiveButton(getString(R.string.dialog_button_settings)) { _, _ ->
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            .setCancelable(false)
            .show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        updateMenuForRole(menu)
        
        // Setup Custom Profile View mit Username
        val profileItem = menu?.findItem(R.id.action_profile_menu)
        val actionView = profileItem?.actionView
        
        if (actionView != null) {
            val avatarImageView = actionView.findViewById<android.widget.ImageView>(R.id.profile_avatar)
            val usernameTextView = actionView.findViewById<android.widget.TextView>(R.id.profile_username)
            val username = sessionManager.getUsername()
            val hasSession = sessionManager.hasValidSession()
            
            // Avatar und Username nur anzeigen wenn eingeloggt
            if (hasSession && !username.isNullOrEmpty()) {
                avatarImageView?.visibility = View.VISIBLE
                usernameTextView?.visibility = View.VISIBLE
                usernameTextView?.text = username
            } else {
                avatarImageView?.visibility = View.GONE
                usernameTextView?.visibility = View.GONE
            }
            
            // Click Listener für Custom View
            actionView.setOnClickListener {
                onOptionsItemSelected(profileItem)
            }
        }
        
        return true
    }

    private fun updateMenuForRole(menu: Menu?) {
        if (menu == null) return
        
        val role = sessionManager.getUserRole()
        
        // Verstecke alle Gruppen zuerst
        menu.setGroupVisible(R.id.menu_group_admin, false)
        menu.setGroupVisible(R.id.menu_group_parent, false)
        menu.setGroupVisible(R.id.menu_group_child, false)
        
        // Zeige Gruppen basierend auf Rolle
        when (role) {
            "ROLE_ADMIN" -> {
                menu.setGroupVisible(R.id.menu_group_admin, true)
                menu.setGroupVisible(R.id.menu_group_parent, true) // Admin kann alles
            }
            "ROLE_PARENT" -> {
                menu.setGroupVisible(R.id.menu_group_parent, true)
            }
            "ROLE_CHILD" -> {
                menu.setGroupVisible(R.id.menu_group_child, true)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val serverUrl = sessionManager.getServerUrl()
        
        return when (item.itemId) {
            // Profil Avatar - Zeige Popup Menü
            R.id.action_profile_menu -> {
                showProfilePopupMenu(item)
                true
            }
            // Admin Items
            R.id.action_admin_users -> {
                webView.loadUrl("$serverUrl/admin/users")
                true
            }
            R.id.action_admin_families -> {
                webView.loadUrl("$serverUrl/admin/families")
                true
            }
            // Parent Items
            R.id.action_parent_chores -> {
                webView.loadUrl("$serverUrl/parent/chores")
                true
            }
            R.id.action_parent_rewards -> {
                webView.loadUrl("$serverUrl/parent/rewards")
                true
            }
            R.id.action_parent_children -> {
                webView.loadUrl("$serverUrl/parent/children")
                true
            }
            // Child Items
            R.id.action_child_chores -> {
                webView.loadUrl("$serverUrl/child/dashboard")
                true
            }
            R.id.action_child_history -> {
                webView.loadUrl("$serverUrl/child/history")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
                
                // 6. Update Toolbar
                updateToolbarWithUsername()
                invalidateOptionsMenu()
                
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

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}
