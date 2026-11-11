package com.example.pinpoint

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PemerintahReportAdapter(
    private var reportList: List<PemerintahReport>,
    private val onItemClick: (PemerintahReport) -> Unit
) : RecyclerView.Adapter<PemerintahReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvReportCode: TextView = itemView.findViewById(R.id.tvReportCode)
        val tvReportDate: TextView = itemView.findViewById(R.id.tvReportDate)
        val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvReportTitle: TextView = itemView.findViewById(R.id.tvReportTitle)
        val ivReportThumbnail: ImageView = itemView.findViewById(R.id.ivReportThumbnail)
        val tvReportLocation: TextView = itemView.findViewById(R.id.tvReportLocation)
        val tvFooterTag: TextView = itemView.findViewById(R.id.tvFooterTag)
        val btnDetailAndAction: View = itemView.findViewById(R.id.btnDetailAndAction)
        val tvBtnActionText: TextView = itemView.findViewById(R.id.tvBtnActionText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pemerintah_report_card, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val item = reportList[position]
        val context = holder.itemView.context

        holder.tvReportCode.text = item.id
        holder.tvReportDate.text = item.date
        holder.tvCategoryName.text = item.category
        holder.tvReportTitle.text = item.title
        holder.tvReportLocation.text = item.location
        holder.tvFooterTag.text = item.footerTag
        holder.tvBtnActionText.text = item.actionText

        // Configure Status Badge
        when (item.status.uppercase()) {
            "SEDANG_DIPROSES" -> {
                holder.tvStatusBadge.text = "● Sedang Diproses"
                holder.tvStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_purple_light)
                holder.tvStatusBadge.setTextColor(Color.parseColor("#613EEA"))
            }
            "SELESAI" -> {
                holder.tvStatusBadge.text = "● Selesai"
                holder.tvStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_green_light)
                holder.tvStatusBadge.setTextColor(Color.parseColor("#00A389"))
            }
            else -> { // TERVERIFIKASI
                holder.tvStatusBadge.text = "● Terverifikasi"
                holder.tvStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_blue_light)
                holder.tvStatusBadge.setTextColor(Color.parseColor("#003C90"))
            }
        }

        // Configure Action Button style
        if (item.isPrimaryButton) {
            holder.btnDetailAndAction.background = ContextCompat.getDrawable(context, R.drawable.bg_btn_primary)
            holder.tvBtnActionText.setTextColor(Color.WHITE)
        } else {
            holder.btnDetailAndAction.background = ContextCompat.getDrawable(context, R.drawable.bg_btn_secondary)
            holder.tvBtnActionText.setTextColor(Color.parseColor("#003C90"))
        }

        // Load Thumbnail with Glide
        if (item.imageUrl.isNotEmpty()) {
            Glide.with(context)
                .load(item.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivReportThumbnail)
        }

        // On Click Listener
        holder.btnDetailAndAction.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = reportList.size

    fun updateList(newList: List<PemerintahReport>) {
        reportList = newList
        notifyDataSetChanged()
    }
}
