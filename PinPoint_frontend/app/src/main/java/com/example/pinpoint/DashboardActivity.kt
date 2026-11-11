package com.example.pinpoint

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val tvWelcomeName = findViewById<TextView>(R.id.tvWelcomeName)
        val tvWelcomeRole = findViewById<TextView>(R.id.tvWelcomeRole)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val name = sharedPreferences.getString("KEY_NAME", null)?.takeIf { it.isNotBlank() } ?: "Pengguna"
        val roleStr = sharedPreferences.getString("KEY_ROLE", "USER") ?: "USER"
        val idRole = sharedPreferences.getLong("KEY_ID_ROLE", 2L)

        val roleLabel = when {
            idRole == 3L || roleStr.contains("PEMERINTAH", ignoreCase = true) || roleStr.contains("DINAS", ignoreCase = true) ->
                "Pemerintah (Role 3)"
            idRole == 1L || roleStr.contains("ADMIN", ignoreCase = true) ->
                "Admin (Role 1)"
            else ->
                "User / Masyarakat (Role 2)"
        }

        tvWelcomeName.text = name
        tvWelcomeRole.text = "Role: $roleLabel"

        btnLogout.setOnClickListener {
            sharedPreferences.edit()
                .putBoolean("KEY_IS_LOGGED_IN", false)
                .remove("KEY_TOKEN")
                .apply()

            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@DashboardActivity, "Silakan tekan tombol Logout untuk keluar", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
