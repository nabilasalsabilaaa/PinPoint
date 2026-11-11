package com.example.pinpoint.admin

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pinpoint.R
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.UserAccountDto

class TambahAkunActivity : AppCompatActivity() {

    // Role sesuai database: 1 = ADMIN, 2 = USER, 3 = PEMERINTAH
    private var selectedRoleName: String = "PEMERINTAH"
    private var selectedRoleId: Long = 3L
    private var isPasswordVisible: Boolean = false

    private lateinit var btnBackAddAccount: ImageView
    private lateinit var cardGovernmentAccount: View
    private lateinit var cardAdminAccount: View
    private lateinit var ivCheckGovernment: ImageView
    private lateinit var ivCheckAdmin: ImageView
    private lateinit var ivGovIcon: ImageView
    private lateinit var ivAdminIcon: ImageView
    private lateinit var etFullName: EditText
    private lateinit var etOfficialEmail: EditText
    private lateinit var etPhoneNumber: EditText

    private lateinit var ivPerm1: ImageView
    private lateinit var ivPerm2: ImageView
    private lateinit var ivPerm3: ImageView
    private lateinit var ivPerm4: ImageView
    private lateinit var ivPerm5: ImageView
    private lateinit var tvPermTitle4: TextView
    private lateinit var tvPermTitle5: TextView

