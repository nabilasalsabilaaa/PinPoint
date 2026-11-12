package com.example.pinpoint

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.pinpoint.admin.AdminDashboardActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- PENGECEKAN SESI & PEMBATASAN ROLE ---
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("KEY_IS_LOGGED_IN", false)

        if (isLoggedIn) {
            val idRole = sharedPreferences.getLong("KEY_ID_ROLE", -1L)
            val roleStr = (sharedPreferences.getString("KEY_ROLE", "") ?: "").uppercase()
            val isAdmin = idRole == 1L || roleStr == "ADMIN" || roleStr == "ROLE_ADMIN"

            val targetIntent = if (isAdmin) {
                Intent(this, AdminDashboardActivity::class.java)
            } else {
                Intent(this, DashboardActivity::class.java)
            }
            startActivity(targetIntent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val imageView = findViewById<ImageView>(R.id.ivIllustration)
        Glide.with(this)
            .load("https://storage.googleapis.com/tagjs-prod.appspot.com/v1/gJiWam6g8N/8u89x0dh_expires_30_days.png")
            .into(imageView)

        val buttonLogin = findViewById<View>(R.id.btnWrapperLogin)
        buttonLogin.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }

        val buttonSignUp = findViewById<View>(R.id.btnWrapperSignUp)
        buttonSignUp.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }
    }
}
