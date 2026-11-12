package com.example.pinpoint.adapter

import android.content.Intent
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
import com.example.pinpoint.R
import com.example.pinpoint.network.ReportDto

class ReportAdapter(
    private var reports: List<ReportDto> = emptyList(),
    private val onItemClick: (ReportDto) -> Unit = {},
    private val onVerifyClick: (ReportDto) -> Unit = {},
    private val onRejectClick: (ReportDto) -> Unit = {}
) : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvReportIdCode: TextView = itemView.findViewById(R.id.tvReportIdCode)
        val tvReportStatusBadge: TextView = itemView.findViewById(R.id.tvReportStatusBadge)
        val tvReportReporterAndDate: TextView = itemView.findViewById(R.id.tvReportReporterAndDate)
        val tvReportCategoryBadge: TextView = itemView.findViewById(R.id.tvReportCategoryBadge)
        val tvReportTitle: TextView = itemView.findViewById(R.id.tvReportTitle)
        val tvReportDescriptionSnippet: TextView = itemView.findViewById(R.id.tvReportDescriptionSnippet)
        val ivReportThumbnail: ImageView = itemView.findViewById(R.id.ivReportThumbnail)
        val tvReportLocation: TextView = itemView.findViewById(R.id.tvReportLocation)
        val tvReportCoordinates: TextView = itemView.findViewById(R.id.tvReportCoordinates)
        val btnOpenMap: TextView = itemView.findViewById(R.id.btnOpenMap)
        val llReportActions: LinearLayout = itemView.findViewById(R.id.llReportActions)
        val btnRejectReport: Button = itemView.findViewById(R.id.btnRejectReport)
        val btnVerifyReport: Button = itemView.findViewById(R.id.btnVerifyReport)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_card, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]

        holder.tvReportIdCode.text = report.resolvedCode()

        val statusKey = report.resolvedStatusKey()
        when (statusKey) {
            "MENUNGGU_VERIFIKASI" -> {
                holder.tvReportStatusBadge.text = "● Menunggu Verifikasi"
                holder.tvReportStatusBadge.setBackgroundResource(R.drawable.bg_badge_orange_light)
                holder.tvReportStatusBadge.setTextColor(Color.parseColor("#D97706"))
                holder.llReportActions.visibility = View.VISIBLE
            }
            "TERVERIFIKASI" -> {
                holder.tvReportStatusBadge.text = "✓ Terverifikasi"
                holder.tvReportStatusBadge.setBackgroundResource(R.drawable.bg_badge_green_light)
                holder.tvReportStatusBadge.setTextColor(Color.parseColor("#00897B"))
                holder.llReportActions.visibility = View.GONE
            }
            "DIPROSES" -> {
                holder.tvReportStatusBadge.text = "● Diproses Dinas"
                holder.tvReportStatusBadge.setBackgroundResource(R.drawable.bg_chip_blue_light)
                holder.tvReportStatusBadge.setTextColor(Color.parseColor("#0B3C8C"))
                holder.llReportActions.visibility = View.GONE
            }
            "SELESAI" -> {
                holder.tvReportStatusBadge.text = "✓ Selesai"
                holder.tvReportStatusBadge.setBackgroundResource(R.drawable.bg_badge_green_light)
                holder.tvReportStatusBadge.setTextColor(Color.parseColor("#166534"))
                holder.llReportActions.visibility = View.GONE
            }
            else -> {
                holder.tvReportStatusBadge.text = "✕ Ditolak"
                holder.tvReportStatusBadge.setBackgroundResource(R.drawable.bg_badge_red_light)
                holder.tvReportStatusBadge.setTextColor(Color.parseColor("#D32F2F"))
                holder.llReportActions.visibility = View.GONE
            }
        }

        val reporter = report.resolvedReporterName()
        val dateStr = report.resolvedCreatedAt()
        holder.tvReportReporterAndDate.text = "Pelapor: $reporter • $dateStr"
        holder.tvReportCategoryBadge.text = report.resolvedCategoryName()
        holder.tvReportTitle.text = "${report.resolvedCategoryName()} - ${report.resolvedTitle()}"
        holder.tvReportDescriptionSnippet.text = report.resolvedDescription()
        holder.tvReportLocation.text = report.resolvedLocationName()
        holder.tvReportCoordinates.text = String.format("%.4f, %.4f", report.latitude ?: -5.1476, report.longitude ?: 119.4327)

        val photoUrl = report.resolvedPhotoUrl()
        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(photoUrl)
                .centerCrop()
                .placeholder(R.drawable.sampul)
                .error(R.drawable.sampul)
                .into(holder.ivReportThumbnail)
        } else {
            holder.ivReportThumbnail.setImageResource(R.drawable.sampul)
        }

        holder.btnOpenMap.setOnClickListener {
            val lat = report.latitude ?: -5.147665
            val lng = report.longitude ?: 119.432732
            val label = Uri.encode(report.resolvedTitle())
            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
            try {
                holder.itemView.context.startActivity(mapIntent)
            } catch (e: Exception) {
                onItemClick(report)
            }
        }

        holder.btnVerifyReport.setOnClickListener { onVerifyClick(report) }
        holder.btnRejectReport.setOnClickListener { onRejectClick(report) }
        holder.itemView.setOnClickListener { onItemClick(report) }
        holder.tvReportTitle.setOnClickListener { onItemClick(report) }
        holder.tvReportDescriptionSnippet.setOnClickListener { onItemClick(report) }
        holder.ivReportThumbnail.setOnClickListener { onItemClick(report) }
    }

    override fun getItemCount(): Int = reports.size

    fun updateData(newReports: List<ReportDto>) {
        this.reports = newReports
        notifyDataSetChanged()
    }
}