    private lateinit var btnGeneratePassword: TextView
    private lateinit var etTempPassword: EditText
    private lateinit var btnTogglePasswordVisibility: ImageView
    private lateinit var btnSubmitAccount: Button
    private lateinit var btnCancelAccount: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_tambah_akun)

        initViews()
        setupTypeSelection()
        setupPasswordTools()

        btnBackAddAccount.setOnClickListener { finish() }
        btnCancelAccount.setOnClickListener { finish() }
        btnSubmitAccount.setOnClickListener { createAccount() }
    }

    private fun initViews() {
        btnBackAddAccount = findViewById(R.id.btnBackAddAccount)
        cardGovernmentAccount = findViewById(R.id.cardGovernmentAccount)
        cardAdminAccount = findViewById(R.id.cardAdminAccount)
        ivCheckGovernment = findViewById(R.id.ivCheckGovernment)
        ivCheckAdmin = findViewById(R.id.ivCheckAdmin)
        ivGovIcon = findViewById(R.id.ivGovIcon)
        ivAdminIcon = findViewById(R.id.ivAdminIcon)
        etFullName = findViewById(R.id.etFullName)
        etOfficialEmail = findViewById(R.id.etOfficialEmail)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)

        ivPerm1 = findViewById(R.id.ivPerm1)
        ivPerm2 = findViewById(R.id.ivPerm2)
        ivPerm3 = findViewById(R.id.ivPerm3)
        ivPerm4 = findViewById(R.id.ivPerm4)
        ivPerm5 = findViewById(R.id.ivPerm5)
        tvPermTitle4 = findViewById(R.id.tvPermTitle4)
        tvPermTitle5 = findViewById(R.id.tvPermTitle5)

        btnGeneratePassword = findViewById(R.id.btnGeneratePassword)
        etTempPassword = findViewById(R.id.etTempPassword)
        btnTogglePasswordVisibility = findViewById(R.id.btnTogglePasswordVisibility)
        btnSubmitAccount = findViewById(R.id.btnSubmitAccount)
        btnCancelAccount = findViewById(R.id.btnCancelAccount)
    }

    private fun setupTypeSelection() {
        updateUIForRole("PEMERINTAH")
        cardGovernmentAccount.setOnClickListener { updateUIForRole("PEMERINTAH") }
        cardAdminAccount.setOnClickListener { updateUIForRole("ADMIN") }
    }

    private fun updateUIForRole(role: String) {
        val isGov = role == "PEMERINTAH"
        selectedRoleName = if (isGov) "PEMERINTAH" else "ADMIN"
        selectedRoleId = if (isGov) 3L else 1L

        // Update kartu pilihan peran
        if (isGov) {
            cardGovernmentAccount.setBackgroundResource(R.drawable.bg_card_selected_blue)
            cardAdminAccount.setBackgroundResource(R.drawable.bg_card_unselected)
            ivCheckGovernment.setImageResource(R.drawable.ic_check_circle_blue)
            ivCheckAdmin.setImageResource(R.drawable.ic_circle_outline)

            ivGovIcon.setBackgroundResource(R.drawable.bg_circle_blue_btn)
            ivGovIcon.setColorFilter(Color.WHITE)
            ivAdminIcon.setBackgroundResource(R.drawable.bg_chip_blue_light)
            ivAdminIcon.setColorFilter(Color.parseColor("#0B3C8C"))
        } else {
            cardGovernmentAccount.setBackgroundResource(R.drawable.bg_card_unselected)
            cardAdminAccount.setBackgroundResource(R.drawable.bg_card_selected_blue)
            ivCheckGovernment.setImageResource(R.drawable.ic_circle_outline)
            ivCheckAdmin.setImageResource(R.drawable.ic_check_circle_blue)

            ivGovIcon.setBackgroundResource(R.drawable.bg_chip_blue_light)
            ivGovIcon.setColorFilter(Color.parseColor("#0B3C8C"))
            ivAdminIcon.setBackgroundResource(R.drawable.bg_circle_blue_btn)
            ivAdminIcon.setColorFilter(Color.WHITE)
        }

        // Checklist hak akses terisi otomatis & permanen (tidak bisa diubah manual)
        ivPerm1.setImageResource(R.drawable.ic_check_circle)
        ivPerm2.setImageResource(R.drawable.ic_check_circle)
        ivPerm3.setImageResource(R.drawable.ic_check_circle)

        if (isGov) {
            ivPerm4.setImageResource(R.drawable.ic_circle_outline)
            ivPerm5.setImageResource(R.drawable.ic_circle_outline)
            tvPermTitle4.setTextColor(Color.parseColor("#94A3B8"))
            tvPermTitle5.setTextColor(Color.parseColor("#94A3B8"))
        } else {
            ivPerm4.setImageResource(R.drawable.ic_check_circle)
            ivPerm5.setImageResource(R.drawable.ic_check_circle)
            tvPermTitle4.setTextColor(Color.parseColor("#0B1C30"))
            tvPermTitle5.setTextColor(Color.parseColor("#0B1C30"))
        }
    }

    private fun setupPasswordTools() {
        btnGeneratePassword.setOnClickListener {
            etTempPassword.setText("PinPoint" + (1000..9999).random())
        }
        btnTogglePasswordVisibility.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etTempPassword.inputType = if (isPasswordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etTempPassword.setSelection(etTempPassword.text.length)
        }
    }

    private fun createAccount() {
        val name = etFullName.text.toString().trim()
        val email = etOfficialEmail.text.toString().trim()
        val phone = etPhoneNumber.text.toString().trim()
        val tempPassword = etTempPassword.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || tempPassword.isEmpty()) {
            Toast.makeText(this, "Harap lengkapi semua kolom wajib!", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmitAccount.isEnabled = false
        btnSubmitAccount.text = "Menyimpan Akun..."

        val rawToken = getSharedPreferences("UserSession", MODE_PRIVATE).getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

        val newAccount = UserAccountDto(
            fullName = name,
            email = email,
            phoneNumber = phone,
            idRole = selectedRoleId,
            role = selectedRoleName,
            password = tempPassword,
            statusAkun = "ACTIVE"
        )

        ApiClient.fetchWithFallback(
            callFactory = { api -> api.createAdminOrGovAccount(token, newAccount) },
            onResponse = { _, response ->
                btnSubmitAccount.isEnabled = true
                btnSubmitAccount.text = "Simpan & Daftarkan Akun"
                if (response.isSuccessful) {
                    val labelRole = if (selectedRoleId == 1L) "Admin" else "Pemerintah"
                    Toast.makeText(this@TambahAkunActivity, "Akun $labelRole berhasil dibuat!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val error = response.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: "Data tidak valid (${response.code()})"
                    Toast.makeText(this@TambahAkunActivity, "Gagal membuat akun: $error", Toast.LENGTH_LONG).show()
                }
            },
            onFailure = { _, t ->
                btnSubmitAccount.isEnabled = true
                btnSubmitAccount.text = "Simpan & Daftarkan Akun"
                Toast.makeText(this@TambahAkunActivity, "Koneksi ke server gagal: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        )
    }
}
