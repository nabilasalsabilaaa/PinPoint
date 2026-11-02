package com.example.pinpoint

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Login : AppCompatActivity() {

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inisialisasi View
        val etEmailOrPhone = findViewById<EditText>(R.id.etEmailOrPhone)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val ivPasswordVisibility = findViewById<ImageView>(R.id.ivPasswordVisibility)
        val cbRememberMe = findViewById<CheckBox>(R.id.cbRememberMe)
        val btnLogin = findViewById<View>(R.id.btnWrapperLogin)
        val tvSignUpLink = findViewById<View>(R.id.tvSignUpLink)
        val tvForgotPassword = findViewById<View>(R.id.tvForgotPassword)

        // SharedPreferences untuk fitur "Ingat Saya"
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val isRemembered = sharedPreferences.getBoolean("KEY_REMEMBER_ME", false)

        if (isRemembered) {
            val savedIdentifier = sharedPreferences.getString("KEY_SAVED_IDENTIFIER", "")
            val savedPassword = sharedPreferences.getString("KEY_SAVED_PASSWORD", "")
            etEmailOrPhone.setText(savedIdentifier)
            etPassword.setText(savedPassword)
            cbRememberMe.isChecked = true
        }

        // Toggle Show/Hide Password
        ivPasswordVisibility.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.inputType = if (isPasswordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etPassword.setSelection(etPassword.text.length)
        }

        // Event Klik "Lupa Kata Sandi?"
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // Tombol Login (Mendukung Email atau Nomor Ponsel berawalan 0)
        btnLogin.setOnClickListener {
            val emailOrPhone = etEmailOrPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (emailOrPhone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap isi email/nomor ponsel dan kata sandi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = LoginRequest(identifier = emailOrPhone, password = password)

            ApiClient.instance.login(request).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val loginData = response.body()

                        if (loginData?.token != null) {
                            val editor = sharedPreferences.edit()

                            // Cek apakah Checkbox "Ingat Saya" dicentang oleh user
                            if (cbRememberMe.isChecked) {
                                editor.putString("KEY_TOKEN", loginData.token)
                                editor.putString("KEY_NAME", loginData.nama)
                                editor.putBoolean("KEY_IS_LOGGED_IN", true)

                                // Simpan kredensial untuk auto-fill
                                editor.putBoolean("KEY_REMEMBER_ME", true)
                                editor.putString("KEY_SAVED_IDENTIFIER", emailOrPhone)
                                editor.putString("KEY_SAVED_PASSWORD", password)
                            } else {
                                // Hapus kredensial jika tidak dicentang
                                editor.remove("KEY_REMEMBER_ME")
                                editor.remove("KEY_SAVED_IDENTIFIER")
                                editor.remove("KEY_SAVED_PASSWORD")
                            }
                            editor.apply()

                            Toast.makeText(this@Login, "Selamat datang, ${loginData.nama}!", Toast.LENGTH_SHORT).show()

                            // Pindah ke Dashboard
                            val intent = Intent(this@Login, DashboardActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@Login, "Login gagal: Token tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Email/Nomor Ponsel atau Password salah!"
                        Toast.makeText(this@Login, "Login Gagal: $errorBody", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@Login, "Gagal terhubung ke server: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // Event Klik ke Halaman Register
        tvSignUpLink.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }
    }
}