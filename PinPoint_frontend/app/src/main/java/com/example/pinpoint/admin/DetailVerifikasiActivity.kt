package com.example.pinpoint.admin

import android.content.Intent
import android.graphics.Color
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
import com.example.pinpoint.PhotoSliderDialog
import com.example.pinpoint.R
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.ReportDto
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

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
    private lateinit var googleAdminVerifyMapView: MapView
    private var googleMap: GoogleMap? = null
    private var reportMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_detail_verifikasi)

        reportId = intent.getStringExtra("REPORT_ID")
        initViews()

        setupGoogleMap(savedInstanceState)

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
    }

    override fun onResume() {
        super.onResume()
        googleAdminVerifyMapView.onResume()
        reportId?.let { loadReportDetail(it) }
    }

    override fun onPause() {
        googleAdminVerifyMapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        googleAdminVerifyMapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        googleAdminVerifyMapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        googleAdminVerifyMapView.onSaveInstanceState(outState)
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
                    tvReportStatusBadge.text = report.resolvedStatusDisplay()
                    tvReportTitle.text = "${report.resolvedCategoryName()} - ${report.resolvedTitle()}"
                    tvCitizenDescription.text = report.resolvedDescription()
                    updateReportMarker()
                    tvReportAddressTitle.text = report.resolvedLocationName()
                    tvReporterNameValue.text = report.resolvedReporterName()
                    tvReporterPhone.text = report.resolvedReporterPhone()
                    val citizenPhotos = report.resolvedPhotoUrls()
                    tvPhotoMetaInfo.text = "Kategori: ${report.resolvedCategoryName()} | ${report.resolvedCreatedAt()}"

                    ivReportPhoto.setOnClickListener {
                        if (citizenPhotos.isNotEmpty()) {
                            PhotoSliderDialog.show(
                                this@DetailVerifikasiActivity,
                                "Foto Laporan Warga (${citizenPhotos.size} Foto)",
                                citizenPhotos,
                                0
                            )
                        }
                    }

                    val cardGovEvidence = findViewById<View>(R.id.cardGovEvidenceDetail)
                    val ivGovEvidence = findViewById<ImageView>(R.id.ivGovEvidencePhoto)
                    val tvGovEvidenceCount = findViewById<TextView>(R.id.tvGovEvidencePhotoCount)
                    val tvGovEvidenceNotes = findViewById<TextView>(R.id.tvGovEvidenceNotes)
                    val layoutGovEvidencePhoto = findViewById<View>(R.id.layoutGovEvidencePhoto)

                    val evPhotos = report.resolvedEvidencePhotoUrls()
                    val evNotes = report.resolvedEvidenceNotes()
                    if (evPhotos.isNotEmpty() || evNotes.isNotBlank()) {
                        cardGovEvidence?.visibility = View.VISIBLE
                        tvGovEvidenceNotes?.text = if (evNotes.isNotBlank()) {
                            "Catatan Pengerjaan (${report.resolvedCompletedBy()}): $evNotes"
                        } else {
                            "Diselesaikan oleh ${report.resolvedCompletedBy()}"
                        }
                        if (evPhotos.isNotEmpty() && ivGovEvidence != null) {
                            tvGovEvidenceCount?.text = "${evPhotos.size} Foto Bukti - Ketuk untuk geser"
                            Glide.with(this@DetailVerifikasiActivity)
                                .load(evPhotos[0])
                                .placeholder(R.drawable.sampul)
                                .error(R.drawable.sampul)
                                .into(ivGovEvidence)
                            layoutGovEvidencePhoto?.setOnClickListener {
                                PhotoSliderDialog.show(
                                    this@DetailVerifikasiActivity,
                                    "Foto Bukti Perbaikan (${evPhotos.size} Foto)",
                                    evPhotos,
                                    0
                                )
                            }
                        } else {
                            layoutGovEvidencePhoto?.visibility = View.GONE
                        }
                    } else {
                        cardGovEvidence?.visibility = View.GONE
                    }

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

    private fun setupGoogleMap(savedInstanceState: Bundle?) {
        googleAdminVerifyMapView =
            findViewById(R.id.googleAdminVerifyMapView)

        googleAdminVerifyMapView.onCreate(savedInstanceState)

        googleAdminVerifyMapView.getMapAsync { map ->
            googleMap = map

            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isMapToolbarEnabled = false
            map.uiSettings.isCompassEnabled = true

            updateReportMarker()
        }
    }

    private fun updateReportMarker() {
        val report = currentReport ?: return
        val map = googleMap ?: return

        val lat = report.latitude
        val lng = report.longitude

        if (lat == null || lng == null ||
            !lat.isFinite() || !lng.isFinite() ||
            lat !in -90.0..90.0 ||
            lng !in -180.0..180.0 ||
            (lat == 0.0 && lng == 0.0)
        ) {
            tvGpsCoordinatesBadge.text = "Koordinat tidak tersedia"
            reportMarker?.remove()
            reportMarker = null
            return
        }

        val location = LatLng(lat, lng)

        tvGpsCoordinatesBadge.text =
            String.format("%.6f, %.6f", lat, lng)

        reportMarker?.remove()

        reportMarker = map.addMarker(
            MarkerOptions()
                .position(location)
                .title(report.resolvedTitle())
                .snippet(report.resolvedLocationName())
        )

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(location, 17f)
        )
    }
}
