package com.example.pinpoint.admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pinpoint.R
import com.example.pinpoint.adapter.CategoryAdapter
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.CategoryDto
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class KategoriActivity : AppCompatActivity() {

    private lateinit var etSearchCategory: EditText
    private lateinit var btnAddCategoryFAB: FloatingActionButton
    private lateinit var rvCategoryList: RecyclerView
    private lateinit var tvStatCategoryActive: TextView
    private lateinit var tvStatTotalReportsCategory: TextView
    
    private lateinit var categoryAdapter: CategoryAdapter

    private var globalCategories: List<CategoryDto> = emptyList()
    private var filteredCategories: List<CategoryDto> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_kategori)

        initViews()
        setupRecyclerView()
        setupSearch()

        NavbarUtils.setupAdminNavbar(this)

        btnAddCategoryFAB.setOnClickListener {
            startActivity(Intent(this, AddKategoriActivity::class.java))
        }

        loadCategories()
    }

    override fun onResume() {
        super.onResume()
        loadCategories()
    }

    private fun initViews() {
        etSearchCategory = findViewById(R.id.etSearchCategory)
        btnAddCategoryFAB = findViewById(R.id.btnAddCategoryFAB)
        rvCategoryList = findViewById(R.id.rvCategoryList)
        tvStatCategoryActive = findViewById(R.id.tvStatCategoryActive)
        tvStatTotalReportsCategory = findViewById(R.id.tvStatTotalReportsCategory)
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            categoryList = emptyList(),
            onEditClick = { category ->
                val generatedCode = category.code ?: category.id?.let { "KTG-MKS-${it.toString().padStart(2, '0')}" }
                val intent = Intent(this, EditKategoriActivity::class.java).apply {
                    putExtra("CATEGORY_ID", category.id)
                    putExtra("CATEGORY_CODE", generatedCode)
                    putExtra("CATEGORY_NAME", category.name)
                    putExtra("CATEGORY_DESC", category.description)
                    putExtra("CATEGORY_ACTIVE", category.isActive == true)
                    putExtra("CATEGORY_REPORTS", category.connectedReportsCount ?: 0)
                    putExtra("CATEGORY_ICON", category.iconName)
                }
                startActivity(intent)
            },
            onDeleteClick = { category -> showDeleteConfirmation(category) }
        )
        rvCategoryList.layoutManager = LinearLayoutManager(this)
        rvCategoryList.adapter = categoryAdapter
        rvCategoryList.isNestedScrollingEnabled = false
    }

    private fun setupSearch() {
        etSearchCategory.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilterLocally() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun applyFilterLocally() {
        val query = etSearchCategory.text.toString().trim()
        filteredCategories = if (query.isEmpty()) {
            globalCategories
        } else {
            globalCategories.filter {
                (it.name?.contains(query, ignoreCase = true) == true) || 
                (it.code?.contains(query, ignoreCase = true) == true)
            }
        }
        categoryAdapter.updateData(filteredCategories)
    }

    private fun loadCategories() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val token = "Bearer " + (sharedPreferences.getString("KEY_TOKEN", "") ?: "")

        ApiClient.instance.getAllCategories(token).enqueue(object : Callback<List<CategoryDto>> {
            override fun onResponse(call: Call<List<CategoryDto>>, response: Response<List<CategoryDto>>) {
                if (response.isSuccessful && response.body() != null) {
                    globalCategories = response.body()!!
                    updateStats()
                    applyFilterLocally()
                }
            }
            override fun onFailure(call: Call<List<CategoryDto>>, t: Throwable) {
                Toast.makeText(this@KategoriActivity, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateStats() {
        val activeCount = globalCategories.count { it.isActive == true }
        val totalReports = globalCategories.sumOf { it.connectedReportsCount ?: 0 }
        tvStatCategoryActive.text = "$activeCount Kategori Aktif"
        tvStatTotalReportsCategory.text = "$totalReports Laporan"
    }

    private fun showDeleteConfirmation(category: CategoryDto) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Kategori")
            .setMessage("Hapus kategori ${category.name}?")
            .setPositiveButton("Hapus") { _, _ -> deleteCategory(category) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteCategory(category: CategoryDto) {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val token = "Bearer " + (sharedPreferences.getString("KEY_TOKEN", "") ?: "")
        category.id?.let { id ->
            ApiClient.instance.deleteCategory(token, id).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@KategoriActivity, "Kategori dihapus", Toast.LENGTH_SHORT).show()
                        loadCategories()
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {}
            })
        }
    }
}
