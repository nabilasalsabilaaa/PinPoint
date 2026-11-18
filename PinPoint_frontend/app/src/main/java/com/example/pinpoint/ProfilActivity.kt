package com.example.pinpoint

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfilActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profil)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainProfil)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Tampilkan Nama Pengguna & Email dari SharedPreferences
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val savedName = sharedPreferences.getString("KEY_NAME", "Pengguna")
        val savedEmail = sharedPreferences.getString("KEY_EMAIL", null)
            ?: sharedPreferences.getString("KEY_SAVED_IDENTIFIER", "-")
        val savedRole = sharedPreferences.getString("KEY_ROLE", "USER")
        val token = sharedPreferences.getString("KEY_TOKEN", null)

        findViewById<TextView>(R.id.tvProfileName).text = savedName
        findViewById<TextView>(R.id.tvProfileEmail).text = savedEmail

        // Sinkronisasi Statistik Laporan dari Database
        val tvStatTotalReport = findViewById<TextView>(R.id.tvStatTotalReport)
        val tvStatCompletedReport = findViewById<TextView>(R.id.tvStatCompletedReport)

        if (!token.isNullOrEmpty()) {
            ApiClient.instance.getMyReports("Bearer $token").enqueue(object : Callback<List<LaporanResponse>> {
                override fun onResponse(
                    call: Call<List<LaporanResponse>>,
                    response: Response<List<LaporanResponse>>
                ) {
                    if (response.isSuccessful) {
                        val reports = response.body() ?: emptyList()
                        val total = reports.size
                        val completed = reports.count { it.idStatus == 5L }
                        tvStatTotalReport.text = total.toString()
                        tvStatCompletedReport.text = completed.toString()
                    }
                }

                override fun onFailure(call: Call<List<LaporanResponse>>, t: Throwable) {
                    // Jika gagal terhubung ke backend, biarkan fallback
                }
            })
        }

        // Menu Klik
        findViewById<View>(R.id.menuBiodata).setOnClickListener {
            val dialog = AlertDialog.Builder(this)
                .setTitle("Biodata Pengguna")
                .setMessage("Nama: $savedName\nEmail: $savedEmail\nPeran: $savedRole")
                .setPositiveButton("Tutup", null)
                .create()
            dialog.show()
        }
        findViewById<View>(R.id.menuKeamanan).setOnClickListener {
            Toast.makeText(this, "Membuka Keamanan & Sandi", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.menuNotifikasiSLA).setOnClickListener {
            Toast.makeText(this, "Membuka Pengaturan Notifikasi", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.menuBantuan).setOnClickListener {
            Toast.makeText(this, "Membuka SOP Dinas & Panduan Bantuan", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.menuTentang).setOnClickListener {
            Toast.makeText(this, "Aplikasi PinPoint v1.0 • Pelaporan Fasilitas Publik Makassar", Toast.LENGTH_LONG).show()
        }

        // Tombol Logout dengan Dialog Konfirmasi
        findViewById<View>(R.id.btnLogoutUser).setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Konfirmasi Keluar")
            builder.setMessage("Apakah Anda yakin ingin keluar dari akun?")
            builder.setPositiveButton("Ya, Keluar") { _, _ ->
                val isRemembered = sharedPreferences.getBoolean("KEY_REMEMBER_ME", false)
                val savedId = sharedPreferences.getString("KEY_SAVED_IDENTIFIER", "")
                val savedPass = sharedPreferences.getString("KEY_SAVED_PASSWORD", "")

                val editor = sharedPreferences.edit()
                editor.clear()
                if (isRemembered) {
                    editor.putBoolean("KEY_REMEMBER_ME", true)
                    editor.putString("KEY_SAVED_IDENTIFIER", savedId)
                    editor.putString("KEY_SAVED_PASSWORD", savedPass)
                }
                editor.apply()

                Toast.makeText(this, "Berhasil keluar dari akun", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, Login::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            builder.setNegativeButton("Batal", null)
            builder.show()
        }

        // Setup Bottom Nav dengan Profil Aktif
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        // Beranda Non-Aktif
        val ivHome = findViewById<ImageView>(R.id.ivNavUserHome)
        val tvHome = findViewById<TextView>(R.id.tvNavUserHome)
        ivHome.setColorFilter(Color.parseColor("#94A3B8"))
        tvHome.setTextColor(Color.parseColor("#94A3B8"))
        tvHome.setTypeface(null, Typeface.NORMAL)

        // Profil Aktif (Warna Biru)
        val ivProfile = findViewById<ImageView>(R.id.ivNavUserProfile)
        val tvProfile = findViewById<TextView>(R.id.tvNavUserProfile)
        ivProfile.setColorFilter(Color.parseColor("#0B58CA"))
        tvProfile.setTextColor(Color.parseColor("#0B58CA"))
        tvProfile.setTypeface(null, Typeface.BOLD)

        // Event Navigasi
        findViewById<View>(R.id.navUserHome).setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        findViewById<View>(R.id.navUserHistory).setOnClickListener {
            val intent = Intent(this, RiwayatActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<View>(R.id.navUserReportCenter).setOnClickListener {
            val intent = Intent(this, FormPelaporanActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.navUserNotification).setOnClickListener {
            val intent = Intent(this, NotifikasiActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
