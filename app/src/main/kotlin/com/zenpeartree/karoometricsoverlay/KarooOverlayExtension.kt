package com.zenpeartree.karoometricsoverlay

import android.util.Log
import io.hammerhead.karooext.extension.KarooExtension

class KarooOverlayExtension : KarooExtension("karoo-obs-overlay", "1") {

    companion object {
        private const val TAG = "KarooOverlayExt"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Extension created — use the app to start the overlay server")
    }
}
