package com.infoproject1

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DataStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("justicebringer_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveEmployees(list: List<Employee>) {
        val json = gson.toJson(list)
        prefs.edit().putString("employees", json).apply()
    }

    fun loadEmployees(): List<Employee> {
        val json = prefs.getString("employees", "[]")
        val type = object : TypeToken<List<Employee>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveUnitLocations(list: List<UnitLocation>) {
        val json = gson.toJson(list)
        prefs.edit().putString("unit_locations", json).apply()
    }

    fun loadUnitLocations(): List<UnitLocation> {
        val json = prefs.getString("unit_locations", "[]")
        val type = object : TypeToken<List<UnitLocation>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveAuditors(list: List<Auditor>) {
        val json = gson.toJson(list)
        prefs.edit().putString("auditors", json).apply()
    }

    fun loadAuditors(): List<Auditor> {
        val json = prefs.getString("auditors", "[]")
        val type = object : TypeToken<List<Auditor>>() {}.type
        return gson.fromJson(json, type)
    }
}