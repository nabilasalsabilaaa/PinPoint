package com.example.pinpoint

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inisialisasi tombol logout
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // 1. Logika ketika Tombol Logout ditekan
        // Logika ketika Tombol Logout ditekan
        btnLogout.setOnClickListener {

            // --- HAPUS SESI / TOKEN SAAT LOGOUT ---
            val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.clear() // Menghapus semua data session yang tersimpan
            editor.apply()

            // Pindah kembali ke halaman Login & hapus riwayat activity
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // 2. Mencegah Tombol Kembali (Back Button) HP agar tidak bisa keluar dashboard
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@DashboardActivity, "Silakan tekan tombol Logout untuk keluar", Toast.LENGTH_SHORT).show()
            }
        })
    }
}