
package com.example.pinpoint

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

class PilihLokasiActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LATITUDE = "EXTRA_LATITUDE"
        const val EXTRA_LONGITUDE = "EXTRA_LONGITUDE"
        const val EXTRA_ADDRESS = "EXTRA_ADDRESS"
    }

    // Google Maps
    private lateinit var googleMapView: MapView
    private var googleMap: GoogleMap? = null

    // GPS
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Komponen tampilan
    private lateinit var tvSelectedCoordinates: TextView
    private lateinit var tvSelectedAddress: TextView
    private lateinit var btnConfirmLocation: Button
    private lateinit var btnGpsMyLocation: View

    // Lokasi terpilih
    private var selectedLatitude: Double = -5.147665
    private var selectedLongitude: Double = 119.432732
    private var selectedAddress: String = "Mendeteksi posisi titik lokasi..."

    private var lastGeocodeLat = 0.0
    private var lastGeocodeLng = 0.0

    private var initialAddress: String? = null
    private var hasInitialLocation = false

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        val fineGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

        val coarseGranted =
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            enableMyLocationLayer()
            detectAndMoveToCurrentLocation(animate = true)
        } else {
            Toast.makeText(
                this,
                "Izin lokasi ditolak. Menampilkan posisi peta default.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_pilih_lokasi)

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        initViews()

        // Ambil koordinat awal dari Activity pemanggil sebelum peta dibuat
        val initialLat = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
        val initialLng = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
        initialAddress = intent.getStringExtra(EXTRA_ADDRESS)

        if (initialLat != 0.0 && initialLng != 0.0) {
            selectedLatitude = initialLat
            selectedLongitude = initialLng
            hasInitialLocation = true

            if (!initialAddress.isNullOrBlank()) {
                selectedAddress = initialAddress!!
                tvSelectedAddress.text = selectedAddress
            }
        }

        setupGoogleMap(savedInstanceState)
    }

    private fun initViews() {
        googleMapView = findViewById(R.id.googleMapView)

        tvSelectedCoordinates =
            findViewById(R.id.tvSelectedCoordinates)

        tvSelectedAddress =
            findViewById(R.id.tvSelectedAddress)

        btnConfirmLocation =
            findViewById(R.id.btnConfirmLocation)

        btnGpsMyLocation =
            findViewById(R.id.btnGpsMyLocation)

        findViewById<ImageView>(R.id.btnBackPicker).setOnClickListener {
            finish()
        }

        btnGpsMyLocation.setOnClickListener {
            if (hasLocationPermission()) {
                enableMyLocationLayer()
                detectAndMoveToCurrentLocation(animate = true)
            } else {
                requestLocationPermission()
            }
        }

        btnConfirmLocation.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra(EXTRA_LATITUDE, selectedLatitude)
                putExtra(EXTRA_LONGITUDE, selectedLongitude)
                putExtra(EXTRA_ADDRESS, selectedAddress)
            }

            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    // =========================================================
    // GOOGLE MAPS
    // =========================================================

    private fun setupGoogleMap(savedInstanceState: Bundle?) {
        googleMapView.onCreate(savedInstanceState)

        googleMapView.getMapAsync { map ->
            googleMap = map

            map.uiSettings.isZoomControlsEnabled = false
            map.uiSettings.isMapToolbarEnabled = false
            map.uiSettings.isCompassEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = false

            // Tentukan posisi awal peta
            val initialPosition = LatLng(
                selectedLatitude,
                selectedLongitude
            )

            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    initialPosition,
                    17.5f
                )
            )

            // Ketika pengguna selesai menggeser peta,
            // ambil koordinat titik tengah peta.
            map.setOnCameraIdleListener {
                updateLocationFromMapCenter()
            }

            if (hasLocationPermission()) {
                enableMyLocationLayer()

                if (!hasInitialLocation) {
                    detectAndMoveToCurrentLocation(animate = false)
                } else {
                    updateCoordinateText()
                }
            } else {
                updateCoordinateText()

                if (!hasInitialLocation) {
                    requestLocationPermission()
                }
            }
        }
    }

    private fun updateLocationFromMapCenter() {
        val center = googleMap?.cameraPosition?.target ?: return

        selectedLatitude = center.latitude
        selectedLongitude = center.longitude

        updateCoordinateText()

        // Hindari reverse geocoding berulang jika pergeseran sangat kecil
        val latDiff = Math.abs(selectedLatitude - lastGeocodeLat)
        val lngDiff = Math.abs(selectedLongitude - lastGeocodeLng)

        if (latDiff > 0.0003 || lngDiff > 0.0003) {
            lastGeocodeLat = selectedLatitude
            lastGeocodeLng = selectedLongitude

            performReverseGeocoding(
                selectedLatitude,
                selectedLongitude
            )
        }
    }

    private fun updateCoordinateText() {
        tvSelectedCoordinates.text = String.format(
            Locale.US,
            "Lat: %.5f • Lng: %.5f",
            selectedLatitude,
            selectedLongitude
        )
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

    // =========================================================
    // IZIN LOKASI DAN GPS
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
    private fun detectAndMoveToCurrentLocation(animate: Boolean) {
        if (!hasLocationPermission()) return

        fusedLocationClient
            .getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            )
            .addOnSuccessListener { location: Location? ->

                if (location != null) {
                    moveToLocation(
                        location.latitude,
                        location.longitude,
                        animate
                    )
                } else {
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { lastLoc: Location? ->

                            if (lastLoc != null) {
                                moveToLocation(
                                    lastLoc.latitude,
                                    lastLoc.longitude,
                                    animate
                                )
                            } else {
                                Toast.makeText(
                                    this,
                                    "Tidak dapat mendeteksi lokasi saat ini. Pastikan GPS aktif.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Gagal mengambil lokasi GPS: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun moveToLocation(
        latitude: Double,
        longitude: Double,
        animate: Boolean
    ) {
        selectedLatitude = latitude
        selectedLongitude = longitude

        val position = LatLng(latitude, longitude)

        val cameraUpdate =
            CameraUpdateFactory.newLatLngZoom(position, 17.5f)

        if (animate) {
            googleMap?.animateCamera(cameraUpdate)
        } else {
            googleMap?.moveCamera(cameraUpdate)
        }

        // CameraIdleListener akan memperbarui koordinat
        // dan menjalankan reverse geocoding.
    }

    // =========================================================
    // REVERSE GEOCODING
    // =========================================================

    private fun performReverseGeocoding(lat: Double, lng: Double) {
        tvSelectedAddress.text = "Mengambil nama jalan & alamat..."

        Thread {
            try {
                val geocoder = Geocoder(
                    this@PilihLokasiActivity,
                    Locale("id", "ID")
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                    geocoder.getFromLocation(lat, lng, 1) { addresses ->

                        val addressText = formatAddress(
                            addresses.firstOrNull(),
                            lat,
                            lng
                        )

                        runOnUiThread {
                            // Hindari hasil alamat lama menimpa titik terbaru
                            if (selectedLatitude == lat &&
                                selectedLongitude == lng
                            ) {
                                selectedAddress = addressText
                                tvSelectedAddress.text = addressText
                            }
                        }
                    }

                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)

                    val addressText = formatAddress(
                        addresses?.firstOrNull(),
                        lat,
                        lng
                    )

                    runOnUiThread {
                        if (selectedLatitude == lat &&
                            selectedLongitude == lng
                        ) {
                            selectedAddress = addressText
                            tvSelectedAddress.text = addressText
                        }
                    }
                }

            } catch (e: Exception) {
                val fallback = String.format(
                    Locale.US,
                    "Titik Lokasi (%.5f, %.5f)",
                    lat,
                    lng
                )

                runOnUiThread {
                    if (selectedLatitude == lat &&
                        selectedLongitude == lng
                    ) {
                        selectedAddress = fallback
                        tvSelectedAddress.text = fallback
                    }
                }
            }
        }.start()
    }

    private fun formatAddress(
        address: Address?,
        lat: Double,
        lng: Double
    ): String {

        if (address == null) {
            return String.format(
                Locale.US,
                "Titik Koordinat (%.5f, %.5f)",
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
        val adminArea = address.adminArea

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
        } else if (!adminArea.isNullOrBlank() &&
            !parts.contains(adminArea)
        ) {
            parts.add(adminArea)
        }

        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            address.getAddressLine(0)
                ?: String.format(
                    Locale.US,
                    "Koordinat: %.5f, %.5f",
                    lat,
                    lng
                )
        }
    }

    // =========================================================
    // GOOGLE MAPVIEW LIFECYCLE
    // =========================================================

    override fun onResume() {
        super.onResume()

        if (::googleMapView.isInitialized) {
            googleMapView.onResume()
        }
    }

    override fun onPause() {
        if (::googleMapView.isInitialized) {
            googleMapView.onPause()
        }

        super.onPause()
    }

    override fun onDestroy() {
        if (::googleMapView.isInitialized) {
            googleMapView.onDestroy()
        }

        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()

        if (::googleMapView.isInitialized) {
            googleMapView.onLowMemory()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (::googleMapView.isInitialized) {
            googleMapView.onSaveInstanceState(outState)
        }
    }
}
