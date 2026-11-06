package com.example.pinpoint

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * TokenManager — Menyimpan JWT token dan data sesi pengguna
 * menggunakan EncryptedSharedPreferences (AES256-SIV + AES256-GCM).
 *
 * Data sensitif yang disimpan:
 * - JWT token (Bearer)
 * - Identifier & password untuk fitur "Ingat Saya"
 * - Nama, email, dan role pengguna
 *
 * Jika EncryptedSharedPreferences gagal dibuat (misalnya saat unit test
 * atau perangkat tanpa Keystore), fallback ke SharedPreferences biasa
 * dengan peringatan di log.
 */
object TokenManager {

    private const val PREFS_FILE_NAME = "pinpoint_secure_prefs"
    private const val TAG = "TokenManager"

    // ── Key Constants ──────────────────────────────────────────────────────
    private const val KEY_JWT_TOKEN      = "jwt_token"
    private const val KEY_IS_LOGGED_IN   = "is_logged_in"
    private const val KEY_USER_NAME      = "user_name"
    private const val KEY_USER_EMAIL     = "user_email"
    private const val KEY_USER_ROLE      = "user_role"
    private const val KEY_REMEMBER_ME    = "remember_me"
    private const val KEY_SAVED_ID       = "saved_identifier"   // email/no.hp untuk auto-fill
    private const val KEY_SAVED_PASSWORD = "saved_password"     // password untuk auto-fill

    // ══════════════════════════════════════════════════════════════════════
    //  INISIALISASI ENCRYPTED SHARED PREFERENCES
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Mengembalikan instance SharedPreferences yang terenkripsi.
     * Gunakan AES256 GCM untuk nilai dan AES256 SIV untuk key.
     * Fallback ke SharedPreferences biasa jika Keystore tidak tersedia.
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context.applicationContext,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences gagal dibuat, fallback ke SP biasa: ${e.message}")
            context.applicationContext.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SIMPAN SESSION SETELAH LOGIN BERHASIL
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Menyimpan token JWT dan data profil pengguna setelah login berhasil.
     *
     * @param context   Application context
     * @param token     JWT Bearer token dari server
     * @param nama      Nama lengkap pengguna
     * @param email     Email pengguna
     * @param role      Role: WARGA / ADMIN / DINAS
     */
    fun saveSession(
        context: Context,
        token: String,
        nama: String,
        email: String,
        role: String
    ) {
        getPrefs(context).edit().apply {
            putString(KEY_JWT_TOKEN, token)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_NAME, nama)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_ROLE, role)
            apply()
        }
        Log.d(TAG, "Sesi disimpan untuk: $email (role=$role)")
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FITUR "INGAT SAYA"
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Menyimpan kredensial login untuk fitur auto-fill "Ingat Saya".
     * Password disimpan terenkripsi oleh EncryptedSharedPreferences.
     *
     * @param context    Application context
     * @param identifier Email atau nomor telepon
     * @param password   Password plaintext (aman karena prefs terenkripsi)
     */
    fun saveRememberMe(context: Context, identifier: String, password: String) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_REMEMBER_ME, true)
            putString(KEY_SAVED_ID, identifier)
            putString(KEY_SAVED_PASSWORD, password)
            apply()
        }
    }

    /** Menghapus data "Ingat Saya" (saat user uncheck checkbox). */
    fun clearRememberMe(context: Context) {
        getPrefs(context).edit().apply {
            remove(KEY_REMEMBER_ME)
            remove(KEY_SAVED_ID)
            remove(KEY_SAVED_PASSWORD)
            apply()
        }
    }

    fun isRememberMe(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_REMEMBER_ME, false)

    fun getSavedIdentifier(context: Context): String =
        getPrefs(context).getString(KEY_SAVED_ID, "") ?: ""

    fun getSavedPassword(context: Context): String =
        getPrefs(context).getString(KEY_SAVED_PASSWORD, "") ?: ""

    // ══════════════════════════════════════════════════════════════════════
    //  GETTER DATA SESI
    // ══════════════════════════════════════════════════════════════════════

    fun getToken(context: Context): String? =
        getPrefs(context).getString(KEY_JWT_TOKEN, null)

    fun isLoggedIn(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)

    fun getUserName(context: Context): String =
        getPrefs(context).getString(KEY_USER_NAME, "") ?: ""

    fun getUserEmail(context: Context): String =
        getPrefs(context).getString(KEY_USER_EMAIL, "") ?: ""

    fun getUserRole(context: Context): String =
        getPrefs(context).getString(KEY_USER_ROLE, "") ?: ""

    /**
     * Mengembalikan header Authorization lengkap dalam format "Bearer {token}".
     * Kembalikan null jika token belum ada.
     */
    fun getBearerToken(context: Context): String? {
        val token = getToken(context) ?: return null
        return "Bearer $token"
    }

    // ══════════════════════════════════════════════════════════════════════
    //  LOGOUT — HAPUS SEMUA SESI
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Menghapus semua data sesi (token, nama, email, role).
     * Data "Ingat Saya" dipertahankan agar tidak perlu input ulang.
     */
    fun clearSession(context: Context) {
        getPrefs(context).edit().apply {
            remove(KEY_JWT_TOKEN)
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_ROLE)
            apply()
        }
        Log.d(TAG, "Sesi pengguna dihapus (logout).")
    }

    /**
     * Menghapus SEMUA data termasuk "Ingat Saya" (full reset).
     */
    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
        Log.d(TAG, "Semua data TokenManager dihapus.")
    }
}
