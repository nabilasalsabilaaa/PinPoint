package com.example.pinpoint.admin

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pinpoint.R
import com.example.pinpoint.adapter.ReportAdapter
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.CategoryDto
import com.example.pinpoint.network.ReportDto

class AdminDashboardActivity : AppCompatActivity() {

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

    private var allReports = listOf<ReportDto>()
    private var selectedCategoryId: Long? = null
    private var selectedCategoryName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pembatasan Role: Hanya Role 1 (ADMIN) yang diizinkan mengakses modul Admin
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val idRole = sharedPreferences.getLong("KEY_ID_ROLE", -1L)
        val roleStr = (sharedPreferences.getString("KEY_ROLE", "") ?: "").uppercase()
        val isAdmin = idRole == 1L || roleStr == "ADMIN" || roleStr == "ROLE_ADMIN"
        if (!isAdmin) {
            startActivity(Intent(this, com.example.pinpoint.DashboardActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_admin_dashboard)

        initViews()
        setupBottomNavigation()
        setupRecyclerView()

        val openVerify = {
            startActivity(Intent(this, VerifikasiLaporanActivity::class.java))
        }
        tvSeeAllReports.setOnClickListener { openVerify() }
        btnCheckCritical.setOnClickListener { openVerify() }

        loadRegisteredCategories()
        loadDashboardData()
    }

    override fun onResume() {
        super.onResume()
        updateGreeting()
        loadRegisteredCategories()
        loadDashboardData()
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
        updateGreeting()
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
