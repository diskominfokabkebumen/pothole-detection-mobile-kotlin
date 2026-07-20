package com.example.hallohalloapp.detection

import android.content.Context

/**
 * Menyimpan data profil pengguna (nama & email) secara permanen di HP,
 * pakai SharedPreferences karena cuma teks sederhana - tidak ada sistem login/akun.
 */
object UserProfileStorage {

    private const val PREFS_NAME = "user_profile"
    private const val KEY_NAME = "profile_name"
    private const val KEY_EMAIL = "profile_email"

    private const val DEFAULT_NAME = "Andi"
    private const val DEFAULT_EMAIL = "andi.kebumen@email.com"

    fun getName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NAME, DEFAULT_NAME) ?: DEFAULT_NAME
    }

    fun getEmail(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_EMAIL, DEFAULT_EMAIL) ?: DEFAULT_EMAIL
    }

    fun saveProfile(context: Context, name: String, email: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .apply()
    }
}