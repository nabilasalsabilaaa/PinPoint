package com.example.pinpoint.admin

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pinpoint.PilihLokasiActivity
import com.example.pinpoint.R
import com.example.pinpoint.adapter.ReportAdapter
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.CategoryDto
import com.example.pinpoint.network.ReportDto
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class AdminDashboardActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var tvAdminGreeting: TextView
    private lateinit var tvCriticalCount: TextView
    private lateinit var btnCheckCritical: Button
    private lateinit var tvStatTotalReports: TextView
    private lateinit var tvStatProcessingReports: TextView
    private lateinit var tvStatResolvedReports: TextView
    private lateinit var tvStatRejectedReports: TextView
    private lateinit var tvSeeAllReports: TextView
    private lateinit var llDashboardCategoryChips: LinearLayout

    private lateinit var rvRecentReports: RecyclerView
    private lateinit var reportAdapter: ReportAdapter

    private var allReports = listOf<ReportDto>()
    private var selectedCategoryId: Long? = null
    private var selectedCategoryName: String? = null

    // Google Maps SDK & Lokasi Admin
    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var hasCenteredAdminMap = false

    // Auto-refresh laporan di dashboard admin secara real-time
    private val autoRefreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val autoRefreshRunnable = object : Runnable {
        override fun run() {
            loadDashboardData()
            autoRefreshHandler.postDelayed(this, 15000)
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            enableMyLocationOnAdminMap()
            detectAndCenterAdminLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pembatasan Role: Hanya Role 1 (ADMIN) yang diizinkan mengakses modul Admin
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val idRole = sharedPreferences.getLong("KEY_ID_ROLE", -1L)
        val roleStr = (sharedPreferences.getString("KEY_ROLE", "") ?: "").uppercase()
        val isAdmin = idRole == 1L || roleStr == "ADMIN" || roleStr == "ROLE_ADMIN"
        if (!isAdmin) {
            val isPemerintah = idRole == 3L || roleStr.contains("PEMERINTAH") || roleStr.contains("DINAS")
            val redirectIntent = if (isPemerintah) {
                Intent(this, com.example.pinpoint.PemerintahDashboardActivity::class.java)
            } else {
                Intent(this, com.example.pinpoint.DashboardActivity::class.java)
            }
            startActivity(redirectIntent)
            finish()
            return
        }

        setContentView(R.layout.activity_admin_dashboard)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initViews()
        setupBottomNavigation()
        setupRecyclerView()

        val openVerify = {
            startActivity(Intent(this, VerifikasiLaporanActivity::class.java))
        }
        tvSeeAllReports.setOnClickListener { openVerify() }
        btnCheckCritical.setOnClickListener { openVerify() }

        // Setup Google Maps Persebaran Laporan
        val mapFragment = supportFragmentManager.findFragmentById(R.id.adminMapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        findViewById<View>(R.id.btnAdminOpenFullMap).setOnClickListener {
            val intent = Intent(this, PilihLokasiActivity::class.java)
            startActivity(intent)
        }

        loadRegisteredCategories()
        loadDashboardData()
    }

    override fun onResume() {
        super.onResume()
        updateGreeting()
        loadRegisteredCategories()
        autoRefreshHandler.post(autoRefreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable)
    }

    private fun initViews() {
        tvAdminGreeting = findViewById(R.id.tvAdminGreeting)
        tvCriticalCount = findViewById(R.id.tvCriticalCount)
        btnCheckCritical = findViewById(R.id.btnCheckCritical)
        tvStatTotalReports = findViewById(R.id.tvStatTotalReports)
        tvStatProcessingReports = findViewById(R.id.tvStatProcessingReports)
        tvStatResolvedReports = findViewById(R.id.tvStatResolvedReports)
        tvStatRejectedReports = findViewById(R.id.tvStatRejectedReports)
        tvSeeAllReports = findViewById(R.id.tvSeeAllReports)
        llDashboardCategoryChips = findViewById(R.id.llDashboardCategoryChips)
        rvRecentReports = findViewById(R.id.rvRecentReports)
        updateGreeting()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true

        val defaultPos = LatLng(-5.147665, 119.432731)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPos, 13f))

        if (hasLocationPermission()) {
            enableMyLocationOnAdminMap()
            if (!hasCenteredAdminMap) {
                detectAndCenterAdminLocation()
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        // Klik InfoWindow pada marker langsung membuka detail verifikasi laporan
        map.setOnInfoWindowClickListener { marker ->
            val reportId = marker.snippet?.substringAfter("ID: ")?.substringBefore("\n")?.trim()
            if (!reportId.isNullOrBlank()) {
                val intent = Intent(this, DetailVerifikasiActivity::class.java).apply {
                    putExtra("REPORT_ID", reportId)
                }
                startActivity(intent)
            }
        }

        syncMapMarkers(allReports)
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationOnAdminMap() {
        if (hasLocationPermission()) {
            googleMap?.isMyLocationEnabled = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun detectAndCenterAdminLocation() {
        if (!hasLocationPermission()) return

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                    hasCenteredAdminMap = true
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            val latLng = LatLng(lastLoc.latitude, lastLoc.longitude)
                            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                            hasCenteredAdminMap = true
                        }
                    }
                }
            }
    }

    private fun syncMapMarkers(reports: List<ReportDto>) {
        val map = googleMap ?: return
        map.clear()

        reports.forEach { report ->
            val lat = report.latitude ?: return@forEach
            val lng = report.longitude ?: return@forEach
            val pos = LatLng(lat, lng)

            val markerColor = when (report.resolvedStatusKey()) {
                "MENUNGGU_VERIFIKASI" -> BitmapDescriptorFactory.HUE_ORANGE
                "TERVERIFIKASI" -> BitmapDescriptorFactory.HUE_AZURE
                "DIPROSES" -> BitmapDescriptorFactory.HUE_VIOLET
                "SELESAI" -> BitmapDescriptorFactory.HUE_GREEN
                "DITOLAK" -> BitmapDescriptorFactory.HUE_RED
                else -> BitmapDescriptorFactory.HUE_RED
            }

            map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title("${report.resolvedCode()}: ${report.resolvedTitle()}")
                    .snippet("ID: ${report.resolvedId()}\n${report.resolvedLocationName()}")
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
            )
        }
    }

    private fun updateGreeting() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val name = sharedPreferences.getString("KEY_NAME", "Admin")?.takeIf { it.isNotBlank() } ?: "Admin"
        tvAdminGreeting.text = "Halo, $name"
    }

    private fun setupBottomNavigation() {
        NavbarUtils.setupAdminNavbar(this)
    }

    private fun setupRecyclerView() {
        rvRecentReports.layoutManager = LinearLayoutManager(this)
        reportAdapter = ReportAdapter(
            reports = emptyList(),
            onItemClick = { report ->
                val intent = Intent(this, DetailVerifikasiActivity::class.java).apply {
                    putExtra("REPORT_ID", report.resolvedId().toString())
                }
                startActivity(intent)
            },
            onVerifyClick = { report ->
                quickVerifyReport(report)
            },
            onRejectClick = { report ->
                val intent = Intent(this, TolakLaporanActivity::class.java).apply {
                    putExtra("REPORT_ID", report.resolvedId().toString())
                    putExtra("REPORT_CODE", report.resolvedCode())
                    putExtra("REPORT_TITLE", report.resolvedTitle())
                    putExtra("REPORTER_NAME", report.resolvedReporterName())
                    putExtra("REPORT_LOCATION", report.resolvedLocationName())
                }
                startActivity(intent)
            }
        )
        rvRecentReports.adapter = reportAdapter
    }

    private fun loadRegisteredCategories() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val rawToken = sharedPreferences.getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

        ApiClient.fetchWithFallback(
            callFactory = { api -> api.getAllCategories(token) },
            onResponse = { _, response ->
                if (response.isSuccessful && response.body() != null) {
                    val categories = response.body()!!.filter { it.resolvedIsActive() }
                    renderCategoryChips(categories)
                }
            },
            onFailure = { _, _ -> }
        )
    }

    private fun renderCategoryChips(categories: List<CategoryDto>) {
        llDashboardCategoryChips.removeAllViews()
        val density = resources.displayMetrics.density
        val padH = (14 * density).toInt()
        val padV = (7 * density).toInt()
        val marginEnd = (8 * density).toInt()

        val chipViews = mutableListOf<Pair<TextView, CategoryDto?>>()

        fun createChip(label: String, category: CategoryDto?): TextView {
            val tv = TextView(this)
            tv.text = label
            tv.textSize = 11f
            tv.setPadding(padH, padV, padH, padV)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = marginEnd
            tv.layoutParams = lp
            tv.isClickable = true
            tv.isFocusable = true
            chipViews.add(Pair(tv, category))
            return tv
        }

        val allChip = createChip("Semua", null)
        llDashboardCategoryChips.addView(allChip)

        categories.forEach { cat ->
            val chip = createChip(cat.resolvedName(), cat)
            llDashboardCategoryChips.addView(chip)
        }

        fun updateChipStyles() {
            chipViews.forEach { (tv, cat) ->
                val isSelected = (selectedCategoryId == null && cat == null) ||
                        (cat != null && cat.resolvedId() == selectedCategoryId)
                if (isSelected) {
                    tv.setBackgroundResource(R.drawable.bg_chip_blue_active)
                    tv.setTextColor(Color.WHITE)
                    tv.setTypeface(null, Typeface.BOLD)
                } else {
                    tv.setBackgroundResource(R.drawable.bg_chip_white)
                    tv.setTextColor(Color.parseColor("#434653"))
                    tv.setTypeface(null, Typeface.NORMAL)
                }
            }
        }

        chipViews.forEach { (tv, cat) ->
            tv.setOnClickListener {
                selectedCategoryId = cat?.resolvedId()
                selectedCategoryName = cat?.resolvedName()
                updateChipStyles()
                applyFilterAndStats()
            }
        }

        updateChipStyles()
    }

    private fun loadDashboardData() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val rawToken = sharedPreferences.getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

        ApiClient.fetchWithFallback(
            callFactory = { api -> api.getDashboardStats(token) },
            onResponse = { _, response ->
                if (response.isSuccessful && response.body() != null) {
                    val stats = response.body()!!
                    tvCriticalCount.text = "${stats.unverifiedReports} Laporan Baru"
                    tvStatTotalReports.text = stats.totalReports.toString()
                    tvStatProcessingReports.text = stats.processedReports.toString()
                    tvStatResolvedReports.text = stats.completedReports.toString()
                    tvStatRejectedReports.text = stats.rejectedReports.toString()
                }
            },
            onFailure = { _, _ -> }
        )

        ApiClient.fetchWithFallback(
            callFactory = { api -> api.getAllReports(token, null) },
            onResponse = { _, response ->
                if (response.isSuccessful && response.body() != null) {
                    allReports = response.body()!!
                    updateStatsFromReports(allReports)
                    applyFilterAndStats()
                    syncMapMarkers(allReports)
                }
            },
            onFailure = { _, _ -> }
        )
    }

    private fun updateStatsFromReports(reports: List<ReportDto>) {
        val total = reports.size
        val unverified = reports.count { it.resolvedStatusKey() == "MENUNGGU_VERIFIKASI" }
        val processed = reports.count { it.resolvedStatusKey() == "TERVERIFIKASI" || it.resolvedStatusKey() == "DIPROSES" }
        val completed = reports.count { it.resolvedStatusKey() == "SELESAI" }
        val rejected = reports.count { it.resolvedStatusKey() == "DITOLAK" }

        tvCriticalCount.text = "$unverified Laporan Baru"
        tvStatTotalReports.text = total.toString()
        tvStatProcessingReports.text = processed.toString()
        tvStatResolvedReports.text = completed.toString()
        tvStatRejectedReports.text = rejected.toString()
    }

    private fun applyFilterAndStats() {
        val filtered = if (selectedCategoryId == null && selectedCategoryName.isNullOrBlank()) {
            allReports
        } else {
            allReports.filter { r ->
                (selectedCategoryId != null && r.idKategori == selectedCategoryId) ||
                (!selectedCategoryName.isNullOrBlank() && r.resolvedCategoryName().equals(selectedCategoryName, ignoreCase = true))
            }
        }
        reportAdapter.updateData(filtered)
        syncMapMarkers(filtered)
    }

    private fun quickVerifyReport(report: ReportDto) {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val rawToken = sharedPreferences.getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"
        val reportId = report.resolvedId().toString()

        ApiClient.fetchWithFallback(
            callFactory = { api -> api.verifyReport(token, reportId) },
            onResponse = { _, response ->
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@AdminDashboardActivity,
                        "Laporan ${report.resolvedCode()} berhasil divalidasi!",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadDashboardData()
                } else {
                    Toast.makeText(this@AdminDashboardActivity, "Gagal memvalidasi laporan (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { _, _ ->
                Toast.makeText(this@AdminDashboardActivity, "Gagal menghubungi server", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
