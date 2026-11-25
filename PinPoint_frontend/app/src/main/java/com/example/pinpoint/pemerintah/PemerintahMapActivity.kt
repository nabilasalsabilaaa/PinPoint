package com.example.pinpoint.pemerintah

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
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
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.pinpoint.PhotoSliderDialog
import com.example.pinpoint.R
import com.example.pinpoint.admin.NavbarUtils
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.CategoryDto
import com.example.pinpoint.network.ReportDto
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class PemerintahMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var activeRoutePoints = mutableListOf<LatLng>()
    private var activeRouteReportId: String? = null
    private var pendingRouteReportId: String? = null
    private var routePolyline: Polyline? = null
    private val routeExecutor = Executors.newSingleThreadExecutor()

    private lateinit var etSearchMap: EditText
    private lateinit var llMapCategoryChips: LinearLayout

    // Tampilan 1: List Vertikal
    private lateinit var cardMapListContainer: CardView
    private lateinit var rvMapReportList: RecyclerView
    private lateinit var tvEmptyMapReportList: TextView
    private lateinit var tvMapListTotalCount: TextView
    private lateinit var compactListAdapter: MapCompactListAdapter

    // Tampilan 2: Floating Carousel ViewPager2 + Tombol X
    private lateinit var layoutFloatingCarouselContainer: LinearLayout
    private lateinit var vpMapFloatingCards: ViewPager2
    private lateinit var btnCloseFloatingCarousel: View
    private lateinit var floatingCarouselAdapter: MapFloatingCarouselAdapter
    private var isFloatingCarouselVisible = false
    private var isUpdatingViewPagerProgrammatically = false

    private val incomingReports = mutableListOf<ReportDto>()
    private val filteredReports = mutableListOf<ReportDto>()
    private val mapMarkers = mutableListOf<Marker>()

    private var selectedReportIndex = 0
    private var selectedCategoryId: Long? = null
    private var selectedCategoryName: String? = null
    private var categoriesList = listOf<CategoryDto>()

    private var isSatelliteMode = false
    private var hasCenteredToUserLocation = false

    private val autoRefreshHandler = Handler(Looper.getMainLooper())
    private var hideInfoWindowRunnable: Runnable? = null
    private val autoRefreshRunnable = object : Runnable {
        override fun run() {
            loadCategoriesAndReports()
            autoRefreshHandler.postDelayed(this, 15000)
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            enableMyLocationOnMap()
            startRealtimeLocationUpdates()
            detectAndMoveToCurrentLocation(animate = true)

            pendingRouteReportId?.let { requestedId ->
                val targetIndex = filteredReports.indexOfFirst {
                    it.resolvedId().toString() == requestedId
                }
                if (targetIndex >= 0) {
                    val targetReport = filteredReports[targetIndex]
                    pendingRouteReportId = null
                    showFloatingCarouselAt(targetIndex, focusMap = true)
                    startInAppRoute(targetReport)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pemerintah_map)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        pendingRouteReportId = intent.getStringExtra(EXTRA_OPEN_ROUTE_REPORT_ID)

        initViews()
        setupListAndCarousel()
        setupSearchAndActions()
        NavbarUtils.setupPemerintahNavbar(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.pemerintahMapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onResume() {
        super.onResume()
        autoRefreshHandler.post(autoRefreshRunnable)
        if (hasLocationPermission()) {
            startRealtimeLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable)
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
    }

    private fun initViews() {
        etSearchMap = findViewById(R.id.etSearchMap)
        llMapCategoryChips = findViewById(R.id.llMapCategoryChips)

        cardMapListContainer = findViewById(R.id.cardMapListContainer)
        rvMapReportList = findViewById(R.id.rvMapReportList)
        tvEmptyMapReportList = findViewById(R.id.tvEmptyMapReportList)
        tvMapListTotalCount = findViewById(R.id.tvMapListTotalCount)

        layoutFloatingCarouselContainer = findViewById(R.id.layoutFloatingCarouselContainer)
        vpMapFloatingCards = findViewById(R.id.vpMapFloatingCards)
        btnCloseFloatingCarousel = findViewById(R.id.btnCloseFloatingCarousel)
    }

    private fun setupListAndCarousel() {
        // 1. Setup List Vertikal (Tampilan 1 Default)
        rvMapReportList.layoutManager = LinearLayoutManager(this)
        compactListAdapter = MapCompactListAdapter(emptyList()) { index ->
            showFloatingCarouselAt(index, focusMap = true)
        }
        rvMapReportList.adapter = compactListAdapter

        // 2. Setup Floating Carousel ViewPager2 (Tampilan 2 Swipe Horizontal dengan Peek Kiri-Kanan)
        floatingCarouselAdapter = MapFloatingCarouselAdapter(
            items = emptyList(),
            onPhotoClick = { report ->
                val photos = report.resolvedPhotoUrls()
                if (photos.isNotEmpty()) {
                    PhotoSliderDialog.show(this, "Foto Laporan (${photos.size} Foto)", photos, 0)
                }
            },
            onRouteClick = { report ->
                startInAppRoute(report)
            },
            onDetailClick = { report ->
                val intent = Intent(this, PemerintahDetailTaskActivity::class.java)
                intent.putExtra("REPORT_ID", report.resolvedId().toString())
                startActivity(intent)
            }
        )
        vpMapFloatingCards.adapter = floatingCarouselAdapter
        vpMapFloatingCards.offscreenPageLimit = 1
        (vpMapFloatingCards.getChildAt(0) as? RecyclerView)?.overScrollMode = View.OVER_SCROLL_NEVER

        // Efek transisi geser horizontal yang halus antar floating card
        vpMapFloatingCards.setPageTransformer { page, position ->
            val absPos = kotlin.math.abs(position)
            page.scaleY = 0.92f + (1f - absPos.coerceAtMost(1f)) * 0.08f
        }

        // Saat card digeser ke samping (kiri/kanan), pindahkan fokus titik di peta!
        vpMapFloatingCards.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (isUpdatingViewPagerProgrammatically) return
                if (position in filteredReports.indices && selectedReportIndex != position) {
                    clearActiveRoute()
                    selectedReportIndex = position
                    focusSelectedReportOnMap()
                }
            }
        })

        // Tombol X untuk menutup Floating Card & kembali ke List Vertikal
        btnCloseFloatingCarousel.setOnClickListener {
            showVerticalListView()
        }
    }

    private fun showFloatingCarouselAt(index: Int, focusMap: Boolean) {
        if (filteredReports.isEmpty()) return
        val safeIndex = index.coerceIn(0, filteredReports.lastIndex)
        if (selectedReportIndex != safeIndex) {
            clearActiveRoute()
        }
        selectedReportIndex = safeIndex
        isFloatingCarouselVisible = true

        cardMapListContainer.visibility = View.GONE
        layoutFloatingCarouselContainer.visibility = View.VISIBLE

        isUpdatingViewPagerProgrammatically = true
        vpMapFloatingCards.setCurrentItem(safeIndex, true)
        vpMapFloatingCards.post { isUpdatingViewPagerProgrammatically = false }

        if (focusMap) {
            focusSelectedReportOnMap()
        }
    }

    private fun showVerticalListView() {
        isFloatingCarouselVisible = false
        layoutFloatingCarouselContainer.visibility = View.GONE
        cardMapListContainer.visibility = View.VISIBLE
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false

        val defaultPos = LatLng(-5.147665, 119.432731)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPos, 14f))

        if (hasLocationPermission()) {
            enableMyLocationOnMap()
            startRealtimeLocationUpdates()
            if (!hasCenteredToUserLocation) {
                detectAndMoveToCurrentLocation(animate = false)
            }
        } else {
            requestLocationPermission()
        }

        // Saat salah satu pin di peta diklik -> tunjukkan titiknya & ubah tampilan ke Floating Carousel Card!
        map.setOnMarkerClickListener { marker ->
            val index = marker.tag as? Int
            if (index != null && index in filteredReports.indices) {
                showFloatingCarouselAt(index, focusMap = true)
            }
            true
        }

        map.setOnInfoWindowClickListener { marker ->
            val index = marker.tag as? Int
            val report = if (index != null && index in filteredReports.indices) filteredReports[index] else null
            if (report != null) {
                val intent = Intent(this, PemerintahDetailTaskActivity::class.java)
                intent.putExtra("REPORT_ID", report.resolvedId().toString())
                startActivity(intent)
            }
        }

        // Klik area kosong di peta kembali ke Mode List Vertikal
        map.setOnMapClickListener {
            if (isFloatingCarouselVisible) {
                showVerticalListView()
            }
        }

        map.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View {
                val density = resources.displayMetrics.density
                val index = marker.tag as? Int
                val report = if (index != null && index in filteredReports.indices) filteredReports[index] else null
                val locationText = report?.resolvedLocationName() ?: (marker.title ?: "Lokasi Kerusakan")

                return TextView(this@PemerintahMapActivity).apply {
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

        syncMarkersOnMap()
    }

    private fun setupSearchAndActions() {
        val btnGps = findViewById<View>(R.id.btnGps)
        val btnLayer = findViewById<View>(R.id.btnLayer)

        findViewById<View>(R.id.btnMapZoomOut)?.setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomOut())
        }
        findViewById<View>(R.id.btnMapZoomIn)?.setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomIn())
        }

        btnGps.setOnClickListener {
            if (hasLocationPermission()) {
                detectAndMoveToCurrentLocation(animate = true)
            } else {
                requestLocationPermission()
            }
        }

        btnLayer.setOnClickListener {
            isSatelliteMode = !isSatelliteMode
            googleMap?.mapType = if (isSatelliteMode) GoogleMap.MAP_TYPE_HYBRID else GoogleMap.MAP_TYPE_NORMAL
            val mode = if (isSatelliteMode) "Satelit" else "Normal"
            Toast.makeText(this, "Mode Peta: $mode", Toast.LENGTH_SHORT).show()
        }

        etSearchMap.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyMapFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationOnMap() {
        if (hasLocationPermission()) {
            googleMap?.isMyLocationEnabled = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRealtimeLocationUpdates() {
        if (!hasLocationPermission()) return

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2500)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {}
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    @SuppressLint("MissingPermission")
    private fun detectAndMoveToCurrentLocation(animate: Boolean) {
        if (!hasLocationPermission()) return

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    val update = CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                    if (animate) googleMap?.animateCamera(update) else googleMap?.moveCamera(update)
                    hasCenteredToUserLocation = true
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            val latLng = LatLng(lastLoc.latitude, lastLoc.longitude)
                            val update = CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                            if (animate) googleMap?.animateCamera(update) else googleMap?.moveCamera(update)
                            hasCenteredToUserLocation = true
                        }
                    }
                }
            }
    }

    private fun showInfoWindowTemporarily(marker: Marker?) {
        if (marker == null) return
        hideInfoWindowRunnable?.let { autoRefreshHandler.removeCallbacks(it) }
        marker.showInfoWindow()
        val runnable = Runnable {
            try {
                marker.hideInfoWindow()
            } catch (_: Exception) {}
        }
        hideInfoWindowRunnable = runnable
        autoRefreshHandler.postDelayed(runnable, 5000)
    }

    private fun focusSelectedReportOnMap() {
        val report = filteredReports.getOrNull(selectedReportIndex) ?: return
        val lat = report.latitude ?: return
        val lng = report.longitude ?: return
        val pos = LatLng(lat, lng)
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16.5f))

        val marker = mapMarkers.getOrNull(selectedReportIndex)
        showInfoWindowTemporarily(marker)
    }

    private fun loadCategoriesAndReports() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val rawToken = sharedPreferences.getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

        ApiClient.fetchWithFallback(
            callFactory = { api -> api.getAllCategories(token) },
            onResponse = { _, response ->
                if (response.isSuccessful && response.body() != null) {
                    categoriesList = response.body()!!.filter { it.resolvedIsActive() }
                    renderCategoryChips()
                }
            },
            onFailure = { _, _ -> }
        )

        ApiClient.fetchWithFallback(
            callFactory = { api -> api.getAllReports(token, null) },
            onResponse = { _, response ->
                if (response.isSuccessful && response.body() != null) {
                    val all = response.body()!!
                    incomingReports.clear()
                    incomingReports.addAll(
                        all.filter {
                            val st = it.resolvedStatusKey()
                            st == "TERVERIFIKASI" || st == "DIPROSES" || st == "SELESAI"
                        }
                    )
                    renderCategoryChips()
                    applyMapFilters()
                }
            },
            onFailure = { _, _ -> }
        )
    }

    private fun renderCategoryChips() {
        llMapCategoryChips.removeAllViews()
        val density = resources.displayMetrics.density
        val padH = (16 * density).toInt()
        val padV = (8 * density).toInt()
        val marginEnd = (8 * density).toInt()

        val chipViews = mutableListOf<Pair<TextView, CategoryDto?>>()

        fun createChip(label: String, category: CategoryDto?): TextView {
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

        val allChip = createChip("Semua (${incomingReports.size})", null)
        llMapCategoryChips.addView(allChip)

        categoriesList.forEach { cat ->
            val count = incomingReports.count { r ->
                (cat.resolvedId() != null && r.idKategori == cat.resolvedId()) ||
                    r.resolvedCategoryName().equals(cat.resolvedName(), ignoreCase = true)
            }
            val chip = createChip("${cat.resolvedName()} ($count)", cat)
            llMapCategoryChips.addView(chip)
        }

        fun updateChipStyles() {
            chipViews.forEach { (tv, cat) ->
                val isSelected = (selectedCategoryId == null && cat == null) ||
                    (cat != null && cat.resolvedId() == selectedCategoryId)
                if (isSelected) {
                    tv.setBackgroundResource(R.drawable.bg_chip_selected)
                    tv.setTextColor(Color.WHITE)
                    tv.setTypeface(null, Typeface.BOLD)
                } else {
                    tv.setBackgroundResource(R.drawable.bg_chip_white)
                    tv.setTextColor(Color.parseColor("#0B3C8C"))
                    tv.setTypeface(null, Typeface.BOLD)
                }
            }
        }

        chipViews.forEach { (tv, cat) ->
            tv.setOnClickListener {
                selectedCategoryId = cat?.resolvedId()
                selectedCategoryName = cat?.resolvedName()
                updateChipStyles()
                applyMapFilters()
            }
        }

        updateChipStyles()
    }

    private fun applyMapFilters() {
        val query = etSearchMap.text?.toString()?.trim()?.lowercase() ?: ""
        val previouslySelectedId = filteredReports.getOrNull(selectedReportIndex)?.id
        filteredReports.clear()

        filteredReports.addAll(
            incomingReports.filter { r ->
                val matchesCat = selectedCategoryId == null && selectedCategoryName.isNullOrBlank() ||
                    (selectedCategoryId != null && r.idKategori == selectedCategoryId) ||
                    (!selectedCategoryName.isNullOrBlank() && r.resolvedCategoryName().equals(selectedCategoryName, ignoreCase = true))

                val matchesQuery = query.isEmpty() ||
                    r.resolvedTitle().lowercase().contains(query) ||
                    r.resolvedLocationName().lowercase().contains(query) ||
                    r.resolvedCode().lowercase().contains(query) ||
                    r.resolvedCategoryName().lowercase().contains(query) ||
                    r.resolvedDescription().lowercase().contains(query)

                matchesCat && matchesQuery
            }
        )

        val previousIndex = previouslySelectedId?.let { id ->
            filteredReports.indexOfFirst { it.id == id }
        } ?: -1
        selectedReportIndex = if (previousIndex >= 0) previousIndex else 0

        // Perbarui kedua adapter (List Vertikal & Floating Carousel)
        compactListAdapter.updateList(filteredReports)
        floatingCarouselAdapter.updateList(filteredReports)
        tvMapListTotalCount.text = "${filteredReports.size} Titik Laporan"

        if (filteredReports.isEmpty()) {
            tvEmptyMapReportList.visibility = View.VISIBLE
            rvMapReportList.visibility = View.GONE
            if (isFloatingCarouselVisible) {
                showVerticalListView()
            }
        } else {
            tvEmptyMapReportList.visibility = View.GONE
            rvMapReportList.visibility = View.VISIBLE
            if (isFloatingCarouselVisible) {
                isUpdatingViewPagerProgrammatically = true
                vpMapFloatingCards.setCurrentItem(selectedReportIndex, false)
                vpMapFloatingCards.post { isUpdatingViewPagerProgrammatically = false }
            }
        }

        // Jika halaman dibuka dari Detail Laporan, pilih laporan tersebut dan tampilkan floating card + rute
        pendingRouteReportId?.let { requestedId ->
            val requestedIndex = filteredReports.indexOfFirst {
                it.resolvedId().toString() == requestedId
            }
            if (requestedIndex >= 0) {
                val requestedReport = filteredReports[requestedIndex]
                pendingRouteReportId = null
                syncMarkersOnMap()
                showFloatingCarouselAt(requestedIndex, focusMap = true)
                startInAppRoute(requestedReport)
                return
            }
        }

        if (activeRouteReportId != null &&
            filteredReports.getOrNull(selectedReportIndex)?.id != activeRouteReportId
        ) {
            clearActiveRoute()
        }
        syncMarkersOnMap()
    }

    private fun resolvePinHueBySeverity(severityRaw: String?): Float {
        val sev = (severityRaw ?: "").lowercase()
        return when {
            sev.contains("kritis") || sev.contains("darurat") || sev.contains("tinggi") || sev.contains("berat") ->
                BitmapDescriptorFactory.HUE_RED
            sev.contains("rendah") || sev.contains("ringan") ->
                BitmapDescriptorFactory.HUE_GREEN
            else ->
                BitmapDescriptorFactory.HUE_ORANGE
        }
    }

    private fun syncMarkersOnMap() {
        val map = googleMap ?: return
        map.clear()
        mapMarkers.clear()

        filteredReports.forEachIndexed { index, report ->
            val lat = report.latitude ?: return@forEachIndexed
            val lng = report.longitude ?: return@forEachIndexed
            val pos = LatLng(lat, lng)

            val markerColor = resolvePinHueBySeverity(report.tingkatKeparahan)

            val markerOptions = MarkerOptions()
                .position(pos)
                .title(report.resolvedLocationName())
                .snippet("${report.resolvedCode()}: ${report.resolvedTitle()}")
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor))

            val marker = map.addMarker(markerOptions)
            if (marker != null) {
                marker.tag = index
                mapMarkers.add(marker)
            }
        }

        if (isFloatingCarouselVisible && mapMarkers.isNotEmpty() && selectedReportIndex in mapMarkers.indices) {
            showInfoWindowTemporarily(mapMarkers[selectedReportIndex])
        }

        routePolyline = null
        drawActiveRoute(moveCamera = false)
    }

    @SuppressLint("MissingPermission")
    private fun startInAppRoute(report: ReportDto) {
        val destinationLat = report.latitude
        val destinationLng = report.longitude

        if (destinationLat == null || destinationLng == null ||
            !destinationLat.isFinite() || !destinationLng.isFinite() ||
            destinationLat !in -90.0..90.0 || destinationLng !in -180.0..180.0 ||
            (destinationLat == 0.0 && destinationLng == 0.0)
        ) {
            Toast.makeText(this, "Koordinat laporan tidak tersedia", Toast.LENGTH_LONG).show()
            return
        }

        if (!hasLocationPermission()) {
            pendingRouteReportId = report.resolvedId().toString()
            requestLocationPermission()
            Toast.makeText(this, "Izinkan akses lokasi untuk menampilkan panduan rute", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "Mengambil lokasi petugas...", Toast.LENGTH_SHORT).show()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    requestOsrmRoute(
                        LatLng(location.latitude, location.longitude),
                        LatLng(destinationLat, destinationLng),
                        report.id
                    )
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                        if (lastLocation != null) {
                            requestOsrmRoute(
                                LatLng(lastLocation.latitude, lastLocation.longitude),
                                LatLng(destinationLat, destinationLng),
                                report.id
                            )
                        } else {
                            Toast.makeText(this, "Lokasi GPS belum tersedia. Aktifkan GPS dan coba lagi.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
    }

    private fun requestOsrmRoute(origin: LatLng, destination: LatLng, reportId: String?) {
        Toast.makeText(this, "Sedang mencari rute...", Toast.LENGTH_SHORT).show()

        routeExecutor.execute {
            var connection: HttpURLConnection? = null
            try {
                val routeUrl = URL(
                    "https://routing.openstreetmap.de/routed-car/route/v1/driving/" +
                        "${origin.longitude},${origin.latitude};" +
                        "${destination.longitude},${destination.latitude}" +
                        "?overview=full&geometries=geojson"
                )
                connection = (routeUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 20000
                    readTimeout = 25000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "PinPoint-Android/1.0 (in-app route preview)")
                }

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw IllegalStateException("Layanan rute menolak permintaan ($responseCode)")
                }

                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val response = JSONObject(responseText)
                val routes = response.optJSONArray("routes")
                if (routes == null || routes.length() == 0) {
                    throw IllegalStateException("Rute menuju laporan tidak ditemukan")
                }

                val geometry = routes.getJSONObject(0).getJSONObject("geometry")
                val coordinates = geometry.getJSONArray("coordinates")
                val points = mutableListOf<LatLng>()
                for (i in 0 until coordinates.length()) {
                    val coordinate = coordinates.getJSONArray(i)
                    points.add(LatLng(coordinate.getDouble(1), coordinate.getDouble(0)))
                }

                if (points.size < 2) throw IllegalStateException("Jalur rute tidak valid")

                runOnUiThread {
                    if (filteredReports.getOrNull(selectedReportIndex)?.id != reportId) {
                        return@runOnUiThread
                    }
                    activeRoutePoints = points
                    activeRouteReportId = reportId
                    drawActiveRoute(moveCamera = true)
                    Toast.makeText(this, "Rute berhasil ditampilkan di peta", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    clearActiveRoute()
                    Toast.makeText(
                        this,
                        e.message ?: "Gagal memuat rute. Periksa koneksi internet.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun drawActiveRoute(moveCamera: Boolean) {
        val map = googleMap ?: return
        if (activeRoutePoints.size < 2) return

        routePolyline?.remove()
        routePolyline = map.addPolyline(
            PolylineOptions()
                .addAll(activeRoutePoints)
                .width(12f)
                .color(Color.parseColor("#1769E0"))
                .geodesic(true)
        )

        if (moveCamera) {
            val boundsBuilder = LatLngBounds.Builder()
            activeRoutePoints.forEach { boundsBuilder.include(it) }
            try {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
            } catch (_: IllegalStateException) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(activeRoutePoints.first(), 14f))
            }
        }
    }

    private fun clearActiveRoute() {
        activeRoutePoints.clear()
        activeRouteReportId = null
        routePolyline?.remove()
        routePolyline = null
    }

    override fun onDestroy() {
        super.onDestroy()
        routeExecutor.shutdownNow()
    }

    // =========================================================================
    // ADAPTER 1: TAMPILAN LIST VERTIKAL RINGKAS (Nomor Laporan + Kategori + Judul)
    // =========================================================================
    private class MapCompactListAdapter(
        private var items: List<ReportDto>,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<MapCompactListAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvCode: TextView = view.findViewById(R.id.tvRowReportCode)
            val tvCategory: TextView = view.findViewById(R.id.tvRowReportCategory)
            val tvStatus: TextView = view.findViewById(R.id.tvRowReportStatus)
            val tvTitle: TextView = view.findViewById(R.id.tvRowReportTitle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pemerintah_map_list_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val report = items[position]
            holder.tvCode.text = report.resolvedCode()
            holder.tvCategory.text = report.resolvedCategoryName().uppercase()
            holder.tvTitle.text = report.resolvedTitle()

            when (report.resolvedStatusKey()) {
                "SELESAI" -> {
                    holder.tvStatus.text = "Sudah Diperbaiki"
                    holder.tvStatus.setTextColor(Color.parseColor("#059669"))
                }
                "DIPROSES" -> {
                    holder.tvStatus.text = "Sedang Diproses"
                    holder.tvStatus.setTextColor(Color.parseColor("#613EEA"))
                }
                else -> {
                    holder.tvStatus.text = "Belum Diperbaiki"
                    holder.tvStatus.setTextColor(Color.parseColor("#D97706"))
                }
            }

            holder.itemView.setOnClickListener {
                onItemClick(holder.bindingAdapterPosition.coerceAtLeast(0))
            }
        }

        override fun getItemCount(): Int = items.size

        fun updateList(newItems: List<ReportDto>) {
            items = newItems.toList()
            notifyDataSetChanged()
        }
    }

    // =========================================================================
    // ADAPTER 2: TAMPILAN FLOATING CAROUSEL CARD (Bisa Digeser ke Samping)
    // =========================================================================
    private class MapFloatingCarouselAdapter(
        private var items: List<ReportDto>,
        private val onPhotoClick: (ReportDto) -> Unit,
        private val onRouteClick: (ReportDto) -> Unit,
        private val onDetailClick: (ReportDto) -> Unit
    ) : RecyclerView.Adapter<MapFloatingCarouselAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvId: TextView = view.findViewById(R.id.tvCardReportId)
            val tvCategory: TextView = view.findViewById(R.id.tvCardReportCategory)
            val tvStatusBadge: TextView = view.findViewById(R.id.tvCardReportStatusBadge)
            val tvTitle: TextView = view.findViewById(R.id.tvCardReportTitle)
            val ivThumb: ImageView = view.findViewById(R.id.ivCardReportThumb)
            val tvLocation: TextView = view.findViewById(R.id.tvCardReportLocation)
            val tvDesc: TextView = view.findViewById(R.id.tvCardReportDesc)
            val tvReporter: TextView = view.findViewById(R.id.tvCardReportReporter)
            val tvTime: TextView = view.findViewById(R.id.tvCardReportTime)
            val btnRoute: View = view.findViewById(R.id.btnCardRouteGuidance)
            val btnDetail: View = view.findViewById(R.id.btnCardReportDetail)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pemerintah_map_floating_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val report = items[position]
            val context = holder.itemView.context

            holder.tvId.text = report.resolvedCode()
            holder.tvCategory.text = report.resolvedCategoryName().uppercase()
            holder.tvTitle.text = report.resolvedTitle()
            holder.tvLocation.text = report.resolvedLocationName()
            holder.tvDesc.text = report.resolvedDescription()
            holder.tvReporter.text = "Pelapor: ${report.resolvedReporterName()}"
            holder.tvTime.text = report.resolvedCreatedAt()

            when (report.resolvedStatusKey()) {
                "SELESAI" -> {
                    holder.tvStatusBadge.text = "Sudah Diperbaiki"
                    holder.tvStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_green_light)
                    holder.tvStatusBadge.setTextColor(Color.parseColor("#00A389"))
                }
                "DIPROSES" -> {
                    holder.tvStatusBadge.text = "Sedang Diproses"
                    holder.tvStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_purple_light)
                    holder.tvStatusBadge.setTextColor(Color.parseColor("#613EEA"))
                }
                else -> {
                    holder.tvStatusBadge.text = "Terverifikasi"
                    holder.tvStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_blue_light)
                    holder.tvStatusBadge.setTextColor(Color.parseColor("#0B3C8C"))
                }
            }

            val photoUrl = report.resolvedPhotoUrl()
            if (!photoUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .load(photoUrl)
                    .placeholder(R.drawable.sampul)
                    .error(R.drawable.sampul)
                    .into(holder.ivThumb)
            } else {
                holder.ivThumb.setImageResource(R.drawable.sampul)
            }

            holder.ivThumb.setOnClickListener { onPhotoClick(report) }
            holder.btnRoute.setOnClickListener { onRouteClick(report) }
            holder.btnDetail.setOnClickListener { onDetailClick(report) }
        }

        override fun getItemCount(): Int = items.size

        fun updateList(newItems: List<ReportDto>) {
            items = newItems.toList()
            notifyDataSetChanged()
        }
    }

    companion object {
        const val EXTRA_OPEN_ROUTE_REPORT_ID = "OPEN_ROUTE_REPORT_ID"
    }
}
