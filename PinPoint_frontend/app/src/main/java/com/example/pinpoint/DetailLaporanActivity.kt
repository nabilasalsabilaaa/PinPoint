
package com.example.pinpoint

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.LaporanResponse
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class DetailLaporanActivity : AppCompatActivity() {

    private var isSupported = false
    private var supportCount = 24

    private lateinit var detailMapView: MapView
    private var googleMap: GoogleMap? = null
    private var detailMarker: Marker? = null

    private var reportLat: Double? = null
    private var reportLng: Double? = null
    private var reportAddr: String? = null
    private var reportTitleText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_detail_laporan)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.mainDetail)
        ) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                0
            )
            insets
        }

        findViewById<ImageView>(R.id.btnBackDetail).setOnClickListener {
            finish()
        }

        val tvTicketNumber =
            findViewById<TextView>(R.id.tvTicketNumber)
        val tvDetailStatusBadge =
            findViewById<TextView>(R.id.tvDetailStatusBadge)
        val tvDetailTitle =
            findViewById<TextView>(R.id.tvDetailTitle)
        val tvDetailLocationAddress =
            findViewById<TextView>(R.id.tvDetailLocationAddress)
        val tvReportDescription =
            findViewById<TextView>(R.id.tvReportDescription)
        val tvReporterName =
            findViewById<TextView>(R.id.tvReporterName)
        val tvGeotagCoordinates =
            findViewById<TextView?>(R.id.tvGeotagCoordinates)

        val reportId = intent.getLongExtra("EXTRA_REPORT_ID", -1L)
        val passedTitle = intent.getStringExtra("EXTRA_REPORT_TITLE")
        val passedDesc = intent.getStringExtra("EXTRA_REPORT_DESC")
        val passedLoc = intent.getStringExtra("EXTRA_REPORT_LOCATION")
        val passedStatus = intent.getLongExtra("EXTRA_REPORT_STATUS", -1L)

        // Inisialisasi Google Maps
        setupGoogleDetailMap(savedInstanceState)

        fun updateUI(
            id: Long?,
            title: String?,
            desc: String?,
            loc: String?,
            status: Long?,
            date: String?,
            lat: Double?,
            lng: Double?
        ) {
            if (id != null && id > 0) {
                tvTicketNumber.text = "#LP-$id • ${date ?: "Terkini"}"
            }

            if (!title.isNullOrBlank()) {
                tvDetailTitle.text = title
                reportTitleText = title
            }

            if (!desc.isNullOrBlank()) {
                tvReportDescription.text = desc
            }

            if (!loc.isNullOrBlank()) {
                tvDetailLocationAddress.text = loc
                reportAddr = loc
            }

            if (lat != null && lng != null &&
                lat != 0.0 && lng != 0.0 &&
                lat.isFinite() && lng.isFinite() &&
                lat in -90.0..90.0 &&
                lng in -180.0..180.0
            ) {
                reportLat = lat
                reportLng = lng

                tvGeotagCoordinates?.text = String.format(
                    Locale.US,
                    "%.4f, %.4f",
                    lat,
                    lng
                )

                updateMapMarker()
            }

            when (status) {
                1L -> {
                    tvDetailStatusBadge.text = "● Menunggu"
                    tvDetailStatusBadge.setBackgroundResource(
                        R.drawable.bg_badge_selesai
                    )
                    tvDetailStatusBadge.setTextColor(
                        Color.parseColor("#0284C7")
                    )
                }

                2L -> {
                    tvDetailStatusBadge.text = "● Terverifikasi"
                    tvDetailStatusBadge.setBackgroundResource(
                        R.drawable.bg_badge_terverifikasi
                    )
                    tvDetailStatusBadge.setTextColor(
                        Color.parseColor("#0B58CA")
                    )
                }

                3L -> {
                    tvDetailStatusBadge.text = "✕ Ditolak"
                    tvDetailStatusBadge.setBackgroundResource(
                        R.drawable.bg_badge_ditolak
                    )
                    tvDetailStatusBadge.setTextColor(
                        Color.parseColor("#B91C1C")
                    )
                }

                4L -> {
                    tvDetailStatusBadge.text = "● Diproses"
                    tvDetailStatusBadge.setBackgroundResource(
                        R.drawable.bg_badge_diproses_mint
                    )
                    tvDetailStatusBadge.setTextColor(
                        Color.parseColor("#0F766E")
                    )
                }

                5L -> {
                    tvDetailStatusBadge.text = "✓ Selesai"
                    tvDetailStatusBadge.setBackgroundResource(
                        R.drawable.bg_badge_selesai
                    )
                    tvDetailStatusBadge.setTextColor(
                        Color.parseColor("#0B58CA")
                    )
                }
            }
        }

        // Terapkan data yang dikirim melalui Intent
        if (passedTitle != null) {
            updateUI(
                reportId,
                passedTitle,
                passedDesc,
                passedLoc,
                passedStatus,
                null,
                null,
                null
            )
        }

        // Ambil data terbaru dari backend
        if (reportId > 0) {
            ApiClient.instance.getReportDetail(reportId)
                .enqueue(object : Callback<LaporanResponse> {

                    override fun onResponse(
                        call: Call<LaporanResponse>,
                        response: Response<LaporanResponse>
                    ) {
                        if (response.isSuccessful) {
                            val report = response.body()

                            if (report != null) {
                                updateUI(
                                    report.id,
                                    report.judul,
                                    report.deskripsi,
                                    report.alamat,
                                    report.idStatus,
                                    report.tanggalLaporan,
                                    report.latitude,
                                    report.longitude
                                )
                            }
                        } else {
                            Toast.makeText(
                                this@DetailLaporanActivity,
                                "Gagal mengambil detail laporan",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: Call<LaporanResponse>,
                        t: Throwable
                    ) {
                        Toast.makeText(
                            this@DetailLaporanActivity,
                            "Gagal terhubung ke server: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }

        // Klik peta untuk melihat titik lokasi pada halaman peta
        findViewById<View>(R.id.cardDetailMap)?.setOnClickListener {

            val lat = reportLat
            val lng = reportLng

            if (lat == null || lng == null) {
                Toast.makeText(
                    this,
                    "Koordinat lokasi laporan belum tersedia",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val mapIntent = Intent(
                this,
                PilihLokasiActivity::class.java
            ).apply {
                putExtra(PilihLokasiActivity.EXTRA_LATITUDE, lat)
                putExtra(PilihLokasiActivity.EXTRA_LONGITUDE, lng)
                putExtra(
                    PilihLokasiActivity.EXTRA_ADDRESS,
                    reportAddr ?: ""
                )
            }

            startActivity(mapIntent)
        }

        val reporterName =
            intent.getStringExtra("EXTRA_REPORTER_NAME")

        if (!reporterName.isNullOrBlank()) {
            tvReporterName.text = "Dilaporkan oleh $reporterName"
        }

        findViewById<View>(R.id.btnFullscreenPhoto).setOnClickListener {
            Toast.makeText(
                this,
                "Membuka foto resolusi penuh...",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Tombol dukung laporan
        val btnSupport = findViewById<Button>(
            R.id.btnDetailSupport
        )

        val tvSupportCount = findViewById<TextView>(
            R.id.tvDetailSupportCount
        )

        btnSupport.setOnClickListener {

            if (!isSupported) {
                isSupported = true
                supportCount++

                btnSupport.text = "✓ Didukung"
                btnSupport.setBackgroundResource(
                    R.drawable.bg_btn_rounded_blue
                )
                btnSupport.setTextColor(Color.WHITE)

                Toast.makeText(
                    this,
                    "Terima kasih telah mendukung pengaduan ini!",
                    Toast.LENGTH_SHORT
                ).show()

            } else {
                isSupported = false
                supportCount--

                btnSupport.text = "👍 Dukung"
                btnSupport.setBackgroundResource(
                    R.drawable.bg_btn_rounded_blue_soft
                )
                btnSupport.setTextColor(
                    Color.parseColor("#0B58CA")
                )

                Toast.makeText(
                    this,
                    "Dukungan dibatalkan",
                    Toast.LENGTH_SHORT
                ).show()
            }

            tvSupportCount.text = "$supportCount Warga"
        }
    }

    // =====================================================
    // GOOGLE MAPS
    // =====================================================

    private fun setupGoogleDetailMap(savedInstanceState: Bundle?) {

        detailMapView = findViewById(R.id.googleDetailMapView)

        detailMapView.onCreate(savedInstanceState)

        detailMapView.getMapAsync { map ->

            googleMap = map

            map.mapType = GoogleMap.MAP_TYPE_NORMAL

            map.uiSettings.apply {
                isZoomControlsEnabled = false
                isCompassEnabled = true
                isMapToolbarEnabled = false
                isScrollGesturesEnabled = true
                isZoomGesturesEnabled = true
                isRotateGesturesEnabled = true
                isTiltGesturesEnabled = false
            }

            // Posisi awal sementara sebelum data laporan diterima
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(-5.147665, 119.432732),
                    14.5f
                )
            )

            // Jika data laporan sudah diterima lebih dahulu,
            // tampilkan marker setelah Google Maps siap.
            updateMapMarker()
        }
    }

    private fun updateMapMarker() {

        val map = googleMap ?: return
        val lat = reportLat ?: return
        val lng = reportLng ?: return

        val position = LatLng(lat, lng)

        detailMarker?.remove()

        detailMarker = map.addMarker(
            MarkerOptions()
                .position(position)
                .title(reportTitleText ?: "Titik Fasilitas Rusak")
                .snippet(reportAddr ?: "Lokasi laporan")
                .icon(
                    BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_RED
                    )
                )
        )

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                position,
                17f
            )
        )

        detailMarker?.showInfoWindow()
    }

    // =====================================================
    // GOOGLE MAPS LIFECYCLE
    // =====================================================

    override fun onResume() {
        super.onResume()
        if (::detailMapView.isInitialized) {
            detailMapView.onResume()
        }
    }

    override fun onPause() {
        if (::detailMapView.isInitialized) {
            detailMapView.onPause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (::detailMapView.isInitialized) {
            detailMapView.onDestroy()
        }
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (::detailMapView.isInitialized) {
            detailMapView.onLowMemory()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::detailMapView.isInitialized) {
            detailMapView.onSaveInstanceState(outState)
        }
    }
}
