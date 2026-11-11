package com.example.pinpoint

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- PENGECEKAN SESI (INGAT SAYA) ---
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("KEY_IS_LOGGED_IN", false)

        if (isLoggedIn) {
            // Jika sudah login, langsung lempar ke Dashboard & hancurkan MainActivity
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        // ------------------------------------

        setContentView(R.layout.activity_main)

        // Muat gambar ilustrasi
        val imageView = findViewById<ImageView>(R.id.ivIllustration)
        Glide.with(this)
            .load("https://storage.googleapis.com/tagjs-prod.appspot.com/v1/gJiWam6g8N/8u89x0dh_expires_30_days.png")
            .into(imageView)

        // Navigasi ke halaman Login
        val buttonLogin = findViewById<View>(R.id.btnWrapperLogin)
        buttonLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        // Navigasi ke halaman Register
        val buttonSignUp = findViewById<View>(R.id.btnWrapperSignUp)
        buttonSignUp.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }
    }
}