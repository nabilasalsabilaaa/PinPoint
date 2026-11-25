package com.example.pinpoint.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pinpoint.PhotoSliderDialog
import com.example.pinpoint.R
import com.example.pinpoint.network.LaporanResponse

class UserReportAdapter(
    private var reports: List<LaporanResponse>,
    private val getSupportCount: (LaporanResponse) -> Int,
    private val isSupportedByMe: (LaporanResponse) -> Boolean,
    private val onSupportToggle: (LaporanResponse) -> Unit,
    private val onDetailClick: (LaporanResponse) -> Unit
) : RecyclerView.Adapter<UserReportAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCardCategory: TextView = itemView.findViewById(R.id.tvCardCategory)
        val tvCardStatus: TextView = itemView.findViewById(R.id.tvCardStatus)
        val tvCardDate: TextView = itemView.findViewById(R.id.tvCardDate)
        val layoutCardPhoto: View = itemView.findViewById(R.id.layoutCardPhoto)
        val ivCardReportPhoto: ImageView = itemView.findViewById(R.id.ivCardReportPhoto)
        val tvCardPhotoCount: TextView = itemView.findViewById(R.id.tvCardPhotoCount)
        val tvCardTitle: TextView = itemView.findViewById(R.id.tvCardTitle)
        val tvCardLocation: TextView = itemView.findViewById(R.id.tvCardLocation)
        val tvCardSupportCount: TextView = itemView.findViewById(R.id.tvCardSupportCount)
        val btnCardSupport: LinearLayout = itemView.findViewById(R.id.btnCardSupport)
        val ivBtnSupportThumb: ImageView = itemView.findViewById(R.id.ivBtnSupportThumb)
        val tvBtnSupportText: TextView = itemView.findViewById(R.id.tvBtnSupportText)
        val btnCardViewDetail: LinearLayout = itemView.findViewById(R.id.btnCardViewDetail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_report_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = reports[position]
        val context = holder.itemView.context

        holder.tvCardCategory.text = report.resolvedCategoryName()
        holder.tvCardDate.text = report.resolvedCreatedAt()
        holder.tvCardTitle.text = report.resolvedTitle()
        holder.tvCardLocation.text = report.resolvedLocation()

        // Status pengerjaan dari database (tanpa emoji)
        when (report.idStatus) {
            2L -> {
                holder.tvCardStatus.text = "Terverifikasi"
                holder.tvCardStatus.setBackgroundResource(R.drawable.bg_badge_terverifikasi)
                holder.tvCardStatus.setTextColor(Color.parseColor("#0B58CA"))
            }
            3L -> {
                holder.tvCardStatus.text = "Ditolak"
                holder.tvCardStatus.setBackgroundResource(R.drawable.bg_badge_ditolak)
                holder.tvCardStatus.setTextColor(Color.parseColor("#B91C1C"))
            }
            4L -> {
                holder.tvCardStatus.text = "Sedang Diproses"
                holder.tvCardStatus.setBackgroundResource(R.drawable.bg_badge_diproses_mint)
                holder.tvCardStatus.setTextColor(Color.parseColor("#0F766E"))
            }
            5L -> {
                holder.tvCardStatus.text = "Selesai"
                holder.tvCardStatus.setBackgroundResource(R.drawable.bg_badge_selesai)
                holder.tvCardStatus.setTextColor(Color.parseColor("#059669"))
            }
            else -> {
                holder.tvCardStatus.text = "Menunggu Verifikasi"
                holder.tvCardStatus.setBackgroundResource(R.drawable.bg_badge_selesai)
                holder.tvCardStatus.setTextColor(Color.parseColor("#0284C7"))
            }
        }

        // Foto laporan dari user
        val photos = report.resolvedPhotoUrls()
        holder.tvCardPhotoCount.text = if (photos.isNotEmpty()) photos.size.toString() else "1"
        val mainPhoto = report.resolvedPhotoUrl()
        if (!mainPhoto.isNullOrBlank()) {
            Glide.with(context)
                .load(mainPhoto)
                .placeholder(R.drawable.sampul)
                .error(R.drawable.sampul)
                .into(holder.ivCardReportPhoto)
        } else {
            holder.ivCardReportPhoto.setImageResource(R.drawable.sampul)
        }

        holder.layoutCardPhoto.setOnClickListener {
            if (photos.isNotEmpty()) {
                PhotoSliderDialog.show(
                    context,
                    "Foto Laporan (${photos.size} Foto)",
                    photos,
                    0
                )
            } else {
                onDetailClick(report)
            }
        }

        // Dukungan Warga
        val supportCount = getSupportCount(report)
        val supported = isSupportedByMe(report)
        holder.tvCardSupportCount.text = "$supportCount Warga Mendukung"

        if (supported) {
            holder.btnCardSupport.setBackgroundResource(R.drawable.bg_btn_rounded_blue)
            holder.tvBtnSupportText.text = "Didukung"
            holder.tvBtnSupportText.setTextColor(Color.WHITE)
            holder.ivBtnSupportThumb.setColorFilter(Color.WHITE)
        } else {
            holder.btnCardSupport.setBackgroundResource(R.drawable.bg_btn_rounded_blue_soft)
            holder.tvBtnSupportText.text = "Dukung"
            holder.tvBtnSupportText.setTextColor(Color.parseColor("#0B58CA"))
            holder.ivBtnSupportThumb.setColorFilter(Color.parseColor("#0B58CA"))
        }

        holder.btnCardSupport.setOnClickListener {
            onSupportToggle(report)
        }

        holder.btnCardViewDetail.setOnClickListener {
            onDetailClick(report)
        }

        holder.itemView.setOnClickListener {
            onDetailClick(report)
        }
    }

    override fun getItemCount(): Int = reports.size

    fun updateData(newReports: List<LaporanResponse>) {
        reports = newReports
        notifyDataSetChanged()
    }
}

