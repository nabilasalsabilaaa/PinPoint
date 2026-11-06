package com.example.pinpoint

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputLayout

/**
 * Register — Layar pendaftaran akun baru PinPoint (role: WARGA).
 *
 * Fitur:
 * - Validasi semua field sebelum submit
 * - Validasi konfirmasi password real-time (TextWatcher)
 * - Checkbox persetujuan Syarat & Ketentuan wajib dicentang
 * - Redirect ke Login setelah berhasil mendaftar
 */
class Register : AppCompatActivity() {

    private lateinit var tilNama: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilNoTelepon: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var cbTerms: MaterialCheckBox
    private lateinit var btnRegister: MaterialButton
    private lateinit var tvLoginLink: View
    private lateinit var progressBar: CircularProgressIndicator

    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        authRepository = AuthRepository(this)
        initViews()
        setupConfirmPasswordWatcher()
        setupClickListeners()
    }

    private fun initViews() {
        tilNama            = findViewById(R.id.tilNama)
        tilEmail           = findViewById(R.id.tilEmail)
        tilNoTelepon       = findViewById(R.id.tilNoTelepon)
        tilPassword        = findViewById(R.id.tilPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        cbTerms            = findViewById(R.id.cbTerms)
        btnRegister        = findViewById(R.id.btnRegister)
        tvLoginLink        = findViewById(R.id.tvLoginLink)
        progressBar        = findViewById(R.id.progressBarRegister)
    }

    /** Validasi konfirmasi password secara real-time saat user mengetik. */
    private fun setupConfirmPasswordWatcher() {
        tilConfirmPassword.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = tilPassword.editText?.text.toString()
                val confirm  = s.toString()
                tilConfirmPassword.error = when {
                    confirm.isEmpty() -> null
                    confirm != password -> "Konfirmasi kata sandi tidak cocok"
                    else -> null
                }
                if (confirm == password && confirm.isNotEmpty()) {
                    tilConfirmPassword.error = null
                }
            }
        })
    }

    private fun setupClickListeners() {

        // ── Tombol Daftar ────────────────────────────────────────────────
        btnRegister.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener

            val nama       = tilNama.editText?.text.toString().trim()
            val email      = tilEmail.editText?.text.toString().trim()
            val noTelepon  = tilNoTelepon.editText?.text.toString().trim().ifEmpty { null }
            val password   = tilPassword.editText?.text.toString()

            setLoading(true)

            authRepository.register(
                nama       = nama,
                email      = email,
                noTelepon  = noTelepon,
                password   = password,
                onSuccess  = { message ->
                    setLoading(false)
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    // Navigasi ke verifikasi OTP
                    val intent = Intent(this, OtpActivity::class.java).apply {
                        putExtra(OtpActivity.EXTRA_EMAIL, email)
                        putExtra(OtpActivity.EXTRA_OTP_TYPE, "EMAIL")
                        putExtra(OtpActivity.EXTRA_SOURCE, "REGISTER")
                    }
                    startActivity(intent)
                    finish()
                },
                onError = { errorMsg ->
                    setLoading(false)
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                }
            )
        }

        // ── Link ke Login ─────────────────────────────────────────────────
        tvLoginLink.setOnClickListener { finish() }
    }

    /**
     * Memvalidasi semua field input.
     * @return true jika semua valid, false jika ada kesalahan.
     */
    private fun validateInputs(): Boolean {
        var isValid = true

        // Reset semua error
        tilNama.error            = null
        tilEmail.error           = null
        tilPassword.error        = null
        tilConfirmPassword.error = null

        val nama     = tilNama.editText?.text.toString().trim()
        val email    = tilEmail.editText?.text.toString().trim()
        val password = tilPassword.editText?.text.toString()
        val confirm  = tilConfirmPassword.editText?.text.toString()

        if (nama.isEmpty()) {
            tilNama.error = "Nama lengkap tidak boleh kosong"
            isValid = false
        }
        if (email.isEmpty()) {
            tilEmail.error = "Email tidak boleh kosong"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Format email tidak valid"
            isValid = false
        }
        if (password.length < 6) {
            tilPassword.error = "Kata sandi minimal 6 karakter"
            isValid = false
        }
        if (confirm != password) {
            tilConfirmPassword.error = "Konfirmasi kata sandi tidak cocok"
            isValid = false
        }
        if (!cbTerms.isChecked) {
            Toast.makeText(this,
                "Anda harus menyetujui Syarat & Ketentuan untuk mendaftar.",
                Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnRegister.isEnabled  = !isLoading
        btnRegister.text       = if (isLoading) "Mendaftarkan..." else "Daftar Sekarang"
    }
}
