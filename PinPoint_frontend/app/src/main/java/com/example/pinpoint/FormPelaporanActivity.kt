package com.example.pinpoint

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FormPelaporanActivity : AppCompatActivity() {

    private var selectedCategory: String = "Jalan Rusak"
    private var selectedUrgency: String = "Kritis / Darurat"

    private lateinit var ivSelectedPhotoPreview: ImageView
    private lateinit var tvCharCounter: TextView
    private lateinit var etDescription: EditText

    // Launcher untuk buka galeri
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            ivSelectedPhotoPreview.visibility = View.VISIBLE
            ivSelectedPhotoPreview.setImageURI(uri)
            Toast.makeText(this, "Foto berhasil dipilih dari galeri", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher untuk kamera (ambil thumbnail gambar)
    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            ivSelectedPhotoPreview.visibility = View.VISIBLE
            ivSelectedPhotoPreview.setImageBitmap(bitmap)
            Toast.makeText(this, "Foto berhasil diambil dari kamera", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_form_pelaporan)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainForm)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Tombol Kembali
        findViewById<ImageView>(R.id.btnBackForm).setOnClickListener {
            finish()
        }

        // Inisialisasi Elemen Upload
        ivSelectedPhotoPreview = findViewById(R.id.ivSelectedPhotoPreview)
        findViewById<View>(R.id.btnTakePhoto).setOnClickListener {
            takePhotoLauncher.launch(null)
        }
        findViewById<View>(R.id.btnPickGallery).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Lokasi GPS & Pin Manual
        findViewById<View>(R.id.btnLocateMe).setOnClickListener {
            Toast.makeText(this, "Mendeteksi posisi GPS akurat...", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnAdjustPinManual).setOnClickListener {
            Toast.makeText(this, "Mode geser pin lokasi manual aktif", Toast.LENGTH_SHORT).show()
        }

        // Setup Pilihan Kategori
        setupCategorySelection()

        // Setup Tingkat Urgensi
        setupUrgencySelection()

        // Counter Karakter Deskripsi
        tvCharCounter = findViewById(R.id.tvCharCounter)
        etDescription = findViewById(R.id.etDescription)
        etDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                tvCharCounter.text = "$length/300 Karakter"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Tombol Kirim Laporan ke Database
        val btnSubmit = findViewById<Button>(R.id.btnSubmitReport)
        btnSubmit.setOnClickListener {
            val description = etDescription.text.toString().trim()
            if (description.isEmpty()) {
                Toast.makeText(this, "Harap isi uraian rinci kejadian terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
            val token = sharedPreferences.getString("KEY_TOKEN", null)

            val categoryId = when (selectedCategory) {
                "Jalan Rusak" -> 1L // Infrastruktur Jalan
                "Drainase & Banjir" -> 2L // Kebersihan Lingkungan
                "Lampu Jalan" -> 3L // Fasilitas Umum
                "Taman & Fasum" -> 3L // Fasilitas Umum
                "Jembatan Penyebrangan" -> 1L // Infrastruktur Jalan
                else -> 3L // Fasilitas Umum
            }

            if (token.isNullOrEmpty()) {
                Toast.makeText(this, "Sesi login tidak valid. Silakan login kembali.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSubmit.isEnabled = false
            btnSubmit.text = "Mengirim..."

            val request = CreateLaporanRequest(
                idKategori = categoryId,
                judul = "Laporan $selectedCategory",
                deskripsi = description,
                alamat = "Makassar, Sulawesi Selatan",
                latitude = -5.147665,
                longitude = 119.432732,
                tingkatKeparahan = selectedUrgency
            )

            ApiClient.instance.createReport("Bearer $token", request).enqueue(object : Callback<LaporanResponse> {
                override fun onResponse(call: Call<LaporanResponse>, response: Response<LaporanResponse>) {
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Kirim Laporan Pengaduan"
                    if (response.isSuccessful) {
                        val result = response.body()
                        Toast.makeText(this@FormPelaporanActivity, "Laporan #${result?.id ?: ""} berhasil dikirim ke database!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Terjadi kesalahan saat menyimpan laporan"
                        Toast.makeText(this@FormPelaporanActivity, "Gagal mengirim: $errorBody", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LaporanResponse>, t: Throwable) {
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Kirim Laporan Pengaduan"
                    Toast.makeText(this@FormPelaporanActivity, "Gagal terhubung ke server: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun setupCategorySelection() {
        val catJalan = findViewById<RelativeLayout>(R.id.catJalanRusak)
        val catLampu = findViewById<RelativeLayout>(R.id.catLampuJalan)
        val catDrainase = findViewById<RelativeLayout>(R.id.catDrainase)
        val catTaman = findViewById<RelativeLayout>(R.id.catTaman)
        val catJembatan = findViewById<RelativeLayout>(R.id.catJembatan)
        val catLainnya = findViewById<RelativeLayout>(R.id.catLainnya)

        val checkCat1 = findViewById<ImageView>(R.id.checkCat1)
        val tvCatTitle1 = findViewById<TextView>(R.id.tvCatTitle1)
        val tvCatSub1 = findViewById<TextView>(R.id.tvCatSub1)

        val tvCatTitle2 = findViewById<TextView>(R.id.tvCatTitle2)
        val tvCatTitle3 = findViewById<TextView>(R.id.tvCatTitle3)
        val tvCatTitle4 = findViewById<TextView>(R.id.tvCatTitle4)
        val tvCatTitle5 = findViewById<TextView>(R.id.tvCatTitle5)
        val tvCatTitle6 = findViewById<TextView>(R.id.tvCatTitle6)

        val categories = listOf(
            Triple(catJalan, "Jalan Rusak", tvCatTitle1),
            Triple(catLampu, "Lampu Jalan", tvCatTitle2),
            Triple(catDrainase, "Drainase & Banjir", tvCatTitle3),
            Triple(catTaman, "Taman & Fasum", tvCatTitle4),
            Triple(catJembatan, "Jembatan Penyebrangan", tvCatTitle5),
            Triple(catLainnya, "Lainnya", tvCatTitle6)
        )

        fun resetAll() {
            for ((view, _, titleView) in categories) {
                view.setBackgroundResource(R.drawable.bg_category_item_unselected)
                titleView.setTextColor(Color.parseColor("#0B1C30"))
            }
            checkCat1.visibility = View.GONE
            tvCatSub1.setTextColor(Color.parseColor("#64748B"))
        }

        for ((view, name, titleView) in categories) {
            view.setOnClickListener {
                resetAll()
                selectedCategory = name
                view.setBackgroundResource(R.drawable.bg_category_item_selected)
                titleView.setTextColor(Color.WHITE)

                if (name == "Jalan Rusak") {
                    checkCat1.visibility = View.VISIBLE
                    tvCatSub1.setTextColor(Color.parseColor("#BFDBFE"))
                }
                Toast.makeText(this, "Kategori: $name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupUrgencySelection() {
        val btnLow = findViewById<TextView>(R.id.btnUrgencyLow)
        val btnMedium = findViewById<TextView>(R.id.btnUrgencyMedium)
        val btnCritical = findViewById<LinearLayout>(R.id.btnUrgencyCritical)
        val tvCriticalText = findViewById<TextView>(R.id.tvUrgencyCriticalText)

        btnLow.setOnClickListener {
            selectedUrgency = "Rendah"
            btnLow.setBackgroundResource(R.drawable.bg_btn_rounded_blue)
            btnLow.setTextColor(Color.WHITE)

            btnMedium.setBackgroundResource(0)
            btnMedium.setTextColor(Color.parseColor("#334155"))

            btnCritical.setBackgroundResource(0)
            tvCriticalText.setTextColor(Color.parseColor("#334155"))
        }

        btnMedium.setOnClickListener {
            selectedUrgency = "Sedang"
            btnMedium.setBackgroundResource(R.drawable.bg_btn_rounded_blue)
            btnMedium.setTextColor(Color.WHITE)

            btnLow.setBackgroundResource(0)
            btnLow.setTextColor(Color.parseColor("#334155"))

            btnCritical.setBackgroundResource(0)
            tvCriticalText.setTextColor(Color.parseColor("#334155"))
        }

        btnCritical.setOnClickListener {
            selectedUrgency = "Kritis / Darurat"
            btnCritical.setBackgroundResource(R.drawable.bg_urgency_critical)
            tvCriticalText.setTextColor(Color.WHITE)

            btnLow.setBackgroundResource(0)
            btnLow.setTextColor(Color.parseColor("#334155"))

            btnMedium.setBackgroundResource(0)
            btnMedium.setTextColor(Color.parseColor("#334155"))
        }
    }
}
