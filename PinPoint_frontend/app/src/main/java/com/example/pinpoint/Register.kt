package com.example.pinpoint

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.RegisterRequest

class Register : AppCompatActivity() {

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        setupLabelStyles()

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val ivPasswordVisibility = findViewById<ImageView>(R.id.ivPasswordVisibility)
        val ivConfirmPasswordVisibility = findViewById<ImageView>(R.id.ivConfirmPasswordVisibility)
        val ivPasswordMatchIcon = findViewById<ImageView>(R.id.ivPasswordMatchIcon)
        val cbTerms = findViewById<CheckBox>(R.id.cbTerms)
        val btnSignUp = findViewById<View>(R.id.btnWrapperSignUp)
        val tvLoginLink = findViewById<View>(R.id.tvLoginLink)

        ivPasswordVisibility.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.inputType = if (isPasswordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etPassword.setSelection(etPassword.text.length)
        }

        ivConfirmPasswordVisibility.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            etConfirmPassword.inputType = if (isConfirmPasswordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etConfirmPassword.setSelection(etConfirmPassword.text.length)
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = etPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()
                if (confirmPassword.isEmpty()) {
                    ivPasswordMatchIcon.visibility = View.GONE
                } else {
                    ivPasswordMatchIcon.visibility = View.VISIBLE
                    ivPasswordMatchIcon.clearColorFilter()
                    if (password == confirmPassword) {
                        ivPasswordMatchIcon.setImageResource(R.drawable.ic_check_circle)
                    } else {
                        ivPasswordMatchIcon.setImageResource(R.drawable.ic_error_circle)
                    }
                }
            }
        }
        etPassword.addTextChangedListener(textWatcher)
        etConfirmPassword.addTextChangedListener(textWatcher)

        btnSignUp.setOnClickListener {
            val name = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString()
            val confirm = etConfirmPassword.text.toString()

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap lengkapi seluruh data wajib!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Format email tidak valid"
                Toast.makeText(this, "Masukkan alamat email yang aktif dan valid!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Kata sandi minimal 6 karakter!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirm) {
                Toast.makeText(this, "Kata sandi tidak cocok!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!cbTerms.isChecked) {
                Toast.makeText(this, "Setujui Syarat & Ketentuan terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSignUp.isEnabled = false
            Toast.makeText(this, "Memeriksa keaktifan email & mengirim kode OTP...", Toast.LENGTH_SHORT).show()

            val request = RegisterRequest(name, email, phone, password)
            ApiClient.fetchWithFallback(
                callFactory = { api -> api.requestRegisterOtp(request) },
                onResponse = { _, response ->
                    btnSignUp.isEnabled = true
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@Register,
                            "Kode OTP 6 digit telah dikirim ke $email",
                            Toast.LENGTH_LONG
                        ).show()

                        val intent = Intent(this@Register, OtpVerificationActivity::class.java).apply {
                            putExtra("EXTRA_MODE", "REGISTER")
                            putExtra("EXTRA_EMAIL", email)
                            putExtra("EXTRA_REG_NAME", name)
                            putExtra("EXTRA_REG_PHONE", phone)
                            putExtra("EXTRA_REG_PASSWORD", password)
                        }
                        startActivity(intent)
                    } else {
                        val errorMsg = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                            ?: "Gagal mengirim OTP pendaftaran"
                        Toast.makeText(this@Register, errorMsg, Toast.LENGTH_LONG).show()
                    }
                },
                onFailure = { _, t ->
                    btnSignUp.isEnabled = true
                    Toast.makeText(
                        this@Register,
                        "Koneksi ke server gagal (${t.localizedMessage})",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }

        tvLoginLink.setOnClickListener { finish() }
    }

    private fun setupLabelStyles() {
        val labels = listOf(
            R.id.tvLabelFullName, R.id.tvLabelEmail, R.id.tvLabelPhone,
            R.id.tvLabelPassword, R.id.tvLabelConfirmPassword
        )
        for (id in labels) {
            val tv = findViewById<TextView>(id)
            val text = tv.text.toString()
            if (text.contains("*")) {
                val span = SpannableString(text)
                val idx = text.indexOf("*")
                span.setSpan(ForegroundColorSpan(Color.RED), idx, idx + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                tv.text = span
            }
        }
    }
}
