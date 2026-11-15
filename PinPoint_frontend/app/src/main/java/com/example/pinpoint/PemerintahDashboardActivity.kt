package com.example.pinpoint

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pinpoint.admin.NavbarUtils
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.ReportDto

class PemerintahDashboardActivity : AppCompatActivity() {

    private lateinit var rvReports: RecyclerView
    private lateinit var reportAdapter: PemerintahReportAdapter
    private lateinit var tvEmptyReports: TextView

    private lateinit var tvStatPerluAksi: TextView
    private lateinit var tvStatVerified: TextView
    private lateinit var tvStatInProgress: TextView
    private lateinit var tvStatCompleted: TextView

    private lateinit var chipFilterAll: TextView
    private lateinit var chipFilterVerified: TextView
    private lateinit var chipFilterInProgress: TextView

    private val activeReportsList = mutableListOf<PemerintahReport>()
    private var currentFilterStatus: String = "ALL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pemerintah_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupRecyclerView()
        setupFilterChips()
        NavbarUtils.setupPemerintahNavbar(this)

        findViewById<View>(R.id.btnCallTeam)?.setOnClickListener {
            Toast.makeText(this, "Menghubungi Tim Siaga Respon Cepat...", Toast.LENGTH_SHORT).show()
        }

        loadGovernmentDashboardData()
    }

    override fun onResume() {
        super.onResume()
        updateOfficerGreeting()
        loadGovernmentDashboardData()
    }

    private fun initViews() {
        rvReports = findViewById(R.id.rvPemerintahReports)
        tvEmptyReports = findViewById(R.id.tvEmptyPemerintahReports)

        tvStatPerluAksi = findViewById(R.id.tvStatPerluAksi)
        tvStatVerified = findViewById(R.id.tvStatVerified)
        tvStatInProgress = findViewById(R.id.tvStatInProgress)
        tvStatCompleted = findViewById(R.id.tvStatCompleted)

        chipFilterAll = findViewById(R.id.chipFilterAll)
        chipFilterVerified = findViewById(R.id.chipFilterVerified)
        chipFilterInProgress = findViewById(R.id.chipFilterInProgress)

        updateOfficerGreeting()
    }

    private fun updateOfficerGreeting() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val userName = sharedPreferences.getString("KEY_NAME", null)?.takeIf { it.isNotBlank() } ?: "Petugas"
        findViewById<TextView>(R.id.tvOfficerName)?.text = userName
    }

    private fun setupRecyclerView() {
        rvReports.layoutManager = LinearLayoutManager(this)
        reportAdapter = PemerintahReportAdapter(emptyList()) { report ->
            val intent = Intent(this, PemerintahDetailTaskActivity::class.java)
            intent.putExtra("REPORT_ID", report.reportId)
            startActivity(intent)
        }
        rvReports.adapter = reportAdapter
    }

    private fun loadGovernmentDashboardData() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val rawToken = sharedPreferences.getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

        ApiClient.fetchWithFallback(
            callFactory = { api -> api.getAllReports(token, null) },
            onResponse = { _, response ->
                if (response.isSuccessful && response.body() != null) {
                    val allDto = response.body()!!
                    updateStatsAndList(allDto)
                }
            },
            onFailure = { _, _ ->
                Toast.makeText(this@PemerintahDashboardActivity, "Gagal memuat data laporan dari server", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateStatsAndList(reports: List<ReportDto>) {
        // Laporan hasil verifikasi dari admin memiliki status TERVERIFIKASI, DIPROSES, atau SELESAI
        val verifiedCount = reports.count { it.resolvedStatusKey() == "TERVERIFIKASI" }
        val inProgressCount = reports.count { it.resolvedStatusKey() == "DIPROSES" }
        val completedCount = reports.count { it.resolvedStatusKey() == "SELESAI" }
        val perluAksiCount = verifiedCount + inProgressCount

        tvStatPerluAksi.text = perluAksiCount.toString()
        tvStatVerified.text = verifiedCount.toString()
        tvStatInProgress.text = inProgressCount.toString()
        tvStatCompleted.text = completedCount.toString()

        chipFilterAll.text = "Semua Tugas ($perluAksiCount)"
        chipFilterVerified.text = "Terverifikasi ($verifiedCount)"
        chipFilterInProgress.text = "Sedang Diproses ($inProgressCount)"

        activeReportsList.clear()
        val tasksReadyForGov = reports.filter {
            val status = it.resolvedStatusKey()
            status == "TERVERIFIKASI" || status == "DIPROSES"
        }

        tasksReadyForGov.forEach { dto ->
            val statusKey = dto.resolvedStatusKey()
            activeReportsList.add(
                PemerintahReport(
                    reportId = dto.resolvedId().toString(),
                    id = dto.resolvedCode(),
                    date = dto.resolvedCreatedAt(),
                    category = dto.resolvedCategoryName(),
                    title = dto.resolvedTitle(),
                    location = dto.resolvedLocationName(),
                    imageUrl = dto.resolvedPhotoUrl() ?: "",
                    status = if (statusKey == "DIPROSES") "SEDANG_DIPROSES" else "TERVERIFIKASI",
                    footerTag = "Pelapor: ${dto.resolvedReporterName()}",
                    actionText = "Detail & Tindak Lanjut",
                    isPrimaryButton = statusKey == "TERVERIFIKASI"
                )
            )
        }

        applyCurrentFilter()
    }

    private fun setupFilterChips() {
        chipFilterAll.setOnClickListener {
            currentFilterStatus = "ALL"
            selectChip(chipFilterAll)
            unselectChip(chipFilterVerified)
            unselectChip(chipFilterInProgress)
            applyCurrentFilter()
        }

        chipFilterVerified.setOnClickListener {
            currentFilterStatus = "TERVERIFIKASI"
            unselectChip(chipFilterAll)
            selectChip(chipFilterVerified)
            unselectChip(chipFilterInProgress)
            applyCurrentFilter()
        }

        chipFilterInProgress.setOnClickListener {
            currentFilterStatus = "SEDANG_DIPROSES"
            unselectChip(chipFilterAll)
            unselectChip(chipFilterVerified)
            selectChip(chipFilterInProgress)
            applyCurrentFilter()
        }
    }

    private fun applyCurrentFilter() {
        val filtered = when (currentFilterStatus) {
            "TERVERIFIKASI" -> activeReportsList.filter { it.status == "TERVERIFIKASI" }
            "SEDANG_DIPROSES" -> activeReportsList.filter { it.status == "SEDANG_DIPROSES" }
            else -> activeReportsList.toList()
        }

        reportAdapter.updateList(filtered)
        if (filtered.isEmpty()) {
            tvEmptyReports.visibility = View.VISIBLE
            rvReports.visibility = View.GONE
        } else {
            tvEmptyReports.visibility = View.GONE
            rvReports.visibility = View.VISIBLE
        }
    }

    private fun selectChip(chip: TextView) {
        chip.background = ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
        chip.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        chip.setTypeface(null, android.graphics.Typeface.BOLD)
    }

    private fun unselectChip(chip: TextView) {
        chip.background = ContextCompat.getDrawable(this, R.drawable.bg_chip_unselected)
        chip.setTextColor(Color.parseColor("#434653"))
        chip.setTypeface(null, android.graphics.Typeface.NORMAL)
    }
}
