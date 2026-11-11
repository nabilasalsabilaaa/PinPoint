package com.example.pinpoint

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pinpoint.admin.AdminDashboardActivity
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.LoginRequest
import com.example.pinpoint.network.LoginResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Login : AppCompatActivity() {

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmailOrPhone = findViewById<EditText>(R.id.etEmailOrPhone)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val ivPasswordVisibility = findViewById<ImageView>(R.id.ivPasswordVisibility)
        val cbRememberMe = findViewById<CheckBox>(R.id.cbRememberMe)
        val btnLogin = findViewById<View>(R.id.btnWrapperLogin)
        val tvSignUpLink = findViewById<View>(R.id.tvSignUpLink)
        val tvForgotPassword = findViewById<View>(R.id.tvForgotPassword)

        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val rememberPrefs = getSharedPreferences("RememberMePrefs", MODE_PRIVATE)
        val permanentPrefs = getSharedPreferences("PermanentUserPrefs", MODE_PRIVATE)

        // Prioritaskan RememberMePrefs yang tidak akan pernah terhapus saat logout
        val isRemembered = rememberPrefs.getBoolean("KEY_REMEMBER_ME", false) ||
                sharedPreferences.getBoolean("KEY_REMEMBER_ME", false)

        if (isRemembered) {
            val savedId = rememberPrefs.getString("KEY_SAVED_IDENTIFIER", null)
                ?: sharedPreferences.getString("KEY_SAVED_IDENTIFIER", "") ?: ""
            val savedPass = rememberPrefs.getString("KEY_SAVED_PASSWORD", null)
                ?: sharedPreferences.getString("KEY_SAVED_PASSWORD", "") ?: ""
            etEmailOrPhone.setText(savedId)
            etPassword.setText(savedPass)
            cbRememberMe.isChecked = true
        }

        ivPasswordVisibility.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.inputType = if (isPasswordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etPassword.setSelection(etPassword.text.length)
        }

        btnLogin.setOnClickListener {
            val emailOrPhone = etEmailOrPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (emailOrPhone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap isi email/nomor ponsel dan kata sandi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            Toast.makeText(this, "Memproses login...", Toast.LENGTH_SHORT).show()

            ApiClient.instance.login(LoginRequest(emailOrPhone, password))
                .enqueue(object : Callback<LoginResponse> {
                    override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                        btnLogin.isEnabled = true
                        if (response.isSuccessful && response.body() != null) {
                            val loginData = response.body()!!

                            if (loginData.isSuspended == true) {
                                Toast.makeText(this@Login, "Akun Anda ditangguhkan. Silakan hubungi Admin.", Toast.LENGTH_LONG).show()
                                return
                            }

                            val resolvedRoleStr = (loginData.role ?: "USER").uppercase()
                            val resolvedIdRole = loginData.idRole ?: when {
                                resolvedRoleStr.contains("ADMIN") -> 1L
                                resolvedRoleStr.contains("PEMERINTAH") || resolvedRoleStr.contains("DINAS") -> 3L
                                else -> 2L
                            }

                            val userEmail = (loginData.email ?: emailOrPhone).lowercase()
                            val permanentPhoto = permanentPrefs.getString("PROFILE_IMAGE_$userEmail", null)
                                ?: permanentPrefs.getString("PROFILE_IMAGE_LAST", null)
                            val resolvedPhoto = permanentPhoto ?: loginData.photoUrl

                            // Simpan kredensial Ingat Saya secara permanen
                            if (cbRememberMe.isChecked) {
                                rememberPrefs.edit()
                                    .putBoolean("KEY_REMEMBER_ME", true)
                                    .putString("KEY_SAVED_IDENTIFIER", emailOrPhone)
                                    .putString("KEY_SAVED_PASSWORD", password)
                                    .apply()
                            } else {
                                rememberPrefs.edit().clear().apply()
                            }

                            val displayName = loginData.nama ?: loginData.fullName ?: emailOrPhone

                            sharedPreferences.edit().apply {
                                putLong("KEY_USER_ID", loginData.id ?: 0L)
                                putLong("KEY_ID_ROLE", resolvedIdRole)
                                putString("KEY_TOKEN", loginData.token)
                                putString("KEY_NAME", displayName)
                                putString("KEY_EMAIL", loginData.email ?: emailOrPhone)
                                putString("KEY_ROLE", resolvedRoleStr)
                                putString("KEY_PROFILE_IMAGE", resolvedPhoto)
                                putString("KEY_PHONE", loginData.phoneNumber ?: loginData.noTelepon)
                                putBoolean("KEY_IS_LOGGED_IN", true)
                                if (cbRememberMe.isChecked) {
                                    putBoolean("KEY_REMEMBER_ME", true)
                                    putString("KEY_SAVED_IDENTIFIER", emailOrPhone)
                                    putString("KEY_SAVED_PASSWORD", password)
                                } else {
                                    remove("KEY_REMEMBER_ME")
                                    remove("KEY_SAVED_IDENTIFIER")
                                    remove("KEY_SAVED_PASSWORD")
                                }
                                apply()
                            }

                            Toast.makeText(this@Login, "Selamat datang, $displayName!", Toast.LENGTH_SHORT).show()

                            // Pembatasan Role: Hanya Role 1 (ADMIN) yang masuk ke AdminDashboardActivity
                            // Role 2 (USER) & Role 3 (PEMERINTAH) masuk ke DashboardActivity simpel
                            val isAdmin = resolvedIdRole == 1L || resolvedRoleStr == "ADMIN" || resolvedRoleStr == "ROLE_ADMIN"
                            val targetIntent = if (isAdmin) {
                                Intent(this@Login, AdminDashboardActivity::class.java)
                            } else {
                                Intent(this@Login, DashboardActivity::class.java)
                            }
                            targetIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(targetIntent)
                            finish()
                        } else {
                            val errBody = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                                ?: "Email/Nomor Ponsel atau kata sandi salah (${response.code()})"
                            Toast.makeText(this@Login, errBody, Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        btnLogin.isEnabled = true
                        Toast.makeText(
                            this@Login,
                            "Tidak dapat terhubung ke server backend. Pastikan server menyala & satu jaringan Wi-Fi (${t.localizedMessage})",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }

        tvSignUpLink.setOnClickListener { startActivity(Intent(this, Register::class.java)) }
        tvForgotPassword.setOnClickListener { startActivity(Intent(this, ForgotPasswordActivity::class.java)) }
    }
}
