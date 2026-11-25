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
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

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
    private lateinit var cardAdminMap: CardView
    private lateinit var cardAdminSelectedPinPopup: CardView
    private lateinit var tvAdminPinPopupLocation: TextView
    private lateinit var tvAdminPinPopupTitle: TextView
    private lateinit var btnAdminPinPopupDetail: TextView
    private lateinit var tvToggleExpandAdminMap: TextView

    private var allReports = listOf<ReportDto>()
    private var displayedReports = listOf<ReportDto>()
    private var selectedCategoryId: Long? = null
    private var selectedCategoryName: String? = null
    private var isAdminMapExpanded = false
    private var selectedAdminPinReport: ReportDto? = null
    private var adminMapRepairFilterMode: String = "ALL"
    private var hideAdminInfoWindowRunnable: Runnable? = null

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
                Intent(this, com.example.pinpoint.pemerintah.PemerintahDashboardActivity::class.java)
            } else {
                Intent(this, com.example.pinpoint.user.DashboardActivity::class.java)
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

        findViewById<View>(R.id.btnAdminCenterLocation)?.setOnClickListener {
            if (hasLocationPermission()) {
                enableMyLocationOnAdminMap()
                detectAndCenterAdminLocation()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
        findViewById<View>(R.id.btnAdminZoomIn).setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomIn())
        }
        findViewById<View>(R.id.btnAdminZoomOut).setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomOut())
        }
        findViewById<View>(R.id.btnExpandAdminMap).setOnClickListener {
            toggleExpandAdminMap()
        }
        findViewById<View>(R.id.barExpandAdminMap).setOnClickListener {
            toggleExpandAdminMap()
        }

        val chipAll = findViewById<TextView?>(R.id.chipAdminMapFilterAll)
        val chipUnresolved = findViewById<TextView?>(R.id.chipAdminMapFilterUnresolved)
        val chipResolved = findViewById<TextView?>(R.id.chipAdminMapFilterResolved)
        val repairChips = listOfNotNull(chipAll, chipUnresolved, chipResolved)

        fun updateRepairChipStyles(active: TextView?) {
            repairChips.forEach { chip ->
                if (chip == active) {
                    chip.setBackgroundResource(R.drawable.bg_btn_rounded_blue)
                    chip.setTextColor(Color.WHITE)
                } else {
                    chip.setBackgroundResource(0)
                    chip.setTextColor(Color.parseColor("#475569"))
                }
            }
        }

        chipAll?.setOnClickListener {
            adminMapRepairFilterMode = "ALL"
            updateRepairChipStyles(chipAll)
            syncMapMarkers(displayedReports)
        }
        chipUnresolved?.setOnClickListener {
            adminMapRepairFilterMode = "UNRESOLVED"
            updateRepairChipStyles(chipUnresolved)
            syncMapMarkers(displayedReports)
        }
        chipResolved?.setOnClickListener {
            adminMapRepairFilterMode = "RESOLVED"
            updateRepairChipStyles(chipResolved)
            syncMapMarkers(displayedReports)
        }

        loadRegisteredCategories()
        loadDashboardData()
    }

    private fun toggleExpandAdminMap() {
        isAdminMapExpanded = !isAdminMapExpanded
        val density = resources.displayMetrics.density
        val targetHeightDp = if (isAdminMapExpanded) 480 else 260
        val params = cardAdminMap.layoutParams
        params.height = (targetHeightDp * density).toInt()
        cardAdminMap.layoutParams = params
        tvToggleExpandAdminMap.text = if (isAdminMapExpanded) "Kecilkan Peta" else "Perluas Peta"
    }

    private fun openAdminReportDetail(report: ReportDto) {
        val intent = Intent(this, DetailVerifikasiActivity::class.java).apply {
            putExtra("REPORT_ID", report.resolvedId().toString())
        }
        startActivity(intent)
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
        cardAdminMap = findViewById(R.id.cardAdminMap)
        cardAdminSelectedPinPopup = findViewById(R.id.cardAdminSelectedPinPopup)
        tvAdminPinPopupLocation = findViewById(R.id.tvAdminPinPopupLocation)
        tvAdminPinPopupTitle = findViewById(R.id.tvAdminPinPopupTitle)
        btnAdminPinPopupDetail = findViewById(R.id.btnAdminPinPopupDetail)
        tvToggleExpandAdminMap = findViewById(R.id.tvToggleExpandAdminMap)

        btnAdminPinPopupDetail.setOnClickListener {
            selectedAdminPinReport?.let { openAdminReportDetail(it) }
        }
        cardAdminSelectedPinPopup.setOnClickListener {
            selectedAdminPinReport?.let { openAdminReportDetail(it) }
        }
        updateGreeting()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isMapToolbarEnabled = false
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false

        val defaultPos = LatLng(-5.147665, 119.432731)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPos, 14f))

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

        syncMapMarkers(displayedReports.ifEmpty { allReports })
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

    private fun resolvePinHueBySeverity(severityRaw: String?): Float {
        val sev = (severityRaw ?: "").lowercase(Locale.getDefault())
        return when {
            sev.contains("kritis") || sev.contains("darurat") || sev.contains("tinggi") || sev.contains("berat") ->
                BitmapDescriptorFactory.HUE_RED
            sev.contains("rendah") || sev.contains("ringan") ->
                BitmapDescriptorFactory.HUE_GREEN
            else ->
                BitmapDescriptorFactory.HUE_ORANGE
        }
    }

    private fun resolveSeverityLabel(severityRaw: String?): String {
        val sev = (severityRaw ?: "").lowercase(Locale.getDefault())
        return when {
            sev.contains("kritis") || sev.contains("darurat") || sev.contains("tinggi") || sev.contains("berat") ->
                "Kritis / Darurat"
            sev.contains("rendah") || sev.contains("ringan") ->
                "Rendah"
            else ->
                "Sedang"
        }
    }

    private fun showAdminInfoWindowTemporarily(marker: Marker) {
        hideAdminInfoWindowRunnable?.let { autoRefreshHandler.removeCallbacks(it) }
        marker.showInfoWindow()
        val runnable = Runnable {
            try {
                marker.hideInfoWindow()
            } catch (_: Exception) {}
        }
        hideAdminInfoWindowRunnable = runnable
        autoRefreshHandler.postDelayed(runnable, 5000)
    }

    private fun syncMapMarkers(reports: List<ReportDto>) {
        displayedReports = reports
        val map = googleMap ?: return
        map.clear()

        val mapReports = when (adminMapRepairFilterMode) {
            "UNRESOLVED" -> reports.filter { it.resolvedStatusKey() != "SELESAI" }
            "RESOLVED" -> reports.filter { it.resolvedStatusKey() == "SELESAI" }
            else -> reports
        }

        map.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View {
                val density = resources.displayMetrics.density
                val report = mapReports.find { it.resolvedId() == (marker.tag as? Long) }
                val locationText = report?.resolvedLocationName() ?: (marker.title ?: "Lokasi Kerusakan")

                return TextView(this@AdminDashboardActivity).apply {
                    text = locationText
                    textSize = 11f
                    setTextColor(Color.parseColor("#0B1C30"))
                    setTypeface(null, Typeface.BOLD)
                    setBackgroundResource(R.drawable.bg_white_rounded_btn)
                    val padH = (12 * density).toInt()
                    val padV = (7 * density).toInt()
                    setPadding(padH, padV, padH, padV)
                    maxWidth = (220 * density).toInt()
                    maxLines = 2
                    elevation = 4 * density
                }
            }

            override fun getInfoContents(marker: Marker): View? = null
        })

        mapReports.forEach { report ->
            val lat = report.latitude ?: return@forEach
            val lng = report.longitude ?: return@forEach
            if (lat == 0.0 && lng == 0.0) return@forEach
            val pos = LatLng(lat, lng)
            val markerColor = resolvePinHueBySeverity(report.tingkatKeparahan)

            val marker = map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(report.resolvedLocationName())
                    .snippet(report.resolvedTitle())
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
            )
            marker?.tag = report.resolvedId()
        }

        map.setOnMarkerClickListener { marker ->
            val report = mapReports.find { it.resolvedId() == (marker.tag as? Long) }
            showAdminInfoWindowTemporarily(marker)
            if (report != null) {
                selectedAdminPinReport = report
                val severityLabel = resolveSeverityLabel(report.tingkatKeparahan)
                val repairState = if (report.resolvedStatusKey() == "SELESAI") "Sudah Diperbaiki" else "Belum Diperbaiki"
                tvAdminPinPopupLocation.text = report.resolvedLocationName()
                tvAdminPinPopupTitle.text = "${report.resolvedTitle()} • $severityLabel ($repairState)"
                cardAdminSelectedPinPopup.visibility = View.VISIBLE
            }
            false
        }

        map.setOnInfoWindowClickListener { marker ->
            marker.hideInfoWindow()
        }

        map.setOnMapClickListener {
            cardAdminSelectedPinPopup.visibility = View.GONE
            selectedAdminPinReport = null
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
