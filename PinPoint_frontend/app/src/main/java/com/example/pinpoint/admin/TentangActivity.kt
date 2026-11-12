package com.example.pinpoint.admin

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.pinpoint.R

class TentangActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tentang)

        findViewById<ImageView>(R.id.btnBackTentang).setOnClickListener {
            finish()
        }
    }
}
