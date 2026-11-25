package com.example.pinpoint.user

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import android.util.Base64
import android.view.Gravity
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
import com.bumptech.glide.Glide
import com.example.pinpoint.PhotoSliderDialog
import com.example.pinpoint.R
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.CreateLaporanRequest
import com.example.pinpoint.network.KategoriResponse
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class FormPelaporanActivity : AppCompatActivity() {

    private var selectedCategoryId: Long = 1L
    private var selectedCategoryName: String = "Jalan Rusak"
    private var selectedUrgency: String = "Kritis / Darurat"

    private lateinit var layoutSelectedPhotosContainer: RelativeLayout
    private lateinit var ivSelectedPhotoPreview: ImageView
    private lateinit var tvSelectedPhotoCount: TextView
    private lateinit var btnClearSelectedPhotos: TextView
    private lateinit var llDynamicCategoryGrid: LinearLayout
    private lateinit var etReportTitle: EditText
    private lateinit var tvCharCounter: TextView
    private lateinit var etDescription: EditText

    private val selectedPhotoPreviewUris = mutableListOf<String>()
    private val selectedPhotoBase64List = mutableListOf<String>()

    private var reportLatitude = -5.147665
    private var reportLongitude = 119.432732
    private var reportAddress = "Makassar, Sulawesi Selatan"

    private lateinit var tvCoordinates: TextView
    private lateinit var tvPinAddress: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var googleFormMapView: MapView
    private var googleMap: GoogleMap? = null

    private var isAutoTrackingUser = true
    private var locationCallback: LocationCallback? = null

    private var lastGeocodeLat = 0.0
    private var lastGeocodeLng = 0.0

    // Pilih lebih dari 1 foto dari galeri (Otomatis koreksi rotasi EXIF agar foto tidak miring)
    private val pickMultipleImagesLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                var added = 0
                for (uri in uris) {
                    val uprightBitmap = decodeUprightBitmapFromUri(uri)
                    if (uprightBitmap != null) {
                        val savedUri = saveTempCameraBitmap(uprightBitmap)
                        val b64 = encodeBitmapToBase64(uprightBitmap)
                        if (savedUri != null && b64 != null) {
                            selectedPhotoPreviewUris.add(savedUri.toString())
                            selectedPhotoBase64List.add(b64)
                            added++
                        }
                    }
                }
                updateSelectedPhotosUI()
                if (added > 0) {
                    Toast.makeText(this, "$added foto berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                }
            }
        }

    // Ambil foto dari kamera (bisa berkali-kali untuk menambah foto)
    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                val savedUri = saveTempCameraBitmap(bitmap)
                val b64 = encodeBitmapToBase64(bitmap)
                if (savedUri != null && b64 != null) {
                    selectedPhotoPreviewUris.add(savedUri.toString())
                    selectedPhotoBase64List.add(b64)
                    updateSelectedPhotosUI()
                    Toast.makeText(
                        this,
                        "Foto kamera ditambahkan (${selectedPhotoPreviewUris.size} foto)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    // Permintaan izin kamera runtime (agar tombol Tambah Kamera berfungsi di Android 6+)
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCameraSafely()
            } else {
                Toast.makeText(
                    this,
                    "Izin kamera diperlukan untuk mengambil foto bukti laporan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private fun launchCameraSafely() {
        try {
            takePhotoLauncher.launch(null)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka kamera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickLocationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val data = result.data!!
                reportLatitude = data.getDoubleExtra(PilihLokasiActivity.EXTRA_LATITUDE, reportLatitude)
                reportLongitude = data.getDoubleExtra(PilihLokasiActivity.EXTRA_LONGITUDE, reportLongitude)
                val newAddress = data.getStringExtra(PilihLokasiActivity.EXTRA_ADDRESS)
                if (!newAddress.isNullOrBlank()) {
                    reportAddress = newAddress
                }
                isAutoTrackingUser = false
                updateLocationUI()
                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(reportLatitude, reportLongitude), 17.5f)
                )
                Toast.makeText(this, "Titik lokasi laporan berhasil disesuaikan", Toast.LENGTH_SHORT).show()
            }
        }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
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
        setContentView(R.layout.activity_user_form_pelaporan)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainForm)) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        findViewById<ImageView>(R.id.btnBackForm).setOnClickListener {
            finish()
        }

        layoutSelectedPhotosContainer = findViewById(R.id.layoutSelectedPhotosContainer)
        ivSelectedPhotoPreview = findViewById(R.id.ivSelectedPhotoPreview)
        tvSelectedPhotoCount = findViewById(R.id.tvSelectedPhotoCount)
        btnClearSelectedPhotos = findViewById(R.id.btnClearSelectedPhotos)
        llDynamicCategoryGrid = findViewById(R.id.llDynamicCategoryGrid)
        etReportTitle = findViewById(R.id.etReportTitle)

        findViewById<View>(R.id.btnTakePhoto).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                launchCameraSafely()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        findViewById<View>(R.id.btnPickGallery).setOnClickListener {
            pickMultipleImagesLauncher.launch("image/*")
        }

        // Klik preview foto untuk melihat slide demi slide di popup
        layoutSelectedPhotosContainer.setOnClickListener {
            if (selectedPhotoPreviewUris.isNotEmpty()) {
                PhotoSliderDialog.show(
                    this,
                    "Foto Bukti Kerusakan (${selectedPhotoPreviewUris.size} Foto)",
                    selectedPhotoPreviewUris,
                    0
                )
            }
        }

        btnClearSelectedPhotos.setOnClickListener {
            selectedPhotoPreviewUris.clear()
            selectedPhotoBase64List.clear()
            updateSelectedPhotosUI()
        }

        tvCoordinates = findViewById(R.id.tvCoordinates)
        tvPinAddress = findViewById(R.id.tvPinAddress)

        setupGoogleFormMap(savedInstanceState)

        findViewById<View>(R.id.btnLocateMe).setOnClickListener {
            isAutoTrackingUser = true
            if (hasLocationPermission()) {
                detectCurrentGpsLocation(showToast = true)
            } else {
                requestLocationPermission()
            }
        }

        val openMapPicker = {
            val intent = Intent(this, PilihLokasiActivity::class.java).apply {
                putExtra(PilihLokasiActivity.EXTRA_LATITUDE, reportLatitude)
                putExtra(PilihLokasiActivity.EXTRA_LONGITUDE, reportLongitude)
                putExtra(PilihLokasiActivity.EXTRA_ADDRESS, reportAddress)
            }
            pickLocationLauncher.launch(intent)
        }

        findViewById<Button>(R.id.btnAdjustPinManual).setOnClickListener { openMapPicker() }
        findViewById<View>(R.id.cardMapPreview)?.setOnClickListener { openMapPicker() }

        // Sinkronisasi Kategori dari Database (yang diatur di Admin)
        fetchDatabaseCategories()

        // Pilihan tingkat bahaya / urgensi
        setupUrgencySelection()

        tvCharCounter = findViewById(R.id.tvCharCounter)
        etDescription = findViewById(R.id.etDescription)
        val scrollViewForm = findViewById<android.widget.ScrollView>(R.id.scrollViewForm)

        val scrollToFocusedInput = { target: View ->
            scrollViewForm?.postDelayed({
                scrollViewForm.smoothScrollTo(0, target.bottom + 240)
            }, 220)
        }

        etReportTitle.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) scrollToFocusedInput(v)
        }
        etReportTitle.setOnClickListener { v -> scrollToFocusedInput(v) }

        etDescription.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) scrollToFocusedInput(v)
        }
        etDescription.setOnClickListener { v -> scrollToFocusedInput(v) }

        etDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                tvCharCounter.text = "$length/300 Karakter"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        setupSubmitButton()

        if (!hasLocationPermission()) {
            requestLocationPermission()
        }
    }

    // =========================================================
    // MULTI-PHOTO HELPER & POPUP SLIDER
    // =========================================================

    private fun updateSelectedPhotosUI() {
        if (selectedPhotoPreviewUris.isEmpty()) {
            layoutSelectedPhotosContainer.visibility = View.GONE
            return
        }
        layoutSelectedPhotosContainer.visibility = View.VISIBLE
        tvSelectedPhotoCount.text = "${selectedPhotoPreviewUris.size} Foto Terpilih • Ketuk untuk lihat slide"
        Glide.with(this)
            .load(Uri.parse(selectedPhotoPreviewUris.last()))
            .centerCrop()
            .into(ivSelectedPhotoPreview)
    }

    private fun saveTempCameraBitmap(bitmap: Bitmap): Uri? {
        return try {
            val file = File(cacheDir, "report_cam_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                out.flush()
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeUprightBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

            var inSampleSize = 1
            val maxDim = 1280
            if (options.outHeight > maxDim || options.outWidth > maxDim) {
                val halfH = options.outHeight / 2
                val halfW = options.outWidth / 2
                while ((halfH / inSampleSize) >= maxDim && (halfW / inSampleSize) >= maxDim) {
                    inSampleSize *= 2
                }
            }

            val decodeOpts = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
            val rawBitmap = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null

            applyExifOrientation(uri, rawBitmap)
        } catch (e: Exception) {
            null
        }
    }

    private fun applyExifOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val exif = contentResolver.openInputStream(uri)?.use { input ->
                android.media.ExifInterface(input)
            } ?: return bitmap

            val orientation = exif.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = android.graphics.Matrix()
            when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                android.media.ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.preScale(-1f, 1f)
                }
                android.media.ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.preScale(-1f, 1f)
                }
                else -> return bitmap
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap): String? {
        return try {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, baos)
            val bytes = baos.toByteArray()
            "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    // =========================================================
    // DYNAMIC CATEGORIES SYNCED WITH ADMIN & DATABASE
    // =========================================================

    private fun fetchDatabaseCategories() {
        val defaultCategories = listOf(
            KategoriResponse(1L, "Jalan Rusak", "Lubang / Aspal Amblas", "jalan", true),
            KategoriResponse(2L, "Drainase & Banjir", "Tersumbat / Meluap", "drainage", true),
            KategoriResponse(3L, "Lampu Jalan", "Padam / Tiang Rusak", "lamp", true)
        )
        renderCategoryGrid(defaultCategories)

        ApiClient.fetchWithFallback(
            callFactory = { api -> api.getCategories() },
            onResponse = { _, response ->
                if (response.isSuccessful) {
                    val activeCats = (response.body() ?: emptyList()).filter {
                        it.aktif != false
                    }
                    if (activeCats.isNotEmpty()) {
                        renderCategoryGrid(activeCats)
                    }
                }
            },
            onFailure = { _, _ -> }
        )
    }

    private fun renderCategoryGrid(categories: List<KategoriResponse>) {
        llDynamicCategoryGrid.removeAllViews()
        if (categories.isEmpty()) return

        // Pilih kategori pertama sebagai default jika belum ada yang cocok
        val existingMatch = categories.firstOrNull { it.id == selectedCategoryId } ?: categories.first()
        selectedCategoryId = existingMatch.id ?: 1L
        selectedCategoryName = existingMatch.namaKategori ?: "Fasilitas Umum"

        val density = resources.displayMetrics.density
        val cardViews = mutableListOf<Triple<RelativeLayout, TextView, TextView>>()

        var currentRow: LinearLayout? = null
        categories.forEachIndexed { index, cat ->
            val catName = cat.namaKategori ?: "Fasilitas Umum"
            if (index % 2 == 0) {
                currentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = (10 * density).toInt()
                    }
                }
                llDynamicCategoryGrid.addView(currentRow)
            }

            val card = RelativeLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    if (index % 2 == 0) {
                        marginEnd = (8 * density).toInt()
                    }
                }
                val pad = (12 * density).toInt()
                setPadding(pad, pad, pad, pad)
                isClickable = true
                isFocusable = true
            }

            val ivIcon = ImageView(this).apply {
                id = View.generateViewId()
                val size = (32 * density).toInt()
                layoutParams = RelativeLayout.LayoutParams(size, size)
                setBackgroundResource(R.drawable.bg_chip_blue_light)
                val iconPad = (6 * density).toInt()
                setPadding(iconPad, iconPad, iconPad, iconPad)
                setImageResource(resolveCategoryIconRes(cat.ikon, catName))
                setColorFilter(Color.parseColor("#0B58CA"))
            }

            val tvTitle = TextView(this).apply {
                id = View.generateViewId()
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.BELOW, ivIcon.id)
                    topMargin = (8 * density).toInt()
                }
                text = catName
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val tvSub = TextView(this).apply {
                id = View.generateViewId()
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.BELOW, tvTitle.id)
                }
                text = cat.deskripsi?.takeIf { it.isNotBlank() } ?: "Fasilitas Umum"
                textSize = 10f
                maxLines = 1
            }

            card.addView(ivIcon)
            card.addView(tvTitle)
            card.addView(tvSub)
            currentRow?.addView(card)

            // Jika jumlah ganjil dan item terakhir, tambahkan spacer agar lebarnya 1 kolom rapi
            if (index == categories.lastIndex && index % 2 == 0) {
                val spacer = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                }
                currentRow?.addView(spacer)
            }

            cardViews.add(Triple(card, tvTitle, tvSub))

            card.setOnClickListener {
                selectedCategoryId = cat.id ?: 1L
                selectedCategoryName = catName
                refreshCategorySelectionStyles(categories, cardViews)
            }
        }

        refreshCategorySelectionStyles(categories, cardViews)
    }

    private fun refreshCategorySelectionStyles(
        categories: List<KategoriResponse>,
        cardViews: List<Triple<RelativeLayout, TextView, TextView>>
    ) {
        categories.forEachIndexed { idx, cat ->
            val (card, tvTitle, tvSub) = cardViews[idx]
            val isSelected = (cat.id ?: -1L) == selectedCategoryId || (cat.namaKategori ?: "") == selectedCategoryName
            if (isSelected) {
                card.setBackgroundResource(R.drawable.bg_category_item_selected)
                tvTitle.setTextColor(Color.WHITE)
                tvSub.setTextColor(Color.parseColor("#BFDBFE"))
            } else {
                card.setBackgroundResource(R.drawable.bg_category_item_unselected)
                tvTitle.setTextColor(Color.parseColor("#0B1C30"))
                tvSub.setTextColor(Color.parseColor("#64748B"))
            }
        }
    }

    private fun resolveCategoryIconRes(iconName: String?, categoryName: String): Int {
        val key = (iconName ?: categoryName).lowercase(Locale.getDefault())
        return when {
            key.contains("jalan") -> R.drawable.jalan
            key.contains("lamp") || key.contains("penerangan") -> R.drawable.lamp
            key.contains("drain") || key.contains("banjir") || key.contains("saluran") -> R.drawable.drainage
            key.contains("jembatan") || key.contains("bridge") -> R.drawable.bridge
            key.contains("taman") || key.contains("park") -> R.drawable.park
            key.contains("halte") || key.contains("bus") -> R.drawable.bus_stop
            key.contains("trotoar") || key.contains("walk") -> R.drawable.walk
            else -> R.drawable.category
        }
    }

    // =========================================================
    // GOOGLE MAPS & GPS
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
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(reportLatitude, reportLongitude), 17.5f)
            )
            map.setOnCameraMoveStartedListener { reason ->
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    isAutoTrackingUser = false
                }
            }
            map.setOnCameraIdleListener { onGoogleMapMove() }
            enableMyLocationOnMap()
            updateLocationUI()

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

        val latDiff = kotlin.math.abs(reportLatitude - lastGeocodeLat)
        val lngDiff = kotlin.math.abs(reportLongitude - lastGeocodeLng)
        if (latDiff > 0.0003 || lngDiff > 0.0003) {
            lastGeocodeLat = reportLatitude
            lastGeocodeLng = reportLongitude
            reverseGeocodeInBackground(reportLatitude, reportLongitude)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationOnMap() {
        if (!hasLocationPermission()) return
        try {
            googleMap?.isMyLocationEnabled = true
        } catch (_: SecurityException) {}
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
    private fun startRealtimeLocationUpdates() {
        if (!hasLocationPermission()) return
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                if (isAutoTrackingUser) {
                    reportLatitude = location.latitude
                    reportLongitude = location.longitude
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 17.5f)
                    )
                    updateLocationUI()
                    reverseGeocodeInBackground(location.latitude, location.longitude)
                }
            }
        }

        locationCallback?.let { callback ->
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        }
    }

    @SuppressLint("MissingPermission")
    private fun detectCurrentGpsLocation(showToast: Boolean) {
        if (!hasLocationPermission()) return
        if (showToast) {
            Toast.makeText(this, "Mendeteksi koordinat GPS...", Toast.LENGTH_SHORT).show()
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    reportLatitude = location.latitude
                    reportLongitude = location.longitude
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 17.5f)
                    )
                    updateLocationUI()
                    reverseGeocodeInBackground(location.latitude, location.longitude)
                    if (showToast) {
                        Toast.makeText(this, "Posisi berhasil disesuaikan dengan GPS terkini", Toast.LENGTH_SHORT).show()
                    }
                } else if (showToast) {
                    Toast.makeText(this, "Lokasi GPS belum tersedia. Coba beberapa saat lagi.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun reverseGeocodeInBackground(lat: Double, lng: Double) {
        Thread {
            try {
                val geocoder = Geocoder(this@FormPelaporanActivity, Locale("id", "ID"))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lng, 1) { addresses ->
                        val formatted = formatAddress(addresses.firstOrNull(), lat, lng)
                        runOnUiThread {
                            reportAddress = formatted
                            updateLocationUI()
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    val formatted = formatAddress(addresses?.firstOrNull(), lat, lng)
                    runOnUiThread {
                        reportAddress = formatted
                        updateLocationUI()
                    }
                }
            } catch (_: Exception) {}
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

    private fun formatAddress(address: Address?, lat: Double, lng: Double): String {
        if (address == null) {
            return String.format(Locale.US, "Koordinat: %.5f, %.5f", lat, lng)
        }
        val parts = mutableListOf<String>()
        val thoroughfare = address.thoroughfare
        val subThoroughfare = address.subThoroughfare
        val subLocality = address.subLocality
        val locality = address.locality
        val subAdminArea = address.subAdminArea

        if (!thoroughfare.isNullOrBlank()) {
            val street = if (!subThoroughfare.isNullOrBlank()) "$thoroughfare No. $subThoroughfare" else thoroughfare
            parts.add(street)
        }
        if (!subLocality.isNullOrBlank() && !parts.contains(subLocality)) parts.add(subLocality)
        if (!locality.isNullOrBlank() && !parts.contains(locality)) parts.add(locality)
        if (!subAdminArea.isNullOrBlank() && !parts.contains(subAdminArea)) parts.add(subAdminArea)

        return if (parts.isNotEmpty()) parts.joinToString(", ") else address.getAddressLine(0) ?: "Makassar, Sulawesi Selatan"
    }

    // =========================================================
    // KIRIM LAPORAN KE DATABASE
    // =========================================================

    private fun setupSubmitButton() {
        val btnSubmit = findViewById<Button>(R.id.btnSubmitReport)

        btnSubmit.setOnClickListener {
            val customTitle = etReportTitle.text.toString().trim()
            val description = etDescription.text.toString().trim()

            if (description.isEmpty()) {
                Toast.makeText(this, "Harap isi uraian rinci kejadian terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
            val rawToken = sharedPreferences.getString("KEY_TOKEN", null)

            if (rawToken.isNullOrEmpty()) {
                Toast.makeText(this, "Sesi login tidak valid. Silakan login kembali.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"
            val finalTitle = if (customTitle.isNotEmpty()) customTitle else "Laporan $selectedCategoryName"

            btnSubmit.isEnabled = false
            btnSubmit.text = "Mengirim Laporan..."

            val request = CreateLaporanRequest(
                idKategori = selectedCategoryId,
                judul = finalTitle,
                deskripsi = description,
                alamat = reportAddress,
                latitude = reportLatitude,
                longitude = reportLongitude,
                tingkatKeparahan = selectedUrgency,
                photoUrls = selectedPhotoBase64List.toList()
            )

            ApiClient.fetchWithFallback(
                callFactory = { api -> api.createReport(token, request) },
                onResponse = { _, response ->
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Kirim Laporan Fasilitas"

                    if (response.isSuccessful) {
                        val result = response.body()
                        Toast.makeText(
                            this@FormPelaporanActivity,
                            "Laporan ${result?.resolvedCode() ?: ""} berhasil dikirim!",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Terjadi kesalahan saat menyimpan laporan"
                        Toast.makeText(this@FormPelaporanActivity, "Gagal mengirim: $errorBody", Toast.LENGTH_LONG).show()
                    }
                },
                onFailure = { _, t ->
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Kirim Laporan Fasilitas"
                    Toast.makeText(
                        this@FormPelaporanActivity,
                        "Gagal terhubung ke server: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun setupUrgencySelection() {
        val btnLow = findViewById<TextView>(R.id.btnUrgencyLow)
        val btnMedium = findViewById<TextView>(R.id.btnUrgencyMedium)
        val btnCritical = findViewById<LinearLayout>(R.id.btnUrgencyCritical)
        val tvCriticalText = findViewById<TextView>(R.id.tvUrgencyCriticalText)
        val ivCriticalIcon = findViewById<ImageView>(R.id.ivUrgencyCriticalIcon)

        btnLow.setOnClickListener {
            selectedUrgency = "Rendah"
            btnLow.setBackgroundResource(R.drawable.bg_btn_rounded_blue)
            btnLow.setTextColor(Color.WHITE)

            btnMedium.setBackgroundResource(0)
            btnMedium.setTextColor(Color.parseColor("#334155"))

            btnCritical.setBackgroundResource(0)
            tvCriticalText.setTextColor(Color.parseColor("#334155"))
            ivCriticalIcon.setColorFilter(Color.parseColor("#334155"))
        }

        btnMedium.setOnClickListener {
            selectedUrgency = "Sedang"
            btnMedium.setBackgroundResource(R.drawable.bg_btn_rounded_blue)
            btnMedium.setTextColor(Color.WHITE)

            btnLow.setBackgroundResource(0)
            btnLow.setTextColor(Color.parseColor("#334155"))

            btnCritical.setBackgroundResource(0)
            tvCriticalText.setTextColor(Color.parseColor("#334155"))
            ivCriticalIcon.setColorFilter(Color.parseColor("#334155"))
        }

        btnCritical.setOnClickListener {
            selectedUrgency = "Kritis / Darurat"
            btnCritical.setBackgroundResource(R.drawable.bg_urgency_critical)
            tvCriticalText.setTextColor(Color.WHITE)
            ivCriticalIcon.setColorFilter(Color.WHITE)

            btnLow.setBackgroundResource(0)
            btnLow.setTextColor(Color.parseColor("#334155"))

            btnMedium.setBackgroundResource(0)
            btnMedium.setTextColor(Color.parseColor("#334155"))
        }
    }

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
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
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
}
