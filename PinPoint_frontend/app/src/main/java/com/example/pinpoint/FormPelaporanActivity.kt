package com.example.pinpoint

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.CreateLaporanRequest
import com.example.pinpoint.network.LaporanResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class FormPelaporanActivity : AppCompatActivity() {

    private var selectedCategory = "Jalan Rusak"
    private var selectedUrgency = "Kritis / Darurat"

    private lateinit var ivSelectedPhotoPreview: ImageView
    private lateinit var tvCharCounter: TextView
    private lateinit var etDescription: EditText

    // Data lokasi laporan
    private var reportLatitude = -5.147665
    private var reportLongitude = 119.432732
    private var reportAddress = "Makassar, Sulawesi Selatan"

    private lateinit var tvCoordinates: TextView
    private lateinit var tvPinAddress: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Google Maps
    private lateinit var googleFormMapView: MapView
    private var googleMap: GoogleMap? = null

    private var isAutoTrackingUser = true
    private var locationCallback: LocationCallback? = null

    private var lastGeocodeLat = 0.0
    private var lastGeocodeLng = 0.0

    // Pilih foto dari galeri
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                ivSelectedPhotoPreview.visibility = View.VISIBLE
                ivSelectedPhotoPreview.setImageURI(uri)

                Toast.makeText(
                    this,
                    "Foto berhasil dipilih dari galeri",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // Ambil foto dari kamera
    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                ivSelectedPhotoPreview.visibility = View.VISIBLE
                ivSelectedPhotoPreview.setImageBitmap(bitmap)

                Toast.makeText(
                    this,
                    "Foto berhasil diambil dari kamera",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // Hasil pemilihan lokasi dari PilihLokasiActivity
    private val pickLocationLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val data = result.data!!

                reportLatitude = data.getDoubleExtra(
                    PilihLokasiActivity.EXTRA_LATITUDE,
                    reportLatitude
                )

                reportLongitude = data.getDoubleExtra(
                    PilihLokasiActivity.EXTRA_LONGITUDE,
                    reportLongitude
                )

                val newAddress = data.getStringExtra(
                    PilihLokasiActivity.EXTRA_ADDRESS
                )

                if (!newAddress.isNullOrBlank()) {
                    reportAddress = newAddress
                }

                // Hentikan tracking otomatis karena pengguna memilih titik manual
                isAutoTrackingUser = false

                updateLocationUI()

                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(reportLatitude, reportLongitude),
                        17.5f
                    )
                )

                Toast.makeText(
                    this,
                    "Titik lokasi laporan berhasil disesuaikan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // Permintaan izin lokasi
    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            val coarseGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineGranted || coarseGranted) {
                enableMyLocationOnMap()
                startRealtimeLocationUpdates()
                detectCurrentGpsLocation(showToast = false)
            } else {
                Toast.makeText(
                    this,
                    "Izin lokasi tidak diberikan. Menampilkan titik default.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_form_pelaporan)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.mainForm)
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

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        // Tombol kembali
        findViewById<ImageView>(R.id.btnBackForm).setOnClickListener {
            finish()
        }

        // Inisialisasi foto
        ivSelectedPhotoPreview =
            findViewById(R.id.ivSelectedPhotoPreview)

        findViewById<View>(R.id.btnTakePhoto).setOnClickListener {
            takePhotoLauncher.launch(null)
        }

        findViewById<View>(R.id.btnPickGallery).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Inisialisasi informasi lokasi
        tvCoordinates = findViewById(R.id.tvCoordinates)
        tvPinAddress = findViewById(R.id.tvPinAddress)

        // Inisialisasi Google Maps
        setupGoogleFormMap(savedInstanceState)

        // Tombol kembali ke lokasi GPS
        findViewById<View>(R.id.btnLocateMe).setOnClickListener {

            isAutoTrackingUser = true

            if (hasLocationPermission()) {
                detectCurrentGpsLocation(showToast = true)
            } else {
                requestLocationPermission()
            }
        }

        // Membuka halaman pemilihan lokasi
        val openMapPicker = {

            val intent = Intent(
                this,
                PilihLokasiActivity::class.java
            ).apply {
                putExtra(
                    PilihLokasiActivity.EXTRA_LATITUDE,
                    reportLatitude
                )

                putExtra(
                    PilihLokasiActivity.EXTRA_LONGITUDE,
                    reportLongitude
                )

                putExtra(
                    PilihLokasiActivity.EXTRA_ADDRESS,
                    reportAddress
                )
            }

            pickLocationLauncher.launch(intent)
        }

        findViewById<Button>(R.id.btnAdjustPinManual).setOnClickListener {
            openMapPicker()
        }

        findViewById<View>(R.id.cardMapPreview)?.setOnClickListener {
            openMapPicker()
        }

        // Pilihan kategori
        setupCategorySelection()

        // Pilihan urgensi
        setupUrgencySelection()

        // Counter karakter deskripsi
        tvCharCounter = findViewById(R.id.tvCharCounter)
        etDescription = findViewById(R.id.etDescription)

        etDescription.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                val length = s?.length ?: 0
                tvCharCounter.text = "$length/300 Karakter"
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        // Tombol kirim laporan
        setupSubmitButton()

        // Izin lokasi
        if (!hasLocationPermission()) {
            requestLocationPermission()
        }
    }

    // =========================================================
    // GOOGLE MAPS
    // =========================================================

    private fun setupGoogleFormMap(savedInstanceState: Bundle?) {

        googleFormMapView = findViewById(R.id.googleFormMapView)

        googleFormMapView.onCreate(savedInstanceState)

        googleFormMapView.getMapAsync { map ->

            googleMap = map

            map.uiSettings.apply {
                isZoomControlsEnabled = false
                isCompassEnabled = true
                isMapToolbarEnabled = false
                isScrollGesturesEnabled = true
                isZoomGesturesEnabled = true
                isRotateGesturesEnabled = true
                isTiltGesturesEnabled = false
            }

            map.mapType = GoogleMap.MAP_TYPE_NORMAL

            // Posisi awal peta
            val initialPosition = LatLng(
                reportLatitude,
                reportLongitude
            )

            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    initialPosition,
                    17.5f
                )
            )

            // Jika pengguna menggeser peta secara manual,
            // hentikan tracking GPS otomatis.
            map.setOnCameraMoveStartedListener { reason ->

                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    isAutoTrackingUser = false
                }
            }

            // Perbarui koordinat berdasarkan titik tengah peta
            map.setOnCameraIdleListener {
                onGoogleMapMove()
            }

            enableMyLocationOnMap()

            updateLocationUI()

            // Ambil posisi GPS setelah peta selesai dimuat
            if (hasLocationPermission() && isAutoTrackingUser) {
                detectCurrentGpsLocation(showToast = false)
            }
        }
    }

    private fun onGoogleMapMove() {

        val center = googleMap?.cameraPosition?.target ?: return

        reportLatitude = center.latitude
        reportLongitude = center.longitude

        updateLocationUI()

        val latDiff = kotlin.math.abs(
            reportLatitude - lastGeocodeLat
        )

        val lngDiff = kotlin.math.abs(
            reportLongitude - lastGeocodeLng
        )

        if (latDiff > 0.0003 || lngDiff > 0.0003) {

            lastGeocodeLat = reportLatitude
            lastGeocodeLng = reportLongitude

            reverseGeocodeInBackground(
                reportLatitude,
                reportLongitude
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationOnMap() {

        if (!hasLocationPermission()) return

        try {
            googleMap?.isMyLocationEnabled = true
        } catch (_: SecurityException) {
            // Abaikan jika izin lokasi berubah
        }
    }

    // =========================================================
    // GPS DAN IZIN LOKASI
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

    private fun requestLocationPermission() {

        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    @SuppressLint("MissingPermission")
    private fun startRealtimeLocationUpdates() {

        if (!hasLocationPermission()) return

        // Hapus callback sebelumnya agar tidak terjadi duplikasi
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            4000
        )
            .setMinUpdateIntervalMillis(2000)
            .build()

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(result: LocationResult) {

                val location = result.lastLocation ?: return

                if (isAutoTrackingUser) {

                    reportLatitude = location.latitude
                    reportLongitude = location.longitude

                    val currentPosition = LatLng(
                        location.latitude,
                        location.longitude
                    )

                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            currentPosition,
                            17.5f
                        )
                    )

                    updateLocationUI()

                    reverseGeocodeInBackground(
                        location.latitude,
                        location.longitude
                    )
                }
            }
        }

        locationCallback?.let { callback ->

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun detectCurrentGpsLocation(showToast: Boolean) {

        if (!hasLocationPermission()) return

        if (showToast) {
            Toast.makeText(
                this,
                "Mendeteksi koordinat GPS...",
                Toast.LENGTH_SHORT
            ).show()
        }

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location: Location? ->

            if (location != null) {

                reportLatitude = location.latitude
                reportLongitude = location.longitude

                val currentPosition = LatLng(
                    location.latitude,
                    location.longitude
                )

                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        currentPosition,
                        17.5f
                    )
                )

                updateLocationUI()

                reverseGeocodeInBackground(
                    location.latitude,
                    location.longitude
                )

                if (showToast) {
                    Toast.makeText(
                        this,
                        "Posisi berhasil disesuaikan dengan GPS terkini",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } else if (showToast) {

                Toast.makeText(
                    this,
                    "Lokasi GPS belum tersedia. Coba beberapa saat lagi.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =========================================================
    // REVERSE GEOCODING / ALAMAT
    // =========================================================

    private fun reverseGeocodeInBackground(
        lat: Double,
        lng: Double
    ) {

        Thread {

            try {

                val geocoder = Geocoder(
                    this@FormPelaporanActivity,
                    Locale("id", "ID")
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                    geocoder.getFromLocation(lat, lng, 1) { addresses ->

                        val formatted = formatAddress(
                            addresses.firstOrNull(),
                            lat,
                            lng
                        )

                        runOnUiThread {

                            reportAddress = formatted
                            updateLocationUI()
                        }
                    }

                } else {

                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(
                        lat,
                        lng,
                        1
                    )

                    val formatted = formatAddress(
                        addresses?.firstOrNull(),
                        lat,
                        lng
                    )

                    runOnUiThread {

                        reportAddress = formatted
                        updateLocationUI()
                    }
                }

            } catch (_: Exception) {
                // Koordinat tetap digunakan jika alamat gagal ditemukan
            }

        }.start()
    }

    private fun updateLocationUI() {

        tvCoordinates.text = String.format(
            Locale.US,
            "Lat %.4f • Long %.4f",
            reportLatitude,
            reportLongitude
        )

        tvPinAddress.text = reportAddress
    }

    private fun formatAddress(
        address: Address?,
        lat: Double,
        lng: Double
    ): String {

        if (address == null) {
            return String.format(
                Locale.US,
                "Koordinat: %.5f, %.5f",
                lat,
                lng
            )
        }

        val parts = mutableListOf<String>()

        val thoroughfare = address.thoroughfare
        val subThoroughfare = address.subThoroughfare
        val subLocality = address.subLocality
        val locality = address.locality
        val subAdminArea = address.subAdminArea

        if (!thoroughfare.isNullOrBlank()) {

            val street = if (!subThoroughfare.isNullOrBlank()) {
                "$thoroughfare No. $subThoroughfare"
            } else {
                thoroughfare
            }

            parts.add(street)
        }

        if (!subLocality.isNullOrBlank() &&
            !parts.contains(subLocality)
        ) {
            parts.add(subLocality)
        }

        if (!locality.isNullOrBlank() &&
            !parts.contains(locality)
        ) {
            parts.add(locality)
        }

        if (!subAdminArea.isNullOrBlank() &&
            !parts.contains(subAdminArea)
        ) {
            parts.add(subAdminArea)
        }

        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            address.getAddressLine(0)
                ?: "Makassar, Sulawesi Selatan"
        }
    }

    // =========================================================
    // KIRIM LAPORAN
    // =========================================================

    private fun setupSubmitButton() {

        val btnSubmit = findViewById<Button>(
            R.id.btnSubmitReport
        )

        btnSubmit.setOnClickListener {

            val description = etDescription.text
                .toString()
                .trim()

            if (description.isEmpty()) {

                Toast.makeText(
                    this,
                    "Harap isi uraian rinci kejadian terlebih dahulu!",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val sharedPreferences = getSharedPreferences(
                "UserSession",
                MODE_PRIVATE
            )

            val token = sharedPreferences.getString(
                "KEY_TOKEN",
                null
            )

            val categoryId = when (selectedCategory) {
                "Jalan Rusak" -> 1L
                "Drainase & Banjir" -> 2L
                "Lampu Jalan" -> 3L
                "Taman & Fasum" -> 3L
                "Jembatan Penyebrangan" -> 1L
                else -> 3L
            }

            if (token.isNullOrEmpty()) {

                Toast.makeText(
                    this,
                    "Sesi login tidak valid. Silakan login kembali.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            btnSubmit.isEnabled = false
            btnSubmit.text = "Mengirim..."

            val request = CreateLaporanRequest(
                idKategori = categoryId,
                judul = "Laporan $selectedCategory",
                deskripsi = description,
                alamat = reportAddress,
                latitude = reportLatitude,
                longitude = reportLongitude,
                tingkatKeparahan = selectedUrgency
            )

            ApiClient.instance.createReport(
                "Bearer $token",
                request
            ).enqueue(object : Callback<LaporanResponse> {

                override fun onResponse(
                    call: Call<LaporanResponse>,
                    response: Response<LaporanResponse>
                ) {

                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Kirim Laporan Pengaduan"

                    if (response.isSuccessful) {

                        val result = response.body()

                        Toast.makeText(
                            this@FormPelaporanActivity,
                            "Laporan #${result?.id ?: ""} berhasil dikirim ke database!",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()

                    } else {

                        val errorBody = response.errorBody()?.string()
                            ?: "Terjadi kesalahan saat menyimpan laporan"

                        Toast.makeText(
                            this@FormPelaporanActivity,
                            "Gagal mengirim: $errorBody",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<LaporanResponse>,
                    t: Throwable
                ) {

                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Kirim Laporan Pengaduan"

                    Toast.makeText(
                        this@FormPelaporanActivity,
                        "Gagal terhubung ke server: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }
    }

    // =========================================================
    // LIFECYCLE GOOGLE MAPS DAN GPS
    // =========================================================

    override fun onResume() {
        super.onResume()

        googleFormMapView.onResume()

        if (hasLocationPermission()) {
            enableMyLocationOnMap()
            startRealtimeLocationUpdates()
        }
    }

    override fun onPause() {

        googleFormMapView.onPause()

        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }

        super.onPause()
    }

    override fun onDestroy() {

        googleFormMapView.onDestroy()

        super.onDestroy()
    }

    override fun onLowMemory() {

        super.onLowMemory()

        googleFormMapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {

        super.onSaveInstanceState(outState)

        googleFormMapView.onSaveInstanceState(outState)
    }

    // =========================================================
    // PILIHAN KATEGORI
    // =========================================================

    private fun setupCategorySelection() {

        val catJalan = findViewById<RelativeLayout>(
            R.id.catJalanRusak
        )

        val catLampu = findViewById<RelativeLayout>(
            R.id.catLampuJalan
        )

        val catDrainase = findViewById<RelativeLayout>(
            R.id.catDrainase
        )

        val catTaman = findViewById<RelativeLayout>(
            R.id.catTaman
        )

        val catJembatan = findViewById<RelativeLayout>(
            R.id.catJembatan
        )

        val catLainnya = findViewById<RelativeLayout>(
            R.id.catLainnya
        )

        val checkCat1 = findViewById<ImageView>(
            R.id.checkCat1
        )

        val tvCatSub1 = findViewById<TextView>(
            R.id.tvCatSub1
        )

        val tvCatTitle1 = findViewById<TextView>(
            R.id.tvCatTitle1
        )

        val tvCatTitle2 = findViewById<TextView>(
            R.id.tvCatTitle2
        )

        val tvCatTitle3 = findViewById<TextView>(
            R.id.tvCatTitle3
        )

        val tvCatTitle4 = findViewById<TextView>(
            R.id.tvCatTitle4
        )

        val tvCatTitle5 = findViewById<TextView>(
            R.id.tvCatTitle5
        )

        val tvCatTitle6 = findViewById<TextView>(
            R.id.tvCatTitle6
        )

        val categories = listOf(
            Triple(catJalan, "Jalan Rusak", tvCatTitle1),
            Triple(catLampu, "Lampu Jalan", tvCatTitle2),
            Triple(catDrainase, "Drainase & Banjir", tvCatTitle3),
            Triple(catTaman, "Taman & Fasum", tvCatTitle4),
            Triple(catJembatan, "Jembatan Penyebrangan", tvCatTitle5),
            Triple(catLainnya, "Lainnya", tvCatTitle6)
        )

        fun resetAll() {

            for ((view, _, titleView) in categories) {

                view.setBackgroundResource(
                    R.drawable.bg_category_item_unselected
                )

                titleView.setTextColor(
                    Color.parseColor("#0B1C30")
                )
            }

            checkCat1.visibility = View.GONE

            tvCatSub1.setTextColor(
                Color.parseColor("#64748B")
            )
        }

        for ((view, name, titleView) in categories) {

            view.setOnClickListener {

                resetAll()

                selectedCategory = name

                view.setBackgroundResource(
                    R.drawable.bg_category_item_selected
                )

                titleView.setTextColor(Color.WHITE)

                if (name == "Jalan Rusak") {

                    checkCat1.visibility = View.VISIBLE

                    tvCatSub1.setTextColor(
                        Color.parseColor("#BFDBFE")
                    )
                }

                Toast.makeText(
                    this,
                    "Kategori: $name",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun setupUrgencySelection() {

        val btnLow = findViewById<TextView>(
            R.id.btnUrgencyLow
        )

        val btnMedium = findViewById<TextView>(
            R.id.btnUrgencyMedium
        )

        val btnCritical = findViewById<LinearLayout>(
            R.id.btnUrgencyCritical
        )

        val tvCriticalText = findViewById<TextView>(
            R.id.tvUrgencyCriticalText
        )

        btnLow.setOnClickListener {

            selectedUrgency = "Rendah"

            btnLow.setBackgroundResource(
                R.drawable.bg_btn_rounded_blue
            )

            btnLow.setTextColor(Color.WHITE)

            btnMedium.setBackgroundResource(0)
            btnMedium.setTextColor(
                Color.parseColor("#334155")
            )

            btnCritical.setBackgroundResource(0)
            tvCriticalText.setTextColor(
                Color.parseColor("#334155")
            )
        }

        btnMedium.setOnClickListener {

            selectedUrgency = "Sedang"

            btnMedium.setBackgroundResource(
                R.drawable.bg_btn_rounded_blue
            )

            btnMedium.setTextColor(Color.WHITE)

            btnLow.setBackgroundResource(0)
            btnLow.setTextColor(
                Color.parseColor("#334155")
            )

            btnCritical.setBackgroundResource(0)
            tvCriticalText.setTextColor(
                Color.parseColor("#334155")
            )
        }

        btnCritical.setOnClickListener {

            selectedUrgency = "Kritis / Darurat"

            btnCritical.setBackgroundResource(
                R.drawable.bg_urgency_critical
            )

            tvCriticalText.setTextColor(Color.WHITE)

            btnLow.setBackgroundResource(0)
            btnLow.setTextColor(
                Color.parseColor("#334155")
            )

            btnMedium.setBackgroundResource(0)
            btnMedium.setTextColor(
                Color.parseColor("#334155")
            )
        }
    }
}