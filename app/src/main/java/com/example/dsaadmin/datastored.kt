package com.example.dsaadmin

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object UserPreferences {
    private val Context.dataStore by preferencesDataStore(name = "user_prefs")

    val questionsStatusKey = stringPreferencesKey("questions_status_json")

    suspend fun saveQuestionsStatus(context: Context, statusMap: Map<String, Boolean>) {
        val json = Gson().toJson(statusMap)
        context.dataStore.edit { prefs ->
            prefs[questionsStatusKey] = json
        }
    }

    suspend fun loadQuestionsStatus(context: Context): Map<String, Boolean> {
        val jsonFlow = context.dataStore.data.map { prefs ->
            prefs[questionsStatusKey] ?: "{}"
        }
        val json = jsonFlow.first()
        return Gson().fromJson(json, object : TypeToken<Map<String, Boolean>>() {}.type)
    }
}
