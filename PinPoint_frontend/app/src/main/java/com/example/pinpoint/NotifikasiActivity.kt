package com.example.pinpoint

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.NotifikasiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotifikasiActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifikasi)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainNotifikasi)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Filter Chips Toggle
        val chipAll = findViewById<TextView>(R.id.chipNotifAll)
        val chipStatus = findViewById<TextView>(R.id.chipNotifStatus)
        val chipFollowup = findViewById<TextView>(R.id.chipNotifFollowup)
        val chips = listOf(chipAll, chipStatus, chipFollowup)

        for (chip in chips) {
            chip.setOnClickListener {
                for (c in chips) {
                    if (c == chip) {
                        c.setBackgroundResource(R.drawable.bg_chip_blue_active)
                        c.setTextColor(Color.WHITE)
                    } else {
                        c.setBackgroundResource(R.drawable.bg_chip_inactive)
                        c.setTextColor(Color.parseColor("#334155"))
                    }
                }
            }
        }

        // Sinkronkan Notifikasi dari Database
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val token = sharedPreferences.getString("KEY_TOKEN", null)
        if (!token.isNullOrEmpty()) {
            ApiClient.instance.getNotifications("Bearer $token").enqueue(object : Callback<List<NotifikasiResponse>> {
                override fun onResponse(call: Call<List<NotifikasiResponse>>, response: Response<List<NotifikasiResponse>>) {
                    if (response.isSuccessful) {
                        val notifs = response.body() ?: emptyList()
                        chipAll.text = "Semua (${notifs.size})"
                        chipStatus.text = "Status Laporan (${notifs.count { it.tipe == "STATUS" || it.tipe == null }})"
                        chipFollowup.text = "Tindak Lanjut (${notifs.count { it.tipe == "TINDAK_LANJUT" }})"
                    }
                }
                override fun onFailure(call: Call<List<NotifikasiResponse>>, t: Throwable) {}
            })
        }

        // Tombol Lihat Progress -> Buka Detail Laporan
        findViewById<Button>(R.id.btnNotifProgress1).setOnClickListener {
            val intent = Intent(this, DetailLaporanActivity::class.java)
            startActivity(intent)
        }

        // Setup Bottom Nav dengan Notifikasi Aktif
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        // Beranda Non-Aktif
        val ivHome = findViewById<ImageView>(R.id.ivNavUserHome)
        val tvHome = findViewById<TextView>(R.id.tvNavUserHome)
        ivHome.setColorFilter(Color.parseColor("#94A3B8"))
        tvHome.setTextColor(Color.parseColor("#94A3B8"))
        tvHome.setTypeface(null, Typeface.NORMAL)

        // Notifikasi Aktif (Warna Biru)
        val ivNotif = findViewById<ImageView>(R.id.ivNavUserNotification)
        val tvNotif = findViewById<TextView>(R.id.tvNavUserNotification)
        ivNotif.setColorFilter(Color.parseColor("#0B58CA"))
        tvNotif.setTextColor(Color.parseColor("#0B58CA"))
        tvNotif.setTypeface(null, Typeface.BOLD)

        // Event Navigasi
        findViewById<View>(R.id.navUserHome).setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        findViewById<View>(R.id.navUserHistory).setOnClickListener {
            val intent = Intent(this, RiwayatActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<View>(R.id.navUserReportCenter).setOnClickListener {
            val intent = Intent(this, FormPelaporanActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.navUserProfile).setOnClickListener {
            val intent = Intent(this, ProfilActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
