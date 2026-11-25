package com.example.pinpoint.pemerintah

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pinpoint.R
import com.example.pinpoint.adapter.PemerintahReportAdapter
import com.example.pinpoint.admin.NavbarUtils
import com.example.pinpoint.model.PemerintahReport
import com.example.pinpoint.network.ApiClient
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

class PemerintahDashboardActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var rvReports: RecyclerView
    private lateinit var reportAdapter: PemerintahReportAdapter
    private lateinit var tvEmptyReports: TextView

    private lateinit var tvStatPerluAksi: TextView
    private lateinit var tvStatVerified: TextView
    private lateinit var tvStatInProgress: TextView
    private lateinit var tvStatCompleted: TextView

    private lateinit var chipFilterAll: TextView
    private lateinit var chipFilterVerified: TextView
    private lateinit var chipFilterInProgress: TextView
    private var chipFilterCompleted: TextView? = null

    private lateinit var cardPemerintahDashboardMap: CardView
    private lateinit var cardGovSelectedPinPopup: CardView
    private lateinit var tvGovPinPopupLocation: TextView
    private lateinit var tvGovPinPopupTitle: TextView
    private lateinit var btnGovPinPopupDetail: TextView
    private lateinit var tvToggleExpandGovDashboardMap: TextView

    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var hasCenteredGovMap = false
    private var isGovMapExpanded = false
    private var selectedGovPinReport: ReportDto? = null
    private val govInfoWindowHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var hideGovInfoWindowRunnable: Runnable? = null

    private val verifiedReportsList = mutableListOf<PemerintahReport>()
    private val verifiedReportDtos = mutableListOf<ReportDto>()
    private var filteredReportDtos = listOf<ReportDto>()
    private var currentFilterStatus: String = "ALL"
    private var govMapRepairFilterMode: String = "ALL"

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            enableMyLocationOnGovMap()
            detectAndCenterGovLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pemerintah_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initViews()
        setupRecyclerView()
        setupFilterChips()
        setupMapControls()
        NavbarUtils.setupPemerintahNavbar(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.pemerintahDashboardMapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        findViewById<View>(R.id.btnCallTeam)?.setOnClickListener {
            Toast.makeText(this, "Menghubungi Tim Siaga Respon Cepat...", Toast.LENGTH_SHORT).show()
        }

        loadGovernmentDashboardData()
    }

    override fun onResume() {
        super.onResume()
        updateOfficerGreeting()
        loadGovernmentDashboardData()
    }

    private fun initViews() {
        rvReports = findViewById(R.id.rvPemerintahReports)
        tvEmptyReports = findViewById(R.id.tvEmptyPemerintahReports)

        tvStatPerluAksi = findViewById(R.id.tvStatPerluAksi)
        tvStatVerified = findViewById(R.id.tvStatVerified)
        tvStatInProgress = findViewById(R.id.tvStatInProgress)
        tvStatCompleted = findViewById(R.id.tvStatCompleted)

        chipFilterAll = findViewById(R.id.chipFilterAll)
        chipFilterVerified = findViewById(R.id.chipFilterVerified)
        chipFilterInProgress = findViewById(R.id.chipFilterInProgress)
        chipFilterCompleted = findViewById(R.id.chipFilterCompleted)

        cardPemerintahDashboardMap = findViewById(R.id.cardPemerintahDashboardMap)
        cardGovSelectedPinPopup = findViewById(R.id.cardGovSelectedPinPopup)
        tvGovPinPopupLocation = findViewById(R.id.tvGovPinPopupLocation)
        tvGovPinPopupTitle = findViewById(R.id.tvGovPinPopupTitle)
        btnGovPinPopupDetail = findViewById(R.id.btnGovPinPopupDetail)
        tvToggleExpandGovDashboardMap = findViewById(R.id.tvToggleExpandGovDashboardMap)

        updateOfficerGreeting()
    }

    private fun setupMapControls() {
        findViewById<View>(R.id.btnGovCenterLocation)?.setOnClickListener {
            if (hasLocationPermission()) {
                enableMyLocationOnGovMap()
                detectAndCenterGovLocation()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
        findViewById<View>(R.id.btnGovZoomIn).setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomIn())
        }
        findViewById<View>(R.id.btnGovZoomOut).setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomOut())
        }
        findViewById<View>(R.id.btnExpandGovDashboardMap).setOnClickListener {
            toggleExpandGovMap()
        }
        findViewById<View>(R.id.barExpandGovDashboardMap).setOnClickListener {
            toggleExpandGovMap()
        }
        btnGovPinPopupDetail.setOnClickListener {
            selectedGovPinReport?.let { openGovReportDetail(it.resolvedId().toString()) }
        }
        cardGovSelectedPinPopup.setOnClickListener {
            selectedGovPinReport?.let { openGovReportDetail(it.resolvedId().toString()) }
        }

        val chipAll = findViewById<TextView?>(R.id.chipGovMapFilterAll)
        val chipUnresolved = findViewById<TextView?>(R.id.chipGovMapFilterUnresolved)
        val chipResolved = findViewById<TextView?>(R.id.chipGovMapFilterResolved)
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
            govMapRepairFilterMode = "ALL"
            updateRepairChipStyles(chipAll)
            syncGovMapMarkers()
        }
        chipUnresolved?.setOnClickListener {
            govMapRepairFilterMode = "UNRESOLVED"
            updateRepairChipStyles(chipUnresolved)
            syncGovMapMarkers()
        }
        chipResolved?.setOnClickListener {
            govMapRepairFilterMode = "RESOLVED"
            updateRepairChipStyles(chipResolved)
            syncGovMapMarkers()
        }
    }

    private fun toggleExpandGovMap() {
        isGovMapExpanded = !isGovMapExpanded
        val density = resources.displayMetrics.density
        val targetHeightDp = if (isGovMapExpanded) 480 else 260
        val params = cardPemerintahDashboardMap.layoutParams
        params.height = (targetHeightDp * density).toInt()
        cardPemerintahDashboardMap.layoutParams = params
        tvToggleExpandGovDashboardMap.text = if (isGovMapExpanded) "Kecilkan Peta" else "Perluas Peta"
    }

    private fun openGovReportDetail(reportId: String) {
        val intent = Intent(this, PemerintahDetailTaskActivity::class.java)
        intent.putExtra("REPORT_ID", reportId)
        startActivity(intent)
    }

    private fun updateOfficerGreeting() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val userName = sharedPreferences.getString("KEY_NAME", null)?.takeIf { it.isNotBlank() } ?: "Petugas"
        findViewById<TextView>(R.id.tvOfficerName)?.text = userName
    }

    private fun setupRecyclerView() {
        rvReports.layoutManager = LinearLayoutManager(this)
        reportAdapter = PemerintahReportAdapter(emptyList()) { report ->
            openGovReportDetail(report.reportId)
        }
        rvReports.adapter = reportAdapter
    }

    private fun loadGovernmentDashboardData() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val rawToken = sharedPreferences.getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

        ApiClient.fetchWithFallback(
            callFactory = { api -> api.getAllReports(token, null) },
            onResponse = { _, response ->
                if (response.isSuccessful && response.body() != null) {
                    val allDto = response.body()!!
                    updateStatsAndList(allDto)
                }
            },
            onFailure = { _, _ ->
                Toast.makeText(this@PemerintahDashboardActivity, "Gagal memuat data laporan dari server", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateStatsAndList(reports: List<ReportDto>) {
        val verifiedCount = reports.count { it.resolvedStatusKey() == "TERVERIFIKASI" }
        val inProgressCount = reports.count { it.resolvedStatusKey() == "DIPROSES" }
        val completedCount = reports.count { it.resolvedStatusKey() == "SELESAI" }
        val perluAksiCount = verifiedCount + inProgressCount
        val totalVerifiedByAdmin = verifiedCount + inProgressCount + completedCount

        tvStatPerluAksi.text = perluAksiCount.toString()
        tvStatVerified.text = verifiedCount.toString()
        tvStatInProgress.text = inProgressCount.toString()
        tvStatCompleted.text = completedCount.toString()

        chipFilterAll.text = "Semua Tugas ($totalVerifiedByAdmin)"
        chipFilterVerified.text = "Terverifikasi ($verifiedCount)"
        chipFilterInProgress.text = "Sedang Diproses ($inProgressCount)"
        chipFilterCompleted?.text = "Selesai ($completedCount)"

        verifiedReportsList.clear()
        verifiedReportDtos.clear()

        val allVerifiedByAdmin = reports.filter {
            val status = it.resolvedStatusKey()
            status == "TERVERIFIKASI" || status == "DIPROSES" || status == "SELESAI"
        }
        verifiedReportDtos.addAll(allVerifiedByAdmin)

        allVerifiedByAdmin.forEach { dto ->
            val statusKey = dto.resolvedStatusKey()
            val mappedStatus = when (statusKey) {
                "DIPROSES" -> "SEDANG_DIPROSES"
                "SELESAI" -> "SELESAI"
                else -> "TERVERIFIKASI"
            }
            verifiedReportsList.add(
                PemerintahReport(
                    reportId = dto.resolvedId().toString(),
                    id = dto.resolvedCode(),
                    date = dto.resolvedCreatedAt(),
                    category = dto.resolvedCategoryName(),
                    title = dto.resolvedTitle(),
                    location = dto.resolvedLocationName(),
                    imageUrl = dto.resolvedPhotoUrl() ?: "",
                    status = mappedStatus,
                    footerTag = "Pelapor: ${dto.resolvedReporterName()}",
                    actionText = if (statusKey == "SELESAI") "Lihat Detail" else "Detail & Tindak Lanjut",
                    isPrimaryButton = statusKey == "TERVERIFIKASI"
                )
            )
        }

        applyCurrentFilter()
    }

    private fun setupFilterChips() {
        val allChips = listOfNotNull(chipFilterAll, chipFilterVerified, chipFilterInProgress, chipFilterCompleted)

        fun updateChipSelection(active: TextView) {
            allChips.forEach { chip ->
                if (chip == active) selectChip(chip) else unselectChip(chip)
            }
        }

        chipFilterAll.setOnClickListener {
            currentFilterStatus = "ALL"
            updateChipSelection(chipFilterAll)
            applyCurrentFilter()
        }

        chipFilterVerified.setOnClickListener {
            currentFilterStatus = "TERVERIFIKASI"
            updateChipSelection(chipFilterVerified)
            applyCurrentFilter()
        }

        chipFilterInProgress.setOnClickListener {
            currentFilterStatus = "SEDANG_DIPROSES"
            updateChipSelection(chipFilterInProgress)
            applyCurrentFilter()
        }

        chipFilterCompleted?.let { chipCompleted ->
            chipCompleted.setOnClickListener {
                currentFilterStatus = "SELESAI"
                updateChipSelection(chipCompleted)
                applyCurrentFilter()
            }
        }
    }

    private fun applyCurrentFilter() {
        val filtered = when (currentFilterStatus) {
            "TERVERIFIKASI" -> verifiedReportsList.filter { it.status == "TERVERIFIKASI" }
            "SEDANG_DIPROSES" -> verifiedReportsList.filter { it.status == "SEDANG_DIPROSES" }
            "SELESAI" -> verifiedReportsList.filter { it.status == "SELESAI" }
            else -> verifiedReportsList.toList()
        }

        filteredReportDtos = when (currentFilterStatus) {
            "TERVERIFIKASI" -> verifiedReportDtos.filter { it.resolvedStatusKey() == "TERVERIFIKASI" }
            "SEDANG_DIPROSES" -> verifiedReportDtos.filter { it.resolvedStatusKey() == "DIPROSES" }
            "SELESAI" -> verifiedReportDtos.filter { it.resolvedStatusKey() == "SELESAI" }
            else -> verifiedReportDtos.toList()
        }

        reportAdapter.updateList(filtered)
        if (filtered.isEmpty()) {
            tvEmptyReports.visibility = View.VISIBLE
            rvReports.visibility = View.GONE
        } else {
            tvEmptyReports.visibility = View.GONE
            rvReports.visibility = View.VISIBLE
        }

        syncGovMapMarkers()
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
            enableMyLocationOnGovMap()
            if (!hasCenteredGovMap) {
                detectAndCenterGovLocation()
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        syncGovMapMarkers()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationOnGovMap() {
        if (hasLocationPermission()) {
            googleMap?.isMyLocationEnabled = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun detectAndCenterGovLocation() {
        if (!hasLocationPermission()) return
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                    hasCenteredGovMap = true
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

    private fun showGovInfoWindowTemporarily(marker: Marker) {
        hideGovInfoWindowRunnable?.let { govInfoWindowHandler.removeCallbacks(it) }
        marker.showInfoWindow()
        val runnable = Runnable {
            try {
                marker.hideInfoWindow()
            } catch (_: Exception) {}
        }
        hideGovInfoWindowRunnable = runnable
        govInfoWindowHandler.postDelayed(runnable, 5000)
    }

    private fun syncGovMapMarkers() {
        val map = googleMap ?: return
        map.clear()

        val mapReports = when (govMapRepairFilterMode) {
            "UNRESOLVED" -> filteredReportDtos.filter { it.resolvedStatusKey() != "SELESAI" }
            "RESOLVED" -> filteredReportDtos.filter { it.resolvedStatusKey() == "SELESAI" }
            else -> filteredReportDtos
        }

        map.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View {
                val density = resources.displayMetrics.density
                val report = mapReports.find { it.resolvedId() == (marker.tag as? Long) }
                val locationText = report?.resolvedLocationName() ?: (marker.title ?: "Lokasi Kerusakan")

                return TextView(this@PemerintahDashboardActivity).apply {
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
            val pinHue = resolvePinHueBySeverity(report.tingkatKeparahan)

            val marker = map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(report.resolvedLocationName())
                    .snippet(report.resolvedTitle())
                    .icon(BitmapDescriptorFactory.defaultMarker(pinHue))
            )
            marker?.tag = report.resolvedId()
        }

        map.setOnMarkerClickListener { marker ->
            val report = mapReports.find { it.resolvedId() == (marker.tag as? Long) }
            showGovInfoWindowTemporarily(marker)
            if (report != null) {
                selectedGovPinReport = report
                val severityLabel = resolveSeverityLabel(report.tingkatKeparahan)
                val repairState = if (report.resolvedStatusKey() == "SELESAI") "Sudah Diperbaiki" else "Belum Diperbaiki"
                tvGovPinPopupLocation.text = report.resolvedLocationName()
                tvGovPinPopupTitle.text = "${report.resolvedTitle()} • $severityLabel ($repairState)"
                cardGovSelectedPinPopup.visibility = View.VISIBLE
            }
            false
        }

        map.setOnInfoWindowClickListener { marker ->
            marker.hideInfoWindow()
        }

        map.setOnMapClickListener {
            cardGovSelectedPinPopup.visibility = View.GONE
            selectedGovPinReport = null
        }
    }

    private fun selectChip(chip: TextView) {
        chip.background = ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
        chip.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        chip.setTypeface(null, Typeface.BOLD)
    }

    private fun unselectChip(chip: TextView) {
        chip.background = ContextCompat.getDrawable(this, R.drawable.bg_chip_unselected)
        chip.setTextColor(Color.parseColor("#434653"))
        chip.setTypeface(null, Typeface.NORMAL)
    }
}
