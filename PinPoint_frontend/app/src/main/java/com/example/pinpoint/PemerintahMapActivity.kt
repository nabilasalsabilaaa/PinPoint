package com.example.pinpoint

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
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.LatLngBounds
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
    // ID laporan yang dikirim dari halaman Detail Laporan untuk langsung dibukakan rutenya.
    private var pendingRouteReportId: String? = null
    private var routePolyline: Polyline? = null
    private val routeExecutor = Executors.newSingleThreadExecutor()

    private lateinit var etSearchMap: EditText
    private lateinit var llMapCategoryChips: LinearLayout

    private lateinit var tvMapReportCounter: TextView
    private lateinit var btnPrevMapReport: TextView
    private lateinit var btnNextMapReport: TextView
    private lateinit var tvMapReportId: TextView
    private lateinit var tvMapReportPriority: TextView
    private lateinit var tvMapReportStatusBadge: TextView
    private lateinit var tvMapReportTitle: TextView
    private lateinit var ivMapReportThumb: ImageView
    private lateinit var tvMapReportLocation: TextView
    private lateinit var tvMapReportDesc: TextView
    private lateinit var tvMapReportDistance: TextView
    private lateinit var tvMapReportTime: TextView

    private val incomingReports = mutableListOf<ReportDto>()
    private val filteredReports = mutableListOf<ReportDto>()
    private val mapMarkers = mutableListOf<Marker>()

    private var selectedReportIndex = 0
    private var selectedCategoryId: Long? = null
    private var selectedCategoryName: String? = null
    private var categoriesList = listOf<CategoryDto>()

    private var isSatelliteMode = false
    private var hasCenteredToUserLocation = false

    // Auto-refresh laporan secara berkala agar update real-time
    private val autoRefreshHandler = Handler(Looper.getMainLooper())
    private val autoRefreshRunnable = object : Runnable {
        override fun run() {
            loadCategoriesAndReports()
            autoRefreshHandler.postDelayed(this, 15000) // Polling update tiap 15 detik
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
                val targetReport = filteredReports.firstOrNull {
                    it.resolvedId().toString() == requestedId
                }
                if (targetReport != null) {
                    pendingRouteReportId = null
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

        tvMapReportCounter = findViewById(R.id.tvMapReportCounter)
        btnPrevMapReport = findViewById(R.id.btnPrevMapReport)
        btnNextMapReport = findViewById(R.id.btnNextMapReport)
        tvMapReportId = findViewById(R.id.tvMapReportId)
        tvMapReportPriority = findViewById(R.id.tvMapReportPriority)
        tvMapReportStatusBadge = findViewById(R.id.tvMapReportStatusBadge)
        tvMapReportTitle = findViewById(R.id.tvMapReportTitle)
        ivMapReportThumb = findViewById(R.id.ivMapReportThumb)
        tvMapReportLocation = findViewById(R.id.tvMapReportLocation)
        tvMapReportDesc = findViewById(R.id.tvMapReportDesc)
        tvMapReportDistance = findViewById(R.id.tvMapReportDistance)
        tvMapReportTime = findViewById(R.id.tvMapReportTime)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false // Menggunakan btnGps buatan sendiri

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

        // Listener klik marker laporan
        map.setOnMarkerClickListener { marker ->
            val index = marker.tag as? Int
            if (index != null && index in filteredReports.indices) {
                if (selectedReportIndex != index) clearActiveRoute()
                selectedReportIndex = index
                bindSelectedReportToCard()
                map.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
                marker.showInfoWindow()
            }
            true
        }

        syncMarkersOnMap()
    }

    private fun setupSearchAndActions() {
        val btnGps = findViewById<View>(R.id.btnGps)
        val btnLayer = findViewById<View>(R.id.btnLayer)
        val btnRouteGuidance = findViewById<View>(R.id.btnRouteGuidance)
        val btnReportDetail = findViewById<View>(R.id.btnReportDetail)

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

        btnPrevMapReport.setOnClickListener {
            if (filteredReports.isNotEmpty() && selectedReportIndex > 0) {
                clearActiveRoute()
                selectedReportIndex--
                bindSelectedReportToCard()
                focusSelectedReportOnMap()
            }
        }

        btnNextMapReport.setOnClickListener {
            if (filteredReports.isNotEmpty() && selectedReportIndex < filteredReports.size - 1) {
                clearActiveRoute()
                selectedReportIndex++
                bindSelectedReportToCard()
                focusSelectedReportOnMap()
            }
        }

        ivMapReportThumb.setOnClickListener {
            val current = filteredReports.getOrNull(selectedReportIndex) ?: return@setOnClickListener
            val photos = current.resolvedPhotoUrls()
            if (photos.isNotEmpty()) {
                PhotoSliderDialog.show(this, "Foto Laporan (${photos.size} Foto)", photos, 0)
            }
        }

        btnRouteGuidance.setOnClickListener {
            val current = filteredReports.getOrNull(selectedReportIndex)
            if (current == null) {
                Toast.makeText(this, "Pilih laporan terlebih dahulu", Toast.LENGTH_SHORT).show()
            } else {
                startInAppRoute(current)
            }
        }

        btnReportDetail.setOnClickListener {
            val current = filteredReports.getOrNull(selectedReportIndex)
            if (current != null) {
                val intent = Intent(this, PemerintahDetailTaskActivity::class.java)
                intent.putExtra("REPORT_ID", current.resolvedId().toString())
                startActivity(intent)
            } else {
                Toast.makeText(this, "Tidak ada laporan yang dipilih", Toast.LENGTH_SHORT).show()
            }
        }
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
            override fun onLocationResult(result: LocationResult) {
                // Google Maps menampilkan lokasi terbaru petugas melalui layer My Location.
            }
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

    private fun focusSelectedReportOnMap() {
        val report = filteredReports.getOrNull(selectedReportIndex) ?: return
        val lat = report.latitude ?: return
        val lng = report.longitude ?: return
        val pos = LatLng(lat, lng)
        googleMap?.animateCamera(CameraUpdateFactory.newLatLng(pos))

        val marker = mapMarkers.getOrNull(selectedReportIndex)
        marker?.showInfoWindow()
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
                    // Sinkronkan laporan untuk pemerintah (TERVERIFIKASI & DIPROSES)
                    incomingReports.addAll(
                        all.filter {
                            val st = it.resolvedStatusKey()
                            st == "TERVERIFIKASI" || st == "DIPROSES"
                        }
                    )
                    renderCategoryChips()
                    applyMapFilters()
                    syncMarkersOnMap()
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

        // Jika halaman dibuka dari Detail Laporan, pilih laporan tersebut dan mulai panduan rute.
        pendingRouteReportId?.let { requestedId ->
            val requestedIndex = filteredReports.indexOfFirst {
                it.resolvedId().toString() == requestedId
            }
            if (requestedIndex >= 0) {
                selectedReportIndex = requestedIndex
                val requestedReport = filteredReports[requestedIndex]
                pendingRouteReportId = null
                bindSelectedReportToCard()
                syncMarkersOnMap()
                focusSelectedReportOnMap()
                startInAppRoute(requestedReport)
                return
            }
        }

        if (activeRouteReportId != null &&
            filteredReports.getOrNull(selectedReportIndex)?.id != activeRouteReportId
        ) {
            clearActiveRoute()
        }
        bindSelectedReportToCard()
        syncMarkersOnMap()
    }

    private fun syncMarkersOnMap() {
        val map = googleMap ?: return
        map.clear()
        mapMarkers.clear()

        filteredReports.forEachIndexed { index, report ->
            val lat = report.latitude ?: return@forEachIndexed
            val lng = report.longitude ?: return@forEachIndexed
            val pos = LatLng(lat, lng)

            val markerColor = when (report.resolvedStatusKey()) {
                "DIPROSES" -> BitmapDescriptorFactory.HUE_VIOLET
                else -> BitmapDescriptorFactory.HUE_AZURE
            }

            val markerOptions = MarkerOptions()
                .position(pos)
                .title("${report.resolvedCode()}: ${report.resolvedTitle()}")
                .snippet(report.resolvedLocationName())
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor))

            val marker = map.addMarker(markerOptions)
            if (marker != null) {
                marker.tag = index
                mapMarkers.add(marker)
            }
        }

        if (mapMarkers.isNotEmpty() && selectedReportIndex in mapMarkers.indices) {
            mapMarkers[selectedReportIndex].showInfoWindow()
        }

        // map.clear() juga menghapus polyline, jadi gambar ulang rute aktif jika ada.
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
        fun requestLocation() {
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
                        }.addOnFailureListener {
                            Toast.makeText(this, "Gagal mendapatkan lokasi GPS", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal mendapatkan lokasi GPS", Toast.LENGTH_LONG).show()
                }
        }
        requestLocation()
    }

    private fun requestOsrmRoute(origin: LatLng, destination: LatLng, reportId: String?) {
        Toast.makeText(this, "Sedang mencari rute...", Toast.LENGTH_SHORT).show()

        routeExecutor.execute {
            var connection: HttpURLConnection? = null
            try {
                // OSRM menggunakan urutan koordinat longitude,latitude. Server FOSSGIS routed-car.
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
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    val detail = errorBody?.take(180)?.takeIf { it.isNotBlank() }
                    throw IllegalStateException(
                        if (detail != null) "Layanan rute menolak permintaan ($responseCode): $detail"
                        else "Layanan rute menolak permintaan ($responseCode)"
                    )
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

    private fun bindSelectedReportToCard() {
        if (filteredReports.isEmpty()) {
            tvMapReportCounter.text = "0 / 0"
            btnPrevMapReport.alpha = 0.4f
            btnNextMapReport.alpha = 0.4f
            tvMapReportId.text = "-"
            tvMapReportPriority.text = "TIDAK ADA DATA"
            tvMapReportStatusBadge.text = "-"
            tvMapReportTitle.text = "Tidak ada laporan yang cocok"
            tvMapReportLocation.text = "Coba kata kunci atau kategori lain"
            tvMapReportDesc.text = "Belum ada laporan masuk yang sesuai dengan pencarian Anda."
            tvMapReportDistance.text = "-"
            tvMapReportTime.text = ""
            ivMapReportThumb.setImageResource(R.drawable.sampul)
            return
        }

        val safeIndex = selectedReportIndex.coerceIn(0, filteredReports.size - 1)
        selectedReportIndex = safeIndex
        val report = filteredReports[safeIndex]

        tvMapReportCounter.text = "${safeIndex + 1} / ${filteredReports.size}"
        btnPrevMapReport.alpha = if (safeIndex > 0) 1.0f else 0.4f
        btnNextMapReport.alpha = if (safeIndex < filteredReports.size - 1) 1.0f else 0.4f

        tvMapReportId.text = report.resolvedCode()
        tvMapReportPriority.text = report.resolvedCategoryName().uppercase()
        tvMapReportTitle.text = report.resolvedTitle()
        tvMapReportLocation.text = report.resolvedLocationName()
        tvMapReportDesc.text = report.resolvedDescription()
        tvMapReportDistance.text = "Pelapor: ${report.resolvedReporterName()}"
        tvMapReportTime.text = report.resolvedCreatedAt()

        when (report.resolvedStatusKey()) {
            "DIPROSES" -> {
                tvMapReportStatusBadge.text = "Sedang Diproses"
                tvMapReportStatusBadge.background = ContextCompat.getDrawable(this, R.drawable.bg_badge_purple_light)
                tvMapReportStatusBadge.setTextColor(Color.parseColor("#613EEA"))
            }
            else -> {
                tvMapReportStatusBadge.text = "Terverifikasi"
                tvMapReportStatusBadge.background = ContextCompat.getDrawable(this, R.drawable.bg_badge_blue_light)
                tvMapReportStatusBadge.setTextColor(Color.parseColor("#0B3C8C"))
            }
        }

        val photoUrl = report.resolvedPhotoUrl()
        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.sampul)
                .error(R.drawable.sampul)
                .into(ivMapReportThumb)
        } else {
            ivMapReportThumb.setImageResource(R.drawable.sampul)
        }
    }

    companion object {
        const val EXTRA_OPEN_ROUTE_REPORT_ID = "OPEN_ROUTE_REPORT_ID"
    }

}