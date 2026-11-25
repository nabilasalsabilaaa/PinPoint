package com.example.pinpoint.user

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pinpoint.R
import com.example.pinpoint.adapter.UserNotificationAdapter
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.NotifikasiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotifikasiActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var tvEmptyNotifications: TextView
    private lateinit var notifAdapter: UserNotificationAdapter

    private lateinit var chipAll: TextView
    private lateinit var chipMasuk: TextView
    private lateinit var chipVerifSelesai: TextView

    private var allNotifications = listOf<NotifikasiResponse>()
    private var currentFilter = "ALL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_notifikasi)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainNotifikasi)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        rvNotifications = findViewById(R.id.rvUserNotifications)
        tvEmptyNotifications = findViewById(R.id.tvEmptyNotifications)
        chipAll = findViewById(R.id.chipNotifAll)
        chipMasuk = findViewById(R.id.chipNotifStatus)
        chipVerifSelesai = findViewById(R.id.chipNotifFollowup)

        rvNotifications.layoutManager = LinearLayoutManager(this)
        notifAdapter = UserNotificationAdapter(emptyList()) { notif ->
            val intent = Intent(this, DetailLaporanActivity::class.java).apply {
                notif.idLaporan?.let { putExtra("EXTRA_REPORT_ID", it) }
            }
            startActivity(intent)
        }
        rvNotifications.adapter = notifAdapter

        setupFilterChips()
        setupBottomNavigation()
        loadNotificationsFromDatabase()
    }

    override fun onResume() {
        super.onResume()
        loadNotificationsFromDatabase()
    }

    private fun setupFilterChips() {
        val chips = listOf(chipAll, chipMasuk, chipVerifSelesai)

        fun selectChip(active: TextView) {
            for (c in chips) {
                if (c == active) {
                    c.setBackgroundResource(R.drawable.bg_chip_blue_active)
                    c.setTextColor(Color.WHITE)
                    c.setTypeface(null, Typeface.BOLD)
                } else {
                    c.setBackgroundResource(R.drawable.bg_chip_inactive)
                    c.setTextColor(Color.parseColor("#334155"))
                    c.setTypeface(null, Typeface.NORMAL)
                }
            }
        }

        chipAll.setOnClickListener {
            currentFilter = "ALL"
            selectChip(chipAll)
            applyNotificationFilter()
        }

        chipMasuk.setOnClickListener {
            currentFilter = "MASUK"
            selectChip(chipMasuk)
            applyNotificationFilter()
        }

        chipVerifSelesai.setOnClickListener {
            currentFilter = "VERIF_SELESAI"
            selectChip(chipVerifSelesai)
            applyNotificationFilter()
        }
    }

    private fun getReadNotifPrefs() = getSharedPreferences("UserNotificationReadPrefs", MODE_PRIVATE)

    private fun getCurrentUserKey(): String {
        val session = getSharedPreferences("UserSession", MODE_PRIVATE)
        return (session.getString("KEY_EMAIL", "guest") ?: "guest").lowercase()
    }

    private fun buildNotifKey(notif: NotifikasiResponse): String {
        return "${getCurrentUserKey()}_${notif.id ?: 0L}_${notif.idLaporan ?: 0L}_${notif.tipe ?: ""}_${notif.judul ?: ""}"
    }

    private fun markAllLoadedNotificationsAsRead(token: String, notifications: List<NotifikasiResponse>) {
        val editor = getReadNotifPrefs().edit()
        notifications.forEach { notif ->
            editor.putBoolean(buildNotifKey(notif), true)
            if (notif.sudahDibaca == false && notif.id != null) {
                ApiClient.instance.markNotificationRead("Bearer $token", notif.id)
                    .enqueue(object : Callback<okhttp3.ResponseBody> {
                        override fun onResponse(
                            call: Call<okhttp3.ResponseBody>,
                            response: Response<okhttp3.ResponseBody>
                        ) {}
                        override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {}
                    })
            }
        }
        editor.apply()
    }

    private fun loadNotificationsFromDatabase() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val token = sharedPreferences.getString("KEY_TOKEN", null)
        if (token.isNullOrEmpty()) return

        ApiClient.instance.getNotifications("Bearer $token")
            .enqueue(object : Callback<List<NotifikasiResponse>> {
                override fun onResponse(
                    call: Call<List<NotifikasiResponse>>,
                    response: Response<List<NotifikasiResponse>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        allNotifications = response.body()!!
                        val masukCount = allNotifications.count { isMasukNotif(it) }
                        val verifSelesaiCount = allNotifications.count { !isMasukNotif(it) }

                        chipAll.text = "Semua (${allNotifications.size})"
                        chipMasuk.text = "Laporan Masuk ($masukCount)"
                        chipVerifSelesai.text = "Verifikasi & Selesai ($verifSelesaiCount)"

                        // Saat pengguna membuka halaman Notifikasi, tandai seluruh notifikasi sudah dibaca
                        // sehingga tanda dot merah langsung hilang.
                        markAllLoadedNotificationsAsRead(token, allNotifications)

                        val tvNavBadge = findViewById<TextView?>(R.id.tvNavBadgeCount)
                        tvNavBadge?.visibility = View.GONE

                        applyNotificationFilter()
                    }
                }

                override fun onFailure(call: Call<List<NotifikasiResponse>>, t: Throwable) {}
            })
    }

    private fun isMasukNotif(notif: NotifikasiResponse): Boolean {
        val t = (notif.tipe ?: "").uppercase()
        val j = (notif.judul ?: "").lowercase()
        return t == "MASUK" || j.contains("masuk")
    }

    private fun applyNotificationFilter() {
        val filtered = when (currentFilter) {
            "MASUK" -> allNotifications.filter { isMasukNotif(it) }
            "VERIF_SELESAI" -> allNotifications.filter { !isMasukNotif(it) }
            else -> allNotifications
        }
        notifAdapter.updateData(filtered)
        if (filtered.isEmpty()) {
            tvEmptyNotifications.visibility = View.VISIBLE
            rvNotifications.visibility = View.GONE
        } else {
            tvEmptyNotifications.visibility = View.GONE
            rvNotifications.visibility = View.VISIBLE
        }
    }

    private fun setupBottomNavigation() {
        com.example.pinpoint.admin.NavbarUtils.setupUserNavbar(this)
    }
}
