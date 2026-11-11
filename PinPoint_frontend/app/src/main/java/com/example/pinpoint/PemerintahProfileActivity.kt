package com.example.pinpoint

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

class PemerintahProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pemerintah_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val ivAvatar = findViewById<ImageView>(R.id.ivProfileAvatar)
        Glide.with(this)
            .load("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300")
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(ivAvatar)

        findViewById<View>(R.id.menuBiodata).setOnClickListener {
            Toast.makeText(this, "Membuka Biodata Diri Petugas Pemerintah...", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.menuSecurity).setOnClickListener {
            Toast.makeText(this, "Membuka Keamanan Akun & Sandi...", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.menuNotification).setOnClickListener {
            Toast.makeText(this, "Membuka Notifikasi & Peringatan SLA...", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.menuHelp).setOnClickListener {
            Toast.makeText(this, "Membuka Pusat Bantuan & SOP Pemerintah...", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.menuAbout).setOnClickListener {
            Toast.makeText(this, "Tentang Aplikasi LaporFasum v1.0.0 (Pemkot Makassar)", Toast.LENGTH_LONG).show()
        }

        findViewById<View>(R.id.btnLogout).setOnClickListener {
            showLogoutConfirmationDialog()
        }

        setupBottomNavigation()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Keluar dari Akun")
            .setMessage("Apakah Anda yakin ingin keluar dari sesi Petugas Pemerintah?")
            .setPositiveButton("Ya, Keluar") { _, _ ->
                Toast.makeText(this, "Berhasil keluar dari akun", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, Login::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupBottomNavigation() {
        val navHome = findViewById<View>(R.id.navPemerintahHome)
        val navMap = findViewById<View>(R.id.navPemerintahMap)
        val navHistory = findViewById<View>(R.id.navPemerintahHistory)
        val navProfile = findViewById<View>(R.id.navPemerintahProfile)

        val ivProfile = findViewById<ImageView>(R.id.ivNavPemerintahProfile)
        val tvProfile = findViewById<TextView>(R.id.tvNavPemerintahProfile)
        ivProfile?.setColorFilter(android.graphics.Color.parseColor("#003C90"))
        tvProfile?.setTextColor(android.graphics.Color.parseColor("#003C90"))
        tvProfile?.setTypeface(null, android.graphics.Typeface.BOLD)

        navHome?.setOnClickListener {
            startActivity(Intent(this, PemerintahDashboardActivity::class.java))
            finish()
        }

        navMap?.setOnClickListener {
            startActivity(Intent(this, PemerintahMapActivity::class.java))
            finish()
        }

        navHistory?.setOnClickListener {
            startActivity(Intent(this, PemerintahHistoryActivity::class.java))
            finish()
        }

        navProfile?.setOnClickListener {
            // Already here
        }
    }
}
