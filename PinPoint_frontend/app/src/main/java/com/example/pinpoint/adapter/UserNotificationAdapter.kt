package com.example.pinpoint.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pinpoint.R
import com.example.pinpoint.network.NotifikasiResponse

class UserNotificationAdapter(
    private var items: List<NotifikasiResponse>,
    private val onItemClick: (NotifikasiResponse) -> Unit
) : RecyclerView.Adapter<UserNotificationAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutNotifIconBg: LinearLayout = itemView.findViewById(R.id.layoutNotifIconBg)
        val ivNotifIcon: ImageView = itemView.findViewById(R.id.ivNotifIcon)
        val tvNotifTitle: TextView = itemView.findViewById(R.id.tvNotifTitle)
        val tvNotifDate: TextView = itemView.findViewById(R.id.tvNotifDate)
        val tvNotifMessage: TextView = itemView.findViewById(R.id.tvNotifMessage)
        val tvNotifStageBadge: TextView = itemView.findViewById(R.id.tvNotifStageBadge)
        val btnNotifOpenDetail: TextView = itemView.findViewById(R.id.btnNotifOpenDetail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_notification_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val title = item.judul ?: "Pembaruan Laporan"
        val titleLower = title.lowercase()
        val tipeUpper = (item.tipe ?: "").uppercase()

        holder.tvNotifTitle.text = title
        holder.tvNotifMessage.text = item.pesan ?: "-"
        holder.tvNotifDate.text = item.tanggalDibuat?.take(16)?.replace("T", " ") ?: "Terkini"

        when {
            tipeUpper == "SELESAI" || tipeUpper == "SUCCESS" || titleLower.contains("selesai") -> {
                holder.layoutNotifIconBg.setBackgroundResource(R.drawable.bg_badge_diproses_mint)
                holder.ivNotifIcon.setImageResource(R.drawable.ic_check_circle)
                holder.ivNotifIcon.setColorFilter(Color.parseColor("#0F766E"))
                holder.tvNotifStageBadge.text = "Selesai Diperbaiki Pemerintah"
                holder.tvNotifStageBadge.setBackgroundResource(R.drawable.bg_badge_diproses_mint)
                holder.tvNotifStageBadge.setTextColor(Color.parseColor("#0F766E"))
            }
            tipeUpper == "VERIFIKASI" || titleLower.contains("diverifikasi") -> {
                holder.layoutNotifIconBg.setBackgroundResource(R.drawable.bg_chip_blue_light)
                holder.ivNotifIcon.setImageResource(R.drawable.verif)
                holder.ivNotifIcon.setColorFilter(Color.parseColor("#0B58CA"))
                holder.tvNotifStageBadge.text = "Diverifikasi Admin"
                holder.tvNotifStageBadge.setBackgroundResource(R.drawable.bg_badge_terverifikasi)
                holder.tvNotifStageBadge.setTextColor(Color.parseColor("#0B58CA"))
            }
            titleLower.contains("ditolak") || tipeUpper == "WARNING" -> {
                holder.layoutNotifIconBg.setBackgroundResource(R.drawable.bg_badge_ditolak)
                holder.ivNotifIcon.setImageResource(R.drawable.ic_info)
                holder.ivNotifIcon.setColorFilter(Color.parseColor("#B91C1C"))
                holder.tvNotifStageBadge.text = "Verifikasi Admin"
                holder.tvNotifStageBadge.setBackgroundResource(R.drawable.bg_badge_ditolak)
                holder.tvNotifStageBadge.setTextColor(Color.parseColor("#B91C1C"))
            }
            else -> {
                holder.layoutNotifIconBg.setBackgroundResource(R.drawable.bg_chip_blue_light)
                holder.ivNotifIcon.setImageResource(R.drawable.ic_bell)
                holder.ivNotifIcon.setColorFilter(Color.parseColor("#0284C7"))
                holder.tvNotifStageBadge.text = "Laporan Masuk"
                holder.tvNotifStageBadge.setBackgroundResource(R.drawable.bg_badge_selesai)
                holder.tvNotifStageBadge.setTextColor(Color.parseColor("#0284C7"))
            }
        }

        holder.btnNotifOpenDetail.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<NotifikasiResponse>) {
        items = newItems
        notifyDataSetChanged()
    }
}

