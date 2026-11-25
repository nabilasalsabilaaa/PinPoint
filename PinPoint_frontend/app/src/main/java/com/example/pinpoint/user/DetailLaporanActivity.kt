package com.example.pinpoint.user

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.pinpoint.PhotoSliderDialog
import com.example.pinpoint.R
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.BuktiPerbaikanResponse
import com.example.pinpoint.network.LaporanResponse
import java.util.Locale

class DetailLaporanActivity : AppCompatActivity() {

    private var isSupported = false
    private var supportCount = 0
    private var reportId: Long = -1L

    private var currentPhotoUrls: List<String> = emptyList()
    private var currentEvidenceUrls: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_user_detail_laporan)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainDetail)) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btnBackDetail).setOnClickListener {
            finish()
        }

        reportId = intent.getLongExtra("EXTRA_REPORT_ID", -1L)
        if (reportId > 0) {
            isSupported = isReportSupportedByMe(reportId)
            supportCount = getEffectiveSupportCount(reportId, 0)
            updateSupportUI()
        }
        val passedTitle = intent.getStringExtra("EXTRA_REPORT_TITLE")
        val passedDesc = intent.getStringExtra("EXTRA_REPORT_DESC")
        val passedLoc = intent.getStringExtra("EXTRA_REPORT_LOCATION")
        val passedStatus = intent.getLongExtra("EXTRA_REPORT_STATUS", -1L)
        val passedReporter = intent.getStringExtra("EXTRA_REPORTER_NAME")

        if (passedTitle != null) {
            applyBasicFields(
                code = if (reportId > 0) "#LP-${String.format("%04d", reportId)}" else "#LP-0001",
                title = passedTitle,
                category = "Fasilitas Umum",
                severity = "Sedang",
                desc = passedDesc,
                loc = passedLoc,
                statusId = passedStatus,
                statusName = null,
                date = null,
                lat = null,
                lng = null,
                reporter = passedReporter,
                phone = null
            )
            // Jika dari halaman sebelumnya status laporan sudah Selesai (5L), tampilkan section Bukti Perbaikan
            if (passedStatus == 5L) {
                findViewById<CardView>(R.id.cardGovEvidenceDetail).visibility = View.VISIBLE
            }
        }

        if (reportId > 0) {
            fetchReportDetail(reportId)
        }

        // Buka slider popup foto laporan
        findViewById<View>(R.id.btnFullscreenPhoto).setOnClickListener {
            if (currentPhotoUrls.isNotEmpty()) {
                PhotoSliderDialog.show(
                    this,
                    "Foto Bukti Laporan (${currentPhotoUrls.size} Foto)",
                    currentPhotoUrls,
                    0
                )
            } else {
                Toast.makeText(this, "Foto laporan menggunakan gambar pratinjau default", Toast.LENGTH_SHORT).show()
            }
        }

        setupSupportButton()
    }

    private fun fetchReportDetail(id: Long) {
        // Ambil data laporan lengkap (termasuk evidencePhotoUrls & evidenceNotes dari AdminReportService)
        ApiClient.fetchWithFallback(
            callFactory = { api -> api.getEnrichedReportById(id) },
            onResponse = { _, response ->
                if (response.isSuccessful && response.body() != null) {
                    val report = response.body()!!
                    bindFullReport(report)
                    if (report.idStatus == 5L && report.resolvedEvidencePhotoUrls().isEmpty()) {
                        fetchFallbackEvidence(id)
                    }
                } else {
                    fetchUserReportDetailFallback(id)
                }
            },
            onFailure = { _, _ ->
                fetchUserReportDetailFallback(id)
            }
        )
    }

    private fun fetchUserReportDetailFallback(id: Long) {
        ApiClient.fetchWithFallback(
            callFactory = { api -> api.getReportDetail(id) },
            onResponse = { _, response ->
                if (response.isSuccessful && response.body() != null) {
                    val report = response.body()!!
                    bindFullReport(report)
                    if (report.idStatus == 5L || report.resolvedEvidencePhotoUrls().isEmpty()) {
                        fetchFallbackEvidence(id)
                    }
                } else {
                    Toast.makeText(this, "Gagal mengambil detail laporan", Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { _, t ->
                Toast.makeText(this, "Gagal terhubung ke server: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun fetchFallbackEvidence(id: Long) {
        ApiClient.fetchWithFallback(
            callFactory = { api -> api.getReportEvidence(id) },
            onResponse = { _, response ->
                if (response.isSuccessful && response.body() != null) {
                    bindDirectEvidence(response.body()!!)
                }
            },
            onFailure = { _, _ -> }
        )
    }

    private fun bindDirectEvidence(ev: BuktiPerbaikanResponse) {
        val cardGovEvidence = findViewById<CardView>(R.id.cardGovEvidenceDetail)
        val ivGovEvidencePhoto = findViewById<ImageView>(R.id.ivGovEvidencePhoto)
        val tvGovEvidencePhotoCount = findViewById<TextView>(R.id.tvGovEvidencePhotoCount)
        val tvGovEvidenceNotes = findViewById<TextView>(R.id.tvGovEvidenceNotes)
        val layoutGovEvidencePhoto = findViewById<View>(R.id.layoutGovEvidencePhoto)

        cardGovEvidence.visibility = View.VISIBLE
        if (!ev.keterangan.isNullOrBlank()) {
            tvGovEvidenceNotes.text = ev.keterangan
        }
        val resolvedUrl = ApiClient.resolveImageUrl(ev.pathFile)
        if (!resolvedUrl.isNullOrBlank()) {
            if (currentEvidenceUrls.isEmpty()) {
                currentEvidenceUrls = listOf(resolvedUrl)
            }
            layoutGovEvidencePhoto.visibility = View.VISIBLE
            tvGovEvidencePhotoCount.text = "${currentEvidenceUrls.size} Foto Bukti • Ketuk untuk lihat"
            Glide.with(this)
                .load(currentEvidenceUrls.first())
                .placeholder(R.drawable.sampul)
                .error(R.drawable.sampul)
                .centerCrop()
                .into(ivGovEvidencePhoto)

            layoutGovEvidencePhoto.setOnClickListener {
                PhotoSliderDialog.show(
                    this,
                    "Foto Bukti Perbaikan Pemerintah (${currentEvidenceUrls.size} Foto)",
                    currentEvidenceUrls,
                    0
                )
            }
        }
    }

    private fun bindFullReport(report: LaporanResponse) {
        applyBasicFields(
            code = report.resolvedCode(),
            title = report.resolvedTitle(),
            category = report.resolvedCategoryName(),
            severity = report.tingkatKeparahan ?: "Sedang",
            desc = report.resolvedDescription(),
            loc = report.resolvedLocation(),
            statusId = report.idStatus,
            statusName = report.resolvedStatusDisplay(),
            date = report.tanggalLaporan ?: report.createdAt,
            lat = report.latitude,
            lng = report.longitude,
            reporter = report.resolvedReporterName(),
            phone = report.resolvedReporterPhone()
        )

        val rid = report.id ?: reportId
        isSupported = isReportSupportedByMe(rid)
        supportCount = getEffectiveSupportCount(rid, report.supportCount ?: 0)
        saveSupportStateLocally(rid, isSupported, supportCount)
        updateSupportUI()

        // Foto Bukti Laporan Warga
        currentPhotoUrls = report.resolvedPhotoUrls()
        val ivDetailPhoto = findViewById<ImageView>(R.id.ivDetailPhoto)
        val tvPhotoBadge = findViewById<TextView>(R.id.tvDetailPhotoCountBadge)

        if (currentPhotoUrls.isNotEmpty()) {
            tvPhotoBadge.text = if (currentPhotoUrls.size > 1) {
                "${currentPhotoUrls.size} Foto • Ketuk untuk geser"
            } else {
                "1 Foto • Ketuk untuk lihat"
            }
            Glide.with(this)
                .load(currentPhotoUrls.first())
                .placeholder(R.drawable.sampul)
                .error(R.drawable.sampul)
                .centerCrop()
                .into(ivDetailPhoto)
        } else {
            tvPhotoBadge.text = "Pratinjau Foto"
            ivDetailPhoto.setImageResource(R.drawable.sampul)
        }

        // Section Bukti Perbaikan Pemerintah (Tampil jika laporan sudah Selesai atau memiliki bukti perbaikan)
        currentEvidenceUrls = report.resolvedEvidencePhotoUrls()
        val cardGovEvidence = findViewById<CardView>(R.id.cardGovEvidenceDetail)
        val ivGovEvidencePhoto = findViewById<ImageView>(R.id.ivGovEvidencePhoto)
        val tvGovEvidencePhotoCount = findViewById<TextView>(R.id.tvGovEvidencePhotoCount)
        val tvGovEvidenceNotes = findViewById<TextView>(R.id.tvGovEvidenceNotes)
        val tvGovCompletedByBadge = findViewById<TextView>(R.id.tvGovCompletedByBadge)
        val layoutGovEvidencePhoto = findViewById<View>(R.id.layoutGovEvidencePhoto)

        val isCompleted = report.idStatus == 5L || report.resolvedStatusDisplay().equals("Selesai", ignoreCase = true)
        val hasEvidence = isCompleted || currentEvidenceUrls.isNotEmpty() || !report.evidenceNotes.isNullOrBlank()

        if (hasEvidence) {
            cardGovEvidence.visibility = View.VISIBLE
            tvGovCompletedByBadge.text = report.completedBy?.takeIf { it.isNotBlank() } ?: "Pemerintah"
            tvGovEvidenceNotes.text = report.evidenceNotes?.takeIf { it.isNotBlank() }
                ?: "Perbaikan fasilitas umum telah diselesaikan secara resmi oleh pihak pemerintah."

            if (currentEvidenceUrls.isNotEmpty()) {
                layoutGovEvidencePhoto.visibility = View.VISIBLE
                tvGovEvidencePhotoCount.text = if (currentEvidenceUrls.size > 1) {
                    "${currentEvidenceUrls.size} Foto Bukti • Ketuk untuk geser"
                } else {
                    "1 Foto Bukti • Ketuk untuk perbesar"
                }
                Glide.with(this)
                    .load(currentEvidenceUrls.first())
                    .placeholder(R.drawable.sampul)
                    .error(R.drawable.sampul)
                    .centerCrop()
                    .into(ivGovEvidencePhoto)

                layoutGovEvidencePhoto.setOnClickListener {
                    PhotoSliderDialog.show(
                        this,
                        "Foto Bukti Perbaikan Pemerintah (${currentEvidenceUrls.size} Foto)",
                        currentEvidenceUrls,
                        0
                    )
                }
            } else {
                layoutGovEvidencePhoto.visibility = View.VISIBLE
                ivGovEvidencePhoto.setImageResource(R.drawable.sampul)
                tvGovEvidencePhotoCount.text = "Bukti Penyelesaian Lapangan"
            }
        } else {
            cardGovEvidence.visibility = View.GONE
        }
    }

    private fun applyBasicFields(
        code: String,
        title: String?,
        category: String?,
        severity: String?,
        desc: String?,
        loc: String?,
        statusId: Long?,
        statusName: String?,
        date: String?,
        lat: Double?,
        lng: Double?,
        reporter: String?,
        phone: String?
    ) {
        val tvTicketNumber = findViewById<TextView>(R.id.tvTicketNumber)
        val tvDetailStatusBadge = findViewById<TextView>(R.id.tvDetailStatusBadge)
        val tvDetailCategory = findViewById<TextView>(R.id.tvDetailCategory)
        val tvDetailSeverityBadge = findViewById<TextView>(R.id.tvDetailSeverityBadge)
        val tvDetailTitle = findViewById<TextView>(R.id.tvDetailTitle)
        val tvDetailDateMeta = findViewById<TextView>(R.id.tvDetailDateMeta)
        val tvDetailLocationAddress = findViewById<TextView>(R.id.tvDetailLocationAddress)
        val tvReportDescription = findViewById<TextView>(R.id.tvReportDescription)
        val tvReporterName = findViewById<TextView>(R.id.tvReporterName)
        val tvReporterPhone = findViewById<TextView>(R.id.tvReporterPhone)
        val tvGeotagCoordinates = findViewById<TextView>(R.id.tvGeotagCoordinates)

        val formattedDate = formatShortDate(date)
        tvTicketNumber.text = if (formattedDate.isNotEmpty()) "ID $code • $formattedDate" else "ID $code"
        if (formattedDate.isNotEmpty()) {
            tvDetailDateMeta.text = "Dilaporkan pada $formattedDate"
        }

        if (!category.isNullOrBlank()) {
            tvDetailCategory.text = category.uppercase(Locale.getDefault())
        }

        if (!severity.isNullOrBlank()) {
            tvDetailSeverityBadge.text = severity
        }

        if (!title.isNullOrBlank()) {
            tvDetailTitle.text = title
        }

        if (!desc.isNullOrBlank()) {
            tvReportDescription.text = desc
        }

        if (!loc.isNullOrBlank()) {
            tvDetailLocationAddress.text = loc
        }

        if (!reporter.isNullOrBlank()) {
            tvReporterName.text = reporter
        }
        tvReporterPhone.text = phone?.takeIf { it.isNotBlank() } ?: "-"

        if (lat != null && lng != null &&
            lat != 0.0 && lng != 0.0 &&
            lat.isFinite() && lng.isFinite() &&
            lat in -90.0..90.0 && lng in -180.0..180.0
        ) {
            tvGeotagCoordinates.text = String.format(Locale.US, "GPS: %.5f, %.5f", lat, lng)
        }

        updateStatusAndTimeline(statusId, statusName, tvDetailStatusBadge)
    }

    private fun updateStatusAndTimeline(
        statusId: Long?,
        statusName: String?,
        tvDetailStatusBadge: TextView
    ) {
        val tvTimelineStageBadge = findViewById<TextView>(R.id.tvTimelineStageBadge)

        val ivStep1Icon = findViewById<ImageView>(R.id.ivStep1Icon)
        val lineStep1 = findViewById<View>(R.id.lineStep1)
        val tvStep1State = findViewById<TextView>(R.id.tvStep1State)

        val ivStep2Icon = findViewById<ImageView>(R.id.ivStep2Icon)
        val lineStep2 = findViewById<View>(R.id.lineStep2)
        val tvStep2Title = findViewById<TextView>(R.id.tvStep2Title)
        val tvStep2State = findViewById<TextView>(R.id.tvStep2State)
        val tvStep2Desc = findViewById<TextView>(R.id.tvStep2Desc)

        val ivStep3Icon = findViewById<ImageView>(R.id.ivStep3Icon)
        val tvStep3Title = findViewById<TextView>(R.id.tvStep3Title)
        val tvStep3State = findViewById<TextView>(R.id.tvStep3State)
        val tvStep3Desc = findViewById<TextView>(R.id.tvStep3Desc)

        ivStep1Icon.setImageResource(R.drawable.ic_check_circle)
        ivStep1Icon.setColorFilter(Color.parseColor("#059669"))
        tvStep1State.text = "Tercatat"
        tvStep1State.setTextColor(Color.parseColor("#059669"))

        when (statusId) {
            1L -> {
                tvDetailStatusBadge.text = "Menunggu Verifikasi"
                tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_badge_selesai)
                tvDetailStatusBadge.setTextColor(Color.parseColor("#0284C7"))

                tvTimelineStageBadge.text = "Tahap 1 dari 3"
                lineStep1.setBackgroundColor(Color.parseColor("#CBD5E1"))

                ivStep2Icon.setImageResource(R.drawable.ic_time)
                ivStep2Icon.setColorFilter(Color.parseColor("#0284C7"))
                tvStep2Title.setTextColor(Color.parseColor("#0B1C30"))
                tvStep2State.text = "Sedang Ditinjau"
                tvStep2State.setTextColor(Color.parseColor("#0284C7"))
                tvStep2Desc.text = "Laporan sedang menunggu pemeriksaan oleh administrator."

                lineStep2.setBackgroundColor(Color.parseColor("#CBD5E1"))
                ivStep3Icon.setImageResource(R.drawable.ic_time)
                ivStep3Icon.setColorFilter(Color.parseColor("#94A3B8"))
                tvStep3Title.setTextColor(Color.parseColor("#64748B"))
                tvStep3State.text = "Menunggu"
                tvStep3State.setTextColor(Color.parseColor("#94A3B8"))
                tvStep3Desc.text = "Menunggu verifikasi admin sebelum diteruskan ke pihak pemerintah."
            }
            2L -> {
                tvDetailStatusBadge.text = "Terverifikasi"
                tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_badge_terverifikasi)
                tvDetailStatusBadge.setTextColor(Color.parseColor("#0B58CA"))

                tvTimelineStageBadge.text = "Tahap 2 dari 3"
                lineStep1.setBackgroundColor(Color.parseColor("#A7F3D0"))

                ivStep2Icon.setImageResource(R.drawable.ic_check_circle)
                ivStep2Icon.setColorFilter(Color.parseColor("#059669"))
                tvStep2Title.setTextColor(Color.parseColor("#0B1C30"))
                tvStep2State.text = "Terverifikasi"
                tvStep2State.setTextColor(Color.parseColor("#059669"))
                tvStep2Desc.text = "Laporan telah diverifikasi oleh administrator dan diteruskan ke pemerintah."

                lineStep2.setBackgroundColor(Color.parseColor("#CBD5E1"))
                ivStep3Icon.setImageResource(R.drawable.ic_time)
                ivStep3Icon.setColorFilter(Color.parseColor("#0B58CA"))
                tvStep3Title.setTextColor(Color.parseColor("#0B1C30"))
                tvStep3State.text = "Antrean Tindakan"
                tvStep3State.setTextColor(Color.parseColor("#0B58CA"))
                tvStep3Desc.text = "Laporan telah masuk ke daftar kerja pihak pemerintah untuk segera ditangani."
            }
            3L -> {
                tvDetailStatusBadge.text = "Ditolak"
                tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_badge_ditolak)
                tvDetailStatusBadge.setTextColor(Color.parseColor("#B91C1C"))

                tvTimelineStageBadge.text = "Ditolak Admin"
                lineStep1.setBackgroundColor(Color.parseColor("#FECACA"))

                ivStep2Icon.setImageResource(R.drawable.ic_alert_triangle)
                ivStep2Icon.setColorFilter(Color.parseColor("#B91C1C"))
                tvStep2Title.setTextColor(Color.parseColor("#B91C1C"))
                tvStep2State.text = "Ditolak"
                tvStep2State.setTextColor(Color.parseColor("#B91C1C"))
                tvStep2Desc.text = "Laporan tidak lolos verifikasi administrator."

                lineStep2.setBackgroundColor(Color.parseColor("#CBD5E1"))
                ivStep3Icon.setImageResource(R.drawable.ic_time)
                ivStep3Icon.setColorFilter(Color.parseColor("#94A3B8"))
                tvStep3Title.setTextColor(Color.parseColor("#94A3B8"))
                tvStep3State.text = "Dihentikan"
                tvStep3State.setTextColor(Color.parseColor("#94A3B8"))
                tvStep3Desc.text = "Laporan tidak diteruskan ke pihak pemerintah."
            }
            4L -> {
                tvDetailStatusBadge.text = "Sedang Diproses"
                tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_badge_diproses_mint)
                tvDetailStatusBadge.setTextColor(Color.parseColor("#0F766E"))

                tvTimelineStageBadge.text = "Tahap 3 dari 3"
                lineStep1.setBackgroundColor(Color.parseColor("#A7F3D0"))

                ivStep2Icon.setImageResource(R.drawable.ic_check_circle)
                ivStep2Icon.setColorFilter(Color.parseColor("#059669"))
                tvStep2Title.setTextColor(Color.parseColor("#0B1C30"))
                tvStep2State.text = "Terverifikasi"
                tvStep2State.setTextColor(Color.parseColor("#059669"))
                tvStep2Desc.text = "Laporan telah diverifikasi oleh administrator."

                lineStep2.setBackgroundColor(Color.parseColor("#A7F3D0"))
                ivStep3Icon.setImageResource(R.drawable.ic_build)
                ivStep3Icon.setColorFilter(Color.parseColor("#0F766E"))
                tvStep3Title.setTextColor(Color.parseColor("#0B1C30"))
                tvStep3State.text = "Sedang Diproses"
                tvStep3State.setTextColor(Color.parseColor("#0F766E"))
                tvStep3Desc.text = "Pihak pemerintah sedang menindaklanjuti perbaikan fasilitas di lokasi laporan."
            }
            5L -> {
                tvDetailStatusBadge.text = "Selesai"
                tvDetailStatusBadge.setBackgroundResource(R.drawable.bg_badge_selesai)
                tvDetailStatusBadge.setTextColor(Color.parseColor("#059669"))

                tvTimelineStageBadge.text = "Selesai (3/3)"
                lineStep1.setBackgroundColor(Color.parseColor("#A7F3D0"))

                ivStep2Icon.setImageResource(R.drawable.ic_check_circle)
                ivStep2Icon.setColorFilter(Color.parseColor("#059669"))
                tvStep2Title.setTextColor(Color.parseColor("#0B1C30"))
                tvStep2State.text = "Terverifikasi"
                tvStep2State.setTextColor(Color.parseColor("#059669"))
                tvStep2Desc.text = "Laporan telah diverifikasi oleh administrator."

                lineStep2.setBackgroundColor(Color.parseColor("#A7F3D0"))
                ivStep3Icon.setImageResource(R.drawable.ic_check_circle)
                ivStep3Icon.setColorFilter(Color.parseColor("#059669"))
                tvStep3Title.setTextColor(Color.parseColor("#0B1C30"))
                tvStep3State.text = "Tuntas Selesai"
                tvStep3State.setTextColor(Color.parseColor("#059669"))
                tvStep3Desc.text = "Perbaikan fasilitas telah diselesaikan oleh pihak pemerintah beserta bukti pengerjaan."
            }
            else -> {
                tvDetailStatusBadge.text = statusName ?: "Menunggu Verifikasi"
            }
        }
    }

    private fun getSupportPrefs() = getSharedPreferences("UserReportSupportPrefs", MODE_PRIVATE)

    private fun getCurrentUserKey(): String {
        val session = getSharedPreferences("UserSession", MODE_PRIVATE)
        return (session.getString("KEY_EMAIL", "guest") ?: "guest").lowercase()
    }

    private fun isReportSupportedByMe(id: Long): Boolean {
        if (id <= 0L) return false
        return getSupportPrefs().getBoolean("SUPPORTED_${getCurrentUserKey()}_$id", false)
    }

    private fun getEffectiveSupportCount(id: Long, serverCount: Int): Int {
        if (id <= 0L) return serverCount
        val savedCount = getSupportPrefs().getInt("COUNT_$id", -1)
        val supportedByMe = isReportSupportedByMe(id)
        val base = if (savedCount >= 0) maxOf(savedCount, serverCount) else serverCount
        return if (supportedByMe && base == 0) 1 else base
    }

    private fun saveSupportStateLocally(id: Long, supported: Boolean, count: Int) {
        if (id <= 0L) return
        getSupportPrefs().edit()
            .putBoolean("SUPPORTED_${getCurrentUserKey()}_$id", supported)
            .putInt("COUNT_$id", count)
            .apply()
    }

    private fun setupSupportButton() {
        val btnSupport = findViewById<View>(R.id.btnDetailSupport)
        btnSupport.setOnClickListener {
            isSupported = !isSupported
            supportCount = if (isSupported) supportCount + 1 else maxOf(0, supportCount - 1)
            saveSupportStateLocally(reportId, isSupported, supportCount)
            updateSupportUI()

            val sharedPrefs = getSharedPreferences("UserSession", MODE_PRIVATE)
            val rawToken = sharedPrefs.getString("KEY_TOKEN", "") ?: ""
            if (rawToken.isEmpty() || reportId <= 0) {
                return@setOnClickListener
            }

            val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"
            ApiClient.fetchWithFallback(
                callFactory = { api -> api.supportReport(token, reportId) },
                onResponse = { _, response ->
                    if (response.isSuccessful) {
                        val body = response.body()
                        val serverCount = (body?.get("supportCount") as? Number)?.toInt()
                        if (serverCount != null && serverCount > supportCount) {
                            supportCount = serverCount
                            saveSupportStateLocally(reportId, isSupported, supportCount)
                            updateSupportUI()
                        }
                    }
                },
                onFailure = { _, _ -> }
            )
        }
    }

    private fun updateSupportUI() {
        val tvSupportCount = findViewById<TextView>(R.id.tvDetailSupportCount)
        val btnSupport = findViewById<View>(R.id.btnDetailSupport)
        val ivSupportIcon = findViewById<ImageView>(R.id.ivDetailSupportIcon)
        val tvSupportBtnText = findViewById<TextView>(R.id.tvDetailSupportBtnText)

        tvSupportCount.text = "$supportCount Dukungan Warga"
        if (isSupported) {
            btnSupport.setBackgroundResource(R.drawable.bg_btn_rounded_blue)
            ivSupportIcon.setColorFilter(Color.WHITE)
            tvSupportBtnText.text = "Didukung"
            tvSupportBtnText.setTextColor(Color.WHITE)
        } else {
            btnSupport.setBackgroundResource(R.drawable.bg_btn_rounded_blue_soft)
            ivSupportIcon.setColorFilter(Color.parseColor("#0B58CA"))
            tvSupportBtnText.text = "Dukung"
            tvSupportBtnText.setTextColor(Color.parseColor("#0B58CA"))
        }
    }

    private fun formatShortDate(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return ""
        return try {
            rawDate.replace("T", " ").take(16)
        } catch (_: Exception) {
            rawDate
        }
    }
}
