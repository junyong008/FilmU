package com.yjy.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

val Context.exampleDataStore: DataStore<Preferences> by preferencesDataStore(name = "example_preferences")
class ExampleDataStore @Inject constructor(context: Context) {

    private val dataStore = context.exampleDataStore

    fun getNumber(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[KEY_NUMBER] ?: 0
        }
    }
    suspend fun addNumber(number: Int) {
        dataStore.edit { settings ->
            val currentNumber = settings[KEY_NUMBER] ?: 0
            settings[KEY_NUMBER] = currentNumber + 1
        }
    }

    companion object {
        private val KEY_NUMBER = intPreferencesKey("number")
    }
}