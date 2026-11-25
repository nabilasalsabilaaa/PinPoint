package com.example.pinpoint.admin

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.example.pinpoint.R
import com.example.pinpoint.pemerintah.PemerintahDashboardActivity
import com.example.pinpoint.pemerintah.PemerintahHistoryActivity
import com.example.pinpoint.pemerintah.PemerintahMapActivity
import com.example.pinpoint.pemerintah.PemerintahProfileActivity
import com.example.pinpoint.user.DashboardActivity
import com.example.pinpoint.user.FormPelaporanActivity
import com.example.pinpoint.user.NotifikasiActivity
import com.example.pinpoint.user.ProfilActivity
import com.example.pinpoint.user.RiwayatActivity

object NavbarUtils {

    private const val COLOR_ACTIVE_TEXT = "#0B58CA"
    private const val COLOR_INACTIVE = "#94A3B8"

    private fun getAdminTabIndex(activity: Activity): Int = when (activity) {
        is AdminDashboardActivity -> 0
        is KategoriActivity -> 1
        is VerifikasiLaporanActivity -> 2
        is KelolaAkunActivity -> 3
        is AdminProfilActivity -> 4
        else -> 0
    }

    private fun getGovTabIndex(activity: Activity): Int = when (activity) {
        is PemerintahDashboardActivity -> 0
        is PemerintahMapActivity -> 1
        is PemerintahHistoryActivity -> 2
        is PemerintahProfileActivity -> 3
        else -> 0
    }

    private fun getUserTabIndex(activity: Activity): Int = when (activity) {
        is DashboardActivity -> 0
        is RiwayatActivity -> 1
        is NotifikasiActivity -> 2
        is ProfilActivity -> 3
        else -> 0
    }

