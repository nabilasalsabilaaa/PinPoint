package com.example.pinpoint.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pinpoint.R

class BantuanActivity : AppCompatActivity() {

    private data class FaqItem(
        val header: LinearLayout,
        val answer: TextView,
        val arrow: ImageView
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bantuan)

        val btnBack = findViewById<ImageView>(R.id.btnBackBantuan)
        val tvEmail = findViewById<TextView>(R.id.tvSupportEmail)
        val btnCallCenterPhone = findViewById<Button>(R.id.btnCallCenterPhone)
        val btnCallCenterWa = findViewById<Button>(R.id.btnCallCenterWa)

        btnBack.setOnClickListener { finish() }

        val faqItems = listOf(
            FaqItem(findViewById(R.id.faqHeader1), findViewById(R.id.tvFaqAnswer1), findViewById(R.id.ivFaqArrow1)),
            FaqItem(findViewById(R.id.faqHeader2), findViewById(R.id.tvFaqAnswer2), findViewById(R.id.ivFaqArrow2)),
            FaqItem(findViewById(R.id.faqHeader3), findViewById(R.id.tvFaqAnswer3), findViewById(R.id.ivFaqArrow3)),
            FaqItem(findViewById(R.id.faqHeader4), findViewById(R.id.tvFaqAnswer4), findViewById(R.id.ivFaqArrow4)),
            FaqItem(findViewById(R.id.faqHeader5), findViewById(R.id.tvFaqAnswer5), findViewById(R.id.ivFaqArrow5))
        )

        // Set initial arrow rotation
        faqItems.forEach { item ->
            item.arrow.rotation = if (item.answer.visibility == View.VISIBLE) 180f else 0f
            item.header.setOnClickListener {
                val isCurrentlyVisible = item.answer.visibility == View.VISIBLE
                if (isCurrentlyVisible) {
                    item.answer.visibility = View.GONE
                    item.arrow.animate().rotation(0f).setDuration(200).start()
                } else {
                    item.answer.visibility = View.VISIBLE
                    item.arrow.animate().rotation(180f).setDuration(200).start()
                }
            }
        }

        btnCallCenterPhone.setOnClickListener {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:08114110112"))
            startActivity(dialIntent)
        }

        btnCallCenterWa.setOnClickListener {
            val waUrl = "https://wa.me/628114110112?text=" + Uri.encode("Halo Helpdesk PinPoint, saya membutuhkan bantuan terkait aplikasi Admin.")
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)))
            } catch (e: Exception) {
                Toast.makeText(this, "Tidak dapat membuka WhatsApp", Toast.LENGTH_SHORT).show()
            }
        }

        tvEmail.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:noreplay.pinpoint@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "Bantuan Sistem PinPoint Admin")
            }
            try {
                startActivity(Intent.createChooser(emailIntent, "Kirim email bantuan..."))
            } catch (e: Exception) {
                Toast.makeText(this, "Aplikasi email tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
