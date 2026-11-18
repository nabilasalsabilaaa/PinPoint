package com.example.pinpoint.admin

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.pinpoint.PemerintahDashboardActivity
import com.example.pinpoint.PemerintahHistoryActivity
import com.example.pinpoint.PemerintahMapActivity
import com.example.pinpoint.PemerintahProfileActivity
import com.example.pinpoint.R

object NavbarUtils {

    private const val COLOR_ACTIVE = "#0B3C8C"
    private const val COLOR_INACTIVE = "#94A3B8"

    fun setupAdminNavbar(activity: Activity) {
        val navHome: View = activity.findViewById(R.id.navHome)
        val navCategory: View = activity.findViewById(R.id.navCategory)
        val navAccount: View = activity.findViewById(R.id.navAccount)
        val navProfile: View = activity.findViewById(R.id.navProfile)
        val navVerify: View = activity.findViewById(R.id.navVerifyCenter)

        highlightActiveTab(activity)

        navHome.setOnClickListener {
            if (activity !is AdminDashboardActivity) {
                val intent = Intent(activity, AdminDashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        navCategory.setOnClickListener {
            if (activity !is KategoriActivity) {
                val intent = Intent(activity, KategoriActivity::class.java)
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        navAccount.setOnClickListener {
            if (activity !is KelolaAkunActivity) {
                val intent = Intent(activity, KelolaAkunActivity::class.java)
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        navProfile.setOnClickListener {
            if (activity !is AdminProfilActivity) {
                val intent = Intent(activity, AdminProfilActivity::class.java)
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        navVerify.setOnClickListener {
            if (activity !is VerifikasiLaporanActivity) {
                val intent = Intent(activity, VerifikasiLaporanActivity::class.java)
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }
    }

    fun setupPemerintahNavbar(activity: Activity) {
        val navHome: View? = activity.findViewById(R.id.navPemerintahHome)
        val navMap: View? = activity.findViewById(R.id.navPemerintahMap)
        val navHistory: View? = activity.findViewById(R.id.navPemerintahHistory)
        val navProfile: View? = activity.findViewById(R.id.navPemerintahProfile)

        val ivHome = activity.findViewById<ImageView>(R.id.ivNavPemerintahHome)
        val tvHome = activity.findViewById<TextView>(R.id.tvNavPemerintahHome)
        val ivMap = activity.findViewById<ImageView>(R.id.ivNavPemerintahMap)
        val tvMap = activity.findViewById<TextView>(R.id.tvNavPemerintahMap)
        val ivHistory = activity.findViewById<ImageView>(R.id.ivNavPemerintahHistory)
        val tvHistory = activity.findViewById<TextView>(R.id.tvNavPemerintahHistory)
        val ivProfile = activity.findViewById<ImageView>(R.id.ivNavPemerintahProfile)
        val tvProfile = activity.findViewById<TextView>(R.id.tvNavPemerintahProfile)

        setTabColor(ivHome, tvHome, COLOR_INACTIVE)
        setTabColor(ivMap, tvMap, COLOR_INACTIVE)
        setTabColor(ivHistory, tvHistory, COLOR_INACTIVE)
        setTabColor(ivProfile, tvProfile, COLOR_INACTIVE)

        when (activity) {
            is PemerintahDashboardActivity -> setTabColor(ivHome, tvHome, COLOR_ACTIVE)
            is PemerintahMapActivity -> setTabColor(ivMap, tvMap, COLOR_ACTIVE)
            is PemerintahHistoryActivity -> setTabColor(ivHistory, tvHistory, COLOR_ACTIVE)
            is PemerintahProfileActivity -> setTabColor(ivProfile, tvProfile, COLOR_ACTIVE)
        }

        navHome?.setOnClickListener {
            if (activity !is PemerintahDashboardActivity) {
                val intent = Intent(activity, PemerintahDashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
                activity.finish()
            }
        }

        navMap?.setOnClickListener {
            if (activity !is PemerintahMapActivity) {
                val intent = Intent(activity, PemerintahMapActivity::class.java)
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
                activity.finish()
            }
        }

        navHistory?.setOnClickListener {
            if (activity !is PemerintahHistoryActivity) {
                val intent = Intent(activity, PemerintahHistoryActivity::class.java)
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
                activity.finish()
            }
        }

        navProfile?.setOnClickListener {
            if (activity !is PemerintahProfileActivity) {
                val intent = Intent(activity, PemerintahProfileActivity::class.java)
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
                activity.finish()
            }
        }
    }

    private fun highlightActiveTab(activity: Activity) {
        val ivHome = activity.findViewById<ImageView>(R.id.ivNavHome)
        val tvHome = activity.findViewById<TextView>(R.id.tvNavHome)
        val ivCategory = activity.findViewById<ImageView>(R.id.ivNavCategory)
        val tvCategory = activity.findViewById<TextView>(R.id.tvNavCategory)
        val ivAccount = activity.findViewById<ImageView>(R.id.ivNavAccount)
        val tvAccount = activity.findViewById<TextView>(R.id.tvNavAccount)
        val ivProfile = activity.findViewById<ImageView>(R.id.ivNavProfile)
        val tvProfile = activity.findViewById<TextView>(R.id.tvNavProfile)
        val tvVerify = activity.findViewById<TextView>(R.id.tvNavVerify)

        setTabColor(ivHome, tvHome, COLOR_INACTIVE)
        setTabColor(ivCategory, tvCategory, COLOR_INACTIVE)
        setTabColor(ivAccount, tvAccount, COLOR_INACTIVE)
        setTabColor(ivProfile, tvProfile, COLOR_INACTIVE)
        tvVerify?.setTextColor(Color.parseColor(COLOR_INACTIVE))

        when (activity) {
            is AdminDashboardActivity -> setTabColor(ivHome, tvHome, COLOR_ACTIVE)
            is KategoriActivity -> setTabColor(ivCategory, tvCategory, COLOR_ACTIVE)
            is KelolaAkunActivity -> setTabColor(ivAccount, tvAccount, COLOR_ACTIVE)
            is AdminProfilActivity -> setTabColor(ivProfile, tvProfile, COLOR_ACTIVE)
            is VerifikasiLaporanActivity -> {
                tvVerify?.setTextColor(Color.parseColor(COLOR_ACTIVE))
                tvVerify?.setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }
    }

    private fun setTabColor(icon: ImageView?, text: TextView?, colorHex: String) {
        val color = Color.parseColor(colorHex)
        icon?.imageTintList = ColorStateList.valueOf(color)
        text?.setTextColor(color)

        if (colorHex == COLOR_ACTIVE) {
            text?.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            text?.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }
}
