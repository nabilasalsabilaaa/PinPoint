package com.example.pinpoint

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputLayout

/**
 * LoginActivity — Layar autentikasi pengguna PinPoint.
 *
 * Fitur:
 * - Login via email atau nomor telepon + password
 * - Toggle show/hide password (bawaan TextInputLayout MD3)
 * - "Ingat Saya" — kredensial disimpan di EncryptedSharedPreferences
 * - Auto-fill dari "Ingat Saya" saat activity dibuka
 * - Redirect langsung ke Dashboard jika sudah login (token masih ada)
 */
class Login : AppCompatActivity() {

    private lateinit var tilEmailOrPhone: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var cbRememberMe: MaterialCheckBox
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvForgotPassword: View
    private lateinit var tvSignUpLink: View
    private lateinit var progressBar: CircularProgressIndicator

    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Auto-redirect jika sudah login ──────────────────────────────
        if (TokenManager.isLoggedIn(this)) {
            navigateToDashboard()
            return
        }

        setContentView(R.layout.activity_login)
        authRepository = AuthRepository(this)
        initViews()
        autoFillRememberMe()
        setupClickListeners()
    }

    private fun initViews() {
        tilEmailOrPhone = findViewById(R.id.tilEmailOrPhone)
        tilPassword     = findViewById(R.id.tilPassword)
        cbRememberMe    = findViewById(R.id.cbRememberMe)
        btnLogin        = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvSignUpLink     = findViewById(R.id.tvSignUpLink)
        progressBar      = findViewById(R.id.progressBarLogin)
    }

    /** Mengisi input secara otomatis dari data "Ingat Saya" yang tersimpan. */
    private fun autoFillRememberMe() {
        if (TokenManager.isRememberMe(this)) {
            tilEmailOrPhone.editText?.setText(TokenManager.getSavedIdentifier(this))
            tilPassword.editText?.setText(TokenManager.getSavedPassword(this))
            cbRememberMe.isChecked = true
        }
    }

    private fun setupClickListeners() {

        // ── Tombol Login ─────────────────────────────────────────────────
        btnLogin.setOnClickListener {
            val identifier = tilEmailOrPhone.editText?.text.toString().trim()
            val password   = tilPassword.editText?.text.toString().trim()

            // Reset error sebelumnya
            tilEmailOrPhone.error = null
            tilPassword.error     = null

            // Validasi input
            var isValid = true
            if (identifier.isEmpty()) {
                tilEmailOrPhone.error = "Email atau nomor ponsel tidak boleh kosong"
                isValid = false
            }
            if (password.isEmpty()) {
                tilPassword.error = "Kata sandi tidak boleh kosong"
                isValid = false
            }
            if (!isValid) return@setOnClickListener

            setLoading(true)

            authRepository.login(
                identifier = identifier,
                password   = password,
                rememberMe = cbRememberMe.isChecked,
                onSuccess  = { loginResponse ->
                    setLoading(false)
                    Toast.makeText(
                        this,
                        "Selamat datang, ${loginResponse.nama}! 👋",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToDashboard()
                },
                onError = { errorMsg ->
                    setLoading(false)
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                }
            )
        }

        // ── Lupa Kata Sandi ──────────────────────────────────────────────
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // ── Link ke Register ─────────────────────────────────────────────
        tvSignUpLink.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnLogin.isEnabled     = !isLoading
        btnLogin.text          = if (isLoading) "Memproses..." else "Masuk"
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
