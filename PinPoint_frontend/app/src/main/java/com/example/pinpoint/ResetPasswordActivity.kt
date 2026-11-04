package com.example.pinpoint

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputLayout

/**
 * ResetPasswordActivity — Layar penetapan password baru.
 *
 * Menerima extras dari OtpActivity:
 *   - EXTRA_EMAIL: email akun yang direset
 *
 * Hanya dapat diakses setelah OTP berhasil diverifikasi.
 */
class ResetPasswordActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EMAIL = "extra_email"
    }

    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var tilConfirmNewPassword: TextInputLayout
    private lateinit var btnResetPassword: MaterialButton
    private lateinit var progressBar: CircularProgressIndicator

    private lateinit var authRepository: AuthRepository
    private var email: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        email = intent.getStringExtra(EXTRA_EMAIL) ?: ""
        authRepository = AuthRepository(this)

        initViews()
        setupConfirmPasswordWatcher()
        setupClickListeners()
    }

    private fun initViews() {
        tilNewPassword        = findViewById(R.id.tilNewPassword)
        tilConfirmNewPassword = findViewById(R.id.tilConfirmNewPassword)
        btnResetPassword      = findViewById(R.id.btnResetPassword)
        progressBar           = findViewById(R.id.progressBarReset)
    }

    private fun setupConfirmPasswordWatcher() {
        tilConfirmNewPassword.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = tilNewPassword.editText?.text.toString()
                val confirm  = s.toString()
                tilConfirmNewPassword.error = when {
                    confirm.isNotEmpty() && confirm != password -> "Konfirmasi kata sandi tidak cocok"
                    else -> null
                }
            }
        })
    }

    private fun setupClickListeners() {
        btnResetPassword.setOnClickListener {
            val newPassword = tilNewPassword.editText?.text.toString()
            val confirm     = tilConfirmNewPassword.editText?.text.toString()

            tilNewPassword.error        = null
            tilConfirmNewPassword.error = null

            if (newPassword.length < 6) {
                tilNewPassword.error = "Kata sandi minimal 6 karakter"
                return@setOnClickListener
            }
            if (newPassword != confirm) {
                tilConfirmNewPassword.error = "Konfirmasi kata sandi tidak cocok"
                return@setOnClickListener
            }

            setLoading(true)

            authRepository.resetPassword(
                email       = email,
                newPassword = newPassword,
                onSuccess   = { message ->
                    setLoading(false)
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    // Kembali ke Login setelah berhasil
                    val intent = Intent(this, Login::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                },
                onError = { errorMsg ->
                    setLoading(false)
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility    = if (isLoading) View.VISIBLE else View.GONE
        btnResetPassword.isEnabled = !isLoading
        btnResetPassword.text     = if (isLoading) "Menyimpan..." else "Simpan Kata Sandi Baru"
    }
}
