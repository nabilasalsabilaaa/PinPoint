package com.example.pinpoint

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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pinpoint.admin.DetailVerifikasiActivity
import com.example.pinpoint.admin.NavbarUtils
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.CategoryDto
import com.example.pinpoint.network.ReportDto

class PemerintahHistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var historyAdapter: PemerintahHistoryAdapter
    private lateinit var etSearchHistory: EditText
    private lateinit var tvEmptyHistory: TextView
    private lateinit var tvHistoryCompletedCount: TextView
    private lateinit var tvHistoryCompletionRate: TextView
    private lateinit var llHistoryCategoryChips: LinearLayout

    private val allHistoryList = mutableListOf<PemerintahHistoryItem>()
    private var categoriesList = listOf<CategoryDto>()
    private var selectedCategoryId: Long? = null
    private var selectedCategoryName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pemerintah_history)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupRecyclerView()
        setupSearch()
        NavbarUtils.setupPemerintahNavbar(this)

        loadCategoriesAndHistory()
    }

    override fun onResume() {
        super.onResume()
        loadCategoriesAndHistory()
    }

    private fun initViews() {
        rvHistory = findViewById(R.id.rvPemerintahHistory)
        etSearchHistory = findViewById(R.id.etSearchHistory)
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory)
        tvHistoryCompletedCount = findViewById(R.id.tvHistoryCompletedCount)
        tvHistoryCompletionRate = findViewById(R.id.tvHistoryCompletionRate)
        llHistoryCategoryChips = findViewById(R.id.llHistoryCategoryChips)
    }

    private fun setupRecyclerView() {
        rvHistory.layoutManager = LinearLayoutManager(this)
        historyAdapter = PemerintahHistoryAdapter(emptyList()) { item ->
            // Saat card diklik mengarahkan ke Detail Verifikasi
            val intent = Intent(this, DetailVerifikasiActivity::class.java)
            intent.putExtra("REPORT_ID", item.reportId)
            startActivity(intent)
        }
        rvHistory.adapter = historyAdapter
    }

    private fun loadCategoriesAndHistory() {
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
                    bindDatabaseHistory(response.body()!!)
                }
            },
            onFailure = { _, _ ->
                Toast.makeText(this@PemerintahHistoryActivity, "Gagal memuat riwayat dari server", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun bindDatabaseHistory(reports: List<ReportDto>) {
        val verifiedCount = reports.count { it.resolvedStatusKey() == "TERVERIFIKASI" }
        val inProgressCount = reports.count { it.resolvedStatusKey() == "DIPROSES" }
        val completedReports = reports.filter { it.resolvedStatusKey() == "SELESAI" }
        val completedCount = completedReports.size
        val totalGovReports = verifiedCount + inProgressCount + completedCount

        tvHistoryCompletedCount.text = completedCount.toString()
        val rate = if (totalGovReports > 0) {
            (completedCount * 100.0 / totalGovReports)
        } else {
            0.0
        }
        tvHistoryCompletionRate.text = String.format("%.1f%%", rate)

        allHistoryList.clear()
        completedReports.forEach { dto ->
            val beforePhoto = dto.resolvedPhotoUrl() ?: ""
            val evPhotos = dto.resolvedEvidencePhotoUrls()
            val afterPhoto = evPhotos.firstOrNull() ?: beforePhoto
            val combinedPhotos = mutableListOf<String>()
            dto.resolvedPhotoUrls().forEach { if (!combinedPhotos.contains(it)) combinedPhotos.add(it) }
            evPhotos.forEach { if (!combinedPhotos.contains(it)) combinedPhotos.add(it) }

            val notes = dto.resolvedEvidenceNotes().takeIf { it.isNotBlank() }
                ?: dto.resolvedDescription()

            allHistoryList.add(
                PemerintahHistoryItem(
                    reportId = dto.resolvedId().toString(),
                    id = dto.resolvedCode(),
                    categoryId = dto.idKategori,
                    categoryName = dto.resolvedCategoryName(),
                    finishDate = "Selesai ${dto.resolvedUpdatedAt()}",
                    status = "SELESAI",
                    title = dto.resolvedTitle(),
                    location = dto.resolvedLocationName(),
                    sectionHeader = "Komparasi Hasil Pengerjaan (${dto.resolvedCode()})",
                    sectionSubHeader = "Ketuk foto untuk geser",
                    beforeImageUrl = beforePhoto,
                    beforeLabel = "SEBELUM",
                    afterImageUrl = afterPhoto,
                    afterLabel = "SESUDAH",
                    allPhotos = combinedPhotos,
                    workNotes = "Catatan Pengerjaan: $notes",
                    teamName = dto.resolvedCompletedBy()
                )
            )
        }

        renderCategoryChips()
        applySearchAndCategoryFilter()
    }

    private fun renderCategoryChips() {
        llHistoryCategoryChips.removeAllViews()
        val density = resources.displayMetrics.density
        val padH = (14 * density).toInt()
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

        val allChip = createChip("Semua", null)
        llHistoryCategoryChips.addView(allChip)

        categoriesList.forEach { cat ->
            val chip = createChip(cat.resolvedName(), cat)
            llHistoryCategoryChips.addView(chip)
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
                    tv.setBackgroundResource(R.drawable.bg_chip_unselected)
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
                applySearchAndCategoryFilter()
            }
        }

        updateChipStyles()
    }

    private fun setupSearch() {
        etSearchHistory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applySearchAndCategoryFilter()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun applySearchAndCategoryFilter() {
        val query = etSearchHistory.text?.toString()?.lowercase()?.trim() ?: ""

        val filtered = allHistoryList.filter { item ->
            val matchesCategory = (selectedCategoryId == null && selectedCategoryName.isNullOrBlank()) ||
                (selectedCategoryId != null && item.categoryId == selectedCategoryId) ||
                (!selectedCategoryName.isNullOrBlank() && item.categoryName.equals(selectedCategoryName, ignoreCase = true))

            val matchesSearch = query.isEmpty() ||
                item.title.lowercase().contains(query) ||
                item.id.lowercase().contains(query) ||
                item.location.lowercase().contains(query) ||
                item.categoryName.lowercase().contains(query) ||
                item.workNotes.lowercase().contains(query) ||
                item.teamName.lowercase().contains(query)

            matchesCategory && matchesSearch
        }

        historyAdapter.updateList(filtered)
        if (filtered.isEmpty()) {
            tvEmptyHistory.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
        } else {
            tvEmptyHistory.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
        }
    }
}
