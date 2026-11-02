package com.example.pinpoint

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Register : AppCompatActivity() {

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Terapkan Styling Bintang Merah & Teks Biru Syarat/Ketentuan secara otomatis
        setupLabelStyles()

        // 1. Inisialisasi semua View berdasarkan ID dari XML
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

        // 2. Logika untuk Icon Mata (Show/Hide Password)
        ivPasswordVisibility.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.inputType = if (isPasswordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etPassword.setSelection(etPassword.text.length)
        }

        // 3. Logika untuk Icon Mata (Show/Hide Konfirmasi Password)
        ivConfirmPasswordVisibility.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            etConfirmPassword.inputType = if (isConfirmPasswordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etConfirmPassword.setSelection(etConfirmPassword.text.length)
        }

        // 4. Real-time Validasi Password Cocok / Tidak (Centang Hijau / Silang Merah)
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
                    if (password == confirmPassword) {
                        ivPasswordMatchIcon.setImageResource(android.R.drawable.checkbox_on_background)
                        ivPasswordMatchIcon.setColorFilter(Color.parseColor("#4CAF50")) // Hijau
                    } else {
                        ivPasswordMatchIcon.setImageResource(android.R.drawable.ic_delete)
                        ivPasswordMatchIcon.setColorFilter(Color.parseColor("#E53935")) // Merah
                    }
                }
            }
        }
        etPassword.addTextChangedListener(textWatcher)
        etConfirmPassword.addTextChangedListener(textWatcher)

        // 5. Logika saat Tombol Sign Up ditekan
        btnSignUp.setOnClickListener {
            val fullNameValue = etFullName.text.toString().trim()
            val emailValue = etEmail.text.toString().trim()
            val phoneValue = etPhone.text.toString().trim()
            val passwordValue = etPassword.text.toString().trim()
            val confirmPasswordValue = etConfirmPassword.text.toString().trim()

            if (fullNameValue.isEmpty() || emailValue.isEmpty() || phoneValue.isEmpty() || passwordValue.isEmpty() || confirmPasswordValue.isEmpty()) {
                Toast.makeText(this, "Harap lengkapi semua data wajib!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (phoneValue.isEmpty()) {
                Toast.makeText(this, "Nomor ponsel tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val formattedPhone = if (phoneValue.startsWith("0")) {
                phoneValue
            } else {
                "0$phoneValue"
            }


            if (passwordValue != confirmPasswordValue) {
                Toast.makeText(this, "Kata sandi dan konfirmasi tidak cocok!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!cbTerms.isChecked) {
                Toast.makeText(this, "Anda harus menyetujui Syarat & Ketentuan!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = RegisterRequest(
                nama = fullNameValue,
                email = emailValue,
                no_telepon = formattedPhone,
                password = passwordValue
            )

            ApiClient.instance.register(request).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@Register, "Pendaftaran berhasil! Silakan login.", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@Register, "Registrasi Gagal: Email atau nomor mungkin sudah terdaftar.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@Register, "Gagal terhubung ke server: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        tvLoginLink.setOnClickListener {
            finish()
        }
    }

    // Fungsi bantu untuk membuat bintang (*) berwarna merah pada label
    private fun setRequiredStar(textView: TextView) {
        val text = textView.text.toString()
        if (text.contains("*")) {
            val spannable = SpannableString(text)
            val starIndex = text.indexOf("*")
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#E53935")),
                starIndex, starIndex + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            textView.text = spannable
        }
    }

    // Fungsi bantu untuk mewarnai teks Syarat & Ketentuan serta Kebijakan Privasi menjadi biru
    private fun setupLabelStyles() {
        setRequiredStar(findViewById(R.id.tvLabelFullName))
        setRequiredStar(findViewById(R.id.tvLabelEmail))
        setRequiredStar(findViewById(R.id.tvLabelPhone))
        setRequiredStar(findViewById(R.id.tvLabelPassword))
        setRequiredStar(findViewById(R.id.tvLabelConfirmPassword))

        val tvTerms = findViewById<TextView>(R.id.tvTermsConditions)
        val termsText = tvTerms.text.toString()
        val spannableString = SpannableString(termsText)

        val startTerms = termsText.indexOf("Syarat & Ketentuan")
        if (startTerms != -1) {
            spannableString.setSpan(
                ForegroundColorSpan(Color.parseColor("#003C90")),
                startTerms, startTerms + "Syarat & Ketentuan".length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val startPrivacy = termsText.indexOf("Kebijakan Privasi")
        if (startPrivacy != -1) {
            spannableString.setSpan(
                ForegroundColorSpan(Color.parseColor("#003C90")),
                startPrivacy, startPrivacy + "Kebijakan Privasi".length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        tvTerms.text = spannableString
    }
}