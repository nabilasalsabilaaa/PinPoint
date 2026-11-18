package com.example.pinpoint.admin

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.pinpoint.R
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.CategoryDto
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddKategoriActivity : AppCompatActivity() {

    private lateinit var btnBackAddCategory: ImageView
    private lateinit var etCategoryCode: EditText
    private lateinit var etCategoryName: EditText
    private lateinit var etCategoryDesc: EditText
    private lateinit var btnSaveCategory: Button
    private lateinit var btnCancelCategory: Button
    private lateinit var tvSelectedIconLabel: TextView

    private var selectedIconName: String = "jalan"
    private lateinit var iconCards: List<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_kategori)

        initViews()
        setupIconSelectors()
        fetchNextCategoryCode()

        btnBackAddCategory.setOnClickListener { finish() }
        btnCancelCategory.setOnClickListener { finish() }
        btnSaveCategory.setOnClickListener { createCategory() }
    }

    private fun initViews() {
        btnBackAddCategory = findViewById(R.id.btnBackAddCategory)
        etCategoryCode = findViewById(R.id.etCategoryCode)
        etCategoryName = findViewById(R.id.etCategoryName)
        etCategoryDesc = findViewById(R.id.etCategoryDesc)
        btnSaveCategory = findViewById(R.id.btnSaveCategory)
        btnCancelCategory = findViewById(R.id.btnCancelCategory)
        tvSelectedIconLabel = findViewById(R.id.tvSelectedIconLabel)

        iconCards = listOf(
            findViewById(R.id.llIconJalan),
            findViewById(R.id.llIconLampu),
            findViewById(R.id.llIconAir),
            findViewById(R.id.llIconTaman),
            findViewById(R.id.llIconSampah),
            findViewById(R.id.llIconTrotoar),
            findViewById(R.id.llIconJembatan),
            findViewById(R.id.llIconHalte)
        )
    }

    private fun fetchNextCategoryCode() {
        val token = "Bearer " + getSharedPreferences("UserSession", MODE_PRIVATE).getString("KEY_TOKEN", "")
        ApiClient.instance.getAllCategories(token).enqueue(object : Callback<List<CategoryDto>> {
            override fun onResponse(call: Call<List<CategoryDto>>, response: Response<List<CategoryDto>>) {
                if (response.isSuccessful && response.body() != null) {
                    val categories = response.body()!!
                    val nextIndex = if (categories.isEmpty()) 1 else {
                        categories.mapNotNull { it.code?.substringAfterLast("-")?.toIntOrNull() }.maxOrNull()?.plus(1) ?: (categories.size + 1)
                    }
                    etCategoryCode.setText("KTG-MKS-${nextIndex.toString().padStart(2, '0')}")
                }
            }
            override fun onFailure(call: Call<List<CategoryDto>>, t: Throwable) {}
        })
    }

    private fun setupIconSelectors() {
        val iconMappings = mapOf(
            R.id.llIconJalan to "jalan", R.id.llIconLampu to "lamp",
            R.id.llIconAir to "drainage", R.id.llIconTaman to "park",
            R.id.llIconSampah to "delete", R.id.llIconTrotoar to "walk",
            R.id.llIconJembatan to "bridge", R.id.llIconHalte to "bus_stop"
        )
        for (card in iconCards) {
            card.setOnClickListener {
                updateIconSelection(card, iconMappings[card.id] ?: "jalan")
            }
        }
    }

    private fun updateIconSelection(selectedCard: LinearLayout, iconName: String) {
        selectedIconName = iconName
        tvSelectedIconLabel.text = "Ikon Terpilih: ${iconName.replaceFirstChar { it.uppercase() }}"
        for (card in iconCards) {
            card.setBackgroundResource(R.drawable.bg_chip_white)
            (card.getChildAt(0) as ImageView).imageTintList = ColorStateList.valueOf(Color.parseColor("#434653"))
            (card.getChildAt(1) as TextView).setTextColor(Color.parseColor("#434653"))
        }
        selectedCard.setBackgroundResource(R.drawable.bg_icon_card_active)
        (selectedCard.getChildAt(0) as ImageView).imageTintList = ColorStateList.valueOf(Color.WHITE)
        (selectedCard.getChildAt(1) as TextView).setTextColor(Color.WHITE)
    }

    private fun createCategory() {
        val name = etCategoryName.text.toString().trim()
        val code = etCategoryCode.text.toString().trim()
        val desc = etCategoryDesc.text.toString().trim()

        if (name.isEmpty() || code.isEmpty()) {
            Toast.makeText(this, "Nama dan Kode wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val token = "Bearer " + getSharedPreferences("UserSession", MODE_PRIVATE).getString("KEY_TOKEN", "")
        
        // Buat objek DTO tanpa ID dan count agar backend tidak error saat validasi
        val newCategory = CategoryDto(
            id = null, 
            code = code, 
            name = name, 
            iconName = selectedIconName,
            description = desc, 
            isActive = true, 
            connectedReportsCount = 0
        )

        ApiClient.instance.createCategory(token, newCategory).enqueue(object : Callback<CategoryDto> {
            override fun onResponse(call: Call<CategoryDto>, response: Response<CategoryDto>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AddKategoriActivity, "Kategori berhasil dibuat", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    // Tampilkan pesan error asli dari backend untuk debugging
                    val errorMsg = response.errorBody()?.string() ?: "Gagal membuat kategori"
                    Toast.makeText(this@AddKategoriActivity, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<CategoryDto>, t: Throwable) {
                Toast.makeText(this@AddKategoriActivity, "Koneksi gagal: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
