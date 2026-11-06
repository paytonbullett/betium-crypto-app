package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "betium_user_settings")

data class UserPreferences(
    val nodeUrl: String = "https://crypto.bullettsocial.space",
    val themeMode: String = "dark", // "system", "dark", "light"
    val accentColorHex: String = "#F59E0B", // Default Betium Gold
    val isOledDark: Boolean = false,
    val showAllTokens: Boolean = false,
    val minerIntensity: Int = 2, // 1: Low (Eco), 2: Medium (Balanced), 3: High (Max)
    val lastKnownBalance: String = "0.00000000",
    val lastKnownHeight: Long = 0L
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val NODE_URL = stringPreferencesKey("betium_node_url")
        val THEME_MODE = stringPreferencesKey("betium_theme_mode")
        val ACCENT_COLOR_HEX = stringPreferencesKey("betium_accent_color")
        val IS_OLED_DARK = booleanPreferencesKey("betium_oled_dark")
        val SHOW_ALL_TOKENS = booleanPreferencesKey("betium_show_all_tokens")
        val MINER_INTENSITY = intPreferencesKey("betium_miner_intensity")
        val LAST_KNOWN_BALANCE = stringPreferencesKey("betium_last_balance")
        val LAST_KNOWN_HEIGHT = longPreferencesKey("betium_last_height")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            nodeUrl = preferences[PreferencesKeys.NODE_URL] ?: "https://crypto.bullettsocial.space",
            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "dark",
            accentColorHex = preferences[PreferencesKeys.ACCENT_COLOR_HEX] ?: "#F59E0B",
            isOledDark = preferences[PreferencesKeys.IS_OLED_DARK] ?: false,
            showAllTokens = preferences[PreferencesKeys.SHOW_ALL_TOKENS] ?: false,
            minerIntensity = preferences[PreferencesKeys.MINER_INTENSITY] ?: 2,
            lastKnownBalance = preferences[PreferencesKeys.LAST_KNOWN_BALANCE] ?: "0.00000000",
            lastKnownHeight = preferences[PreferencesKeys.LAST_KNOWN_HEIGHT] ?: 0L
        )
    }

    suspend fun updateNodeUrl(url: String) {
        val cleanUrl = url.trim().trimEnd('/')
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NODE_URL] = cleanUrl
        }
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun updateAccentColor(hex: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCENT_COLOR_HEX] = hex
        }
    }

    suspend fun updateOledDark(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_OLED_DARK] = enabled
        }
    }

    suspend fun updateShowAllTokens(showAll: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_ALL_TOKENS] = showAll
        }
    }

    suspend fun updateMinerIntensity(intensity: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MINER_INTENSITY] = intensity
        }
    }

    suspend fun updateLastKnownState(balance: String, height: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_KNOWN_BALANCE] = balance
            preferences[PreferencesKeys.LAST_KNOWN_HEIGHT] = height
        }
    }
}
