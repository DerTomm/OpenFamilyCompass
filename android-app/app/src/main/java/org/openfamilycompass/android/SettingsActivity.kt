package org.openfamilycompass.android

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var editServerUrl: EditText
    private lateinit var buttonSave: Button
    private lateinit var buttonReset: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Setup ActionBar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.settings_title)
        }

        // Initialize components
        sessionManager = SessionManager(this)
        editServerUrl = findViewById(R.id.editServerUrl)
        buttonSave = findViewById(R.id.buttonSave)
        buttonReset = findViewById(R.id.buttonReset)

        // Load current URL
        editServerUrl.setText(sessionManager.getServerUrl())

        // Setup buttons
        buttonSave.setOnClickListener {
            saveSettings()
        }

        buttonReset.setOnClickListener {
            resetToDefault()
        }
    }

    private fun saveSettings() {
        val url = editServerUrl.text.toString().trim()

        // Validate URL
        if (url.isEmpty()) {
            editServerUrl.error = getString(R.string.error_url_empty)
            return
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            editServerUrl.error = getString(R.string.error_url_invalid)
            return
        }

        // Remove trailing slash
        val cleanUrl = url.trimEnd('/')
        
        // Prüfe, ob sich die URL geändert hat
        val oldUrl = sessionManager.getServerUrl()
        val urlChanged = oldUrl != cleanUrl

        // Save URL
        sessionManager.saveServerUrl(cleanUrl)
        
        // Wenn URL geändert wurde, Session löschen und MainActivity neu laden
        if (urlChanged) {
            sessionManager.clearSession()
            Toast.makeText(this, getString(R.string.toast_settings_saved), Toast.LENGTH_SHORT).show()
            
            // Setze Result, damit MainActivity neu lädt
            setResult(RESULT_OK)
        } else {
            Toast.makeText(this, getString(R.string.toast_settings_saved), Toast.LENGTH_SHORT).show()
        }

        // Go back
        finish()
    }

    private fun resetToDefault() {
        editServerUrl.setText(SessionManager.DEFAULT_SERVER_URL)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
