package com.example.pinpoint.user

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.example.pinpoint.adapter.UserReportAdapter
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.KategoriResponse
import com.example.pinpoint.network.LaporanResponse
import com.example.pinpoint.network.NotifikasiResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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

class DashboardActivity : AppCompatActivity() {

    private var backPressedTime: Long = 0
    private var isMapExpanded = false

    // Google Maps
    private lateinit var googleDashboardMapView: MapView
    private lateinit var cardDashboardMap: CardView
    private lateinit var cardSelectedPinPopup: CardView
    private lateinit var tvPinPopupLocation: TextView
    private lateinit var tvPinPopupTitle: TextView
    private lateinit var btnPinPopupDetail: TextView
    private lateinit var tvToggleExpandMap: TextView
    private lateinit var tvPortalLocationSubtitle: TextView
    private lateinit var tvDashboardMapLocationPill: TextView
    private var googleMap: GoogleMap? = null
    private var selectedPinReport: LaporanResponse? = null
    private val mapInfoWindowHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var hideInfoWindowRunnable: Runnable? = null

    // GPS
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Filter & List Laporan
    private lateinit var etSearch: EditText
    private lateinit var llUserCategoryChips: LinearLayout
    private lateinit var rvUserDashboardReports: RecyclerView
    private lateinit var tvEmptyUserDashboardReports: TextView
    private lateinit var userReportAdapter: UserReportAdapter

