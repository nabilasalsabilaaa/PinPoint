package com.example.pinpoint

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.LaporanResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DetailLaporanActivity : AppCompatActivity() {

    private var isSupported = false
    private var supportCount = 24

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detail_laporan)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainDetail)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Tombol Kembali
        findViewById<ImageView>(R.id.btnBackDetail).setOnClickListener {
            finish()
        }

        val tvTicketNumber = findViewById<TextView>(R.id.tvTicketNumber)
        val tvDetailStatusBadge = findViewById<TextView>(R.id.tvDetailStatusBadge)
        val tvDetailTitle = findViewById<TextView>(R.id.tvDetailTitle)
        val tvDetailLocationAddress = findViewById<TextView>(R.id.tvDetailLocationAddress)
        val tvReportDescription = findViewById<TextView>(R.id.tvReportDescription)
        val tvReporterName = findViewById<TextView>(R.id.tvReporterName)

        val reportId = intent.getLongExtra("EXTRA_REPORT_ID", -1L)
        val passedTitle = intent.getStringExtra("EXTRA_REPORT_TITLE")
        val passedDesc = intent.getStringExtra("EXTRA_REPORT_DESC")
        val passedLoc = intent.getStringExtra("EXTRA_REPORT_LOCATION")
        val passedStatus = intent.getLongExtra("EXTRA_REPORT_STATUS", -1L)

        fun updateUI(id: Long?, title: String?, desc: String?, loc: String?, status: Long?, date: String?) {
            if (id != null && id > 0) {
                tvTicketNumber.text = "#LP-$id  •  ${date ?: "Terkini"}"
            }
            if (!title.isNullOrBlank()) tvDetailTitle.text = title
            if (!desc.isNullOrBlank()) tvReportDescription.text = desc
            if (!loc.isNullOrBlank()) tvDetailLocationAddress.text = loc

            when (status) {
                1L -> {
                    tvDetailStatusBadge.text = "● Menunggu"
                    tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_badge_selesai)
                    tvDetailStatusBadge.setTextColor(Color.parseColor("#0284C7"))
                }
                2L -> {
                    tvDetailStatusBadge.text = "● Terverifikasi"
                    tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_badge_terverifikasi)
                    tvDetailStatusBadge.setTextColor(Color.parseColor("#0B58CA"))
                }
                3L -> {
                    tvDetailStatusBadge.text = "✕ Ditolak"
                    tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_badge_ditolak)
                    tvDetailStatusBadge.setTextColor(Color.parseColor("#B91C1C"))
                }
                4L -> {
                    tvDetailStatusBadge.text = "● Diproses"
                    tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_badge_diproses_mint)
                    tvDetailStatusBadge.setTextColor(Color.parseColor("#0F766E"))
                }
                5L -> {
                    tvDetailStatusBadge.text = "✓ Selesai"
                    tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_badge_selesai)
                    tvDetailStatusBadge.setTextColor(Color.parseColor("#0B58CA"))
                }
            }
        }

        // Terapkan data yang sudah ada di Intent
        if (passedTitle != null) {
            updateUI(reportId, passedTitle, passedDesc, passedLoc, passedStatus, null)
        }

        // Jika ada ID laporan, ambil data detail terbaru dari Database
        if (reportId > 0) {
            ApiClient.instance.getReportDetail(reportId).enqueue(object : Callback<LaporanResponse> {
                override fun onResponse(call: Call<LaporanResponse>, response: Response<LaporanResponse>) {
                    if (response.isSuccessful) {
                        val report = response.body()
                        if (report != null) {
                            updateUI(report.id, report.judul, report.deskripsi, report.alamat, report.idStatus, report.tanggalLaporan)
                        }
                    }
                }
                override fun onFailure(call: Call<LaporanResponse>, t: Throwable) {}
            })
        }

        val reporterName = intent.getStringExtra("EXTRA_REPORTER_NAME")
        if (!reporterName.isNullOrBlank()) {
            tvReporterName.text = "Dilaporkan oleh $reporterName"
        }

        // Tombol Fullscreen Foto
        findViewById<android.view.View>(R.id.btnFullscreenPhoto).setOnClickListener {
            Toast.makeText(this, "Membuka foto resolusi penuh...", Toast.LENGTH_SHORT).show()
        }

        // Toggle Tombol Dukung
        val btnSupport = findViewById<Button>(R.id.btnDetailSupport)
        val tvSupportCount = findViewById<TextView>(R.id.tvDetailSupportCount)

        btnSupport.setOnClickListener {
            if (!isSupported) {
                isSupported = true
                supportCount++
                btnSupport.text = "✓ Didukung"
                btnSupport.setBackgroundResource(R.drawable.bg_btn_rounded_blue)
                btnSupport.setTextColor(Color.WHITE)
                Toast.makeText(this, "Terima kasih telah mendukung pengaduan ini!", Toast.LENGTH_SHORT).show()
            } else {
                isSupported = false
                supportCount--
                btnSupport.text = "👍 Dukung"
                btnSupport.setBackgroundResource(R.drawable.bg_btn_rounded_blue_soft)
                btnSupport.setTextColor(Color.parseColor("#0B58CA"))
                Toast.makeText(this, "Dukungan dibatalkan", Toast.LENGTH_SHORT).show()
            }
            tvSupportCount.text = "$supportCount Warga"
        }
    }
}
