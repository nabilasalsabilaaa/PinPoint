package com.example.pinpoint.pemerintah

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.pinpoint.PhotoSliderDialog
import com.example.pinpoint.R
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.GovernmentCompleteRequest
import com.example.pinpoint.network.ReportDto
import java.io.ByteArrayOutputStream

class PemerintahDetailTaskActivity : AppCompatActivity() {

    private var reportId: String? = null
    private var currentReport: ReportDto? = null
    private var isReportCompleted: Boolean = false

    private lateinit var tvDetailId: TextView
    private lateinit var tvDetailStatusBadge: TextView
    private lateinit var tvDetailReportTitle: TextView
    private lateinit var tvDetailDate: TextView
    private lateinit var tvDetailUrgency: TextView
    private lateinit var tvReporterName: TextView
    private lateinit var tvReporterPhone: TextView
    private lateinit var tvDetailCategory: TextView
    private lateinit var tvDetailLocationTitle: TextView
    private lateinit var tvDetailLocationSub: TextView
    private lateinit var tvDetailGps: TextView
    private lateinit var tvCitizenDescription: TextView
    private lateinit var tvInitialPhotoTimestamp: TextView
    private lateinit var tvBeforePhotoCount: TextView
    private lateinit var tvAfterPhotoCountBadge: TextView
    private lateinit var tvAfterPhotoOverlayHint: TextView

    private lateinit var ivConditionBefore: ImageView
    private lateinit var ivConditionAfter: ImageView
    private lateinit var layoutConditionBefore: View
    private lateinit var btnUploadPhoto: View
    private lateinit var btnAddMoreEvidencePhoto: TextView
    private lateinit var btnPreviewEvidencePhotos: TextView
    private lateinit var etWorkNotes: EditText
    private lateinit var switchCloseTicket: SwitchCompat
    private lateinit var btnMarkCompleted: TextView
    private lateinit var btnSaveDraft: TextView
    private lateinit var bottomActionContainer: View
    private lateinit var ivCameraBadgeOverlay: View
    private lateinit var layoutSwitchCloseTicket: View
    private lateinit var tvEvidenceSectionSubtitle: TextView

    private val citizenPhotoUrls = mutableListOf<String>()
    private val evidenceDisplayUrls = mutableListOf<String>()
    private val evidenceUploadPayloads = mutableListOf<String>()

    private val pickMultipleImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            addSelectedEvidenceUris(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pemerintah_detail_task)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        reportId = intent.getStringExtra("REPORT_ID")?.replace("#LP-MKS-0", "")?.trim()
        initViews()
        setupListeners()

        val idToLoad = intent.getStringExtra("REPORT_ID")
        if (!idToLoad.isNullOrBlank()) {
            loadReportDetailFromDatabase(idToLoad)
        } else {
            Toast.makeText(this, "ID Laporan tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initViews() {
        tvDetailId = findViewById(R.id.tvDetailId)
        tvDetailStatusBadge = findViewById(R.id.tvDetailStatusBadge)
        tvDetailReportTitle = findViewById(R.id.tvDetailReportTitle)
        tvDetailDate = findViewById(R.id.tvDetailDate)
        tvDetailUrgency = findViewById(R.id.tvDetailUrgency)
        tvReporterName = findViewById(R.id.tvReporterName)
        tvReporterPhone = findViewById(R.id.tvReporterPhone)
        tvDetailCategory = findViewById(R.id.tvDetailCategory)
        tvDetailLocationTitle = findViewById(R.id.tvDetailLocationTitle)
        tvDetailLocationSub = findViewById(R.id.tvDetailLocationSub)
        tvDetailGps = findViewById(R.id.tvDetailGps)
        tvCitizenDescription = findViewById(R.id.tvCitizenDescription)
        tvInitialPhotoTimestamp = findViewById(R.id.tvInitialPhotoTimestamp)
        tvBeforePhotoCount = findViewById(R.id.tvBeforePhotoCount)
        tvAfterPhotoCountBadge = findViewById(R.id.tvAfterPhotoCountBadge)
        tvAfterPhotoOverlayHint = findViewById(R.id.tvAfterPhotoOverlayHint)

        ivConditionBefore = findViewById(R.id.ivConditionBefore)
        ivConditionAfter = findViewById(R.id.ivConditionAfter)
        layoutConditionBefore = findViewById(R.id.layoutConditionBefore)
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto)
        btnAddMoreEvidencePhoto = findViewById(R.id.btnAddMoreEvidencePhoto)
        btnPreviewEvidencePhotos = findViewById(R.id.btnPreviewEvidencePhotos)
        etWorkNotes = findViewById(R.id.etWorkNotes)
        switchCloseTicket = findViewById(R.id.switchCloseTicket)
        btnMarkCompleted = findViewById(R.id.btnMarkCompleted)
        btnSaveDraft = findViewById(R.id.btnSaveDraft)
        bottomActionContainer = findViewById(R.id.bottomActionContainer)
        ivCameraBadgeOverlay = findViewById(R.id.ivCameraBadgeOverlay)
        layoutSwitchCloseTicket = findViewById(R.id.layoutSwitchCloseTicket)
        tvEvidenceSectionSubtitle = findViewById(R.id.tvEvidenceSectionSubtitle)
    }

