package com.example.pinpoint.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.pinpoint.R
import com.example.pinpoint.network.UserAccountDto
import java.io.File

class AccountAdapter(
    private var accountList: List<UserAccountDto>,
    private val onActionLeftClick: (UserAccountDto) -> Unit,
    private val onActionRightClick: (UserAccountDto) -> Unit,
    private val onMoreOptionsClick: (UserAccountDto) -> Unit = {}
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAvatarLetter: TextView = itemView.findViewById(R.id.tvAvatarLetter)
        val ivAvatarPhoto: ImageView = itemView.findViewById(R.id.ivAvatarPhoto)
        val tvAccountName: TextView = itemView.findViewById(R.id.tvAccountName)
        val tvAccountRoleBadge: TextView = itemView.findViewById(R.id.tvAccountRoleBadge)
        val btnAccountMoreOption: ImageView = itemView.findViewById(R.id.btnAccountMoreOption)
        val tvAccountEmail: TextView = itemView.findViewById(R.id.tvAccountEmail)
        val tvAccountPhone: TextView = itemView.findViewById(R.id.tvAccountPhone)
        val containerSuspendInfo: LinearLayout = itemView.findViewById(R.id.containerSuspendInfo)
        val tvSuspendReason: TextView = itemView.findViewById(R.id.tvSuspendReason)
        val tvJoinDate: TextView = itemView.findViewById(R.id.tvJoinDate)
        val tvReportCountBadge: TextView = itemView.findViewById(R.id.tvReportCountBadge)
        val tvStatusActiveBadge: TextView = itemView.findViewById(R.id.tvStatusActiveBadge)
        val btnActionLeft: Button = itemView.findViewById(R.id.btnActionLeft)
        val btnActionRight: Button = itemView.findViewById(R.id.btnActionRight)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_account_card, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = accountList[position]
        val context = holder.itemView.context

        // Hilangkan ikon titik tiga pada card akun
        holder.btnAccountMoreOption.visibility = View.GONE

        // Nama & Huruf Inisial Default
        val displayName = account.resolvedFullName()
        holder.tvAccountName.text = displayName
        holder.tvAvatarLetter.text = if (displayName.isNotBlank()) displayName.take(1).uppercase() else "?"

        // Sinkronisasi Foto Profil pada Card Akun
        val emailKey = (account.email ?: "").trim().lowercase()
        val resolvedPhoto = resolveAccountPhoto(context, emailKey, account.photoUrl)
        if (!resolvedPhoto.isNullOrBlank()) {
            holder.tvAvatarLetter.visibility = View.GONE
            holder.ivAvatarPhoto.visibility = View.VISIBLE
            Glide.with(context)
                .load(resolvedPhoto)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .error(R.drawable.user)
                .into(holder.ivAvatarPhoto)
        } else {
            holder.ivAvatarPhoto.visibility = View.GONE
            holder.tvAvatarLetter.visibility = View.VISIBLE
        }

        // Role Badge (1 = Admin, 2 = User, 3 = Pemerintah)
        val resolvedRole = account.resolvedRole()
        holder.tvAccountRoleBadge.text = when (resolvedRole) {
            "ADMIN" -> "Admin (Super Admin)"
            "PEMERINTAH" -> "Admin Pemerintahan"
            else -> "Warga / User"
        }

        // Kontak
        holder.tvAccountEmail.text = account.email?.takeIf { it.isNotBlank() } ?: "-"
        holder.tvAccountPhone.text = account.resolvedPhone()

        // Logika Status, Tandai Spam & Hapus Akun
        val isSuspended = account.isSuspended == true ||
                account.statusAkun.equals("SUSPENDED", ignoreCase = true) ||
                account.statusAkun.equals("BLOCKED", ignoreCase = true)

        holder.btnActionLeft.visibility = View.VISIBLE
        holder.btnActionRight.visibility = View.VISIBLE

        if (isSuspended) {
            holder.containerSuspendInfo.visibility = View.VISIBLE
            holder.tvSuspendReason.text = "Alasan: " + (account.suspendReason ?: "Akun ditandai sebagai spam oleh Admin.")
            holder.tvStatusActiveBadge.text = "DITANGGUHKAN"
            holder.tvStatusActiveBadge.setBackgroundResource(R.drawable.bg_badge_red_light)
            holder.tvStatusActiveBadge.setTextColor(Color.parseColor("#D32F2F"))

            holder.btnActionLeft.text = "Buka Spam"
            holder.btnActionLeft.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0B3C8C"))
            holder.btnActionLeft.setTextColor(Color.WHITE)

            holder.btnActionRight.text = "Hapus Akun"
            holder.btnActionRight.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
            holder.btnActionRight.setTextColor(Color.parseColor("#D32F2F"))
        } else {
            holder.containerSuspendInfo.visibility = View.GONE
            holder.tvStatusActiveBadge.text = "AKTIF"
            holder.tvStatusActiveBadge.setBackgroundResource(R.drawable.bg_badge_green_light)
            holder.tvStatusActiveBadge.setTextColor(Color.parseColor("#166534"))

            holder.btnActionLeft.text = "Tandai Spam"
            holder.btnActionLeft.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
            holder.btnActionLeft.setTextColor(Color.parseColor("#B45309"))

            holder.btnActionRight.text = "Hapus Akun"
            holder.btnActionRight.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
            holder.btnActionRight.setTextColor(Color.parseColor("#D32F2F"))
        }

        holder.tvJoinDate.text = "Bergabung: " + (account.createdAt ?: "-")
        holder.tvReportCountBadge.text = "${account.reportCount ?: 0} Laporan Terkirim"

        // Listeners
        holder.btnActionLeft.setOnClickListener { onActionLeftClick(account) }
        holder.btnActionRight.setOnClickListener { onActionRightClick(account) }
    }

    private fun resolveAccountPhoto(context: Context, emailKey: String, serverPhotoUrl: String?): String? {
        val permanentPrefs = context.getSharedPreferences("PermanentUserPrefs", Context.MODE_PRIVATE)
        val sessionPrefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val sessionEmail = (sessionPrefs.getString("KEY_EMAIL", "") ?: "").trim().lowercase()

        if (emailKey.isNotBlank()) {
            val savedByEmail = permanentPrefs.getString("PROFILE_IMAGE_$emailKey", null)
            if (!savedByEmail.isNullOrBlank()) return savedByEmail

            val safeEmail = emailKey.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val localFile = File(context.filesDir, "profile_picture_$safeEmail.jpg")
            if (localFile.exists()) return Uri.fromFile(localFile).toString()

            if (emailKey == sessionEmail) {
                val sessionPhoto = sessionPrefs.getString("KEY_PROFILE_IMAGE", null)
                    ?: permanentPrefs.getString("PROFILE_IMAGE_LAST", null)
                if (!sessionPhoto.isNullOrBlank()) return sessionPhoto
            }
        }
        return serverPhotoUrl?.takeIf { it.isNotBlank() }
    }

    override fun getItemCount(): Int = accountList.size

    fun updateData(newAccounts: List<UserAccountDto>) {
        this.accountList = newAccounts
        notifyDataSetChanged()
    }
}
