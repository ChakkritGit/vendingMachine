package com.thanes.vending.dataStore

import android.content.Context
import com.google.gson.Gson
import com.thanes.vending.data.UserData

object DataManager {
  private const val PREF_NAME = "user_prefs"
  private const val TOKEN_KEY = "auth_token"
  private const val USER_DATA = "user_data"

  suspend fun saveToken(context: Context, token: String) {
    val prefs = context.getSharedPreferences(com.thanes.vending.dataStore.DataManager.PREF_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(com.thanes.vending.dataStore.DataManager.TOKEN_KEY, token).apply()
  }

  suspend fun saveUserData(context: Context, userData: UserData) {
    val prefs = context.getSharedPreferences(com.thanes.vending.dataStore.DataManager.PREF_NAME, Context.MODE_PRIVATE)
    val jsonString = Gson().toJson(userData)
    prefs.edit().putString(com.thanes.vending.dataStore.DataManager.USER_DATA, jsonString).apply()
  }

  suspend fun getToken(context: Context): String {
    val prefs = context.getSharedPreferences(com.thanes.vending.dataStore.DataManager.PREF_NAME, Context.MODE_PRIVATE)
    return prefs.getString(com.thanes.vending.dataStore.DataManager.TOKEN_KEY, "") ?: ""
  }

  suspend fun getUserData(context: Context): UserData? {
    val prefs = context.getSharedPreferences(com.thanes.vending.dataStore.DataManager.PREF_NAME, Context.MODE_PRIVATE)
    val jsonString = prefs.getString(com.thanes.vending.dataStore.DataManager.USER_DATA, null)

    return try {
      jsonString?.let {
        Gson().fromJson(it, UserData::class.java)
      }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

//    suspend fun clearToken(context: Context) {
//        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
//        prefs.edit().remove(TOKEN_KEY).apply()
//    }

  suspend fun clearAll(context: Context) {
    val prefs = context.getSharedPreferences(com.thanes.vending.dataStore.DataManager.PREF_NAME, Context.MODE_PRIVATE)
    prefs.edit().clear().apply()
  }
}