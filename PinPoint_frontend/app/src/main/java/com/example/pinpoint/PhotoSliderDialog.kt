package com.example.pinpoint

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide

object PhotoSliderDialog {

    fun show(
        context: Context,
        title: String,
        photoUrls: List<String>,
        startIndex: Int = 0
    ) {
        val validPhotos = photoUrls.filter { it.isNotBlank() }
        if (validPhotos.isEmpty()) return

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_photo_slider)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvTitle = dialog.findViewById<TextView>(R.id.tvSliderTitle)
        val tvCounter = dialog.findViewById<TextView>(R.id.tvSliderCounter)
        val btnClose = dialog.findViewById<TextView>(R.id.btnCloseSlider)
        val btnPrev = dialog.findViewById<TextView>(R.id.btnPrevPhoto)
        val btnNext = dialog.findViewById<TextView>(R.id.btnNextPhoto)
        val vpSlider = dialog.findViewById<ViewPager2>(R.id.vpPhotoSlider)

        tvTitle.text = title

        vpSlider.adapter = object : RecyclerView.Adapter< PhotoViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
                val imageView = ImageView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
                return PhotoViewHolder(imageView)
            }

            override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
                val url = validPhotos[position]
                Glide.with(holder.imageView.context)
                    .load(url)
                    .placeholder(R.drawable.sampul)
                    .error(R.drawable.sampul)
                    .into(holder.imageView)
            }

            override fun getItemCount(): Int = validPhotos.size
        }

        fun updateCounter(pos: Int) {
            tvCounter.text = "${pos + 1} / ${validPhotos.size}"
            btnPrev.alpha = if (pos > 0) 1.0f else 0.45f
            btnNext.alpha = if (pos < validPhotos.size - 1) 1.0f else 0.45f
        }

        vpSlider.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateCounter(position)
            }
        })

        val safeStart = startIndex.coerceIn(0, validPhotos.size - 1)
        vpSlider.setCurrentItem(safeStart, false)
        updateCounter(safeStart)

        btnPrev.setOnClickListener {
            val current = vpSlider.currentItem
            if (current > 0) {
                vpSlider.currentItem = current - 1
            }
        }

        btnNext.setOnClickListener {
            val current = vpSlider.currentItem
            if (current < validPhotos.size - 1) {
                vpSlider.currentItem = current + 1
            }
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private class PhotoViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)
}

