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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.LaporanResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RiwayatActivity : AppCompatActivity() {

    private var reportsList: List<LaporanResponse> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_riwayat)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRiwayat)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val tvFilterStatus = findViewById<TextView>(R.id.tvCurrentFilterStatus)
        val tvEmptyRiwayat = findViewById<TextView>(R.id.tvEmptyRiwayat)
        val cardReport1 = findViewById<View>(R.id.cardReport1)
        val tvTicket1 = findViewById<TextView>(R.id.tvTicket1)
        val tvStatusBadge1 = findViewById<TextView>(R.id.tvStatusBadge1)
        val tvTitle1 = findViewById<TextView>(R.id.tvTitle1)
        val tvAddress1 = findViewById<TextView>(R.id.tvAddress1)
        val btnTrackReport1 = findViewById<Button>(R.id.btnTrackReport1)

        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val token = sharedPreferences.getString("KEY_TOKEN", null)

        if (!token.isNullOrEmpty()) {
            ApiClient.instance.getMyReports("Bearer $token").enqueue(object : Callback<List<LaporanResponse>> {
                override fun onResponse(call: Call<List<LaporanResponse>>, response: Response<List<LaporanResponse>>) {
                    if (response.isSuccessful) {
                        reportsList = response.body() ?: emptyList()
                        tvFilterStatus.text = "Semua Status (${reportsList.size})"

                        if (reportsList.isNotEmpty()) {
                            tvEmptyRiwayat.visibility = View.GONE
                            cardReport1.visibility = View.VISIBLE
                            val first = reportsList.first()

                            tvTicket1.text = "#LP-${first.id}  •  ${first.tanggalLaporan ?: "Terkini"}"
                            tvTitle1.text = first.judul ?: "Laporan Pengaduan"
                            tvAddress1.text = first.alamat ?: "Makassar, Sulawesi Selatan"

                            when (first.idStatus) {
                                1L -> {
                                    tvStatusBadge1.text = "● Menunggu"
                                    tvStatusBadge1.setBackgroundResource(R.drawable.bg_badge_selesai)
                                    tvStatusBadge1.setTextColor(Color.parseColor("#0284C7"))
                                }
                                2L -> {
                                    tvStatusBadge1.text = "● Terverifikasi"
                                    tvStatusBadge1.setBackgroundResource(R.drawable.bg_badge_terverifikasi)
                                    tvStatusBadge1.setTextColor(Color.parseColor("#0B58CA"))
                                }
                                3L -> {
                                    tvStatusBadge1.text = "✕ Ditolak"
                                    tvStatusBadge1.setBackgroundResource(R.drawable.bg_badge_ditolak)
                                    tvStatusBadge1.setTextColor(Color.parseColor("#B91C1C"))
                                }
                                4L -> {
                                    tvStatusBadge1.text = "● Diproses"
                                    tvStatusBadge1.setBackgroundResource(R.drawable.bg_badge_diproses_mint)
                                    tvStatusBadge1.setTextColor(Color.parseColor("#0F766E"))
                                }
                                5L -> {
                                    tvStatusBadge1.text = "✓ Selesai"
                                    tvStatusBadge1.setBackgroundResource(R.drawable.bg_badge_selesai)
                                    tvStatusBadge1.setTextColor(Color.parseColor("#0B58CA"))
                                }
                            }

                            btnTrackReport1.setOnClickListener {
                                val intent = Intent(this@RiwayatActivity, DetailLaporanActivity::class.java).apply {
                                    putExtra("EXTRA_REPORT_ID", first.id)
                                    putExtra("EXTRA_REPORT_TITLE", first.judul)
                                    putExtra("EXTRA_REPORT_DESC", first.deskripsi)
                                    putExtra("EXTRA_REPORT_LOCATION", first.alamat)
                                    putExtra("EXTRA_REPORT_STATUS", first.idStatus)
                                }
                                startActivity(intent)
                            }
                        } else {
                            tvEmptyRiwayat.visibility = View.VISIBLE
                            cardReport1.visibility = View.GONE
                        }
                    }
                }

                override fun onFailure(call: Call<List<LaporanResponse>>, t: Throwable) {
                    // Biarkan fallback data
                }
            })
        }

        // Dialog Filter Status
        findViewById<View>(R.id.btnFilterStatus).setOnClickListener {
            val filterOptions = arrayOf(
                "Semua Status (${reportsList.size})",
                "Diproses (${reportsList.count { it.idStatus == 4L }})",
                "Selesai (${reportsList.count { it.idStatus == 5L }})",
                "Menunggu (${reportsList.count { it.idStatus == 1L }})",
                "Ditolak (${reportsList.count { it.idStatus == 3L }})"
            )
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Pilih Filter Status")
            builder.setItems(filterOptions) { _, which ->
                tvFilterStatus.text = filterOptions[which]
                Toast.makeText(this, "Filter: ${filterOptions[which]}", Toast.LENGTH_SHORT).show()
            }
            builder.show()
        }

        // Aksi Tombol-Tombol Kartu Laporan Lainnya
        findViewById<Button>(R.id.btnViewProof2).setOnClickListener {
            Toast.makeText(this, "Menampilkan foto bukti perbaikan jalan selesai...", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnDetailReport3).setOnClickListener {
            val intent = Intent(this, DetailLaporanActivity::class.java)
            startActivity(intent)
        }

        // Setup Bottom Navigation Bar dengan Tab "Riwayat" Aktif
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        // Set Beranda menjadi Non-Aktif
        val ivHome = findViewById<ImageView>(R.id.ivNavUserHome)
        val tvHome = findViewById<TextView>(R.id.tvNavUserHome)
        ivHome.setColorFilter(Color.parseColor("#94A3B8"))
        tvHome.setTextColor(Color.parseColor("#94A3B8"))
        tvHome.setTypeface(null, Typeface.NORMAL)

        // Set Riwayat menjadi Aktif (Warna Biru)
        val ivHistory = findViewById<ImageView>(R.id.ivNavUserHistory)
        val tvHistory = findViewById<TextView>(R.id.tvNavUserHistory)
        ivHistory.setColorFilter(Color.parseColor("#0B58CA"))
        tvHistory.setTextColor(Color.parseColor("#0B58CA"))
        tvHistory.setTypeface(null, Typeface.BOLD)

        // Event Klik Beranda
        findViewById<View>(R.id.navUserHome).setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // Event Klik (+) Lapor
        findViewById<View>(R.id.navUserReportCenter).setOnClickListener {
            val intent = Intent(this, FormPelaporanActivity::class.java)
            startActivity(intent)
        }

        // Event Klik Notifikasi
        findViewById<View>(R.id.navUserNotification).setOnClickListener {
            val intent = Intent(this, NotifikasiActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Event Klik Profil
        findViewById<View>(R.id.navUserProfile).setOnClickListener {
            val intent = Intent(this, ProfilActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
