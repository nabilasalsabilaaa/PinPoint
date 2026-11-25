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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.RegisterRequest

class Register : AppCompatActivity() {

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val rootView = findViewById<View>(R.id.registerRoot)
        val svSignUp = findViewById<ScrollView>(R.id.svSignUp)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomPadding = maxOf(systemBars.bottom, imeInsets.bottom)
            v.setPadding(systemBars.left, 0, systemBars.right, bottomPadding)
            if (imeInsets.bottom > 0) {
                currentFocus?.let { focused ->
                    scrollViewToChild(svSignUp, focused)
                }
            }
            insets
        }

        setupLabelStyles()

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)

        listOf(etFullName, etEmail, etPhone, etPassword, etConfirmPassword).forEach { field ->
            field.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) scrollViewToChild(svSignUp, v)
            }
            field.setOnClickListener { v ->
                scrollViewToChild(svSignUp, v)
            }
        }
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
            val rawPhone = etPhone.text.toString().trim()
            val phone = normalizePhoneNumber(rawPhone)
            val password = etPassword.text.toString()
            val confirm = etConfirmPassword.text.toString()

            if (name.isEmpty() || email.isEmpty() || rawPhone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap lengkapi seluruh data wajib!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Format email tidak valid"
                Toast.makeText(this, "Masukkan alamat email yang aktif dan valid!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (phone.length < 9 || phone.length > 15 || !phone.startsWith("0") || !phone.all { it.isDigit() }) {
                etPhone.error = "Format nomor ponsel tidak valid"
                Toast.makeText(this, "Masukkan nomor ponsel yang valid (awalan 0 atau 62)!", Toast.LENGTH_SHORT).show()
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

    private fun scrollViewToChild(scrollView: ScrollView, target: View) {
        scrollView.postDelayed({
            val rect = android.graphics.Rect()
            target.getDrawingRect(rect)
            try {
                scrollView.offsetDescendantRectToMyCoords(target, rect)
                val targetY = (rect.top - 140).coerceAtLeast(0)
                scrollView.smoothScrollTo(0, targetY)
            } catch (_: Exception) {
                target.requestRectangleOnScreen(rect, false)
            }
        }, 180)
    }

    private fun normalizePhoneNumber(raw: String): String {
        val cleaned = raw.trim().replace(Regex("[\\s\\-().]"), "")
        if (cleaned.isEmpty()) return ""
        return when {
            cleaned.startsWith("+62") -> "0" + cleaned.substring(3)
            cleaned.startsWith("62") -> "0" + cleaned.substring(2)
            cleaned.startsWith("8") -> "0$cleaned"
            else -> cleaned
        }
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