    private fun setupListeners() {
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnOpenNavMap).setOnClickListener {
            val report = currentReport
            if (report == null) {
                Toast.makeText(this, "Data laporan belum tersedia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val lat = report.latitude
            val lng = report.longitude
            if (lat == null || lng == null ||
                !lat.isFinite() || !lng.isFinite() ||
                lat !in -90.0..90.0 || lng !in -180.0..180.0 ||
                (lat == 0.0 && lng == 0.0)
            ) {
                Toast.makeText(this, "Koordinat GPS laporan tidak tersedia", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Buka halaman peta Pemerintah dan minta halaman tersebut langsung membuat rute
            // menuju laporan yang sedang dibuka ini.
            val navigationIntent = Intent(this, PemerintahMapActivity::class.java).apply {
                putExtra(
                    PemerintahMapActivity.EXTRA_OPEN_ROUTE_REPORT_ID,
                    report.resolvedId().toString()
                )
            }
            startActivity(navigationIntent)
        }

        layoutConditionBefore.setOnClickListener {
            if (citizenPhotoUrls.isNotEmpty()) {
                PhotoSliderDialog.show(
                    this,
                    "Foto Kondisi Awal (${citizenPhotoUrls.size} Foto)",
                    citizenPhotoUrls,
                    0
                )
            } else {
                Toast.makeText(this, "Tidak ada foto kondisi awal tambahan", Toast.LENGTH_SHORT).show()
            }
        }

        btnUploadPhoto.setOnClickListener {
            if (evidenceDisplayUrls.isNotEmpty()) {
                PhotoSliderDialog.show(
                    this,
                    "Foto Bukti Perbaikan (${evidenceDisplayUrls.size} Foto)",
                    evidenceDisplayUrls,
                    0
                )
            } else if (!isReportCompleted) {
                pickMultipleImagesLauncher.launch("image/*")
            } else {
                Toast.makeText(this, "Tidak ada foto bukti perbaikan untuk ditampilkan", Toast.LENGTH_SHORT).show()
            }
        }

        btnAddMoreEvidencePhoto.setOnClickListener {
            if (!isReportCompleted) {
                pickMultipleImagesLauncher.launch("image/*")
            }
        }

        btnPreviewEvidencePhotos.setOnClickListener {
            if (evidenceDisplayUrls.isNotEmpty()) {
                PhotoSliderDialog.show(
                    this,
                    "Foto Bukti Perbaikan (${evidenceDisplayUrls.size} Foto)",
                    evidenceDisplayUrls,
                    0
                )
            } else {
                Toast.makeText(this, "Silakan pilih foto bukti perbaikan terlebih dahulu", Toast.LENGTH_SHORT).show()
            }
        }

        val scrollViewGovDetail = findViewById<android.widget.ScrollView>(R.id.scrollViewGovDetail)
        val scrollToNotes = {
            scrollViewGovDetail?.postDelayed({
                scrollViewGovDetail.smoothScrollTo(0, etWorkNotes.bottom + 240)
            }, 220)
        }
        etWorkNotes.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) scrollToNotes()
        }
        etWorkNotes.setOnClickListener { scrollToNotes() }

        btnSaveDraft.setOnClickListener {
            submitProcessStatus()
        }

        btnMarkCompleted.setOnClickListener {
            submitCompletionWithEvidence()
        }
    }

    private fun loadReportDetailFromDatabase(rawId: String) {
        val numericId = rawId.filter { it.isDigit() }.takeLast(4).toLongOrNull()?.toString() ?: rawId
        reportId = numericId

        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val rawToken = sharedPreferences.getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

        ApiClient.fetchWithFallback(
            callFactory = { api -> api.getReportDetail(token, numericId) },
            onResponse = { _, response ->
                if (response.isSuccessful && response.body() != null) {
                    bindReportToUi(response.body()!!)
                } else {
                    Toast.makeText(this@PemerintahDetailTaskActivity, "Gagal memuat detail laporan", Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { _, _ ->
                Toast.makeText(this@PemerintahDetailTaskActivity, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun bindReportToUi(report: ReportDto) {
        currentReport = report
        reportId = report.resolvedId().toString()

        tvDetailId.text = report.resolvedCode()
        tvDetailReportTitle.text = report.resolvedTitle()
        tvDetailDate.text = report.resolvedCreatedAt()
        tvDetailUrgency.text = report.resolvedUrgency()
        tvReporterName.text = report.resolvedReporterName()
        tvReporterPhone.text = report.resolvedReporterPhone()
        tvDetailCategory.text = report.resolvedCategoryName()
        tvDetailLocationTitle.text = report.resolvedLocationName()
        tvDetailLocationSub.text = "Kategori: ${report.resolvedCategoryName()}"

        val lat = report.latitude ?: -5.1476
        val lng = report.longitude ?: 119.4327
        tvDetailGps.text = String.format("GPS: %.5f, %.5f", lat, lng)

        tvCitizenDescription.text = report.resolvedDescription()
        tvInitialPhotoTimestamp.text = report.resolvedCreatedAt()

        when (report.resolvedStatusKey()) {
            "DIPROSES" -> {
                tvDetailStatusBadge.text = "Sedang Diproses"
                tvDetailStatusBadge.background = ContextCompat.getDrawable(this, R.drawable.bg_badge_purple_light)
                tvDetailStatusBadge.setTextColor(Color.parseColor("#613EEA"))
            }
            "SELESAI" -> {
                tvDetailStatusBadge.text = "Selesai"
                tvDetailStatusBadge.background = ContextCompat.getDrawable(this, R.drawable.bg_badge_green_light)
                tvDetailStatusBadge.setTextColor(Color.parseColor("#00A389"))
            }
            else -> {
                tvDetailStatusBadge.text = "Terverifikasi"
                tvDetailStatusBadge.background = ContextCompat.getDrawable(this, R.drawable.bg_badge_blue_light)
                tvDetailStatusBadge.setTextColor(Color.parseColor("#0B3C8C"))
            }
        }

        // Foto kondisi awal warga
        citizenPhotoUrls.clear()
        citizenPhotoUrls.addAll(report.resolvedPhotoUrls())
        if (citizenPhotoUrls.isNotEmpty()) {
            tvBeforePhotoCount.text = "${citizenPhotoUrls.size} Foto - Ketuk untuk geser"
            Glide.with(this)
                .load(citizenPhotoUrls[0])
                .placeholder(R.drawable.sampul)
                .error(R.drawable.sampul)
                .into(ivConditionBefore)
        } else {
            tvBeforePhotoCount.text = "1 Foto Default"
            ivConditionBefore.setImageResource(R.drawable.sampul)
        }

        // Jika sudah ada catatan / bukti pengerjaan sebelumnya dari database
        val existingNotes = report.resolvedEvidenceNotes()
        if (existingNotes.isNotBlank() && etWorkNotes.text.isNullOrBlank()) {
            etWorkNotes.setText(existingNotes)
        } else if (report.resolvedStatusKey() == "SELESAI" && etWorkNotes.text.isNullOrBlank()) {
            etWorkNotes.setText("Perbaikan fasilitas umum telah diselesaikan secara resmi oleh pihak pemerintah.")
        }

        val existingEvPhotos = report.resolvedEvidencePhotoUrls()
        if (existingEvPhotos.isNotEmpty() && evidenceDisplayUrls.isEmpty()) {
            evidenceDisplayUrls.addAll(existingEvPhotos)
            evidenceUploadPayloads.addAll(existingEvPhotos)
            refreshEvidencePhotosUi()
        } else if (report.resolvedStatusKey() == "SELESAI" && evidenceDisplayUrls.isEmpty()) {
            tvAfterPhotoCountBadge.text = "Tuntas Diperbaiki"
            tvAfterPhotoOverlayHint.text = "Bukti perbaikan telah tercatat pada laporan selesai"
            ivConditionAfter.setImageResource(R.drawable.sampul)
        }

        // Jika laporan sudah ditandai selesai, kunci form & hilangkan tombol aksi agar tidak bisa diedit kembali
        isReportCompleted = report.resolvedStatusKey() == "SELESAI"
        applyCompletedReadOnlyState(isReportCompleted)
    }

    private fun applyCompletedReadOnlyState(completed: Boolean) {
        if (completed) {
            bottomActionContainer.visibility = View.GONE
            btnAddMoreEvidencePhoto.visibility = View.GONE
            ivCameraBadgeOverlay.visibility = View.GONE
            layoutSwitchCloseTicket.visibility = View.GONE
            tvEvidenceSectionSubtitle.text = "Laporan telah diselesaikan secara resmi dan tidak dapat diubah lagi"

            etWorkNotes.isEnabled = false
            etWorkNotes.isFocusable = false
            etWorkNotes.isFocusableInTouchMode = false
            etWorkNotes.isCursorVisible = false
            etWorkNotes.keyListener = null

            if (evidenceDisplayUrls.isNotEmpty()) {
                tvAfterPhotoCountBadge.text = "${evidenceDisplayUrls.size} Foto Bukti"
                tvAfterPhotoOverlayHint.text = "Ketuk untuk melihat & menggeser foto bukti perbaikan"
            }
        } else {
            bottomActionContainer.visibility = View.VISIBLE
            btnAddMoreEvidencePhoto.visibility = View.VISIBLE
            ivCameraBadgeOverlay.visibility = View.VISIBLE
            layoutSwitchCloseTicket.visibility = View.VISIBLE
            tvEvidenceSectionSubtitle.text = "Unggah foto bukti pengerjaan (bisa lebih dari 1 foto) & keterangan"
        }
    }

    private fun addSelectedEvidenceUris(uris: List<Uri>) {
        var addedCount = 0
        for (uri in uris) {
            val uriStr = uri.toString()
            if (!evidenceDisplayUrls.contains(uriStr)) {
                val encoded = encodeUriToBase64Data(uri)
                if (encoded != null) {
                    evidenceDisplayUrls.add(uriStr)
                    evidenceUploadPayloads.add(encoded)
                    addedCount++
                }
            }
        }
        if (addedCount > 0) {
            refreshEvidencePhotosUi()
            Toast.makeText(
                this,
                "$addedCount foto bukti ditambahkan (Total: ${evidenceDisplayUrls.size} foto)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun refreshEvidencePhotosUi() {
        val count = evidenceDisplayUrls.size
        if (count > 0) {
            tvAfterPhotoCountBadge.text = if (isReportCompleted) "$count Foto Bukti" else "$count Foto Terpilih"
            tvAfterPhotoOverlayHint.text = if (isReportCompleted) {
                "Ketuk untuk melihat & menggeser foto bukti perbaikan"
            } else {
                "$count Foto Bukti Siap Diunggah - Ketuk untuk melihat & menggeser foto"
            }
            btnPreviewEvidencePhotos.text = "Lihat & Geser Foto ($count)"
            Glide.with(this)
                .load(evidenceDisplayUrls.last())
                .placeholder(R.drawable.sampul)
                .error(R.drawable.sampul)
                .into(ivConditionAfter)
        } else {
            tvAfterPhotoCountBadge.text = "Belum ada foto dipilih"
            tvAfterPhotoOverlayHint.text = "Ketuk untuk memilih foto bukti perbaikan (bisa pilih lebih dari 1 foto)"
            btnPreviewEvidencePhotos.text = "Lihat & Geser Foto (0)"
            ivConditionAfter.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    private fun encodeUriToBase64Data(uri: Uri): String? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            var sampleSize = 1
            while (options.outWidth / sampleSize > 1024 || options.outHeight / sampleSize > 1024) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val rawBitmap = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return uri.toString()

            val uprightBitmap = applyExifOrientation(uri, rawBitmap)

            val baos = ByteArrayOutputStream()
            uprightBitmap.compress(Bitmap.CompressFormat.JPEG, 82, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            uri.toString()
        }
    }

    private fun applyExifOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val exif = contentResolver.openInputStream(uri)?.use { input ->
                android.media.ExifInterface(input)
            } ?: return bitmap

            val orientation = exif.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = android.graphics.Matrix()
            when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                android.media.ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.preScale(-1f, 1f)
                }
                android.media.ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.preScale(-1f, 1f)
                }
                else -> return bitmap
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun submitProcessStatus() {
        val id = reportId ?: return
        val notes = etWorkNotes.text.toString().trim()
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val rawToken = sharedPreferences.getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

        btnSaveDraft.isEnabled = false
        btnSaveDraft.text = "Memproses..."

        val req = GovernmentCompleteRequest(
            keterangan = notes,
            photoUrls = evidenceUploadPayloads.toList(),
            sendNotification = true
        )

        ApiClient.fetchWithFallback(
            callFactory = { api -> api.processGovernmentReport(token, id, req) },
            onResponse = { _, response ->
                btnSaveDraft.isEnabled = true
                btnSaveDraft.text = "Tandai Sedang Diproses"
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@PemerintahDetailTaskActivity,
                        "Status laporan diperbarui menjadi Sedang Diproses dan notifikasi dikirim ke pelapor.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@PemerintahDetailTaskActivity,
                        "Gagal memperbarui status laporan (${response.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onFailure = { _, _ ->
                btnSaveDraft.isEnabled = true
                btnSaveDraft.text = "Tandai Sedang Diproses"
                Toast.makeText(this@PemerintahDetailTaskActivity, "Koneksi ke server gagal", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun submitCompletionWithEvidence() {
        val id = reportId ?: return
        val notes = etWorkNotes.text.toString().trim()

        if (evidenceUploadPayloads.isEmpty()) {
            Toast.makeText(this, "Harap unggah minimal 1 foto bukti pengerjaan terlebih dahulu.", Toast.LENGTH_SHORT).show()
            return
        }

        if (notes.isBlank()) {
            etWorkNotes.error = "Harap isi keterangan hasil pengerjaan"
            Toast.makeText(this, "Harap isi keterangan hasil pengerjaan terlebih dahulu.", Toast.LENGTH_SHORT).show()
            return
        }

        val sendNotif = switchCloseTicket.isChecked
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val rawToken = sharedPreferences.getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

        btnMarkCompleted.isEnabled = false
        btnMarkCompleted.text = "Menyimpan Bukti & Menyelesaikan..."

        val req = GovernmentCompleteRequest(
            keterangan = notes,
            photoUrls = evidenceUploadPayloads.toList(),
            sendNotification = sendNotif
        )

        ApiClient.fetchWithFallback(
            callFactory = { api -> api.completeGovernmentReport(token, id, req) },
            onResponse = { _, response ->
                btnMarkCompleted.isEnabled = true
                btnMarkCompleted.text = "Tandai Selesai & Kirim Bukti"
                if (response.isSuccessful) {
                    val code = currentReport?.resolvedCode() ?: "#$id"
                    val notifInfo = if (sendNotif) "Notifikasi telah dikirim ke pelapor." else "Tanpa notifikasi."
                    Toast.makeText(
                        this@PemerintahDetailTaskActivity,
                        "Laporan $code berhasil diselesaikan! $notifInfo",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@PemerintahDetailTaskActivity,
                        "Gagal menyelesaikan laporan (${response.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onFailure = { _, _ ->
                btnMarkCompleted.isEnabled = true
                btnMarkCompleted.text = "Tandai Selesai & Kirim Bukti"
                Toast.makeText(this@PemerintahDetailTaskActivity, "Koneksi ke server gagal", Toast.LENGTH_SHORT).show()
            }
        )
    }
}