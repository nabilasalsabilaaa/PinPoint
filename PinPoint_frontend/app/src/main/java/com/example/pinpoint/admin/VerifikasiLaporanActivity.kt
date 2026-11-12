package com.example.pinpoint.admin

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
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

class VerifikasiLaporanActivity : AppCompatActivity() {

    private lateinit var etSearchReport: EditText
    private lateinit var llVerifCategoryChips: LinearLayout
    private lateinit var rvVerifReports: RecyclerView
    private lateinit var layoutEmptyState: View
    private var tvEmptyTitle: TextView? = null
    private var tvEmptySubtitle: TextView? = null

    private lateinit var reportAdapter: ReportAdapter
    private var allReports = listOf<ReportDto>()

    private var currentFilterMode = "ALL" // "ALL", "PENDING", or "CATEGORY"
    private var selectedCategoryId: Long? = null
    private var selectedCategoryName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_verifikasi_laporan)

        initViews()
        setupBottomNavigation()
        setupRecyclerView()
        setupSearch()

        loadRegisteredCategories()
        fetchReports()
    }

    override fun onResume() {
        super.onResume()
        loadRegisteredCategories()
        fetchReports()
    }

    private fun initViews() {
        etSearchReport = findViewById(R.id.etSearchReport)
        llVerifCategoryChips = findViewById(R.id.llVerifCategoryChips)
        rvVerifReports = findViewById(R.id.rvVerifReports)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        tvEmptyTitle = layoutEmptyState.findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = layoutEmptyState.findViewById(R.id.tvEmptySubtitle)
    }

    private fun setupBottomNavigation() {
        NavbarUtils.setupAdminNavbar(this)
    }

    private fun setupRecyclerView() {
        rvVerifReports.layoutManager = LinearLayoutManager(this)
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
        rvVerifReports.adapter = reportAdapter
    }

    private fun setupSearch() {
        etSearchReport.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterReports()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
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
                    renderFilterChips(categories)
                }
            },
            onFailure = { _, _ -> }
        )
    }

    private data class ChipOption(
        val mode: String,
        val label: String,
        val categoryId: Long? = null,
        val categoryName: String? = null
    )

    private fun renderFilterChips(categories: List<CategoryDto>) {
        llVerifCategoryChips.removeAllViews()
        val density = resources.displayMetrics.density
        val padH = (14 * density).toInt()
        val padV = (8 * density).toInt()
        val marginEnd = (8 * density).toInt()

        val options = mutableListOf(
            ChipOption("ALL", "Semua"),
            ChipOption("PENDING", "Menunggu")
        )
        categories.forEach { cat ->
            options.add(ChipOption("CATEGORY", cat.resolvedName(), cat.resolvedId(), cat.resolvedName()))
        }

        val chipViews = mutableListOf<Pair<TextView, ChipOption>>()

        options.forEach { opt ->
            val tv = TextView(this)
            tv.text = opt.label
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
            chipViews.add(Pair(tv, opt))
            llVerifCategoryChips.addView(tv)
        }

        fun updateChipStyles() {
            chipViews.forEach { (tv, opt) ->
                val isSelected = when (opt.mode) {
                    "ALL" -> currentFilterMode == "ALL"
                    "PENDING" -> currentFilterMode == "PENDING"
                    "CATEGORY" -> currentFilterMode == "CATEGORY" && selectedCategoryId == opt.categoryId
                    else -> false
                }
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

        chipViews.forEach { (tv, opt) ->
            tv.setOnClickListener {
                currentFilterMode = opt.mode
                selectedCategoryId = opt.categoryId
                selectedCategoryName = opt.categoryName
                updateChipStyles()
                filterReports()
            }
        }

        updateChipStyles()
    }

    private fun fetchReports() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val rawToken = sharedPreferences.getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

        ApiClient.fetchWithFallback(
            callFactory = { api -> api.getAllReports(token, null) },
            onResponse = { _, response ->
                if (response.isSuccessful && response.body() != null) {
                    allReports = response.body()!!
                    filterReports()
                } else {
                    showEmptyState("Gagal Memuat Laporan", "Terjadi kesalahan saat mengambil data dari server.")
                }
            },
            onFailure = { _, _ ->
                showEmptyState("Koneksi Bermasalah", "Periksa jaringan internet Anda dan coba lagi.")
            }
        )
    }

    private fun filterReports() {
        val query = etSearchReport.text.toString().lowercase().trim()
        val filteredList = allReports.filter { report ->
            val matchesQuery = query.isEmpty() ||
                    report.resolvedTitle().lowercase().contains(query) ||
                    report.resolvedCode().lowercase().contains(query) ||
                    report.resolvedReporterName().lowercase().contains(query) ||
                    report.resolvedCategoryName().lowercase().contains(query) ||
                    report.resolvedLocationName().lowercase().contains(query)

            val matchesFilter = when (currentFilterMode) {
                "ALL" -> true
                "PENDING" -> report.resolvedStatusKey() == "MENUNGGU_VERIFIKASI"
                "CATEGORY" -> (selectedCategoryId != null && report.idKategori == selectedCategoryId) ||
                        (!selectedCategoryName.isNullOrBlank() && report.resolvedCategoryName().equals(selectedCategoryName, ignoreCase = true))
                else -> true
            }

            matchesQuery && matchesFilter
        }

        if (filteredList.isEmpty()) {
            showEmptyState("Belum Ada Laporan", "Tidak ada laporan yang cocok dengan filter saat ini.")
        } else {
            layoutEmptyState.visibility = View.GONE
            rvVerifReports.visibility = View.VISIBLE
            reportAdapter.updateData(filteredList)
        }
    }

    private fun showEmptyState(title: String, subtitle: String) {
        rvVerifReports.visibility = View.GONE
        layoutEmptyState.visibility = View.VISIBLE
        tvEmptyTitle?.text = title
        tvEmptySubtitle?.text = subtitle
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
                    Toast.makeText(this@VerifikasiLaporanActivity, "Laporan ${report.resolvedCode()} berhasil divalidasi!", Toast.LENGTH_SHORT).show()
                    fetchReports()
                } else {
                    Toast.makeText(this@VerifikasiLaporanActivity, "Gagal memvalidasi laporan (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { _, _ ->
                Toast.makeText(this@VerifikasiLaporanActivity, "Koneksi gagal", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