    private var allReports = listOf<LaporanResponse>()
    private var filteredReports = listOf<LaporanResponse>()
    private var categoriesList = listOf<KategoriResponse>()
    private var selectedCategoryId: Long? = null
    private var selectedCategoryName: String? = null
    // Filter peta global: "ALL", "UNRESOLVED" (Belum Diperbaiki), "RESOLVED" (Sudah Diperbaiki)
    private var mapRepairFilterMode: String = "ALL"

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            enableMyLocationLayer()
            detectAndCenterDashboardLocation()
        } else {
            tvPortalLocationSubtitle.text = "Lokasi Default: Kota Makassar, Sulawesi Selatan"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initViews()
        setupRecyclerView()
        setupGoogleDashboardMap(savedInstanceState)
        setupSearchAndMapControls()
        setupBottomNavigation()

        loadUserNotificationsBadge()
        loadCategoriesFromDatabase()
        loadAllReportsFromDatabase()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isMapExpanded) {
                        toggleExpandMap()
                        return
                    }
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

    private fun initViews() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val savedName = intent.getStringExtra("EXTRA_NAME")
            ?: sharedPreferences.getString("KEY_NAME", null)
        findViewById<TextView>(R.id.tvUserName).text =
            if (!savedName.isNullOrBlank()) savedName else "Pengguna"

        tvPortalLocationSubtitle = findViewById(R.id.tvPortalLocationSubtitle)
        tvDashboardMapLocationPill = findViewById(R.id.tvDashboardMapLocationPill)
        cardDashboardMap = findViewById(R.id.cardDashboardMap)
        cardSelectedPinPopup = findViewById(R.id.cardSelectedPinPopup)
        tvPinPopupLocation = findViewById(R.id.tvPinPopupLocation)
        tvPinPopupTitle = findViewById(R.id.tvPinPopupTitle)
        btnPinPopupDetail = findViewById(R.id.btnPinPopupDetail)
        tvToggleExpandMap = findViewById(R.id.tvToggleExpandMap)
        etSearch = findViewById(R.id.etSearch)
        llUserCategoryChips = findViewById(R.id.llUserCategoryChips)
        rvUserDashboardReports = findViewById(R.id.rvUserDashboardReports)
        tvEmptyUserDashboardReports = findViewById(R.id.tvEmptyUserDashboardReports)

        btnPinPopupDetail.setOnClickListener {
            selectedPinReport?.let { openReportDetail(it) }
        }
        cardSelectedPinPopup.setOnClickListener {
            selectedPinReport?.let { openReportDetail(it) }
        }
    }

    private fun setupRecyclerView() {
        rvUserDashboardReports.layoutManager = LinearLayoutManager(this)
        userReportAdapter = UserReportAdapter(
            reports = emptyList(),
            getSupportCount = { report -> getEffectiveSupportCount(report) },
            isSupportedByMe = { report -> isReportSupportedByMe(report.id ?: 0L) },
            onSupportToggle = { report -> toggleSupportForReport(report) },
            onDetailClick = { report ->
                openReportDetail(report)
            }
        )
        rvUserDashboardReports.adapter = userReportAdapter
    }

    private fun openReportDetail(report: LaporanResponse) {
        val intent = Intent(this, DetailLaporanActivity::class.java).apply {
            putExtra("EXTRA_REPORT_ID", report.id ?: -1L)
            putExtra("EXTRA_REPORT_TITLE", report.resolvedTitle())
            putExtra("EXTRA_REPORT_DESC", report.resolvedDescription())
            putExtra("EXTRA_REPORT_LOCATION", report.resolvedLocation())
            putExtra("EXTRA_REPORT_STATUS", report.idStatus ?: 1L)
            putExtra("EXTRA_REPORTER_NAME", report.resolvedReporterName())
        }
        startActivity(intent)
    }

    private fun setupSearchAndMapControls() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFiltersAndSort()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<View>(R.id.btnTopNotif).setOnClickListener {
            startActivity(Intent(this, NotifikasiActivity::class.java))
        }

        findViewById<View>(R.id.btnCenterLocation)?.setOnClickListener {
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

        findViewById<View>(R.id.btnZoomIn).setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomIn())
        }

        findViewById<View>(R.id.btnZoomOut).setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomOut())
        }

        findViewById<View>(R.id.btnExpandDashboardMap).setOnClickListener {
            toggleExpandMap()
        }

        findViewById<View>(R.id.barExpandDashboardMap).setOnClickListener {
            toggleExpandMap()
        }

        findViewById<View>(R.id.btnQuickReport).setOnClickListener {
            startActivity(Intent(this, FormPelaporanActivity::class.java))
        }

        val chipAll = findViewById<TextView?>(R.id.chipMapFilterAll)
        val chipUnresolved = findViewById<TextView?>(R.id.chipMapFilterUnresolved)
        val chipResolved = findViewById<TextView?>(R.id.chipMapFilterResolved)
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
            mapRepairFilterMode = "ALL"
            updateRepairChipStyles(chipAll)
            syncReportMarkersOnGoogleMap()
        }
        chipUnresolved?.setOnClickListener {
            mapRepairFilterMode = "UNRESOLVED"
            updateRepairChipStyles(chipUnresolved)
            syncReportMarkersOnGoogleMap()
        }
        chipResolved?.setOnClickListener {
            mapRepairFilterMode = "RESOLVED"
            updateRepairChipStyles(chipResolved)
            syncReportMarkersOnGoogleMap()
        }
    }

    private fun toggleExpandMap() {
        isMapExpanded = !isMapExpanded
        val density = resources.displayMetrics.density
        val targetHeightDp = if (isMapExpanded) 480 else 260
        val params = cardDashboardMap.layoutParams
        params.height = (targetHeightDp * density).toInt()
        cardDashboardMap.layoutParams = params
        tvToggleExpandMap.text = if (isMapExpanded) "Kecilkan Peta" else "Perluas Peta"
    }

    private fun isNotificationReadLocally(notif: NotifikasiResponse): Boolean {
        val readPrefs = getSharedPreferences("UserNotificationReadPrefs", MODE_PRIVATE)
        val key = "${getCurrentUserKey()}_${notif.id ?: 0L}_${notif.idLaporan ?: 0L}_${notif.tipe ?: ""}_${notif.judul ?: ""}"
        return readPrefs.getBoolean(key, false)
    }

    private fun loadUserNotificationsBadge() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val token = sharedPreferences.getString("KEY_TOKEN", null)
        val tvTopBadgeCount = findViewById<TextView>(R.id.tvTopBadgeCount)
        val tvNavBadgeCount = findViewById<TextView?>(R.id.tvNavBadgeCount)

        if (!token.isNullOrEmpty()) {
            ApiClient.instance.getNotifications("Bearer $token")
                .enqueue(object : Callback<List<NotifikasiResponse>> {
                    override fun onResponse(
                        call: Call<List<NotifikasiResponse>>,
                        response: Response<List<NotifikasiResponse>>
                    ) {
                        if (response.isSuccessful) {
                            val notifs = response.body() ?: emptyList()
                            val unread = notifs.count {
                                it.sudahDibaca == false && !isNotificationReadLocally(it)
                            }
                            if (unread > 0) {
                                tvTopBadgeCount.visibility = View.VISIBLE
                                tvTopBadgeCount.text = unread.toString()
                                tvNavBadgeCount?.visibility = View.VISIBLE
                                tvNavBadgeCount?.text = unread.toString()
                            } else {
                                tvTopBadgeCount.visibility = View.GONE
                                tvNavBadgeCount?.visibility = View.GONE
                            }
                        }
                    }

                    override fun onFailure(call: Call<List<NotifikasiResponse>>, t: Throwable) {}
                })
        } else {
            tvTopBadgeCount.visibility = View.GONE
            tvNavBadgeCount?.visibility = View.GONE
        }
    }

    private fun loadCategoriesFromDatabase() {
        ApiClient.instance.getCategories().enqueue(object : Callback<List<KategoriResponse>> {
            override fun onResponse(
                call: Call<List<KategoriResponse>>,
                response: Response<List<KategoriResponse>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    categoriesList = response.body()!!.filter { it.aktif != false }
                    renderCategoryChips()
                }
            }

            override fun onFailure(call: Call<List<KategoriResponse>>, t: Throwable) {}
        })
    }

    private fun renderCategoryChips() {
        llUserCategoryChips.removeAllViews()
        val density = resources.displayMetrics.density
        val padH = (16 * density).toInt()
        val padV = (9 * density).toInt()
        val marginEnd = (8 * density).toInt()

        val chipViews = mutableListOf<Pair<TextView, KategoriResponse?>>()

        fun createChip(label: String, category: KategoriResponse?): TextView {
            val tv = TextView(this)
            tv.text = label
            tv.textSize = 12f
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

        val allChip = createChip("Semua (${allReports.size})", null)
        llUserCategoryChips.addView(allChip)

        categoriesList.forEach { cat ->
            val count = allReports.count { r ->
                (cat.id != null && r.idKategori == cat.id) ||
                    r.resolvedCategoryName().equals(cat.namaKategori ?: "", ignoreCase = true)
            }
            val chip = createChip("${cat.namaKategori ?: "Kategori"} ($count)", cat)
            llUserCategoryChips.addView(chip)
        }

        fun updateChipStyles() {
            chipViews.forEach { (tv, cat) ->
                val isSelected = (selectedCategoryId == null && cat == null) ||
                    (cat != null && cat.id == selectedCategoryId)
                if (isSelected) {
                    tv.setBackgroundResource(R.drawable.bg_chip_blue_active)
                    tv.setTextColor(Color.WHITE)
                    tv.setTypeface(null, Typeface.BOLD)
                } else {
                    tv.setBackgroundResource(R.drawable.bg_chip_inactive)
                    tv.setTextColor(Color.parseColor("#334155"))
                    tv.setTypeface(null, Typeface.NORMAL)
                }
            }
        }

        chipViews.forEach { (tv, cat) ->
            tv.setOnClickListener {
                selectedCategoryId = cat?.id
                selectedCategoryName = cat?.namaKategori
                updateChipStyles()
                applyFiltersAndSort()
            }
        }

        updateChipStyles()
    }

    private fun loadAllReportsFromDatabase() {
        ApiClient.instance.getReportsForMap()
            .enqueue(object : Callback<List<LaporanResponse>> {
                override fun onResponse(
                    call: Call<List<LaporanResponse>>,
                    response: Response<List<LaporanResponse>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        allReports = response.body()!!
                        renderCategoryChips()
                        applyFiltersAndSort()
                    }
                }

                override fun onFailure(call: Call<List<LaporanResponse>>, t: Throwable) {}
            })
    }

    private fun applyFiltersAndSort() {
        val query = etSearch.text?.toString()?.trim()?.lowercase() ?: ""

        val matched = allReports.filter { r ->
            val matchesCat = (selectedCategoryId == null && selectedCategoryName.isNullOrBlank()) ||
                (selectedCategoryId != null && r.idKategori == selectedCategoryId) ||
                (!selectedCategoryName.isNullOrBlank() && r.resolvedCategoryName().equals(selectedCategoryName, ignoreCase = true))

            val matchesQuery = query.isEmpty() ||
                r.resolvedTitle().lowercase().contains(query) ||
                r.resolvedLocation().lowercase().contains(query) ||
                r.resolvedCategoryName().lowercase().contains(query) ||
                r.resolvedDescription().lowercase().contains(query) ||
                r.resolvedCode().lowercase().contains(query)

            matchesCat && matchesQuery
        }

        // Urutkan berdasarkan kerusakan yang paling banyak didukung oleh user
        filteredReports = matched.sortedWith(
            compareByDescending<LaporanResponse> { getEffectiveSupportCount(it) }
                .thenByDescending { it.id ?: 0L }
        )

        userReportAdapter.updateData(filteredReports)
        if (filteredReports.isEmpty()) {
            tvEmptyUserDashboardReports.visibility = View.VISIBLE
            rvUserDashboardReports.visibility = View.GONE
        } else {
            tvEmptyUserDashboardReports.visibility = View.GONE
            rvUserDashboardReports.visibility = View.VISIBLE
        }

        syncReportMarkersOnGoogleMap()
    }

    private fun getSupportPrefs() = getSharedPreferences("UserReportSupportPrefs", MODE_PRIVATE)

    private fun getCurrentUserKey(): String {
        val session = getSharedPreferences("UserSession", MODE_PRIVATE)
        return (session.getString("KEY_EMAIL", "guest") ?: "guest").lowercase()
    }

    private fun isReportSupportedByMe(reportId: Long): Boolean {
        return getSupportPrefs().getBoolean("SUPPORTED_${getCurrentUserKey()}_$reportId", false)
    }

    private fun getEffectiveSupportCount(report: LaporanResponse): Int {
        val id = report.id ?: 0L
        val prefs = getSupportPrefs()
        val savedCount = prefs.getInt("COUNT_$id", -1)
        val serverCount = report.supportCount ?: 0
        val supportedByMe = isReportSupportedByMe(id)
        val base = if (savedCount >= 0) maxOf(savedCount, serverCount) else serverCount
        return if (supportedByMe && base == 0) 1 else base
    }

    private fun toggleSupportForReport(report: LaporanResponse) {
        val id = report.id ?: return
        val prefs = getSupportPrefs()
        val userKey = getCurrentUserKey()
        val currentlySupported = isReportSupportedByMe(id)
        val currentCount = getEffectiveSupportCount(report)

        val newSupported = !currentlySupported
        val newCount = if (newSupported) currentCount + 1 else maxOf(0, currentCount - 1)

        prefs.edit()
            .putBoolean("SUPPORTED_${userKey}_$id", newSupported)
            .putInt("COUNT_$id", newCount)
            .apply()

        val session = getSharedPreferences("UserSession", MODE_PRIVATE)
        val token = session.getString("KEY_TOKEN", null)
        if (!token.isNullOrEmpty()) {
            ApiClient.instance.supportReport("Bearer $token", id)
                .enqueue(object : Callback<Map<String, Any>> {
                    override fun onResponse(
                        call: Call<Map<String, Any>>,
                        response: Response<Map<String, Any>>
                    ) {}
                    override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {}
                })
        }

        applyFiltersAndSort()
    }

    // =========================================================
    // GOOGLE MAPS & LOKASI USER LOGIN
    // =========================================================

    private fun setupGoogleDashboardMap(savedInstanceState: Bundle?) {
        googleDashboardMapView = findViewById(R.id.googleDashboardMapView)
        googleDashboardMapView.onCreate(savedInstanceState)

        googleDashboardMapView.getMapAsync { map ->
            googleMap = map
            map.uiSettings.isZoomControlsEnabled = false
            map.uiSettings.isMapToolbarEnabled = false
            map.uiSettings.isCompassEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = false

            val defaultPosition = LatLng(-5.147665, 119.432731)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPosition, 15f))

            syncReportMarkersOnGoogleMap()

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

    private fun getMapFilteredReports(): List<LaporanResponse> {
        return when (mapRepairFilterMode) {
            "UNRESOLVED" -> filteredReports.filter { it.idStatus != 5L }
            "RESOLVED" -> filteredReports.filter { it.idStatus == 5L }
            else -> filteredReports
        }
    }

    private fun showInfoWindowTemporarily(marker: Marker) {
        hideInfoWindowRunnable?.let { mapInfoWindowHandler.removeCallbacks(it) }
        marker.showInfoWindow()
        val runnable = Runnable {
            try {
                marker.hideInfoWindow()
            } catch (_: Exception) {}
        }
        hideInfoWindowRunnable = runnable
        mapInfoWindowHandler.postDelayed(runnable, 5000)
    }

    private fun syncReportMarkersOnGoogleMap() {
        val map = googleMap ?: return
        map.clear()
        val mapReports = getMapFilteredReports()

        // Custom InfoWindow bulat dengan sudut membulat: hanya menampilkan nama jalan/lokasi (hilang otomatis 5 detik)
        map.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View {
                val density = resources.displayMetrics.density
                val report = mapReports.find { it.id == marker.tag }
                val locationText = report?.resolvedLocation() ?: (marker.title ?: "Lokasi Kerusakan")

                return TextView(this@DashboardActivity).apply {
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
            val latitude = report.latitude ?: return@forEach
            val longitude = report.longitude ?: return@forEach
            if (latitude == 0.0 && longitude == 0.0) return@forEach
            val position = LatLng(latitude, longitude)
            val pinHue = resolvePinHueBySeverity(report.tingkatKeparahan)

            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(report.resolvedLocation())
                    .snippet(report.resolvedTitle())
                    .icon(BitmapDescriptorFactory.defaultMarker(pinHue))
            )
            marker?.tag = report.id
        }

        map.setOnMarkerClickListener { marker ->
            val report = mapReports.find { it.id == marker.tag }
            showInfoWindowTemporarily(marker)

            if (report != null) {
                selectedPinReport = report
                val severityLabel = resolveSeverityLabel(report.tingkatKeparahan)
                val repairState = if (report.idStatus == 5L) "Sudah Diperbaiki" else "Belum Diperbaiki"
                tvPinPopupLocation.text = report.resolvedLocation()
                tvPinPopupTitle.text = "${report.resolvedTitle()} • $severityLabel ($repairState)"
                cardSelectedPinPopup.visibility = View.VISIBLE
            }
            false
        }

        map.setOnInfoWindowClickListener { marker ->
            marker.hideInfoWindow()
        }

        map.setOnMapClickListener {
            cardSelectedPinPopup.visibility = View.GONE
            selectedPinReport = null
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun detectAndCenterDashboardLocation() {
        if (!hasLocationPermission()) return

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    onUserLocationDetected(location.latitude, location.longitude)
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                        if (lastLocation != null) {
                            onUserLocationDetected(lastLocation.latitude, lastLocation.longitude)
                        } else {
                            tvPortalLocationSubtitle.text = "Wilayah Kota Makassar, Sulawesi Selatan"
                        }
                    }
                }
            }
    }

    private fun onUserLocationDetected(latitude: Double, longitude: Double) {
        val position = LatLng(latitude, longitude)
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16f))

        tvPortalLocationSubtitle.text = String.format(
            Locale.US,
            "Titik Lokasi Anda: %.4f, %.4f",
            latitude,
            longitude
        )

        Thread {
            try {
                val geocoder = Geocoder(this@DashboardActivity, Locale("id", "ID"))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        val addr = addresses.firstOrNull()
                        val areaText = buildAreaLabel(addr, latitude, longitude)
                        runOnUiThread {
                            tvPortalLocationSubtitle.text = "Lokasi Anda: $areaText"
                            tvDashboardMapLocationPill.text = areaText
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    val addr = addresses?.firstOrNull()
                    val areaText = buildAreaLabel(addr, latitude, longitude)
                    runOnUiThread {
                        tvPortalLocationSubtitle.text = "Lokasi Anda: $areaText"
                        tvDashboardMapLocationPill.text = areaText
                    }
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun buildAreaLabel(
        address: android.location.Address?,
        lat: Double,
        lng: Double
    ): String {
        if (address == null) {
            return String.format(Locale.US, "Koordinat %.4f, %.4f", lat, lng)
        }
        val parts = mutableListOf<String>()
        if (!address.thoroughfare.isNullOrBlank()) parts.add(address.thoroughfare)
        if (!address.subLocality.isNullOrBlank() && !parts.contains(address.subLocality)) {
            parts.add(address.subLocality)
        }
        if (!address.locality.isNullOrBlank() && !parts.contains(address.locality)) {
            parts.add(address.locality)
        }
        if (!address.subAdminArea.isNullOrBlank() && !parts.contains(address.subAdminArea)) {
            parts.add(address.subAdminArea)
        }
        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            address.getAddressLine(0) ?: String.format(Locale.US, "Koordinat %.4f, %.4f", lat, lng)
        }
    }

    private fun setupBottomNavigation() {
        com.example.pinpoint.admin.NavbarUtils.setupUserNavbar(this)
    }

    override fun onResume() {
        super.onResume()
        if (::googleDashboardMapView.isInitialized) {
            googleDashboardMapView.onResume()
        }
        loadUserNotificationsBadge()
        loadCategoriesFromDatabase()
        loadAllReportsFromDatabase()
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
