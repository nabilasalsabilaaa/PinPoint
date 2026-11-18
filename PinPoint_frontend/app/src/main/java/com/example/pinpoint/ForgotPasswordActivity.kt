package com.example.pinpoint

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.CheckAccountRequest
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.json.JSONObject

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnSubmit: Button
    private lateinit var btnBackToLogin: View
    private lateinit var adapter: ForgotPasswordPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        viewPager = findViewById(R.id.viewPagerForgotPassword)
        tabLayout = findViewById(R.id.tabLayoutForgotPassword)
        btnSubmit = findViewById(R.id.btnSubmitForgotPassword)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)

        adapter = ForgotPasswordPagerAdapter()
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            if (position == 0) {
                tab.text = "  Email"
                tab.setIcon(R.drawable.mail)
            } else {
                tab.text = "  WhatsApp"
                tab.setIcon(R.drawable.call)
            }
        }.attach()

        btnSubmit.setOnClickListener {
            val currentTab = viewPager.currentItem
            val currentInput = adapter.getInputValue(currentTab).trim()
            if (currentInput.isEmpty()) {
                val label = if (currentTab == 0) "alamat email" else "nomor WhatsApp/telepon"
                Toast.makeText(this, "Harap masukkan $label Anda!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val reqType = if (currentTab == 0) "EMAIL" else "PHONE"
            btnSubmit.isEnabled = false
            btnSubmit.text = "Memeriksa Akun & Mengirim OTP..."

            ApiClient.fetchWithFallback(
                callFactory = { api -> api.checkAccount(CheckAccountRequest(currentInput, reqType)) },
                onResponse = { _, response ->
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Kirim Kode OTP (6 Digit)"

                    if (response.isSuccessful) {
                        val rawBody = response.body()?.string() ?: ""
                        var targetEmail = currentInput
                        try {
                            val json = JSONObject(rawBody)
                            if (json.has("email")) {
                                targetEmail = json.getString("email")
                            }
                        } catch (_: Exception) {}

                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            "Kode OTP 6 digit telah dikirim ke $targetEmail",
                            Toast.LENGTH_LONG
                        ).show()

                        val intent = Intent(this@ForgotPasswordActivity, OtpVerificationActivity::class.java).apply {
                            putExtra("EXTRA_MODE", "FORGOT_PASSWORD")
                            putExtra("EXTRA_EMAIL", targetEmail)
                            putExtra("EXTRA_IDENTIFIER", currentInput)
                            putExtra("EXTRA_TYPE", reqType)
                        }
                        startActivity(intent)
                    } else {
                        val errMsg = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                            ?: "Akun tidak ditemukan atau email tidak aktif!"
                        Toast.makeText(this@ForgotPasswordActivity, errMsg, Toast.LENGTH_LONG).show()
                    }
                },
                onFailure = { _, t ->
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Kirim Kode OTP (6 Digit)"
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Gagal terhubung ke server (${t.localizedMessage})",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }

        btnBackToLogin.setOnClickListener { finish() }
    }

    class ForgotPasswordPagerAdapter : RecyclerView.Adapter<ForgotPasswordPagerAdapter.PageViewHolder>() {
        private val holders = mutableMapOf<Int, PageViewHolder>()

        class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvLabel: TextView = view.findViewById(R.id.tvInputLabel)
            val etInput: EditText = view.findViewById(R.id.etInput)
            val tvHint: TextView = view.findViewById(R.id.tvInfoHint)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_forgot_password_page, parent, false)
            return PageViewHolder(view)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            holders[position] = holder
            if (position == 0) {
                holder.tvLabel.text = "Alamat Email Terdaftar"
                holder.etInput.hint = "contohemail@gmail.com"
                holder.etInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                holder.tvHint.text = "Pastikan email Anda aktif. Kode OTP 6 digit akan dikirimkan melalui noreplay.pinpoint@gmail.com."
            } else {
                holder.tvLabel.text = "Nomor Telepon / WhatsApp Terdaftar"
                holder.etInput.hint = "081234567890"
                holder.etInput.inputType = InputType.TYPE_CLASS_PHONE
                holder.tvHint.text = "Sistem akan mendeteksi email aktif yang terhubung dengan nomor telepon akun Anda dan mengirimkan kode OTP 6 digit."
            }
        }

        override fun getItemCount(): Int = 2

        fun getInputValue(position: Int): String {
            return holders[position]?.etInput?.text?.toString() ?: ""
        }
    }
}
