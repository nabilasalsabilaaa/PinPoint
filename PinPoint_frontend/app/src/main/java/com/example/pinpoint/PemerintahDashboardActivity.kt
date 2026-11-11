package com.example.pinpoint

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PemerintahDashboardActivity : AppCompatActivity() {

    private lateinit var rvReports: RecyclerView
    private lateinit var reportAdapter: PemerintahReportAdapter

    private lateinit var chipFilterAll: TextView
    private lateinit var chipFilterVerified: TextView
    private lateinit var chipFilterInProgress: TextView

    private val allReportsList = mutableListOf<PemerintahReport>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pemerintah_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Inisialisasi Views
        rvReports = findViewById(R.id.rvPemerintahReports)
        chipFilterAll = findViewById(R.id.chipFilterAll)
        chipFilterVerified = findViewById(R.id.chipFilterVerified)
        chipFilterInProgress = findViewById(R.id.chipFilterInProgress)

        val btnCallTeam = findViewById<View>(R.id.btnCallTeam)

        // 2. Buat Dummy Data Sesuai Desain Figma
        setupDummyData()

        // 3. Setup RecyclerView & Adapter
        rvReports.layoutManager = LinearLayoutManager(this)
        reportAdapter = PemerintahReportAdapter(allReportsList) { report ->
            val intent = Intent(this, PemerintahDetailTaskActivity::class.java)
            intent.putExtra("REPORT_ID", report.id)
            startActivity(intent)
        }
        rvReports.adapter = reportAdapter

        // 4. Setup Filter Chips
        setupFilterChips()

        // 5. Setup Action Banner "Panggil Tim"
        btnCallTeam.setOnClickListener {
            Toast.makeText(this, "Menghubungi Tim Siaga Respon Cepat Posko Pettarani...", Toast.LENGTH_LONG).show()
        }

        // 6. Setup Bottom Navigation Clicks
        setupBottomNavigation()
    }

    private fun setupDummyData() {
        allReportsList.clear()
        allReportsList.add(
            PemerintahReport(
                id = "#LP-MKS-0840",
                date = "14 Feb 2025",
                category = "JALAN & JEMBATAN",
                title = "Lubang jalan sedalam 12cm di lajur cepat membahayakan pengendara motor",
                location = "Jl. A.P. Pettarani (Depan Menara Bosowa), Rappocini",
                imageUrl = "https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?w=300",
                status = "TERVERIFIKASI",
                footerTag = "⚠️ Urgensi Tinggi",
                actionText = "Detail & Tindak Lanjut ➔",
                isPrimaryButton = true
            )
        )

        allReportsList.add(
            PemerintahReport(
                id = "#LP-MKS-0838",
                date = "13 Feb 2025",
                category = "PENERANGAN JALAN",
                title = "Tiang lampu miring 30 derajat rawan roboh pasca tertabrak",
                location = "Jl. Perintis Kemerdekaan KM 10, Tamalanrea, Makassar",
                imageUrl = "https://images.unsplash.com/photo-1509114397022-ed747cca3f65?w=300",
                status = "SEDANG_DIPROSES",
                footerTag = "👥 Regu B-Tamalanrea",
                actionText = "Detail & Tindak Lanjut ➔",
                isPrimaryButton = false
            )
        )

        allReportsList.add(
            PemerintahReport(
                id = "#LP-MKS-0835",
                date = "13 Feb 2025",
                category = "SALURAN & DRAINASE",
                title = "Sedimen lumpur tebal menyumbat gorong-gorong pembuangan",
                location = "Jl. Hertasning Baru, Panakkukang, Makassar",
                imageUrl = "https://images.unsplash.com/photo-1541888946425-d0fbb186a5b3?w=300",
                status = "TERVERIFIKASI",
                footerTag = "⏰ Jadwal Esok Hari",
                actionText = "Detail & Tindak Lanjut ➔",
                isPrimaryButton = false
            )
        )
    }

    private fun setupFilterChips() {
        chipFilterAll.setOnClickListener {
            selectChip(chipFilterAll)
            unselectChip(chipFilterVerified)
            unselectChip(chipFilterInProgress)
            reportAdapter.updateList(allReportsList)
        }

        chipFilterVerified.setOnClickListener {
            unselectChip(chipFilterAll)
            selectChip(chipFilterVerified)
            unselectChip(chipFilterInProgress)
            val filtered = allReportsList.filter { it.status.uppercase() == "TERVERIFIKASI" }
            reportAdapter.updateList(filtered)
        }

        chipFilterInProgress.setOnClickListener {
            unselectChip(chipFilterAll)
            unselectChip(chipFilterVerified)
            selectChip(chipFilterInProgress)
            val filtered = allReportsList.filter { it.status.uppercase() == "SEDANG_DIPROSES" }
            reportAdapter.updateList(filtered)
        }
    }

    private fun selectChip(chip: TextView) {
        chip.background = ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
        chip.setTextColor(ContextCompat.getColor(this, android.R.color.white))
    }

    private fun unselectChip(chip: TextView) {
        chip.background = ContextCompat.getDrawable(this, R.drawable.bg_chip_unselected)
        chip.setTextColor(android.graphics.Color.parseColor("#434653"))
    }

    private fun setupBottomNavigation() {
        val navHome = findViewById<View>(R.id.navPemerintahHome)
        val navMap = findViewById<View>(R.id.navPemerintahMap)
        val navHistory = findViewById<View>(R.id.navPemerintahHistory)
        val navProfile = findViewById<View>(R.id.navPemerintahProfile)

        val ivHome = findViewById<ImageView>(R.id.ivNavPemerintahHome)
        val tvHome = findViewById<TextView>(R.id.tvNavPemerintahHome)
        ivHome?.setColorFilter(android.graphics.Color.parseColor("#003C90"))
        tvHome?.setTextColor(android.graphics.Color.parseColor("#003C90"))
        tvHome?.setTypeface(null, android.graphics.Typeface.BOLD)

        navHome?.setOnClickListener {
            Toast.makeText(this, "Anda berada di Beranda Pemerintah", Toast.LENGTH_SHORT).show()
        }

        navMap?.setOnClickListener {
            val intent = Intent(this, PemerintahMapActivity::class.java)
            startActivity(intent)
            finish()
        }

        navHistory?.setOnClickListener {
            val intent = Intent(this, PemerintahHistoryActivity::class.java)
            startActivity(intent)
            finish()
        }

        navProfile?.setOnClickListener {
            val intent = Intent(this, PemerintahProfileActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
