package com.example.pinpoint

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.ResetPasswordWithOtpRequest

class ResetPasswordNewActivity : AppCompatActivity() {

    private var isNewPassVisible = false
    private var isConfirmPassVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password_new)

        val email = intent.getStringExtra("EXTRA_EMAIL") ?: ""
        val otp = intent.getStringExtra("EXTRA_OTP") ?: ""

        val btnBack = findViewById<ImageView>(R.id.btnBackResetNew)
        val tvResetEmailInfo = findViewById<TextView>(R.id.tvResetEmailInfo)
        val etNewPass = findViewById<EditText>(R.id.etNewResetPassword)
        val etConfirmPass = findViewById<EditText>(R.id.etConfirmResetPassword)
        val ivToggleNew = findViewById<ImageView>(R.id.ivToggleNewResetPass)
        val ivToggleConfirm = findViewById<ImageView>(R.id.ivToggleConfirmResetPass)
        val ivMatchIcon = findViewById<ImageView>(R.id.ivResetMatchIcon)
        val btnSave = findViewById<Button>(R.id.btnSaveNewResetPassword)

        tvResetEmailInfo.text = "Verifikasi kode OTP untuk $email berhasil! Silakan masukkan kata sandi baru Anda."

        btnBack.setOnClickListener { finish() }

        ivToggleNew.setOnClickListener {
            isNewPassVisible = !isNewPassVisible
            etNewPass.inputType = if (isNewPassVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etNewPass.setSelection(etNewPass.text.length)
        }

        ivToggleConfirm.setOnClickListener {
            isConfirmPassVisible = !isConfirmPassVisible
            etConfirmPass.inputType = if (isConfirmPassVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etConfirmPass.setSelection(etConfirmPass.text.length)
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val p1 = etNewPass.text.toString()
                val p2 = etConfirmPass.text.toString()
                if (p2.isEmpty()) {
                    ivMatchIcon.visibility = View.GONE
                } else {
                    ivMatchIcon.visibility = View.VISIBLE
                    ivMatchIcon.clearColorFilter()
                    if (p1 == p2) {
                        ivMatchIcon.setImageResource(R.drawable.ic_check_circle)
                    } else {
                        ivMatchIcon.setImageResource(R.drawable.ic_error_circle)
                    }
                }
            }
        }
        etNewPass.addTextChangedListener(watcher)
        etConfirmPass.addTextChangedListener(watcher)

        btnSave.setOnClickListener {
            val newPassword = etNewPass.text.toString().trim()
            val confirmPassword = etConfirmPass.text.toString().trim()

            if (newPassword.length < 6) {
                Toast.makeText(this, "Kata sandi baru minimal 6 karakter!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Konfirmasi kata sandi tidak cocok!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text = "Menyimpan Kata Sandi..."

            val req = ResetPasswordWithOtpRequest(
                email = email,
                otp = otp,
                newPassword = newPassword
            )

            ApiClient.fetchWithFallback(
                callFactory = { api -> api.resetPasswordWithOtp(req) },
                onResponse = { _, response ->
                    btnSave.isEnabled = true
                    btnSave.text = "Simpan Kata Sandi Baru"
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@ResetPasswordNewActivity,
                            "Kata sandi berhasil diperbarui! Silakan login dengan kata sandi baru Anda.",
                            Toast.LENGTH_LONG
                        ).show()
                        val intent = Intent(this@ResetPasswordNewActivity, Login::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        val errMsg = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                            ?: "Gagal menyimpan kata sandi baru!"
                        Toast.makeText(this@ResetPasswordNewActivity, errMsg, Toast.LENGTH_LONG).show()
                    }
                },
                onFailure = { _, t ->
                    btnSave.isEnabled = true
                    btnSave.text = "Simpan Kata Sandi Baru"
                    Toast.makeText(
                        this@ResetPasswordNewActivity,
                        "Gagal menghubungi server (${t.localizedMessage})",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
}

