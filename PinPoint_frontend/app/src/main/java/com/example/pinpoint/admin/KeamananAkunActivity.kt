package com.example.pinpoint.admin

import android.graphics.Color
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
import com.example.pinpoint.R
import com.example.pinpoint.model.ChangePasswordRequest
import com.example.pinpoint.network.ApiClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class KeamananAkunActivity : AppCompatActivity() {

    private var isCurrentVisible = false
    private var isNewVisible = false
    private var isConfirmVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_keamanan)

        val btnBack = findViewById<ImageView>(R.id.btnBackKeamanan)
        val tvSecurityEmailInfo = findViewById<TextView>(R.id.tvSecurityEmailInfo)
        val etCurrentPassword = findViewById<EditText>(R.id.etCurrentPassword)
        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)
        val etConfirmNewPassword = findViewById<EditText>(R.id.etConfirmNewPassword)
        val ivToggleCurrentPass = findViewById<ImageView>(R.id.ivToggleCurrentPass)
        val ivToggleNewPass = findViewById<ImageView>(R.id.ivToggleNewPass)
        val ivToggleConfirmNewPass = findViewById<ImageView>(R.id.ivToggleConfirmNewPass)
        val ivMatchNewPass = findViewById<ImageView>(R.id.ivMatchNewPass)
        val tvPassStrength = findViewById<TextView>(R.id.tvPassStrength)
        val btnSaveNewPassword = findViewById<Button>(R.id.btnSaveNewPassword)

        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val email = sharedPreferences.getString("KEY_EMAIL", "admin@pinpoint.id") ?: "admin@pinpoint.id"
        tvSecurityEmailInfo.text = "Sesi Terautentikasi: $email"

        btnBack.setOnClickListener { finish() }

        ivToggleCurrentPass.setOnClickListener {
            isCurrentVisible = !isCurrentVisible
            togglePasswordField(etCurrentPassword, isCurrentVisible)
        }

        ivToggleNewPass.setOnClickListener {
            isNewVisible = !isNewVisible
            togglePasswordField(etNewPassword, isNewVisible)
        }

        ivToggleConfirmNewPass.setOnClickListener {
            isConfirmVisible = !isConfirmVisible
            togglePasswordField(etConfirmNewPassword, isConfirmVisible)
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val newPass = etNewPassword.text.toString()
                val confirmPass = etConfirmNewPassword.text.toString()

                // Indikator kekuatan sandi
                when {
                    newPass.isEmpty() -> {
                        tvPassStrength.text = "Minimal 6 karakter"
                        tvPassStrength.setTextColor(Color.parseColor("#64748B"))
                    }
                    newPass.length < 6 -> {
                        tvPassStrength.text = "Terlalu Pendek"
                        tvPassStrength.setTextColor(Color.parseColor("#C62828"))
                    }
                    newPass.length >= 8 && newPass.any { it.isDigit() } -> {
                        tvPassStrength.text = "Kuat"
                        tvPassStrength.setTextColor(Color.parseColor("#2E7D32"))
                    }
                    else -> {
                        tvPassStrength.text = "Cukup Baik"
                        tvPassStrength.setTextColor(Color.parseColor("#0B3C8C"))
                    }
                }

                // Indikator kecocokan sandi dengan icon vector bulat bersih
                if (confirmPass.isEmpty()) {
                    ivMatchNewPass.visibility = View.GONE
                } else {
                    ivMatchNewPass.visibility = View.VISIBLE
                    ivMatchNewPass.clearColorFilter()
                    if (newPass == confirmPass) {
                        ivMatchNewPass.setImageResource(R.drawable.ic_check_circle)
                    } else {
                        ivMatchNewPass.setImageResource(R.drawable.ic_error_circle)
                    }
                }
            }
        }

        etNewPassword.addTextChangedListener(watcher)
        etConfirmNewPassword.addTextChangedListener(watcher)

        btnSaveNewPassword.setOnClickListener {
            val currentPass = etCurrentPassword.text.toString()
            val newPass = etNewPassword.text.toString()
            val confirmPass = etConfirmNewPassword.text.toString()

            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Harap lengkapi semua kolom kata sandi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass.length < 6) {
                Toast.makeText(this, "Kata sandi baru minimal 6 karakter!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                Toast.makeText(this, "Konfirmasi kata sandi baru tidak cocok!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSaveNewPassword.isEnabled = false
            btnSaveNewPassword.text = "Memperbarui Kata Sandi..."

            val rawToken = sharedPreferences.getString("KEY_TOKEN", "") ?: ""
            val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"
            val changePasswordRequest = ChangePasswordRequest(
                currentPassword = currentPass,
                newPassword = newPass
            )

            ApiClient.instance.changePassword(token, changePasswordRequest)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        btnSaveNewPassword.isEnabled = true
                        btnSaveNewPassword.text = "Simpan Kata Sandi Baru"
                        if (response.isSuccessful) {
                            val rememberPrefs = getSharedPreferences("RememberMePrefs", MODE_PRIVATE)
                            if (rememberPrefs.getBoolean("KEY_REMEMBER_ME", false)) {
                                rememberPrefs.edit().putString("KEY_SAVED_PASSWORD", newPass).apply()
                            }
                            if (sharedPreferences.getBoolean("KEY_REMEMBER_ME", false)) {
                                sharedPreferences.edit().putString("KEY_SAVED_PASSWORD", newPass).apply()
                            }

                            Toast.makeText(this@KeamananAkunActivity, "Kata sandi berhasil diperbarui!", Toast.LENGTH_LONG).show()
                            finish()
                        } else {
                            val err = response.errorBody()?.string() ?: "Kata sandi saat ini salah"
                            Toast.makeText(this@KeamananAkunActivity, "Gagal: $err", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        btnSaveNewPassword.isEnabled = true
                        btnSaveNewPassword.text = "Simpan Kata Sandi Baru"
                        Toast.makeText(this@KeamananAkunActivity, "Koneksi ke server gagal", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun togglePasswordField(editText: EditText, visible: Boolean) {
        editText.inputType = if (visible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        editText.setSelection(editText.text.length)
    }
}
