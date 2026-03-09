package com.infoproject1

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("justicebringer_prefs", Context.MODE_PRIVATE)

    fun saveEmployeeFile(uri: String) {
        prefs.edit().putString("employee_file", uri).apply()
    }

    fun getEmployeeFile(): String? {
        return prefs.getString("employee_file", null)
    }

    fun saveLocationFile(uri: String) {
        prefs.edit().putString("location_file", uri).apply()
    }

    fun getLocationFile(): String? {
        return prefs.getString("location_file", null)
    }

    fun saveAuditorFile(uri: String) {
        prefs.edit().putString("auditor_file", uri).apply()
    }

    fun getAuditorFile(): String? {
        return prefs.getString("auditor_file", null)
    }
}