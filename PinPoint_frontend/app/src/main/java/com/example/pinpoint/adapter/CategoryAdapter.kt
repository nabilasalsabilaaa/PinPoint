package com.example.pinpoint.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pinpoint.R
import com.example.pinpoint.network.CategoryDto

class CategoryAdapter(
    private var categoryList: List<CategoryDto>,
    private val onEditClick: (CategoryDto) -> Unit,
    private val onDeleteClick: (CategoryDto) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        val tvCategoryCode: TextView = itemView.findViewById(R.id.tvCategoryIdCode)
        val tvCategoryStatusBadge: TextView = itemView.findViewById(R.id.tvCategoryStatusBadge)
        val tvCategoryTitle: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvCategoryDescription: TextView = itemView.findViewById(R.id.tvCategoryDescription)
        val tvReportCount: TextView = itemView.findViewById(R.id.tvCategoryActiveLoad)
        val btnEditCategory: View = itemView.findViewById(R.id.btnEditCategory)
        val btnDeleteCategory: View = itemView.findViewById(R.id.btnDeleteCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_card, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categoryList[position]

        val displayCode = category.code ?: category.id?.let { "KTG-MKS-${it.toString().padStart(2, '0')}" } ?: "-"
        holder.tvCategoryCode.text = "ID: $displayCode"
        holder.tvCategoryTitle.text = category.name ?: "Tanpa Nama"
        holder.tvCategoryDescription.text = category.description ?: "-"
        holder.tvReportCount.text = "${category.connectedReportsCount ?: 0} laporan"

        // Set Icon based on iconName
        val iconName = category.iconName?.lowercase() ?: "jalan"
        val iconResId = when (iconName) {
            "jalan" -> R.drawable.jalan
            "lamp", "lampu" -> R.drawable.lamp
            "drainage", "air" -> R.drawable.drainage
            "park", "taman" -> R.drawable.park
            "delete", "sampah" -> R.drawable.delete
            "walk", "trotoar" -> R.drawable.walk
            "bridge", "jembatan" -> R.drawable.bridge
            "bus_stop", "halte" -> R.drawable.bus_stop
            else -> android.R.drawable.ic_menu_manage
        }
        holder.ivCategoryIcon.setImageResource(iconResId)

        if (category.isActive == true) {
            holder.tvCategoryStatusBadge.text = "Aktif"
            holder.tvCategoryStatusBadge.setBackgroundResource(R.drawable.bg_badge_green_light)
        } else {
            holder.tvCategoryStatusBadge.text = "Nonaktif"
            holder.tvCategoryStatusBadge.setBackgroundResource(R.drawable.bg_badge_red_light)
        }

        holder.btnEditCategory.setOnClickListener { onEditClick(category) }
        holder.btnDeleteCategory.setOnClickListener { onDeleteClick(category) }
    }

    override fun getItemCount(): Int = categoryList.size

    fun updateData(newCategories: List<CategoryDto>) {
        this.categoryList = newCategories
        notifyDataSetChanged()
    }
}
