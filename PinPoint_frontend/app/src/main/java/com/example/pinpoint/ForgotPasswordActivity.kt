package com.example.pinpoint

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ForgotPasswordActivity : AppCompatActivity() {

    private enum class SelectedMode { EMAIL, PHONE }
    private var currentMode = SelectedMode.EMAIL
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        authRepository = AuthRepository(this)

        val btnTabEmail = findViewById<LinearLayout>(R.id.btnTabEmail)
        val btnTabPhone = findViewById<LinearLayout>(R.id.btnTabPhone)
        val ivTabEmailIcon = findViewById<ImageView>(R.id.ivTabEmailIcon)
        val tvTabEmailText = findViewById<TextView>(R.id.tvTabEmailText)
        val ivTabPhoneIcon = findViewById<ImageView>(R.id.ivTabPhoneIcon)
        val tvTabPhoneText = findViewById<TextView>(R.id.tvTabPhoneText)

        val tvInputLabel = findViewById<TextView>(R.id.tvInputLabel)
        val etInput = findViewById<EditText>(R.id.etInput)
        val tvInfoHint = findViewById<TextView>(R.id.tvInfoHint)

        val btnSubmit = findViewById<View>(R.id.btnSubmitForgotPassword)
        val btnBackToLogin = findViewById<View>(R.id.btnBackToLogin)

        // Switch ke Tab Email
        btnTabEmail.setOnClickListener {
            if (currentMode != SelectedMode.EMAIL) {
                currentMode = SelectedMode.EMAIL

                // Style Tab Email Active
                btnTabEmail.setBackgroundColor(Color.WHITE)
                btnTabEmail.elevation = 4f
                tvTabEmailText.setTextColor(Color.parseColor("#0B1C30"))
                ivTabEmailIcon.setColorFilter(Color.parseColor("#003C90"))

                // Style Tab Phone Inactive
                btnTabPhone.background = null
                btnTabPhone.elevation = 0f
                tvTabPhoneText.setTextColor(Color.parseColor("#737784"))
                ivTabPhoneIcon.setColorFilter(Color.parseColor("#737784"))

                // Update Input Text
                tvInputLabel.text = "Alamat Email"
                etInput.hint = "contohemail@gmail.com"
                etInput.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                etInput.text.clear()
                tvInfoHint.text = "Pastikan akun email Anda aktif untuk menerima link pemulihan instan."
            }
        }

        // Switch ke Tab Phone
        btnTabPhone.setOnClickListener {
            if (currentMode != SelectedMode.PHONE) {
                currentMode = SelectedMode.PHONE

                // Style Tab Phone Active
                btnTabPhone.setBackgroundColor(Color.WHITE)
                btnTabPhone.elevation = 4f
                tvTabPhoneText.setTextColor(Color.parseColor("#0B1C30"))
                ivTabPhoneIcon.setColorFilter(Color.parseColor("#003C90"))

                // Style Tab Email Inactive
                btnTabEmail.background = null
                btnTabEmail.elevation = 0f
                tvTabEmailText.setTextColor(Color.parseColor("#737784"))
                ivTabEmailIcon.setColorFilter(Color.parseColor("#737784"))

                // Update Input Text
                tvInputLabel.text = "No Telepon"
                etInput.hint = "+62 891 234 567"
                etInput.inputType = InputType.TYPE_CLASS_PHONE
                etInput.text.clear()
                tvInfoHint.text = "Pastikan Nomor WhatsApp Anda aktif untuk menerima link pemulihan instan."
            }
        }

        // Submit Lupa Password / Minta OTP
        btnSubmit.setOnClickListener {
            val inputValue = etInput.text.toString().trim()
            if (inputValue.isEmpty()) {
                val fieldName = if (currentMode == SelectedMode.EMAIL) "email" else "nomor WhatsApp"
                Toast.makeText(this, "Harap masukkan $fieldName Anda!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val modeString = if (currentMode == SelectedMode.EMAIL) "EMAIL" else "PHONE"

            btnSubmit.isEnabled = false

            authRepository.sendOtp(
                identifier = inputValue,
                type = modeString,
                onSuccess = { message ->
                    btnSubmit.isEnabled = true
                    Toast.makeText(this@ForgotPasswordActivity, message, Toast.LENGTH_SHORT).show()

                    // Berpindah ke layar Verifikasi OTP
                    val intent = Intent(this@ForgotPasswordActivity, OtpActivity::class.java).apply {
                        putExtra(OtpActivity.EXTRA_EMAIL, inputValue)
                        putExtra(OtpActivity.EXTRA_OTP_TYPE, modeString)
                    }
                    startActivity(intent)
                    finish()
                },
                onError = { errorMsg ->
                    btnSubmit.isEnabled = true
                    Toast.makeText(this@ForgotPasswordActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            )
        }

        btnBackToLogin.setOnClickListener {
            finish()
        }
    }
}
