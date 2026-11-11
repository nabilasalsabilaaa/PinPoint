package com.example.pinpoint

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PemerintahHistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var historyAdapter: PemerintahHistoryAdapter
    private lateinit var etSearchHistory: EditText
    private val allHistoryList = mutableListOf<PemerintahHistoryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pemerintah_history)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Inisialisasi Views
        rvHistory = findViewById(R.id.rvPemerintahHistory)
        etSearchHistory = findViewById(R.id.etSearchHistory)
        val btnDownloadRecap = findViewById<View>(R.id.btnDownloadRecap)

        // 2. Buat Dummy Data Sesuai Desain Figma Riwayat
        setupDummyData()

        // 3. Setup RecyclerView & Adapter
        rvHistory.layoutManager = LinearLayoutManager(this)
        historyAdapter = PemerintahHistoryAdapter(allHistoryList) { item ->
            Toast.makeText(this, "Membuka Detail Dokumentasi: ${item.id}", Toast.LENGTH_SHORT).show()
        }
        rvHistory.adapter = historyAdapter

        // 4. Setup Pencarian (Search)
        setupSearch()

        // 5. Setup Tombol Unduh Rekap
        btnDownloadRecap.setOnClickListener {
            Toast.makeText(this, "Mengunduh Rekap Laporan Resmi (PDF/Excel) - TTD Digital Pemkot...", Toast.LENGTH_LONG).show()
        }

        // 6. Setup Bottom Navigation
        setupBottomNavigation()
    }

    private fun setupDummyData() {
        allHistoryList.clear()

        allHistoryList.add(
            PemerintahHistoryItem(
                id = "#LP-MKS-0840",
                finishDate = "Selesai 14 Feb 2025",
                status = "ARSIP_DITUTUP",
                title = "Pengaspalan Ruas Amblas Pettarani",
                location = "Jl. A.P. Pettarani, Rappocini (Depan Telkom)",
                sectionHeader = "📄 Komparasi Hasil Pengerjaan (BAP-0840)",
                sectionSubHeader = "Ketuk untuk perbesar",
                beforeImageUrl = "https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?w=300",
                beforeLabel = "SEBELUM",
                afterImageUrl = "https://images.unsplash.com/photo-1584467735813-9942d367425f?w=300",
                afterLabel = "SESUDAH",
                workNotes = "💬 Catatan Pengerjaan: Pengaspalan hotmix 4m tuntas oleh Regu Bina Marga. Volume aspal 3.2 ton, drainase sisi barat dinormalisasi.",
                teamName = "Regu 03 Bina Marga"
            )
        )

        allHistoryList.add(
            PemerintahHistoryItem(
                id = "#LP-MKS-0812",
                finishDate = "Selesai 12 Feb 2025",
                status = "SELESAI",
                title = "Penggantian Tiang & LED PJU Roboh",
                location = "Jl. Urip Sumoharjo, Panakkukang (Flyover Km 4)",
                sectionHeader = "📄 Dokumentasi Tiang PJU & Iluminasi Malam",
                sectionSubHeader = "⏱️ 4 Jam Pengerjaan",
                beforeImageUrl = "https://images.unsplash.com/photo-1509114397022-ed747cca3f65?w=300",
                beforeLabel = "KERUSAKAN",
                afterImageUrl = "https://images.unsplash.com/photo-1513836279014-a89f7a76ae86?w=300",
                afterLabel = "TERPASANG",
                workNotes = "⚡ Catatan Pengerjaan: Penggantian tiang oktagonal 9m dan modul luminer LED Smart Streetlight 120W. Tegangan listrik stabil 220V.",
                teamName = "Regu Elektrikal PJU"
            )
        )

        allHistoryList.add(
            PemerintahHistoryItem(
                id = "#LP-MKS-0790",
                finishDate = "Selesai 10 Feb 2025",
                status = "SELESAI",
                title = "Pemberishan Sedimen Saluran Primer",
                location = "Jl. Letjen Hertasning, Rappocini (Kanal Barat)",
                sectionHeader = "🖼️ Foto Bukti Pengerjaan Drainase (3 Dokumentasi)",
                sectionSubHeader = "BAP-0790",
                beforeImageUrl = "https://images.unsplash.com/photo-1541888946425-d0fbb186a5b3?w=300",
                beforeLabel = "PENGERUKAN",
                afterImageUrl = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=300",
                afterLabel = "ALIRAN BERSIH",
                workNotes = "💧 Catatan Pengerjaan: Pengangkatan 18 m³ sedimen lumpur dan pemotongan akar liar penyumbat gorong-gorong. Arus air kini mengalir lancar menuju kanal utama.",
                teamName = "Satgas Drainase Wilayah III"
            )
        )
    }

    private fun setupSearch() {
        etSearchHistory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase().trim()
                val filtered = allHistoryList.filter {
                    it.title.lowercase().contains(query) ||
                            it.id.lowercase().contains(query) ||
                            it.location.lowercase().contains(query) ||
                            it.teamName.lowercase().contains(query)
                }
                historyAdapter.updateList(filtered)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupBottomNavigation() {
        val navHome = findViewById<View>(R.id.navPemerintahHome)
        val navMap = findViewById<View>(R.id.navPemerintahMap)
        val navHistory = findViewById<View>(R.id.navPemerintahHistory)
        val navProfile = findViewById<View>(R.id.navPemerintahProfile)

        // Highlight History in bottom nav
        val ivHistory = findViewById<ImageView>(R.id.ivNavPemerintahHistory)
        val tvHistory = findViewById<TextView>(R.id.tvNavPemerintahHistory)
        ivHistory?.setColorFilter(android.graphics.Color.parseColor("#003C90"))
        tvHistory?.setTextColor(android.graphics.Color.parseColor("#003C90"))
        tvHistory?.setTypeface(null, android.graphics.Typeface.BOLD)

        navHome?.setOnClickListener {
            startActivity(Intent(this, PemerintahDashboardActivity::class.java))
            finish()
        }

        navMap?.setOnClickListener {
            startActivity(Intent(this, PemerintahMapActivity::class.java))
            finish()
        }

        navHistory?.setOnClickListener {
            // Already here
        }

        navProfile?.setOnClickListener {
            startActivity(Intent(this, PemerintahProfileActivity::class.java))
            finish()
        }
    }
}
