package com.example.pinpoint.user

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pinpoint.R
import com.example.pinpoint.adapter.UserReportAdapter
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.LaporanResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RiwayatActivity : AppCompatActivity() {

    private lateinit var rvUserHistory: RecyclerView
    private lateinit var tvEmptyRiwayat: TextView
    private lateinit var tvFilterStatus: TextView
    private lateinit var etSearchTicket: EditText
    private lateinit var historyAdapter: UserReportAdapter

    private var myReportsList: List<LaporanResponse> = emptyList()
    private var selectedStatusFilter: Long? = null // null = Semua Status

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_riwayat)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRiwayat)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        rvUserHistory = findViewById(R.id.rvUserHistory)
        tvEmptyRiwayat = findViewById(R.id.tvEmptyRiwayat)
        tvFilterStatus = findViewById(R.id.tvCurrentFilterStatus)
        etSearchTicket = findViewById(R.id.etSearchTicket)

        rvUserHistory.layoutManager = LinearLayoutManager(this)
        historyAdapter = UserReportAdapter(
            reports = emptyList(),
            getSupportCount = { r -> getEffectiveSupportCount(r) },
            isSupportedByMe = { r -> isReportSupportedByMe(r.id ?: 0L) },
            onSupportToggle = { r -> toggleSupportForReport(r) },
            onDetailClick = { report ->
                val intent = Intent(this@RiwayatActivity, DetailLaporanActivity::class.java).apply {
                    putExtra("EXTRA_REPORT_ID", report.id ?: -1L)
                    putExtra("EXTRA_REPORT_TITLE", report.resolvedTitle())
                    putExtra("EXTRA_REPORT_DESC", report.resolvedDescription())
                    putExtra("EXTRA_REPORT_LOCATION", report.resolvedLocation())
                    putExtra("EXTRA_REPORT_STATUS", report.idStatus ?: 1L)
                    putExtra("EXTRA_REPORTER_NAME", report.resolvedReporterName())
                }
                startActivity(intent)
            }
        )
        rvUserHistory.adapter = historyAdapter

        setupSearchAndFilter()
        setupBottomNavigation()
        loadMyReportsFromDatabase()
    }

    override fun onResume() {
        super.onResume()
        loadMyReportsFromDatabase()
    }

    private fun setupSearchAndFilter() {
        etSearchTicket.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyHistoryFilter()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<View>(R.id.btnFilterStatus).setOnClickListener {
            val allCount = myReportsList.size
            val pendingCount = myReportsList.count { it.idStatus == 1L }
            val verifiedCount = myReportsList.count { it.idStatus == 2L }
            val processCount = myReportsList.count { it.idStatus == 4L }
            val completedCount = myReportsList.count { it.idStatus == 5L }
            val rejectedCount = myReportsList.count { it.idStatus == 3L }

            val filterOptions = arrayOf(
                "Semua Status ($allCount)",
                "Menunggu Verifikasi ($pendingCount)",
                "Terverifikasi ($verifiedCount)",
                "Sedang Diproses ($processCount)",
                "Selesai ($completedCount)",
                "Ditolak ($rejectedCount)"
            )
            val statusValues = arrayOf<Long?>(null, 1L, 2L, 4L, 5L, 3L)

            AlertDialog.Builder(this)
                .setTitle("Pilih Filter Status")
                .setItems(filterOptions) { _, which ->
                    selectedStatusFilter = statusValues[which]
                    tvFilterStatus.text = filterOptions[which]
                    applyHistoryFilter()
                }
                .show()
        }
    }

    private fun loadMyReportsFromDatabase() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val token = sharedPreferences.getString("KEY_TOKEN", null)
        if (token.isNullOrEmpty()) return

        ApiClient.instance.getMyReports("Bearer $token")
            .enqueue(object : Callback<List<LaporanResponse>> {
                override fun onResponse(
                    call: Call<List<LaporanResponse>>,
                    response: Response<List<LaporanResponse>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        myReportsList = response.body()!!
                        if (selectedStatusFilter == null) {
                            tvFilterStatus.text = "Semua Status (${myReportsList.size})"
                        }
                        applyHistoryFilter()
                    }
                }

                override fun onFailure(call: Call<List<LaporanResponse>>, t: Throwable) {}
            })
    }

    private fun applyHistoryFilter() {
        val query = etSearchTicket.text?.toString()?.trim()?.lowercase() ?: ""

        val filtered = myReportsList.filter { r ->
            val matchesStatus = selectedStatusFilter == null || r.idStatus == selectedStatusFilter
            val matchesQuery = query.isEmpty() ||
                r.resolvedCode().lowercase().contains(query) ||
                (r.id?.toString()?.contains(query) == true) ||
                r.resolvedTitle().lowercase().contains(query) ||
                r.resolvedLocation().lowercase().contains(query) ||
                r.resolvedCategoryName().lowercase().contains(query) ||
                r.resolvedDescription().lowercase().contains(query)
            matchesStatus && matchesQuery
        }

        historyAdapter.updateData(filtered)
        if (filtered.isEmpty()) {
            tvEmptyRiwayat.visibility = View.VISIBLE
            rvUserHistory.visibility = View.GONE
        } else {
            tvEmptyRiwayat.visibility = View.GONE
            rvUserHistory.visibility = View.VISIBLE
        }
    }

    private fun getSupportPrefs() = getSharedPreferences("UserReportSupportPrefs", MODE_PRIVATE)

    private fun getCurrentUserKey(): String {
        val session = getSharedPreferences("UserSession", MODE_PRIVATE)
        return (session.getString("KEY_EMAIL", "guest") ?: "guest").lowercase()
    }

    private fun isReportSupportedByMe(reportId: Long): Boolean {
        return getSupportPrefs().getBoolean("SUPPORTED_${getCurrentUserKey()}_$reportId", false)
    }

    private fun getEffectiveSupportCount(report: LaporanResponse): Int {
        val id = report.id ?: 0L
        val savedCount = getSupportPrefs().getInt("COUNT_$id", -1)
        val serverCount = report.supportCount ?: 0
        val supportedByMe = isReportSupportedByMe(id)
        val base = if (savedCount >= 0) maxOf(savedCount, serverCount) else serverCount
        return if (supportedByMe && base == 0) 1 else base
    }

    private fun toggleSupportForReport(report: LaporanResponse) {
        val id = report.id ?: return
        val prefs = getSupportPrefs()
        val userKey = getCurrentUserKey()
        val currentlySupported = isReportSupportedByMe(id)
        val currentCount = getEffectiveSupportCount(report)

        val newSupported = !currentlySupported
        val newCount = if (newSupported) currentCount + 1 else maxOf(0, currentCount - 1)

        prefs.edit()
            .putBoolean("SUPPORTED_${userKey}_$id", newSupported)
            .putInt("COUNT_$id", newCount)
            .apply()

        val session = getSharedPreferences("UserSession", MODE_PRIVATE)
        val token = session.getString("KEY_TOKEN", null)
        if (!token.isNullOrEmpty()) {
            ApiClient.instance.supportReport("Bearer $token", id)
                .enqueue(object : Callback<Map<String, Any>> {
                    override fun onResponse(
                        call: Call<Map<String, Any>>,
                        response: Response<Map<String, Any>>
                    ) {}
                    override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {}
                })
        }

        applyHistoryFilter()
    }

    private fun setupBottomNavigation() {
        com.example.pinpoint.admin.NavbarUtils.setupUserNavbar(this)
    }
}
