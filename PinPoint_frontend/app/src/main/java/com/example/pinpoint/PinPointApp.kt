package com.example.pinpoint

import android.app.Application
import org.osmdroid.config.Configuration

class PinPointApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )

        Configuration.getInstance().userAgentValue =
            "PinPoint/1.0 (Android; contact: noreplay.pinpoint@gmail.com)"
    }
}