package com.example.vpnapp

import android.app.Application
import com.google.android.gms.ads.MobileAds

class VpnApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            MobileAds.initialize(this) {}
        } catch (_: Exception) {
            // Test ortamında reklam init başarısız olabilir; uygulamayı düşürme
        }
    }
}
