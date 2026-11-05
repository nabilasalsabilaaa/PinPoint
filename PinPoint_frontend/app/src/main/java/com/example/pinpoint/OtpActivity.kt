package com.example.pinpoint

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText

/**
 * OtpActivity — Layar verifikasi kode OTP 6-digit.
 *
 * Menerima extras dari ForgotPasswordActivity:
 *   - EXTRA_EMAIL: email tujuan OTP
 *   - EXTRA_OTP_TYPE: "EMAIL" atau "PHONE"
 *
 * Fitur:
 * - 6 EditText terpisah dengan auto-advance (otomatis pindah ke kotak berikutnya)
 * - Auto-delete ke kotak sebelumnya saat Backspace
 * - Timer countdown 15 menit
 * - Tombol "Kirim Ulang" aktif setelah timer habis
 * - Navigasi ke layar Reset Password setelah OTP valid
 */
class OtpActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EMAIL    = "extra_email"
        const val EXTRA_OTP_TYPE = "extra_otp_type"
        const val EXTRA_SOURCE   = "extra_source"  // "REGISTER" or "FORGOT_PASSWORD"
    }

    private lateinit var tvOtpEmail: TextView
    private lateinit var tvOtpTimer: TextView
    private lateinit var btnVerifyOtp: MaterialButton
    private lateinit var btnResendOtp: MaterialButton
    private lateinit var btnBackToLogin: MaterialButton
    private lateinit var progressBar: CircularProgressIndicator

    private lateinit var otpFields: List<TextInputEditText>
    private lateinit var authRepository: AuthRepository

    private var email: String = ""
    private var otpType: String = "EMAIL"
    private var source: String = "FORGOT_PASSWORD"
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        email   = intent.getStringExtra(EXTRA_EMAIL) ?: ""
        otpType = intent.getStringExtra(EXTRA_OTP_TYPE) ?: "EMAIL"
        source  = intent.getStringExtra(EXTRA_SOURCE) ?: "FORGOT_PASSWORD"
        authRepository = AuthRepository(this)

        initViews()
        startTimer()
        setupOtpAutoAdvance()
        setupClickListeners()
    }

    private fun initViews() {
        tvOtpEmail    = findViewById(R.id.tvOtpEmail)
        tvOtpTimer    = findViewById(R.id.tvOtpTimer)
        btnVerifyOtp  = findViewById(R.id.btnVerifyOtp)
        btnResendOtp  = findViewById(R.id.btnResendOtp)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)
        progressBar    = findViewById(R.id.progressBarOtp)

        otpFields = listOf(
            findViewById(R.id.etOtp1),
            findViewById(R.id.etOtp2),
            findViewById(R.id.etOtp3),
            findViewById(R.id.etOtp4),
            findViewById(R.id.etOtp5),
            findViewById(R.id.etOtp6)
        )

        // Tampilkan email tujuan OTP
        tvOtpEmail.text = email
        // Fokus ke kotak pertama
        otpFields[0].requestFocus()
    }

    // ══════════════════════════════════════════════════════════════════════
    //  OTP AUTO-ADVANCE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Mengatur TextWatcher pada setiap kotak OTP:
     * - Ketik 1 digit → pindah otomatis ke kotak berikutnya
     * - Hapus (backspace saat kosong) → kembali ke kotak sebelumnya
     */
    private fun setupOtpAutoAdvance() {
        otpFields.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && index < otpFields.size - 1) {
                        // Pindah ke kotak berikutnya
                        otpFields[index + 1].requestFocus()
                    }
                    if (s?.isEmpty() == true && index > 0) {
                        // Kembali ke kotak sebelumnya
                        otpFields[index - 1].requestFocus()
                    }
                }
            })
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  TIMER COUNTDOWN 15 MENIT
    // ══════════════════════════════════════════════════════════════════════

    private fun startTimer() {
        btnResendOtp.isEnabled = false
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(15 * 60 * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                tvOtpTimer.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                tvOtpTimer.text = "00:00"
                btnResendOtp.isEnabled = true
            }
        }.start()
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CLICK LISTENERS
    // ══════════════════════════════════════════════════════════════════════

    private fun setupClickListeners() {

        // ── Tombol Verifikasi ────────────────────────────────────────────
        btnVerifyOtp.setOnClickListener {
            val otpCode = otpFields.joinToString("") { it.text.toString() }

            if (otpCode.length < 6 || otpCode.any { !it.isDigit() }) {
                Toast.makeText(this, "Masukkan 6 digit kode OTP dengan benar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(true)

            authRepository.verifyOtp(
                email   = email,
                otpCode = otpCode,
                onSuccess = {
                    setLoading(false)
                    if (source == "REGISTER") {
                        // Dari registrasi → OTP terverifikasi → ke Login
                        Toast.makeText(this, "Email berhasil diverifikasi! Silakan login.", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, Login::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    } else {
                        // Dari lupa password → ke Reset Password
                        val intent = Intent(this, ResetPasswordActivity::class.java).apply {
                            putExtra(ResetPasswordActivity.EXTRA_EMAIL, email)
                        }
                        startActivity(intent)
                    }
                    finish()
                },
                onError = { errorMsg ->
                    setLoading(false)
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                    // Bersihkan semua kotak OTP
                    otpFields.forEach { it.setText("") }
                    otpFields[0].requestFocus()
                }
            )
        }

        // ── Kirim Ulang OTP ─────────────────────────────────────────────
        btnResendOtp.setOnClickListener {
            setLoading(true)
            authRepository.sendOtp(
                identifier = email,
                type       = otpType,
                onSuccess  = { message ->
                    setLoading(false)
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    // Restart timer
                    otpFields.forEach { it.setText("") }
                    otpFields[0].requestFocus()
                    startTimer()
                },
                onError = { errorMsg ->
                    setLoading(false)
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                }
            )
        }

        // ── Kembali ke Login ─────────────────────────────────────────────
        btnBackToLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility  = if (isLoading) View.VISIBLE else View.GONE
        btnVerifyOtp.isEnabled  = !isLoading
        btnVerifyOtp.text       = if (isLoading) "Memverifikasi..." else "Verifikasi"
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
