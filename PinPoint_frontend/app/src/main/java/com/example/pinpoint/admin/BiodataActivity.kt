package com.example.pinpoint.admin

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pinpoint.R
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.UserAccountDto

class BiodataActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biodata)

        val btnBack = findViewById<ImageView>(R.id.btnBackBiodata)
        val etName = findViewById<EditText>(R.id.etProfileName)
        val etEmail = findViewById<EditText>(R.id.etProfileEmail)
        val etPhone = findViewById<EditText>(R.id.etProfilePhone)
        val btnSave = findViewById<Button>(R.id.btnSaveBiodata)

        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val rawToken = sharedPreferences.getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

        val currentEmail = sharedPreferences.getString("KEY_EMAIL", "") ?: ""
        val currentName = sharedPreferences.getString("KEY_NAME", "") ?: ""
        val currentPhone = sharedPreferences.getString("KEY_PHONE", null)

        etName.setText(currentName)
        etEmail.setText(if (currentEmail.isNotBlank()) currentEmail else "-")
        etPhone.setText(if (!currentPhone.isNullOrBlank()) currentPhone else "Memuat nomor telepon...")

        // Pastikan nomor telepon & email sinkron dari database jika belum tersimpan
        ApiClient.fetchWithFallback(
            callFactory = { api -> api.getAllUsers(token, null) },
            onResponse = { _, response ->
                if (response.isSuccessful && response.body() != null) {
                    val me = response.body()!!.find { it.email.equals(currentEmail, ignoreCase = true) }
                    if (me != null) {
                        val resolvedPhone = me.resolvedPhone()
                        etPhone.setText(resolvedPhone)
                        sharedPreferences.edit().putString("KEY_PHONE", resolvedPhone).apply()
                        if (etName.text.isNullOrBlank()) {
                            etName.setText(me.resolvedFullName())
                        }
                    } else if (currentPhone.isNullOrBlank()) {
                        etPhone.setText("-")
                    }
                }
            },
            onFailure = { _, _ ->
                if (currentPhone.isNullOrBlank()) {
                    etPhone.setText("-")
                }
            }
        )

        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()

            if (newName.isEmpty()) {
                etName.error = "Nama lengkap tidak boleh kosong"
                Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text = "Menyimpan..."

            val updateReq = UserAccountDto(
                id = null,
                fullName = newName,
                email = etEmail.text.toString(),
                phoneNumber = etPhone.text.toString(),
                role = sharedPreferences.getString("KEY_ROLE", "ADMIN"),
                photoUrl = sharedPreferences.getString("KEY_PROFILE_IMAGE", null)
            )

            ApiClient.fetchWithFallback(
                callFactory = { api -> api.updateProfile(token, updateReq) },
                onResponse = { _, response ->
                    btnSave.isEnabled = true
                    btnSave.text = "Simpan Perubahan Nama"
                    if (response.isSuccessful) {
                        sharedPreferences.edit().apply {
                            putString("KEY_NAME", newName)
                            apply()
                        }
                        Toast.makeText(this@BiodataActivity, "Nama lengkap berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@BiodataActivity, "Gagal memperbarui biodata (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { _, _ ->
                    btnSave.isEnabled = true
                    btnSave.text = "Simpan Perubahan Nama"
                    Toast.makeText(this@BiodataActivity, "Kesalahan koneksi ke server", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
