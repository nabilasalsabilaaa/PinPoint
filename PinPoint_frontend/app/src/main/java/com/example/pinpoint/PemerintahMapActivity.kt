package com.example.pinpoint

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class PemerintahMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var chipMapAll: TextView
    private lateinit var chipMapJalan: TextView
    private lateinit var chipMapPenerangan: TextView
    private lateinit var etSearchMap: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))

        enableEdgeToEdge()
        setContentView(R.layout.activity_pemerintah_map)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mapView = findViewById(R.id.mapView)
        chipMapAll = findViewById(R.id.chipMapAll)
        chipMapJalan = findViewById(R.id.chipMapJalan)
        chipMapPenerangan = findViewById(R.id.chipMapPenerangan)
        etSearchMap = findViewById(R.id.etSearchMap)

        val btnGps = findViewById<View>(R.id.btnGps)
        val btnLayer = findViewById<View>(R.id.btnLayer)
        val btnRouteGuidance = findViewById<View>(R.id.btnRouteGuidance)
        val btnReportDetail = findViewById<View>(R.id.btnReportDetail)

        setupMap()
        setupFilterChips()

        btnGps.setOnClickListener {
            val makassarPoint = GeoPoint(-5.147665, 119.432731)
            mapView.controller.animateTo(makassarPoint)
            mapView.controller.setZoom(16.0)
            Toast.makeText(this, "Menemukan Lokasi GPS Anda di Posko Pemerintah", Toast.LENGTH_SHORT).show()
        }

        btnLayer.setOnClickListener {
            Toast.makeText(this, "Mengganti Jenis Lapisan Peta (Satelit / Standar)", Toast.LENGTH_SHORT).show()
        }

        btnRouteGuidance.setOnClickListener {
            Toast.makeText(this, "Memulai Navigasi Rute Menuju Jl. A.P. Pettarani...", Toast.LENGTH_LONG).show()
        }

        btnReportDetail.setOnClickListener {
            Toast.makeText(this, "Membuka Detail Laporan #LP-MKS-0840", Toast.LENGTH_SHORT).show()
        }

        setupBottomNavigation()
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val mapController = mapView.controller
        mapController.setZoom(15.0)

        val makassarCenter = GeoPoint(-5.147665, 119.432731)
        mapController.setCenter(makassarCenter)

        val markerPettarani = Marker(mapView)
        markerPettarani.position = GeoPoint(-5.1512, 119.4285)
        markerPettarani.title = "#LP-MKS-0840: Jalan Berlubang Parah"
        markerPettarani.snippet = "Jl. A.P. Pettarani No. 102, Rappocini"
        markerPettarani.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(markerPettarani)

        val markerUrip = Marker(mapView)
        markerUrip.position = GeoPoint(-5.1325, 119.4380)
        markerUrip.title = "#LP-MKS-0812: Penggantian Tiang & LED PJU"
        markerUrip.snippet = "Jl. Urip Sumoharjo, Panakkukang"
        markerUrip.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(markerUrip)

        val markerPosko = Marker(mapView)
        markerPosko.position = GeoPoint(-5.1476, 119.4320)
        markerPosko.title = "Posko Reaksi Cepat Pemerintah Kota Makassar"
        markerPosko.snippet = "2 Regu Siaga Standby"
        markerPosko.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(markerPosko)

        mapView.invalidate()
    }

    private fun setupFilterChips() {
        chipMapAll.setOnClickListener {
            selectMapChip(chipMapAll, "Semua (28)")
            unselectMapChip(chipMapJalan)
            unselectMapChip(chipMapPenerangan)
            Toast.makeText(this, "Menampilkan Semua 28 Titik Laporan", Toast.LENGTH_SHORT).show()
        }

        chipMapJalan.setOnClickListener {
            selectMapChip(chipMapJalan, "● Jalan (18)")
            unselectMapChip(chipMapAll)
            unselectMapChip(chipMapPenerangan)
            Toast.makeText(this, "Menampilkan 18 Titik Kerusakan Jalan", Toast.LENGTH_SHORT).show()
        }

        chipMapPenerangan.setOnClickListener {
            selectMapChip(chipMapPenerangan, "● Penerangan (4)")
            unselectMapChip(chipMapAll)
            unselectMapChip(chipMapJalan)
            Toast.makeText(this, "Menampilkan 4 Titik Penerangan PJU", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectMapChip(chip: TextView, text: String) {
        chip.background = getDrawable(R.drawable.bg_chip_selected)
        chip.setTextColor(android.graphics.Color.WHITE)
        chip.text = text
    }

    private fun unselectMapChip(chip: TextView) {
        chip.background = getDrawable(R.drawable.bg_chip_white)
        chip.setTextColor(android.graphics.Color.parseColor("#003C90"))
    }

    private fun setupBottomNavigation() {
        val navHome = findViewById<View>(R.id.navPemerintahHome)
        val navMap = findViewById<View>(R.id.navPemerintahMap)
        val navHistory = findViewById<View>(R.id.navPemerintahHistory)
        val navProfile = findViewById<View>(R.id.navPemerintahProfile)

        val ivMap = findViewById<ImageView>(R.id.ivNavPemerintahMap)
        val tvMap = findViewById<TextView>(R.id.tvNavPemerintahMap)
        ivMap?.setColorFilter(android.graphics.Color.parseColor("#003C90"))
        tvMap?.setTextColor(android.graphics.Color.parseColor("#003C90"))
        tvMap?.setTypeface(null, android.graphics.Typeface.BOLD)

        val ivHome = findViewById<ImageView>(R.id.ivNavPemerintahHome)
        val tvHome = findViewById<TextView>(R.id.tvNavPemerintahHome)
        ivHome?.setColorFilter(android.graphics.Color.parseColor("#94A3B8"))
        tvHome?.setTextColor(android.graphics.Color.parseColor("#94A3B8"))
        tvHome?.setTypeface(null, android.graphics.Typeface.NORMAL)

        navHome?.setOnClickListener {
            startActivity(Intent(this, PemerintahDashboardActivity::class.java))
            finish()
        }

        navMap?.setOnClickListener {
            // Already here
        }

        navHistory?.setOnClickListener {
            startActivity(Intent(this, PemerintahHistoryActivity::class.java))
            finish()
        }

        navProfile?.setOnClickListener {
            startActivity(Intent(this, PemerintahProfileActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
