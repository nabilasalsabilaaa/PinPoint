package com.example.pinpoint

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
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {

    private enum class SelectedMode { EMAIL, PHONE }
    private var currentMode = SelectedMode.EMAIL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

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

        // Submit Lupa Password / OTP
        btnSubmit.setOnClickListener {
            val inputValue = etInput.text.toString().trim()
            if (inputValue.isEmpty()) {
                val fieldName = if (currentMode == SelectedMode.EMAIL) "email" else "nomor WhatsApp"
                Toast.makeText(this, "Harap masukkan $fieldName Anda!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = CheckAccountRequest(
                identifier = inputValue,
                type = if (currentMode == SelectedMode.EMAIL) "EMAIL" else "PHONE"
            )

            // Panggil API Pengecekan Akun
            ApiClient.instance.checkAccount(request).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            "Tautan verifikasi / OTP berhasil dikirim!",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Akun tidak terdaftar atau tidak aktif!"
                        Toast.makeText(this@ForgotPasswordActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@ForgotPasswordActivity, "Gagal terhubung ke server: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        btnBackToLogin.setOnClickListener {
            finish()
        }
    }
}