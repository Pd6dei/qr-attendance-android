package com.example.qrverificationapp.data

import android.content.Context

class SessionManager(context: Context) {

    private val prefs =
        context.getSharedPreferences("qr_app_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER = "key_user"
        private const val KEY_ATTENDANCE_TYPE = "key_attendance_type"
    }

    /* ---------------- USER ---------------- */

    fun saveUser(userName: String) {
        prefs.edit().putString(KEY_USER, userName).apply()
    }

    fun getUser(): String? {
        return prefs.getString(KEY_USER, null)
    }

    /* ---------------- ATTENDANCE TYPE ---------------- */

    fun saveAttendanceType(type: String) {
        prefs.edit().putString(KEY_ATTENDANCE_TYPE, type).apply()
    }

    fun getAttendanceType(): String? {
        return prefs.getString(KEY_ATTENDANCE_TYPE, null)
    }

    /* ---------------- CLEAR SESSION ---------------- */

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}


