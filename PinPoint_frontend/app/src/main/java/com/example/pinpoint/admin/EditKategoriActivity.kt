package com.example.pinpoint.admin

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.example.pinpoint.R
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.CategoryDto
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditKategoriActivity : AppCompatActivity() {

    private var categoryId: Long? = null
    private var selectedIconName: String = "jalan"
    private lateinit var iconCards: List<LinearLayout>

    private lateinit var btnBackEditCategory: ImageView
    private lateinit var tvCategoryStatusBadge: TextView
    private lateinit var tvCategoryCodeBadge: TextView
    private lateinit var tvConnectedReportsBadge: TextView
    private lateinit var ivHeaderCategoryIcon: ImageView
    private lateinit var tvHeaderCategoryTitle: TextView
    private lateinit var etTaxonomyCode: EditText
    private lateinit var etEditCategoryName: EditText
    private lateinit var tvSelectedIconLabel: TextView
    private lateinit var tvCharCounter: TextView
    private lateinit var etEditCategoryDesc: EditText
    private lateinit var switchCategoryActive: SwitchMaterial
    private lateinit var btnUpdateCategory: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_edit_kategori)

        initViews()
        setupIconSelectors()
        populateIntentData()
        setupCharCounter()

        btnBackEditCategory.setOnClickListener { finish() }
        btnUpdateCategory.setOnClickListener { updateCategory() }
    }

    private fun initViews() {
        btnBackEditCategory = findViewById(R.id.btnBackEditCategory)
        tvCategoryStatusBadge = findViewById(R.id.tvCategoryStatusBadge)
        tvCategoryCodeBadge = findViewById(R.id.tvCategoryCodeBadge)
        tvConnectedReportsBadge = findViewById(R.id.tvConnectedReportsBadge)
        ivHeaderCategoryIcon = findViewById(R.id.ivHeaderCategoryIcon)
        tvHeaderCategoryTitle = findViewById(R.id.tvHeaderCategoryTitle)
        etTaxonomyCode = findViewById(R.id.etTaxonomyCode)
        etEditCategoryName = findViewById(R.id.etEditCategoryName)
        tvSelectedIconLabel = findViewById(R.id.tvSelectedIconLabel)
        tvCharCounter = findViewById(R.id.tvCharCounter)
        etEditCategoryDesc = findViewById(R.id.etEditCategoryDesc)
        switchCategoryActive = findViewById(R.id.switchCategoryActive)
        btnUpdateCategory = findViewById(R.id.btnUpdateCategory)

        iconCards = listOf(
            findViewById(R.id.llEditIconJalan),
            findViewById(R.id.llEditIconLampu),
            findViewById(R.id.llEditIconAir),
            findViewById(R.id.llEditIconTaman),
            findViewById(R.id.llEditIconSampah),
            findViewById(R.id.llEditIconTrotoar),
            findViewById(R.id.llEditIconJembatan),
            findViewById(R.id.llEditIconHalte)
        )
    }

    private fun setupIconSelectors() {
        val iconMappings = mapOf(
            R.id.llEditIconJalan to "jalan",
            R.id.llEditIconLampu to "lamp",
            R.id.llEditIconAir to "drainage",
            R.id.llEditIconTaman to "park",
            R.id.llEditIconSampah to "delete",
            R.id.llEditIconTrotoar to "walk",
            R.id.llEditIconJembatan to "bridge",
            R.id.llEditIconHalte to "bus_stop"
        )

        for (card in iconCards) {
            card.setOnClickListener {
                val iconName = iconMappings[card.id] ?: "jalan"
                updateIconSelection(card, iconName)
            }
        }
    }

    private fun updateIconSelection(selectedCard: LinearLayout, iconName: String) {
        selectedIconName = iconName
        val displayName = iconName.replaceFirstChar { it.uppercase() }
        tvSelectedIconLabel.text = "Ikon Terpilih: $displayName"

        for (card in iconCards) {
            card.setBackgroundResource(R.drawable.bg_chip_white)
            val icon = card.getChildAt(0) as ImageView
            icon.imageTintList = ColorStateList.valueOf(Color.parseColor("#434653"))
        }

        selectedCard.setBackgroundResource(R.drawable.bg_icon_card_active)
        val selectedIcon = selectedCard.getChildAt(0) as ImageView
        selectedIcon.imageTintList = ColorStateList.valueOf(Color.WHITE)

        updateHeaderIcon(iconName)
    }

    private fun updateHeaderIcon(iconName: String) {
        val iconResId = when (iconName.lowercase()) {
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
        ivHeaderCategoryIcon.setImageResource(iconResId)
    }

    private fun populateIntentData() {
        if (intent.hasExtra("CATEGORY_ID")) {
            categoryId = intent.getLongExtra("CATEGORY_ID", -1L)
        }
        val code = intent.getStringExtra("CATEGORY_CODE") ?: ""
        val name = intent.getStringExtra("CATEGORY_NAME") ?: ""
        val desc = intent.getStringExtra("CATEGORY_DESC") ?: ""
        val isActive = intent.getBooleanExtra("CATEGORY_ACTIVE", true)
        val reportsCount = intent.getIntExtra("CATEGORY_REPORTS", 0)
        selectedIconName = intent.getStringExtra("CATEGORY_ICON") ?: "jalan"

        tvCategoryCodeBadge.text = code
        tvConnectedReportsBadge.text = "$reportsCount Laporan Terhubung"
        tvHeaderCategoryTitle.text = name
        etTaxonomyCode.setText(code)
        etEditCategoryName.setText(name)
        etEditCategoryDesc.setText(desc)
        switchCategoryActive.isChecked = isActive

        updateStatusBadge(isActive)

        val iconMappings = mapOf(
            "jalan" to R.id.llEditIconJalan,
            "lamp" to R.id.llEditIconLampu,
            "lampu" to R.id.llEditIconLampu,
            "drainage" to R.id.llEditIconAir,
            "air" to R.id.llEditIconAir,
            "park" to R.id.llEditIconTaman,
            "taman" to R.id.llEditIconTaman,
            "delete" to R.id.llEditIconSampah,
            "sampah" to R.id.llEditIconSampah,
            "walk" to R.id.llEditIconTrotoar,
            "trotoar" to R.id.llEditIconTrotoar,
            "bridge" to R.id.llEditIconJembatan,
            "jembatan" to R.id.llEditIconJembatan,
            "bus_stop" to R.id.llEditIconHalte,
            "halte" to R.id.llEditIconHalte
        )
        val cardId = iconMappings[selectedIconName.lowercase()] ?: R.id.llEditIconJalan
        val selectedCard = findViewById<LinearLayout>(cardId)
        updateIconSelection(selectedCard, selectedIconName)
    }

    private fun updateStatusBadge(isActive: Boolean) {
        if (isActive) {
            tvCategoryStatusBadge.text = "Aktif"
            tvCategoryStatusBadge.setBackgroundResource(R.drawable.bg_badge_green_light)
        } else {
            tvCategoryStatusBadge.text = "Nonaktif"
            tvCategoryStatusBadge.setBackgroundResource(R.drawable.bg_badge_red_light)
        }
    }

    private fun setupCharCounter() {
        etEditCategoryDesc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                tvCharCounter.text = "$length/250"
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateCategory() {
        val name = etEditCategoryName.text.toString().trim()
        val desc = etEditCategoryDesc.text.toString().trim()
        val code = etTaxonomyCode.text.toString().trim()
        val isActive = switchCategoryActive.isChecked

        if (name.isEmpty()) {
            Toast.makeText(this, "Nama Kategori tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val rawToken = sharedPreferences.getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

        val updatedCategory = CategoryDto(
            id = categoryId,
            code = code,
            name = name,
            iconName = selectedIconName,
            description = desc,
            isActive = isActive,
            connectedReportsCount = intent.getIntExtra("CATEGORY_REPORTS", 0)
        )

        val targetId = categoryId ?: return
        ApiClient.fetchWithFallback(
            callFactory = { api -> api.updateCategory(token, targetId, updatedCategory) },
            onResponse = { _, response ->
                if (response.isSuccessful) {
                    Toast.makeText(this@EditKategoriActivity, "Perubahan disimpan", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@EditKategoriActivity, "Gagal memperbarui kategori", Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { _, _ ->
                Toast.makeText(this@EditKategoriActivity, "Koneksi gagal", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
