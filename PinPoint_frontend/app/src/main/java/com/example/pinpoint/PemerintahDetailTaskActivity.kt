package com.example.pinpoint

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

class PemerintahDetailTaskActivity : AppCompatActivity() {

    private lateinit var etWorkNotes: EditText
    private lateinit var switchCloseTicket: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pemerintah_detail_task)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Inisialisasi Views
        val btnBack = findViewById<View>(R.id.btnBack)
        val btnOpenNavMap = findViewById<View>(R.id.btnOpenNavMap)
        val btnUploadPhoto = findViewById<View>(R.id.btnUploadPhoto)
        val btnMarkCompleted = findViewById<View>(R.id.btnMarkCompleted)
        val btnSaveDraft = findViewById<View>(R.id.btnSaveDraft)
        etWorkNotes = findViewById(R.id.etWorkNotes)
        switchCloseTicket = findViewById(R.id.switchCloseTicket)

        val ivConditionBefore = findViewById<ImageView>(R.id.ivConditionBefore)
        val ivConditionAfter = findViewById<ImageView>(R.id.ivConditionAfter)

        // 2. Load Images with Glide
        Glide.with(this)
            .load("https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?w=500")
            .into(ivConditionBefore)

        Glide.with(this)
            .load("https://images.unsplash.com/photo-1584467735813-9942d367425f?w=500")
            .into(ivConditionAfter)

        // 3. Setup Click Listeners
        btnBack.setOnClickListener {
            finish()
        }

        btnOpenNavMap.setOnClickListener {
            Toast.makeText(this, "Membuka peta navigasi menuju Jl. A.P. Pettarani...", Toast.LENGTH_SHORT).show()
        }

        btnUploadPhoto.setOnClickListener {
            Toast.makeText(this, "Mengambil foto bukti perbaikan baru dengan geotag kamera...", Toast.LENGTH_SHORT).show()
        }

        btnSaveDraft.setOnClickListener {
            Toast.makeText(this, "Draft laporan perbaikan berhasil disimpan.", Toast.LENGTH_SHORT).show()
        }

        btnMarkCompleted.setOnClickListener {
            val notes = etWorkNotes.text.toString()
            val isClosed = switchCloseTicket.isChecked
            if (notes.isBlank()) {
                Toast.makeText(this, "Harap isi keterangan hasil pengerjaan terlebih dahulu.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(
                this,
                "Laporan #LP-MKS-0840 berhasil ditandai Selesai! (Status tutup resmi: $isClosed)",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }
}
