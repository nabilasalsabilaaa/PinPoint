
package com.example.pinpoint

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.LaporanResponse
import com.example.pinpoint.network.NotifikasiResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardActivity : AppCompatActivity() {

    private var isSupported = false
    private var supportCount = 24
    private var backPressedTime: Long = 0
    private var featuredReportId: Long? = null

    // Google Maps
    private lateinit var googleDashboardMapView: MapView
    private var googleMap: GoogleMap? = null

    // GPS
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Data laporan dari backend
    private var reportList = listOf<LaporanResponse>()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fine || coarse) {
            enableMyLocationLayer()
            detectAndCenterDashboardLocation()
        } else {
            Toast.makeText(
                this,
                "Izin lokasi diperlukan untuk menampilkan posisi Anda",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Tampilkan nama pengguna dari sesi login
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)

        val savedName = intent.getStringExtra("EXTRA_NAME")
            ?: sharedPreferences.getString("KEY_NAME", null)

        val tvUserName = findViewById<TextView>(R.id.tvUserName)

        tvUserName.text = if (!savedName.isNullOrBlank()) {
            savedName
        } else {
            "Pengguna"
        }

        // Sinkronkan notifikasi dan badge dari database
        val token = sharedPreferences.getString("KEY_TOKEN", null)
        val tvTopBadgeCount = findViewById<TextView>(R.id.tvTopBadgeCount)

        if (!token.isNullOrEmpty()) {
            ApiClient.instance.getNotifications("Bearer $token")
                .enqueue(object : Callback<List<NotifikasiResponse>> {

                    override fun onResponse(
                        call: Call<List<NotifikasiResponse>>,
                        response: Response<List<NotifikasiResponse>>
                    ) {
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

                    override fun onFailure(
                        call: Call<List<NotifikasiResponse>>,
                        t: Throwable
                    ) {
                        // Tidak mengganggu tampilan dashboard jika gagal mengambil notifikasi
                    }
                })
        }

        // Inisialisasi Google Maps
        setupGoogleDashboardMap(savedInstanceState)

        // Sinkronkan laporan unggulan dan marker dari database
        loadReportsForMapAndFeatured()

        // Search Bar dan Filter
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val btnFilter = findViewById<ImageView>(R.id.btnFilter)

        btnFilter.setOnClickListener {
            Toast.makeText(
                this,
                "Filter kategori & status dipilih",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Tombol notifikasi atas
        findViewById<View>(R.id.btnTopNotif).setOnClickListener {
            val intent = Intent(this, NotifikasiActivity::class.java)
            startActivity(intent)
        }

        // Setup Filter Chips
        setupFilterChips()

        // Kontrol zoom Google Maps
        findViewById<View>(R.id.btnZoomIn).setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomIn())
        }

        findViewById<View>(R.id.btnZoomOut).setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomOut())
        }

        // CTA Banner: Buat Laporan Cepat
        findViewById<Button>(R.id.btnQuickReport).setOnClickListener {
            val intent = Intent(this, FormPelaporanActivity::class.java)
            startActivity(intent)
        }

        // Interaksi tombol dukung
        val btnSupport = findViewById<Button>(R.id.btnSupport)
        val tvSupportCount = findViewById<TextView>(R.id.tvSupportCount)

        btnSupport.setOnClickListener {
            if (!isSupported) {
                isSupported = true
                supportCount++

                btnSupport.text = "✓ Didukung"
                btnSupport.setBackgroundResource(R.drawable.bg_btn_rounded_blue)
                btnSupport.setTextColor(
                    ContextCompat.getColor(this, android.R.color.white)
                )

                Toast.makeText(
                    this,
                    "Terima kasih telah mendukung laporan ini!",
                    Toast.LENGTH_SHORT
                ).show()

            } else {
                isSupported = false
                supportCount--

                btnSupport.text = "👍 Dukung"
                btnSupport.setBackgroundResource(R.drawable.bg_btn_rounded_blue_soft)
                btnSupport.setTextColor(
                    android.graphics.Color.parseColor("#0B58CA")
                )

                Toast.makeText(
                    this,
                    "Dukungan dibatalkan",
                    Toast.LENGTH_SHORT
                ).show()
            }

            tvSupportCount.text = "$supportCount Warga Mendukung"
        }

        // Tombol lihat detail laporan
        findViewById<Button>(R.id.btnViewDetail).setOnClickListener {
            val intent = Intent(this, DetailLaporanActivity::class.java).apply {
                featuredReportId?.let {
                    putExtra("EXTRA_REPORT_ID", it)
                }
            }

            startActivity(intent)
        }

        // Setup navigasi bawah
        setupBottomNavigation()

        // Handler tombol kembali: double tap untuk keluar
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {
                    if (backPressedTime + 2000 > System.currentTimeMillis()) {
                        finishAffinity()
                    } else {
                        Toast.makeText(
                            this@DashboardActivity,
                            "Tekan sekali lagi untuk keluar",
                            Toast.LENGTH_SHORT
                        ).show()

                        backPressedTime = System.currentTimeMillis()
                    }
                }
            }
        )
    }

    // =========================================================
    // GOOGLE MAPS
    // =========================================================

    private fun setupGoogleDashboardMap(savedInstanceState: Bundle?) {
        googleDashboardMapView = findViewById(R.id.googleDashboardMapView)

        googleDashboardMapView.onCreate(savedInstanceState)

        googleDashboardMapView.getMapAsync { map ->
            googleMap = map

            map.uiSettings.isZoomControlsEnabled = false
            map.uiSettings.isMapToolbarEnabled = false
            map.uiSettings.isCompassEnabled = true

            // Posisi awal peta: Makassar
            val defaultPosition = LatLng(-5.147665, 119.432731)

            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(defaultPosition, 15f)
            )

            // Tampilkan marker laporan yang sudah diterima
            syncReportMarkersOnGoogleMap()

            // Aktifkan titik biru lokasi pengguna jika izin tersedia
            if (hasLocationPermission()) {
                enableMyLocationLayer()
                detectAndCenterDashboardLocation()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationLayer() {
        if (!hasLocationPermission()) return

        try {
            googleMap?.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // Memuat marker laporan dari database
    private fun syncReportMarkersOnGoogleMap() {
        val map = googleMap ?: return

        map.clear()

        reportList.forEach { report ->
            val latitude = report.latitude ?: return@forEach
            val longitude = report.longitude ?: return@forEach

            val position = LatLng(latitude, longitude)

            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(report.judul ?: "Laporan Kerusakan")
                    .snippet(report.alamat ?: "Makassar")
            )

            marker?.tag = report.id
        }

        // Ketika marker laporan ditekan
        map.setOnMarkerClickListener { marker ->

            val selectedReport = reportList.find {
                it.id == marker.tag
            }

            if (selectedReport != null) {
                featuredReportId = selectedReport.id

                findViewById<TextView>(R.id.tvReportTitle).text =
                    selectedReport.judul ?: "Laporan Fasilitas Publik"

                findViewById<TextView>(R.id.tvReportLocation).text =
                    selectedReport.alamat ?: "Makassar, Sulawesi Selatan"
            }

            marker.showInfoWindow()
            true
        }
    }

    // =========================================================
    // DATA LAPORAN
    // =========================================================

    private fun loadReportsForMapAndFeatured() {
        val tvReportTitle = findViewById<TextView>(R.id.tvReportTitle)
        val tvReportLocation = findViewById<TextView>(R.id.tvReportLocation)

        ApiClient.instance.getReportsForMap()
            .enqueue(object : Callback<List<LaporanResponse>> {

                override fun onResponse(
                    call: Call<List<LaporanResponse>>,
                    response: Response<List<LaporanResponse>>
                ) {
                    if (response.isSuccessful) {
                        val reports = response.body() ?: emptyList()

                        reportList = reports

                        if (reports.isNotEmpty()) {
                            val latest = reports.last()

                            featuredReportId = latest.id

                            tvReportTitle.text =
                                latest.judul ?: "Laporan Fasilitas Publik"

                            tvReportLocation.text =
                                latest.alamat ?: "Makassar, Sulawesi Selatan"
                        }

                        // Sinkronkan marker setelah data diterima
                        syncReportMarkersOnGoogleMap()
                    }
                }

                override fun onFailure(
                    call: Call<List<LaporanResponse>>,
                    t: Throwable
                ) {
                    // Peta tetap dapat digunakan meskipun API gagal
                }
            })
    }

    // =========================================================
    // GPS DAN LOKASI PENGGUNA
    // =========================================================

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        return fine == PackageManager.PERMISSION_GRANTED ||
                coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun detectAndCenterDashboardLocation() {
        if (!hasLocationPermission()) return

        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->

                if (location != null) {
                    moveDashboardCamera(
                        location.latitude,
                        location.longitude
                    )
                } else {
                    // Gunakan lokasi terakhir jika lokasi saat ini belum tersedia
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { lastLocation ->

                            if (lastLocation != null) {
                                moveDashboardCamera(
                                    lastLocation.latitude,
                                    lastLocation.longitude
                                )
                            }
                        }
                }
            }
    }

    private fun moveDashboardCamera(latitude: Double, longitude: Double) {
        val position = LatLng(latitude, longitude)

        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(position, 16f)
        )
    }

    // =========================================================
    // FILTER KATEGORI
    // =========================================================

    private fun setupFilterChips() {
        val chipAll = findViewById<TextView>(R.id.chipAll)
        val chipPothole = findViewById<TextView>(R.id.chipPothole)
        val chipStreetlight = findViewById<TextView>(R.id.chipStreetlight)
        val chipDrainage = findViewById<TextView>(R.id.chipDrainage)

        val chips = listOf(
            chipAll,
            chipPothole,
            chipStreetlight,
            chipDrainage
        )

        fun selectChip(selected: TextView) {
            for (chip in chips) {
                if (chip == selected) {
                    chip.setBackgroundResource(R.drawable.bg_chip_blue_active)
                    chip.setTextColor(
                        ContextCompat.getColor(this, android.R.color.white)
                    )
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip_inactive)
                    chip.setTextColor(
                        android.graphics.Color.parseColor("#334155")
                    )
                }
            }
        }

        chipAll.setOnClickListener {
            selectChip(chipAll)
        }

        chipPothole.setOnClickListener {
            selectChip(chipPothole)
        }

        chipStreetlight.setOnClickListener {
            selectChip(chipStreetlight)
        }

        chipDrainage.setOnClickListener {
            selectChip(chipDrainage)
        }
    }

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private fun setupBottomNavigation() {

        // Beranda
        findViewById<View>(R.id.navUserHome).setOnClickListener {
            // Sudah berada di halaman Beranda
        }

        // Riwayat
        findViewById<View>(R.id.navUserHistory).setOnClickListener {
            val intent = Intent(this, RiwayatActivity::class.java)
            startActivity(intent)
        }

        // Tombol tengah: Lapor
        findViewById<View>(R.id.navUserReportCenter).setOnClickListener {
            val intent = Intent(this, FormPelaporanActivity::class.java)
            startActivity(intent)
        }

        // Notifikasi
        findViewById<View>(R.id.navUserNotification).setOnClickListener {
            val intent = Intent(this, NotifikasiActivity::class.java)
            startActivity(intent)
        }

        // Profil
        findViewById<View>(R.id.navUserProfile).setOnClickListener {
            val intent = Intent(this, ProfilActivity::class.java)
            startActivity(intent)
        }
    }

    // =========================================================
    // GOOGLE MAPVIEW LIFECYCLE
    // =========================================================

    override fun onResume() {
        super.onResume()

        if (::googleDashboardMapView.isInitialized) {
            googleDashboardMapView.onResume()
        }

        loadReportsForMapAndFeatured()
    }

    override fun onPause() {
        if (::googleDashboardMapView.isInitialized) {
            googleDashboardMapView.onPause()
        }

        super.onPause()
    }

    override fun onDestroy() {
        if (::googleDashboardMapView.isInitialized) {
            googleDashboardMapView.onDestroy()
        }

        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()

        if (::googleDashboardMapView.isInitialized) {
            googleDashboardMapView.onLowMemory()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (::googleDashboardMapView.isInitialized) {
            googleDashboardMapView.onSaveInstanceState(outState)
        }
    }
}