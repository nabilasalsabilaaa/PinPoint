package com.example.pinpoint

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.LaporanResponse
import com.example.pinpoint.network.NotifikasiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardActivity : AppCompatActivity() {

    private var isSupported = false
    private var supportCount = 24
    private var backPressedTime: Long = 0
    private var featuredReportId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Tampilkan nama pengguna dari sesi login jika ada
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val savedName = intent.getStringExtra("EXTRA_NAME") ?: sharedPreferences.getString("KEY_NAME", null)
        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        if (!savedName.isNullOrBlank()) {
            tvUserName.text = savedName
        } else {
            tvUserName.text = "Pengguna"
        }

        // Sinkronkan Notifikasi & Badge dari Database
        val token = sharedPreferences.getString("KEY_TOKEN", null)
        val tvTopBadgeCount = findViewById<TextView>(R.id.tvTopBadgeCount)
        if (!token.isNullOrEmpty()) {
            ApiClient.instance.getNotifications("Bearer $token").enqueue(object : Callback<List<NotifikasiResponse>> {
                override fun onResponse(call: Call<List<NotifikasiResponse>>, response: Response<List<NotifikasiResponse>>) {
                    if (response.isSuccessful) {
                        val notifs = response.body() ?: emptyList()
                        val unread = notifs.count { it.sudahDibaca == false }
                        if (unread > 0) {
                            tvTopBadgeCount.visibility = View.VISIBLE
                            tvTopBadgeCount.text = unread.toString()
                        } else {
                            tvTopBadgeCount.visibility = View.GONE
                        }
                    }
                }
                override fun onFailure(call: Call<List<NotifikasiResponse>>, t: Throwable) {}
            })
        }

        // Sinkronkan Laporan Unggulan dari Database
        val tvReportTitle = findViewById<TextView>(R.id.tvReportTitle)
        val tvReportLocation = findViewById<TextView>(R.id.tvReportLocation)

        ApiClient.instance.getReportsForMap().enqueue(object : Callback<List<LaporanResponse>> {
            override fun onResponse(call: Call<List<LaporanResponse>>, response: Response<List<LaporanResponse>>) {
                if (response.isSuccessful) {
                    val reports = response.body() ?: emptyList()
                    if (reports.isNotEmpty()) {
                        val latest = reports.last()
                        featuredReportId = latest.id
                        tvReportTitle.text = latest.judul ?: "Laporan Fasilitas Publik"
                        tvReportLocation.text = latest.alamat ?: "Makassar, Sulawesi Selatan"
                    }
                }
            }
            override fun onFailure(call: Call<List<LaporanResponse>>, t: Throwable) {}
        })

        // Search Bar & Filter
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val btnFilter = findViewById<ImageView>(R.id.btnFilter)
        btnFilter.setOnClickListener {
            Toast.makeText(this, "Filter kategori & status dipilih", Toast.LENGTH_SHORT).show()
        }

        // Tombol Notifikasi Atas -> Buka NotifikasiActivity
        findViewById<View>(R.id.btnTopNotif).setOnClickListener {
            val intent = Intent(this, NotifikasiActivity::class.java)
            startActivity(intent)
        }

        // Setup Filter Chips
        setupFilterChips()

        // Kontrol Zoom Peta
        findViewById<View>(R.id.btnZoomIn).setOnClickListener {
            Toast.makeText(this, "Perbesar Peta", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btnZoomOut).setOnClickListener {
            Toast.makeText(this, "Perkecil Peta", Toast.LENGTH_SHORT).show()
        }

        // CTA Banner: Buat Laporan Cepat -> Pindah ke FormPelaporanActivity
        findViewById<Button>(R.id.btnQuickReport).setOnClickListener {
            val intent = Intent(this, FormPelaporanActivity::class.java)
            startActivity(intent)
        }

        // Interaksi Tombol Dukung (Toggle Support)
        val btnSupport = findViewById<Button>(R.id.btnSupport)
        val tvSupportCount = findViewById<TextView>(R.id.tvSupportCount)

        btnSupport.setOnClickListener {
            if (!isSupported) {
                isSupported = true
                supportCount++
                btnSupport.text = "✓ Didukung"
                btnSupport.setBackgroundResource(R.drawable.bg_btn_rounded_blue)
                btnSupport.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                Toast.makeText(this, "Terima kasih telah mendukung laporan ini!", Toast.LENGTH_SHORT).show()
            } else {
                isSupported = false
                supportCount--
                btnSupport.text = "👍 Dukung"
                btnSupport.setBackgroundResource(R.drawable.bg_btn_rounded_blue_soft)
                btnSupport.setTextColor(android.graphics.Color.parseColor("#0B58CA"))
                Toast.makeText(this, "Dukungan dibatalkan", Toast.LENGTH_SHORT).show()
            }
            tvSupportCount.text = "$supportCount Warga Mendukung"
        }

        // Tombol Lihat Detail Laporan -> Pindah ke DetailLaporanActivity dengan ID Database
        findViewById<Button>(R.id.btnViewDetail).setOnClickListener {
            val intent = Intent(this, DetailLaporanActivity::class.java).apply {
                featuredReportId?.let { putExtra("EXTRA_REPORT_ID", it) }
            }
            startActivity(intent)
        }

        // Setup Navigasi Bawah (Bottom Navigation Bar)
        setupBottomNavigation()

        // Handler Tombol Kembali (Double tap to exit / toast)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finishAffinity()
                } else {
                    Toast.makeText(this@DashboardActivity, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
                    backPressedTime = System.currentTimeMillis()
                }
            }
        })
    }

    private fun setupFilterChips() {
        val chipAll = findViewById<TextView>(R.id.chipAll)
        val chipPothole = findViewById<TextView>(R.id.chipPothole)
        val chipStreetlight = findViewById<TextView>(R.id.chipStreetlight)
        val chipDrainage = findViewById<TextView>(R.id.chipDrainage)

        val chips = listOf(chipAll, chipPothole, chipStreetlight, chipDrainage)

        fun selectChip(selected: TextView) {
            for (chip in chips) {
                if (chip == selected) {
                    chip.setBackgroundResource(R.drawable.bg_chip_blue_active)
                    chip.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip_inactive)
                    chip.setTextColor(android.graphics.Color.parseColor("#334155"))
                }
            }
        }

        chipAll.setOnClickListener { selectChip(chipAll) }
        chipPothole.setOnClickListener { selectChip(chipPothole) }
        chipStreetlight.setOnClickListener { selectChip(chipStreetlight) }
        chipDrainage.setOnClickListener { selectChip(chipDrainage) }
    }

    private fun setupBottomNavigation() {
        // 1. Beranda
        findViewById<View>(R.id.navUserHome).setOnClickListener {
            // Sudah berada di Beranda
        }

        // 2. Riwayat -> Buka RiwayatActivity
        findViewById<View>(R.id.navUserHistory).setOnClickListener {
            val intent = Intent(this, RiwayatActivity::class.java)
            startActivity(intent)
        }

        // 3. Tombol Tengah: (+) Lapor -> Buka Form Pelaporan
        findViewById<View>(R.id.navUserReportCenter).setOnClickListener {
            val intent = Intent(this, FormPelaporanActivity::class.java)
            startActivity(intent)
        }

        // 4. Notifikasi -> Buka NotifikasiActivity
        findViewById<View>(R.id.navUserNotification).setOnClickListener {
            val intent = Intent(this, NotifikasiActivity::class.java)
            startActivity(intent)
        }

        // 5. Profil -> Buka ProfilActivity
        findViewById<View>(R.id.navUserProfile).setOnClickListener {
            val intent = Intent(this, ProfilActivity::class.java)
            startActivity(intent)
        }
    }
}
