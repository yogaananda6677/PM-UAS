package ananda.yoga.projectuasmobile

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object SettingsHelper {

    private const val PREF_SESSION = "user_session"
    private const val PREF_APP     = "AppPref"

    fun saveTema(context: Context, isDark: Boolean) {
        context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE)
            .edit().putBoolean("is_dark_mode", isDark).apply()
    }

    fun isDarkMode(context: Context): Boolean {
        return context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE)
            .getBoolean("is_dark_mode", false)
    }

    fun applyTema(context: Context) {
        if (isDarkMode(context)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    fun toggleTema(context: Context) {
        saveTema(context, !isDarkMode(context))
        applyTema(context)
    }

    // ========== SESI LOGIN ==========
    fun saveSession(
        context: Context,
        token: String,
        nama: String,
        username: String,
        email: String,
        role: String,
        idUser: String
    ) {
        context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_login", true)
            .putString("token", token)
            .putString("nama", nama)
            .putString("username", username)
            .putString("email", email)
            .putString("role", role)
            .putString("id_user", idUser)
            .apply()
    }

    fun isLogin(context: Context): Boolean {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE)
            .getBoolean("is_login", false)
    }

    fun getToken(context: Context): String {
        return context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE)
            .getString("token", "") ?: ""
    }

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREF_SESSION, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}