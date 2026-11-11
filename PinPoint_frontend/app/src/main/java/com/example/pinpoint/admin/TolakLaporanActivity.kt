package com.example.pinpoint.admin

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pinpoint.R
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.RejectReportRequest

class TolakLaporanActivity : AppCompatActivity() {

    private var reportId: String? = null

    private lateinit var btnBackRejectReport: ImageView
    private lateinit var tvReportIdCode: TextView
    private lateinit var tvReportStatusBadge: TextView
    private lateinit var tvReportTitle: TextView
    private lateinit var tvReporterName: TextView
    private lateinit var tvReportLocation: TextView
    private lateinit var tvSelectedReasonCount: TextView
    private lateinit var tvCharCounter: TextView
    private lateinit var etRejectDescription: EditText
    private lateinit var btnConfirmReject: Button
    private lateinit var btnCancelReject: Button

    private data class ReasonOption(
        val id: Int,
        val title: String,
        val cardView: LinearLayout,
        val checkIcon: ImageView,
        var isSelected: Boolean = false
    )

    private val reasonOptions = mutableListOf<ReasonOption>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_tolak_laporan)

        reportId = intent.getStringExtra("REPORT_ID")
        initViews()
        prefillFromIntent()

        btnBackRejectReport.setOnClickListener { finish() }
        btnCancelReject.setOnClickListener { finish() }

        if (!reportId.isNullOrBlank()) {
            loadReportSummary(reportId!!)
        }

        setupMultiSelectReasons()
        setupCharCounter()
        btnConfirmReject.setOnClickListener { submitRejection() }
    }

    private fun initViews() {
        btnBackRejectReport = findViewById(R.id.btnBackRejectReport)
        tvReportIdCode = findViewById(R.id.tvReportIdCode)
        tvReportStatusBadge = findViewById(R.id.tvReportStatusBadge)
        tvReportTitle = findViewById(R.id.tvReportTitle)
        tvReporterName = findViewById(R.id.tvReporterName)
        tvReportLocation = findViewById(R.id.tvReportLocation)
        tvSelectedReasonCount = findViewById(R.id.tvSelectedReasonCount)
        tvCharCounter = findViewById(R.id.tvCharCounter)
        etRejectDescription = findViewById(R.id.etRejectDescription)
        btnConfirmReject = findViewById(R.id.btnConfirmReject)
        btnCancelReject = findViewById(R.id.btnCancelReject)

        reasonOptions.clear()
        reasonOptions.add(
            ReasonOption(
                1,
                "Data Laporan Tidak Jelas / Foto Buram",
                findViewById(R.id.cardReason1),
                findViewById(R.id.ivCheckReason1)
            )
        )
        reasonOptions.add(
            ReasonOption(
                2,
                "Bukan Kewenangan Pemkot / Dinas PU",
                findViewById(R.id.cardReason2),
                findViewById(R.id.ivCheckReason2)
            )
        )
        reasonOptions.add(
            ReasonOption(
                3,
                "Laporan Duplikat",
                findViewById(R.id.cardReason3),
                findViewById(R.id.ivCheckReason3)
            )
        )
        reasonOptions.add(
            ReasonOption(
                4,
                "Lokasi GPS Tidak Sesuai / Fiktif",
                findViewById(R.id.cardReason4),
                findViewById(R.id.ivCheckReason4)
            )
        )
        reasonOptions.add(
            ReasonOption(
                5,
                "Lainnya",
                findViewById(R.id.cardReason5),
                findViewById(R.id.ivCheckReason5)
            )
        )
    }

    private fun prefillFromIntent() {
        intent.getStringExtra("REPORT_CODE")?.let { tvReportIdCode.text = it }
        intent.getStringExtra("REPORT_TITLE")?.let { tvReportTitle.text = it }
        intent.getStringExtra("REPORTER_NAME")?.let { tvReporterName.text = "Pelapor: $it" }
        intent.getStringExtra("REPORT_LOCATION")?.let { tvReportLocation.text = it }
    }

    private fun setupMultiSelectReasons() {
        reasonOptions.forEach { option ->
            option.cardView.setOnClickListener {
                option.isSelected = !option.isSelected
                updateReasonUI()
            }
        }
        updateReasonUI()
    }

    private fun updateReasonUI() {
        var count = 0
        reasonOptions.forEach { option ->
            if (option.isSelected) {
                count++
                option.cardView.setBackgroundResource(R.drawable.bg_card_selected_blue)
                option.checkIcon.setImageResource(R.drawable.ic_check_circle_blue)
            } else {
                option.cardView.setBackgroundResource(R.drawable.bg_card_unselected)
                option.checkIcon.setImageResource(R.drawable.ic_circle_outline)
            }
        }

        if (count == 0) {
            tvSelectedReasonCount.text = "Wajib Dipilih (Bisa > 1)"
            tvSelectedReasonCount.setTextColor(Color.parseColor("#C62828"))
        } else {
            tvSelectedReasonCount.text = "$count Alasan Dipilih"
            tvSelectedReasonCount.setTextColor(Color.parseColor("#0B3C8C"))
        }
    }

    private fun loadReportSummary(id: String) {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val rawToken = sharedPreferences.getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

        ApiClient.fetchWithFallback(
            callFactory = { api -> api.getReportDetail(token, id) },
            onResponse = { _, response ->
                if (response.isSuccessful && response.body() != null) {
                    val report = response.body()!!
                    tvReportIdCode.text = report.resolvedCode()
                    tvReportStatusBadge.text = "● Tahap Verifikasi"
                    tvReportTitle.text = "${report.resolvedCategoryName()} - ${report.resolvedTitle()}"
                    tvReporterName.text = "Pelapor: ${report.resolvedReporterName()}"
                    tvReportLocation.text = report.resolvedLocationName()
                }
            },
            onFailure = { _, _ -> }
        )
    }

    private fun setupCharCounter() {
        etRejectDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                tvCharCounter.text = "$length / 250"
                if (length >= 20) {
                    tvCharCounter.setTextColor(Color.parseColor("#1B5E20"))
                } else {
                    tvCharCounter.setTextColor(Color.parseColor("#64748B"))
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun submitRejection() {
        val selectedReasons = reasonOptions.filter { it.isSelected }.map { it.title }
        if (selectedReasons.isEmpty()) {
            Toast.makeText(this, "Wajib memilih minimal 1 alasan penolakan laporan!", Toast.LENGTH_SHORT).show()
            return
        }

        val note = etRejectDescription.text.toString().trim()
        if (note.length < 20) {
            etRejectDescription.error = "Minimal 20 karakter"
            Toast.makeText(this, "Penjelasan lengkap wajib diisi minimal 20 karakter!", Toast.LENGTH_SHORT).show()
            return
        }

        val id = reportId
        if (id.isNullOrBlank()) {
            Toast.makeText(this, "ID Laporan tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        btnConfirmReject.isEnabled = false
        btnConfirmReject.text = "Memproses Penolakan..."

        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val rawToken = sharedPreferences.getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

        val request = RejectReportRequest(reasonCategory = selectedReasons, rejectionNote = note)
        ApiClient.fetchWithFallback(
            callFactory = { api -> api.rejectReport(token, id, request) },
            onResponse = { _, response ->
                btnConfirmReject.isEnabled = true
                btnConfirmReject.text = "✕  Konfirmasi Tolak Laporan"
                if (response.isSuccessful) {
                    Toast.makeText(this@TolakLaporanActivity, "Laporan berhasil ditolak (${selectedReasons.size} alasan)", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this@TolakLaporanActivity, "Gagal menolak laporan (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { _, _ ->
                btnConfirmReject.isEnabled = true
                btnConfirmReject.text = "✕  Konfirmasi Tolak Laporan"
                Toast.makeText(this@TolakLaporanActivity, "Koneksi ke server gagal", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