    private fun applySlideTransition(activity: Activity, fromIndex: Int, toIndex: Int) {
        if (toIndex >= fromIndex) {
            activity.overridePendingTransition(R.anim.fade_in_smooth, R.anim.fade_out_smooth)
        } else {
            activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    fun setupAdminNavbar(activity: Activity) {
        val navHome: View = activity.findViewById(R.id.navHome)
        val navCategory: View = activity.findViewById(R.id.navCategory)
        val navAccount: View = activity.findViewById(R.id.navAccount)
        val navProfile: View = activity.findViewById(R.id.navProfile)
        val navVerify: View = activity.findViewById(R.id.navVerifyCenter)

        val currentIdx = getAdminTabIndex(activity)
        highlightActiveAdminTab(activity)

        navHome.setOnClickListener {
            if (activity !is AdminDashboardActivity) {
                val intent = Intent(activity, AdminDashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                applySlideTransition(activity, currentIdx, 0)
            }
        }

        navCategory.setOnClickListener {
            if (activity !is KategoriActivity) {
                val intent = Intent(activity, KategoriActivity::class.java)
                activity.startActivity(intent)
                applySlideTransition(activity, currentIdx, 1)
            }
        }

        navVerify.setOnClickListener {
            if (activity !is VerifikasiLaporanActivity) {
                val intent = Intent(activity, VerifikasiLaporanActivity::class.java)
                activity.startActivity(intent)
                applySlideTransition(activity, currentIdx, 2)
            }
        }

        navAccount.setOnClickListener {
            if (activity !is KelolaAkunActivity) {
                val intent = Intent(activity, KelolaAkunActivity::class.java)
                activity.startActivity(intent)
                applySlideTransition(activity, currentIdx, 3)
            }
        }

        navProfile.setOnClickListener {
            if (activity !is AdminProfilActivity) {
                val intent = Intent(activity, AdminProfilActivity::class.java)
                activity.startActivity(intent)
                applySlideTransition(activity, currentIdx, 4)
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

        val currentIdx = getGovTabIndex(activity)

        setTabState(ivHome, tvHome, false)
        setTabState(ivMap, tvMap, false)
        setTabState(ivHistory, tvHistory, false)
        setTabState(ivProfile, tvProfile, false)

        when (activity) {
            is PemerintahDashboardActivity -> setTabState(ivHome, tvHome, true)
            is PemerintahMapActivity -> setTabState(ivMap, tvMap, true)
            is PemerintahHistoryActivity -> setTabState(ivHistory, tvHistory, true)
            is PemerintahProfileActivity -> setTabState(ivProfile, tvProfile, true)
        }

        navHome?.setOnClickListener {
            if (activity !is PemerintahDashboardActivity) {
                val intent = Intent(activity, PemerintahDashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                applySlideTransition(activity, currentIdx, 0)
                activity.finish()
            }
        }

        navMap?.setOnClickListener {
            if (activity !is PemerintahMapActivity) {
                val intent = Intent(activity, PemerintahMapActivity::class.java)
                activity.startActivity(intent)
                applySlideTransition(activity, currentIdx, 1)
                activity.finish()
            }
        }

        navHistory?.setOnClickListener {
            if (activity !is PemerintahHistoryActivity) {
                val intent = Intent(activity, PemerintahHistoryActivity::class.java)
                activity.startActivity(intent)
                applySlideTransition(activity, currentIdx, 2)
                activity.finish()
            }
        }

        navProfile?.setOnClickListener {
            if (activity !is PemerintahProfileActivity) {
                val intent = Intent(activity, PemerintahProfileActivity::class.java)
                activity.startActivity(intent)
                applySlideTransition(activity, currentIdx, 3)
                activity.finish()
            }
        }
    }

    fun setupUserNavbar(activity: Activity) {
        val navHome = activity.findViewById<View?>(R.id.navUserHome)
        val navHistory = activity.findViewById<View?>(R.id.navUserHistory)
        val navReportCenter = activity.findViewById<View?>(R.id.navUserReportCenter)
        val navNotif = activity.findViewById<View?>(R.id.navUserNotification)
        val navProfile = activity.findViewById<View?>(R.id.navUserProfile)

        val ivHome = activity.findViewById<ImageView?>(R.id.ivNavUserHome)
        val tvHome = activity.findViewById<TextView?>(R.id.tvNavUserHome)
        val ivHistory = activity.findViewById<ImageView?>(R.id.ivNavUserHistory)
        val tvHistory = activity.findViewById<TextView?>(R.id.tvNavUserHistory)
        val ivNotif = activity.findViewById<ImageView?>(R.id.ivNavUserNotification)
        val tvNotif = activity.findViewById<TextView?>(R.id.tvNavUserNotification)
        val ivProfile = activity.findViewById<ImageView?>(R.id.ivNavUserProfile)
        val tvProfile = activity.findViewById<TextView?>(R.id.tvNavUserProfile)

        val currentIdx = getUserTabIndex(activity)

        setTabState(ivHome, tvHome, false)
        setTabState(ivHistory, tvHistory, false)
        setTabState(ivNotif, tvNotif, false)
        setTabState(ivProfile, tvProfile, false)

        when (activity) {
            is DashboardActivity -> setTabState(ivHome, tvHome, true)
            is RiwayatActivity -> setTabState(ivHistory, tvHistory, true)
            is NotifikasiActivity -> setTabState(ivNotif, tvNotif, true)
            is ProfilActivity -> setTabState(ivProfile, tvProfile, true)
        }

        navHome?.setOnClickListener {
            if (activity !is DashboardActivity) {
                val intent = Intent(activity, DashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                applySlideTransition(activity, currentIdx, 0)
                activity.finish()
            }
        }

        navHistory?.setOnClickListener {
            if (activity !is RiwayatActivity) {
                val intent = Intent(activity, RiwayatActivity::class.java)
                activity.startActivity(intent)
                applySlideTransition(activity, currentIdx, 1)
                if (activity !is DashboardActivity) activity.finish()
            }
        }

        navReportCenter?.setOnClickListener {
            val intent = Intent(activity, FormPelaporanActivity::class.java)
            activity.startActivity(intent)
            activity.overridePendingTransition(R.anim.fade_in_smooth, R.anim.fade_out_smooth)
        }

        navNotif?.setOnClickListener {
            if (activity !is NotifikasiActivity) {
                val intent = Intent(activity, NotifikasiActivity::class.java)
                activity.startActivity(intent)
                applySlideTransition(activity, currentIdx, 2)
                if (activity !is DashboardActivity) activity.finish()
            }
        }

        navProfile?.setOnClickListener {
            if (activity !is ProfilActivity) {
                val intent = Intent(activity, ProfilActivity::class.java)
                activity.startActivity(intent)
                applySlideTransition(activity, currentIdx, 3)
                if (activity !is DashboardActivity) activity.finish()
            }
        }
    }

    private fun highlightActiveAdminTab(activity: Activity) {
        val ivHome = activity.findViewById<ImageView>(R.id.ivNavHome)
        val tvHome = activity.findViewById<TextView>(R.id.tvNavHome)
        val ivCategory = activity.findViewById<ImageView>(R.id.ivNavCategory)
        val tvCategory = activity.findViewById<TextView>(R.id.tvNavCategory)
        val ivAccount = activity.findViewById<ImageView>(R.id.ivNavAccount)
        val tvAccount = activity.findViewById<TextView>(R.id.tvNavAccount)
        val ivProfile = activity.findViewById<ImageView>(R.id.ivNavProfile)
        val tvProfile = activity.findViewById<TextView>(R.id.tvNavProfile)
        val tvVerify = activity.findViewById<TextView>(R.id.tvNavVerify)

        setTabState(ivHome, tvHome, false)
        setTabState(ivCategory, tvCategory, false)
        setTabState(ivAccount, tvAccount, false)
        setTabState(ivProfile, tvProfile, false)
        tvVerify?.setTextColor(Color.parseColor(COLOR_INACTIVE))

        when (activity) {
            is AdminDashboardActivity -> setTabState(ivHome, tvHome, true)
            is KategoriActivity -> setTabState(ivCategory, tvCategory, true)
            is KelolaAkunActivity -> setTabState(ivAccount, tvAccount, true)
            is AdminProfilActivity -> setTabState(ivProfile, tvProfile, true)
            is VerifikasiLaporanActivity -> {
                tvVerify?.setTextColor(Color.parseColor(COLOR_ACTIVE_TEXT))
                tvVerify?.setTypeface(null, Typeface.BOLD)
            }
        }
    }

    private fun setTabState(icon: ImageView?, text: TextView?, isActive: Boolean) {
        if (isActive) {
            icon?.setBackgroundResource(R.drawable.bg_nav_icon_active_blue)
            icon?.imageTintList = ColorStateList.valueOf(Color.WHITE)
            icon?.scaleX = 0.86f
            icon?.scaleY = 0.86f
            icon?.animate()
                ?.scaleX(1f)
                ?.scaleY(1f)
                ?.setDuration(200)
                ?.setInterpolator(DecelerateInterpolator())
                ?.start()

            text?.setTextColor(Color.parseColor(COLOR_ACTIVE_TEXT))
            text?.setTypeface(null, Typeface.BOLD)
        } else {
            icon?.setBackgroundResource(0)
            icon?.imageTintList = ColorStateList.valueOf(Color.parseColor(COLOR_INACTIVE))
            icon?.scaleX = 1f
            icon?.scaleY = 1f

            text?.setTextColor(Color.parseColor(COLOR_INACTIVE))
            text?.setTypeface(null, Typeface.NORMAL)
        }
    }
}
