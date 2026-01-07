package org.openfamilycompass.android

import android.app.Application
import com.google.firebase.FirebaseApp

class OpenFamilyCompassApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}