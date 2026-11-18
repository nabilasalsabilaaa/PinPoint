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

class PemerintahHistoryAdapter(
    private var historyList: List<PemerintahHistoryItem>,
    private val onDetailClick: (PemerintahHistoryItem) -> Unit
) : RecyclerView.Adapter<PemerintahHistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHistoryCode: TextView = itemView.findViewById(R.id.tvHistoryCode)
        val tvHistoryFinishDate: TextView = itemView.findViewById(R.id.tvHistoryFinishDate)
        val tvHistoryStatusBadge: TextView = itemView.findViewById(R.id.tvHistoryStatusBadge)
        val tvHistoryTitle: TextView = itemView.findViewById(R.id.tvHistoryTitle)
        val tvHistoryLocation: TextView = itemView.findViewById(R.id.tvHistoryLocation)
        val tvHistorySectionHeader: TextView = itemView.findViewById(R.id.tvHistorySectionHeader)
        val tvHistorySectionSub: TextView = itemView.findViewById(R.id.tvHistorySectionSub)
        val ivBefore: ImageView = itemView.findViewById(R.id.ivBefore)
        val tvBeforeLabel: TextView = itemView.findViewById(R.id.tvBeforeLabel)
        val ivAfter: ImageView = itemView.findViewById(R.id.ivAfter)
        val tvAfterLabel: TextView = itemView.findViewById(R.id.tvAfterLabel)
        val tvHistoryNotes: TextView = itemView.findViewById(R.id.tvHistoryNotes)
        val tvHistoryTeam: TextView = itemView.findViewById(R.id.tvHistoryTeam)
        val btnDetailDokumentasi: View = itemView.findViewById(R.id.btnDetailDokumentasi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pemerintah_history_card, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]
        val context = holder.itemView.context

        holder.tvHistoryCode.text = item.id
        holder.tvHistoryFinishDate.text = item.finishDate
        holder.tvHistoryTitle.text = item.title
        holder.tvHistoryLocation.text = item.location
        holder.tvHistorySectionHeader.text = item.sectionHeader
        holder.tvHistorySectionSub.text = item.sectionSubHeader
        holder.tvBeforeLabel.text = item.beforeLabel
        holder.tvAfterLabel.text = item.afterLabel
        holder.tvHistoryNotes.text = item.workNotes
        holder.tvHistoryTeam.text = "Kategori: ${item.categoryName}"

        holder.tvHistoryStatusBadge.text = "Selesai"
        holder.tvHistoryStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_green_light)
        holder.tvHistoryStatusBadge.setTextColor(Color.parseColor("#00A389"))

        if (item.beforeImageUrl.isNotEmpty()) {
            Glide.with(context)
                .load(item.beforeImageUrl)
                .placeholder(R.drawable.sampul)
                .error(R.drawable.sampul)
                .into(holder.ivBefore)
        } else {
            holder.ivBefore.setImageResource(R.drawable.sampul)
        }

        if (item.afterImageUrl.isNotEmpty()) {
            Glide.with(context)
                .load(item.afterImageUrl)
                .placeholder(R.drawable.sampul)
                .error(R.drawable.sampul)
                .into(holder.ivAfter)
        } else {
            holder.ivAfter.setImageResource(R.drawable.sampul)
        }

        holder.ivBefore.setOnClickListener {
            if (item.allPhotos.isNotEmpty()) {
                PhotoSliderDialog.show(context, "Dokumentasi Sebelum & Sesudah", item.allPhotos, 0)
            } else {
                onDetailClick(item)
            }
        }

        holder.ivAfter.setOnClickListener {
            if (item.allPhotos.isNotEmpty()) {
                val startIdx = if (item.allPhotos.size > 1) 1 else 0
                PhotoSliderDialog.show(context, "Dokumentasi Sebelum & Sesudah", item.allPhotos, startIdx)
            } else {
                onDetailClick(item)
            }
        }

        holder.btnDetailDokumentasi.setOnClickListener { onDetailClick(item) }
        holder.itemView.setOnClickListener { onDetailClick(item) }
    }

    override fun getItemCount(): Int = historyList.size

    fun updateList(newList: List<PemerintahHistoryItem>) {
        historyList = newList
        notifyDataSetChanged()
    }
}
