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

                // Nach erfolgreichem Login: Session speichern
                if (url?.contains("/dashboard") == true || url?.contains("/perform_login") == true) {
                    // Warte kurz, damit Cookies gesetzt werden
                    webView.postDelayed({
                        detectAndSaveSession()
                    }, 500)
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
        // Versuche Benutzernamen aus der Seite zu extrahieren
        webView.evaluateJavascript(
            "(function() { " +
            "  var authElement = document.querySelector('[sec\\\\:authentication=\"name\"]');" +
            "  return authElement ? authElement.textContent.trim() : null;" +
            "})();"
        ) { result ->
            val username = result?.trim('"')
            if (!username.isNullOrEmpty() && username != "null") {
                sessionManager.saveSession(username)
                runOnUiThread {
                    Toast.makeText(this, "Angemeldet als $username", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_refresh -> {
                webView.reload()
                true
            }
            R.id.action_logout -> {
                showLogoutConfirmation()
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
