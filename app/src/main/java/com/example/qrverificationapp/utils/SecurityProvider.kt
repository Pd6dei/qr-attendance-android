package com.example.qrverificationapp.utils

import android.app.Application
import org.conscrypt.Conscrypt
import java.security.Security

class SecurityProvider : Application() {

    override fun onCreate() {
        super.onCreate()

        if (Security.getProvider("Conscrypt") == null) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }
    }
}

