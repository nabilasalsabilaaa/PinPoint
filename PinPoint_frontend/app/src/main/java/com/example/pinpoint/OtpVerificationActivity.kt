package com.example.pinpoint

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.CheckAccountRequest
import com.example.pinpoint.network.RegisterRequest
import com.example.pinpoint.network.VerifyOtpRequest

class OtpVerificationActivity : AppCompatActivity() {

    private lateinit var otpBoxes: List<EditText>
    private lateinit var tvOtpTitle: TextView
    private lateinit var tvOtpSubtitle: TextView
    private lateinit var tvOtpEmailTarget: TextView
    private lateinit var tvOtpTimer: TextView
    private lateinit var btnVerifyOtp: Button
    private lateinit var tvResendOtp: TextView

    private var mode: String = "FORGOT_PASSWORD"
    private var email: String = ""
    private var countDownTimer: CountDownTimer? = null
    private var isExpired: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)

        mode = intent.getStringExtra("EXTRA_MODE") ?: "FORGOT_PASSWORD"
        email = intent.getStringExtra("EXTRA_EMAIL") ?: ""

        val btnBack = findViewById<ImageView>(R.id.btnBackOtp)
        tvOtpTitle = findViewById(R.id.tvOtpTitle)
        tvOtpSubtitle = findViewById(R.id.tvOtpSubtitle)
        tvOtpEmailTarget = findViewById(R.id.tvOtpEmailTarget)
        tvOtpTimer = findViewById(R.id.tvOtpTimer)
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp)
        tvResendOtp = findViewById(R.id.tvResendOtp)

        otpBoxes = listOf(
            findViewById(R.id.etOtp1),
            findViewById(R.id.etOtp2),
            findViewById(R.id.etOtp3),
            findViewById(R.id.etOtp4),
            findViewById(R.id.etOtp5),
            findViewById(R.id.etOtp6)
        )

        tvOtpEmailTarget.text = email

        if (mode == "REGISTER") {
            tvOtpTitle.text = "Verifikasi Email Pendaftaran"
            tvOtpSubtitle.text = "Masukkan 6 digit kode OTP yang dikirim dari noreplay.pinpoint@gmail.com untuk mengaktifkan akun baru Anda:"
            btnVerifyOtp.text = "Verifikasi & Buat Akun"
        } else {
            tvOtpTitle.text = "Verifikasi Reset Kata Sandi"
            tvOtpSubtitle.text = "Masukkan 6 digit kode OTP yang dikirim dari noreplay.pinpoint@gmail.com untuk mengatur ulang kata sandi:"
            btnVerifyOtp.text = "Verifikasi Kode OTP"
        }

        setupOtpInputs()
        startFifteenMinuteCountdown()

        btnBack.setOnClickListener { finish() }

        btnVerifyOtp.setOnClickListener {
            verifyOtpCode()
        }

        tvResendOtp.setOnClickListener {
            resendOtpCode()
        }
    }

    private fun setupOtpInputs() {
        for (i in otpBoxes.indices) {
            otpBoxes[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s != null && s.length == 1 && i < otpBoxes.size - 1) {
                        otpBoxes[i + 1].requestFocus()
                    }
                }
            })

            otpBoxes[i].setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (otpBoxes[i].text.isEmpty() && i > 0) {
                        otpBoxes[i - 1].requestFocus()
                        otpBoxes[i - 1].setText("")
                        return@setOnKeyListener true
                    }
                }
                false
            }
        }
        otpBoxes[0].requestFocus()
    }

    private fun startFifteenMinuteCountdown() {
        countDownTimer?.cancel()
        isExpired = false
        val fifteenMinutesMs = 15 * 60 * 1000L

        countDownTimer = object : CountDownTimer(fifteenMinutesMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val totalSeconds = millisUntilFinished / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                tvOtpTimer.text = String.format("Masa berlaku OTP: %02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                isExpired = true
                tvOtpTimer.text = "Kode OTP telah kedaluwarsa (lebih dari 15 menit)!"
            }
        }.start()
    }

    private fun getEnteredOtp(): String {
        return otpBoxes.joinToString("") { it.text.toString().trim() }
    }

    private fun verifyOtpCode() {
        if (isExpired) {
            Toast.makeText(this, "Kode OTP sudah lewat 15 menit! Silakan klik Kirim Ulang.", Toast.LENGTH_LONG).show()
            return
        }

        val otpCode = getEnteredOtp()
        if (otpCode.length < 6) {
            Toast.makeText(this, "Harap masukkan lengkap 6 digit kode OTP!", Toast.LENGTH_SHORT).show()
            return
        }

        btnVerifyOtp.isEnabled = false
        val originalText = btnVerifyOtp.text.toString()
        btnVerifyOtp.text = "Memverifikasi OTP..."

        val request = VerifyOtpRequest(email = email, otp = otpCode, mode = mode)

        if (mode == "REGISTER") {
            ApiClient.fetchWithFallback(
                callFactory = { api -> api.verifyRegisterOtp(request) },
                onResponse = { _, response ->
                    btnVerifyOtp.isEnabled = true
                    btnVerifyOtp.text = originalText
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@OtpVerificationActivity,
                            "Verifikasi OTP cocok! Akun Anda berhasil dibuat. Silakan login.",
                            Toast.LENGTH_LONG
                        ).show()
                        val intent = Intent(this@OtpVerificationActivity, Login::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        val errMsg = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                            ?: "Kode OTP salah! Gagal membuat akun."
                        Toast.makeText(this@OtpVerificationActivity, errMsg, Toast.LENGTH_LONG).show()
                    }
                },
                onFailure = { _, t ->
                    btnVerifyOtp.isEnabled = true
                    btnVerifyOtp.text = originalText
                    Toast.makeText(
                        this@OtpVerificationActivity,
                        "Gagal menghubungi server (${t.localizedMessage})",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        } else {
            ApiClient.fetchWithFallback(
                callFactory = { api -> api.verifyOtp(request) },
                onResponse = { _, response ->
                    btnVerifyOtp.isEnabled = true
                    btnVerifyOtp.text = originalText
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@OtpVerificationActivity,
                            "Kode OTP terverifikasi! Silakan buat kata sandi baru.",
                            Toast.LENGTH_SHORT
                        ).show()
                        val intent = Intent(this@OtpVerificationActivity, ResetPasswordNewActivity::class.java).apply {
                            putExtra("EXTRA_EMAIL", email)
                            putExtra("EXTRA_OTP", otpCode)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        val errMsg = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                            ?: "Kode OTP salah! Tidak dapat mereset kata sandi."
                        Toast.makeText(this@OtpVerificationActivity, errMsg, Toast.LENGTH_LONG).show()
                    }
                },
                onFailure = { _, t ->
                    btnVerifyOtp.isEnabled = true
                    btnVerifyOtp.text = originalText
                    Toast.makeText(
                        this@OtpVerificationActivity,
                        "Gagal menghubungi server (${t.localizedMessage})",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun resendOtpCode() {
        tvResendOtp.isEnabled = false
        Toast.makeText(this, "Mengirim ulang kode OTP ke $email...", Toast.LENGTH_SHORT).show()

        if (mode == "REGISTER") {
            val name = intent.getStringExtra("EXTRA_REG_NAME") ?: ""
            val phone = intent.getStringExtra("EXTRA_REG_PHONE") ?: ""
            val password = intent.getStringExtra("EXTRA_REG_PASSWORD") ?: ""
            ApiClient.fetchWithFallback(
                callFactory = { api -> api.requestRegisterOtp(RegisterRequest(name, email, phone, password)) },
                onResponse = { _, response ->
                    tvResendOtp.isEnabled = true
                    if (response.isSuccessful) {
                        otpBoxes.forEach { it.setText("") }
                        otpBoxes[0].requestFocus()
                        startFifteenMinuteCountdown()
                        Toast.makeText(this@OtpVerificationActivity, "Kode OTP baru telah dikirim ke $email", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@OtpVerificationActivity, "Gagal mengirim ulang OTP", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { _, _ ->
                    tvResendOtp.isEnabled = true
                    Toast.makeText(this@OtpVerificationActivity, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            val identifier = intent.getStringExtra("EXTRA_IDENTIFIER") ?: email
            val type = intent.getStringExtra("EXTRA_TYPE") ?: "EMAIL"
            ApiClient.fetchWithFallback(
                callFactory = { api -> api.checkAccount(CheckAccountRequest(identifier, type)) },
                onResponse = { _, response ->
                    tvResendOtp.isEnabled = true
                    if (response.isSuccessful) {
                        otpBoxes.forEach { it.setText("") }
                        otpBoxes[0].requestFocus()
                        startFifteenMinuteCountdown()
                        Toast.makeText(this@OtpVerificationActivity, "Kode OTP baru telah dikirim ke $email", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@OtpVerificationActivity, "Gagal mengirim ulang OTP", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { _, _ ->
                    tvResendOtp.isEnabled = true
                    Toast.makeText(this@OtpVerificationActivity, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}

