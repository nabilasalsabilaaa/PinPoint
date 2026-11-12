package com.example.pinpoint.admin

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.pinpoint.R
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.ReportDto

class DetailVerifikasiActivity : AppCompatActivity() {

    private var reportId: String? = null
    private var currentReport: ReportDto? = null

    private lateinit var btnBackDetailVerify: ImageView
    private lateinit var tvReportIdCode: TextView
    private lateinit var tvReportStatusBadge: TextView
    private lateinit var tvReportTitle: TextView
    private lateinit var ivReportPhoto: ImageView
    private lateinit var tvPhotoMetaInfo: TextView
    private lateinit var tvCitizenDescription: TextView
    private lateinit var tvGpsCoordinatesBadge: TextView
    private lateinit var tvReportAddressTitle: TextView
    private lateinit var tvReporterNameValue: TextView
    private lateinit var tvReporterPhone: TextView
    private lateinit var btnApproveReport: Button
    private lateinit var btnRejectReport: Button
    private lateinit var layoutActionButtons: LinearLayout
    private lateinit var mapContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_detail_verifikasi)

        reportId = intent.getStringExtra("REPORT_ID")
        initViews()

        btnBackDetailVerify.setOnClickListener { finish() }

        if (reportId != null) {
            loadReportDetail(reportId!!)
        } else {
            Toast.makeText(this, "ID Laporan tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnApproveReport.setOnClickListener { approveReport() }
        btnRejectReport.setOnClickListener {
            val intent = Intent(this, TolakLaporanActivity::class.java).apply {
                putExtra("REPORT_ID", reportId)
                currentReport?.let { r ->
                    putExtra("REPORT_CODE", r.resolvedCode())
                    putExtra("REPORT_TITLE", r.resolvedTitle())
                    putExtra("REPORTER_NAME", r.resolvedReporterName())
                    putExtra("REPORT_LOCATION", r.resolvedLocationName())
                }
            }
            startActivity(intent)
        }

        mapContainer.setOnClickListener {
            currentReport?.let { r ->
                val lat = r.latitude ?: -5.1476
                val lng = r.longitude ?: 119.4327
                val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(r.resolvedTitle())})")
                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                try {
                    startActivity(mapIntent)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onResume() {
        super.onResume()
        reportId?.let { loadReportDetail(it) }
    }

    private fun initViews() {
        btnBackDetailVerify = findViewById(R.id.btnBackDetailVerify)
        tvReportIdCode = findViewById(R.id.tvReportIdCode)
        tvReportStatusBadge = findViewById(R.id.tvReportStatusBadge)
        tvReportTitle = findViewById(R.id.tvReportTitle)
        ivReportPhoto = findViewById(R.id.ivReportPhoto)
        tvPhotoMetaInfo = findViewById(R.id.tvPhotoMetaInfo)
        tvCitizenDescription = findViewById(R.id.tvCitizenDescription)
        tvGpsCoordinatesBadge = findViewById(R.id.tvGpsCoordinatesBadge)
        tvReportAddressTitle = findViewById(R.id.tvReportAddressTitle)
        tvReporterNameValue = findViewById(R.id.tvReporterNameValue)
        tvReporterPhone = findViewById(R.id.tvReporterPhone)
        btnApproveReport = findViewById(R.id.btnApproveReport)
        btnRejectReport = findViewById(R.id.btnRejectReport)
        layoutActionButtons = findViewById(R.id.layoutActionButtons)
        mapContainer = findViewById(R.id.mapContainer)
    }

    private fun loadReportDetail(id: String) {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val rawToken = sharedPreferences.getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

        ApiClient.fetchWithFallback(
            callFactory = { api -> api.getReportDetail(token, id) },
            onResponse = { _, response ->
                if (response.isSuccessful && response.body() != null) {
                    val report = response.body()!!
                    currentReport = report
                    tvReportIdCode.text = "ID ${report.resolvedCode()}"
                    tvReportStatusBadge.text = "● ${report.resolvedStatusDisplay()}"
                    tvReportTitle.text = "${report.resolvedCategoryName()} - ${report.resolvedTitle()}"
                    tvCitizenDescription.text = report.resolvedDescription()
                    val lat = report.latitude ?: -5.1476
                    val lng = report.longitude ?: 119.4327
                    tvGpsCoordinatesBadge.text = String.format("%.4f, %.4f", lat, lng)
                    tvReportAddressTitle.text = report.resolvedLocationName()
                    tvReporterNameValue.text = report.resolvedReporterName()
                    tvReporterPhone.text = report.resolvedReporterPhone()
                    tvPhotoMetaInfo.text = "Kategori: ${report.resolvedCategoryName()} • ${report.resolvedCreatedAt()}"

                    when (report.resolvedStatusKey()) {
                        "MENUNGGU_VERIFIKASI" -> {
                            tvReportStatusBadge.setBackgroundResource(R.drawable.bg_badge_orange_light)
                            tvReportStatusBadge.setTextColor(Color.parseColor("#E65100"))
                            layoutActionButtons.visibility = View.VISIBLE
                        }
                        "TERVERIFIKASI", "DIPROSES", "SELESAI" -> {
                            tvReportStatusBadge.setBackgroundResource(R.drawable.bg_badge_green_light)
                            tvReportStatusBadge.setTextColor(Color.parseColor("#00897B"))
                            layoutActionButtons.visibility = View.GONE
                        }
                        "DITOLAK" -> {
                            tvReportStatusBadge.setBackgroundResource(R.drawable.bg_badge_red_light)
                            tvReportStatusBadge.setTextColor(Color.parseColor("#D32F2F"))
                            layoutActionButtons.visibility = View.GONE
                        }
                        else -> {
                            layoutActionButtons.visibility = View.VISIBLE
                        }
                    }

                    val photo = report.resolvedPhotoUrl()
                    if (!photo.isNullOrEmpty()) {
                        Glide.with(this@DetailVerifikasiActivity)
                            .load(photo)
                            .placeholder(R.drawable.sampul)
                            .error(R.drawable.sampul)
                            .into(ivReportPhoto)
                    } else {
                        ivReportPhoto.setImageResource(R.drawable.sampul)
                    }
                }
            },
            onFailure = { _, _ ->
                Toast.makeText(this@DetailVerifikasiActivity, "Gagal memuat detail laporan", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun approveReport() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val rawToken = sharedPreferences.getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

        reportId?.let { id ->
            ApiClient.fetchWithFallback(
                callFactory = { api -> api.verifyReport(token, id) },
                onResponse = { _, response ->
                    if (response.isSuccessful) {
                        Toast.makeText(this@DetailVerifikasiActivity, "Laporan berhasil diverifikasi & diteruskan ke Dinas!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@DetailVerifikasiActivity, "Gagal memverifikasi (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { _, _ ->
                    Toast.makeText(this@DetailVerifikasiActivity, "Koneksi gagal", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
