package com.example.pinpoint.admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pinpoint.R
import com.example.pinpoint.adapter.AccountAdapter
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.UserAccountDto
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class KelolaAkunActivity : AppCompatActivity() {

    private lateinit var etSearchAccount: EditText
    private lateinit var rvAccountList: RecyclerView
    private lateinit var tvStatTotalAccount: TextView
    private lateinit var tvStatGovAccount: TextView
    private lateinit var tvStatAdminAccount: TextView
    private lateinit var tvStatSpamAccount: TextView
    private lateinit var btnAddAccountFAB: FloatingActionButton

    private lateinit var btnStatAll: View
    private lateinit var btnStatGov: View
    private lateinit var btnStatAdmin: View
    private lateinit var btnStatSpam: View

    private lateinit var accountAdapter: AccountAdapter
    private var globalUsersList: List<UserAccountDto> = emptyList()
    private var currentFilterType: FilterType = FilterType.ALL

    enum class FilterType { ALL, DINAS, ADMIN, SPAM }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_kelola_akun)

        initViews()
        setupRecyclerView()
        setupSearchAndFilters()

        NavbarUtils.setupAdminNavbar(this)

        btnAddAccountFAB.setOnClickListener {
            startActivity(Intent(this, TambahAkunActivity::class.java))
        }

        fetchAllUsers()
    }

    override fun onResume() {
        super.onResume()
        fetchAllUsers()
    }

    private fun initViews() {
        etSearchAccount = findViewById(R.id.etSearchAccount)
        rvAccountList = findViewById(R.id.rvAccountList)
        tvStatTotalAccount = findViewById(R.id.tvStatTotalAccount)
        tvStatGovAccount = findViewById(R.id.tvStatGovAccount)
        tvStatAdminAccount = findViewById(R.id.tvStatAdminAccount)
        tvStatSpamAccount = findViewById(R.id.tvStatSpamAccount)
        btnAddAccountFAB = findViewById(R.id.btnAddAccountFAB)

        btnStatAll = findViewById(R.id.btnStatAll)
        btnStatGov = findViewById(R.id.btnStatGov)
        btnStatAdmin = findViewById(R.id.btnStatAdmin)
        btnStatSpam = findViewById(R.id.btnStatSpam)
    }

    private fun setupRecyclerView() {
        accountAdapter = AccountAdapter(
            accountList = emptyList(),
            onActionLeftClick = { account ->
                val isSuspended = account.isSuspended == true ||
                        account.statusAkun.equals("SUSPENDED", ignoreCase = true) ||
                        account.statusAkun.equals("BLOCKED", ignoreCase = true)
                if (isSuspended) {
                    updateAccountStatus(account, "ACTIVE", "Akun dipulihkan dari Spam")
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Tandai Akun Spam")
                        .setMessage("Tandai akun ${account.resolvedFullName()} sebagai spam/ditangguhkan?")
                        .setPositiveButton("Tandai Spam") { _, _ ->
                            updateAccountStatus(account, "SUSPENDED", "Akun ditandai sebagai Spam")
                        }
                        .setNegativeButton("Batal", null)
                        .show()
                }
            },
            onActionRightClick = { account ->
                confirmDeleteAccount(account)
            }
        )
        rvAccountList.layoutManager = LinearLayoutManager(this)
        rvAccountList.adapter = accountAdapter
    }

    private fun fetchAllUsers() {
        val rawToken = getSharedPreferences("UserSession", MODE_PRIVATE).getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"
        ApiClient.instance.getAllUsers(token).enqueue(object : Callback<List<UserAccountDto>> {
            override fun onResponse(call: Call<List<UserAccountDto>>, response: Response<List<UserAccountDto>>) {
                if (response.isSuccessful && response.body() != null) {
                    globalUsersList = response.body()!!
                    updateStatsDisplay()
                    applyFilterAndSearch()
                }
            }
            override fun onFailure(call: Call<List<UserAccountDto>>, t: Throwable) {
                Toast.makeText(this@KelolaAkunActivity, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateStatsDisplay() {
        tvStatTotalAccount.text = globalUsersList.size.toString()
        tvStatGovAccount.text = globalUsersList.count { it.resolvedRole() == "PEMERINTAH" }.toString()
        tvStatAdminAccount.text = globalUsersList.count { it.resolvedRole() == "ADMIN" }.toString()
        tvStatSpamAccount.text = globalUsersList.count {
            it.isSuspended == true ||
            it.statusAkun.equals("SUSPENDED", ignoreCase = true) ||
            it.statusAkun.equals("BLOCKED", ignoreCase = true)
        }.toString()
    }

    private fun setupSearchAndFilters() {
        etSearchAccount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilterAndSearch() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnStatAll.setOnClickListener { currentFilterType = FilterType.ALL; applyFilterAndSearch() }
        btnStatGov.setOnClickListener { currentFilterType = FilterType.DINAS; applyFilterAndSearch() }
        btnStatAdmin.setOnClickListener { currentFilterType = FilterType.ADMIN; applyFilterAndSearch() }
        btnStatSpam.setOnClickListener { currentFilterType = FilterType.SPAM; applyFilterAndSearch() }
    }

    private fun applyFilterAndSearch() {
        var list = globalUsersList

        list = when (currentFilterType) {
            FilterType.ALL -> list
            FilterType.DINAS -> list.filter { it.resolvedRole() == "PEMERINTAH" }
            FilterType.ADMIN -> list.filter { it.resolvedRole() == "ADMIN" }
            FilterType.SPAM -> list.filter {
                it.isSuspended == true ||
                it.statusAkun.equals("SUSPENDED", ignoreCase = true) ||
                it.statusAkun.equals("BLOCKED", ignoreCase = true)
            }
        }

        val query = etSearchAccount.text.toString().trim()
        if (query.isNotEmpty()) {
            list = list.filter {
                it.resolvedFullName().contains(query, ignoreCase = true) ||
                it.email?.contains(query, ignoreCase = true) == true
            }
        }

        accountAdapter.updateData(list)
        updateActiveStatUI()
    }

    private fun updateActiveStatUI() {
        val views = listOf(btnStatAll, btnStatGov, btnStatAdmin, btnStatSpam)
        views.forEach { it.alpha = 0.5f }
        when (currentFilterType) {
            FilterType.ALL -> btnStatAll.alpha = 1.0f
            FilterType.DINAS -> btnStatGov.alpha = 1.0f
            FilterType.ADMIN -> btnStatAdmin.alpha = 1.0f
            FilterType.SPAM -> btnStatSpam.alpha = 1.0f
        }
    }

    private fun updateAccountStatus(account: UserAccountDto, status: String, actionName: String) {
        val accountId = account.id ?: return
        val rawToken = getSharedPreferences("UserSession", MODE_PRIVATE).getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

        val body = mapOf(
            "isSuspended" to (status == "SUSPENDED"),
            "statusAkun" to status,
            "active" to (status == "ACTIVE")
        )

        ApiClient.instance.updateUserStatus(token, accountId, body).enqueue(object : Callback<UserAccountDto> {
            override fun onResponse(call: Call<UserAccountDto>, response: Response<UserAccountDto>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@KelolaAkunActivity, "Berhasil: $actionName", Toast.LENGTH_SHORT).show()
                    fetchAllUsers()
                } else {
                    Toast.makeText(this@KelolaAkunActivity, "Gagal mengubah status akun", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<UserAccountDto>, t: Throwable) {
                Toast.makeText(this@KelolaAkunActivity, "Koneksi gagal", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun confirmDeleteAccount(account: UserAccountDto) {
        val accountId = account.id ?: return
        AlertDialog.Builder(this)
            .setTitle("Hapus Akun")
            .setMessage("Apakah Anda yakin ingin menghapus akun ${account.resolvedFullName()} (${account.email ?: "-"}) secara permanen?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteAccount(accountId, account.resolvedFullName())
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteAccount(accountId: Long, accountName: String) {
        val rawToken = getSharedPreferences("UserSession", MODE_PRIVATE).getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"

        ApiClient.instance.deleteUser(token, accountId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@KelolaAkunActivity, "Akun $accountName berhasil dihapus", Toast.LENGTH_SHORT).show()
                    fetchAllUsers()
                } else {
                    Toast.makeText(this@KelolaAkunActivity, "Gagal menghapus akun (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@KelolaAkunActivity, "Koneksi gagal saat menghapus akun", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
