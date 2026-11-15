package com.example.pinpoint

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.pinpoint.admin.NavbarUtils
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.CategoryDto
import com.example.pinpoint.network.ReportDto
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class PemerintahMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
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
    private var selectedReportIndex = 0
    private var selectedCategoryId: Long? = null
    private var selectedCategoryName: String? = null
    private var categoriesList = listOf<CategoryDto>()

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

        initViews()
        setupMap()
        setupSearchAndActions()
        NavbarUtils.setupPemerintahNavbar(this)

        loadCategoriesAndReports()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        loadCategoriesAndReports()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    private fun initViews() {
        mapView = findViewById(R.id.mapView)
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

    private fun setupMap() {
        // Biarkan peta seperti semula (access block tidak masalah sesuai instruksi)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        val mapController = mapView.controller
        mapController.setZoom(15.0)
        mapController.setCenter(GeoPoint(-5.147665, 119.432731))
    }

    private fun setupSearchAndActions() {
        val btnGps = findViewById<View>(R.id.btnGps)
        val btnLayer = findViewById<View>(R.id.btnLayer)
        val btnRouteGuidance = findViewById<View>(R.id.btnRouteGuidance)
        val btnReportDetail = findViewById<View>(R.id.btnReportDetail)

        // Tombol lokasi dan satelit dibiarkan sesuai instruksi
        btnGps.setOnClickListener {
            Toast.makeText(this, "Fitur lokasi GPS belum diaktifkan", Toast.LENGTH_SHORT).show()
        }

        btnLayer.setOnClickListener {
            Toast.makeText(this, "Fitur lapisan peta satelit belum diaktifkan", Toast.LENGTH_SHORT).show()
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
                selectedReportIndex--
                bindSelectedReportToCard()
            }
        }

        btnNextMapReport.setOnClickListener {
            if (filteredReports.isNotEmpty() && selectedReportIndex < filteredReports.size - 1) {
                selectedReportIndex++
                bindSelectedReportToCard()
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
            if (current != null) {
                val lat = current.latitude ?: -5.1476
                val lng = current.longitude ?: 119.4327
                val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(current.resolvedLocationName())})")
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (_: Exception) {
                    Toast.makeText(this, "Navigasi menuju ${current.resolvedLocationName()}", Toast.LENGTH_SHORT).show()
                }
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
                    // Sinkronkan dengan laporan masuk untuk pemerintah (TERVERIFIKASI & DIPROSES)
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

        selectedReportIndex = 0
        bindSelectedReportToCard()
    }

    private fun syncMarkersOnMap() {
        mapView.overlays.clear()
        filteredReports.forEachIndexed { index, report ->
            val lat = report.latitude ?: -5.1476
            val lng = report.longitude ?: 119.4327
            val marker = Marker(mapView)
            marker.position = GeoPoint(lat, lng)
            marker.title = "${report.resolvedCode()}: ${report.resolvedTitle()}"
            marker.snippet = report.resolvedLocationName()
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.setOnMarkerClickListener { _, _ ->
                selectedReportIndex = index
                bindSelectedReportToCard()
                true
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
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
}
