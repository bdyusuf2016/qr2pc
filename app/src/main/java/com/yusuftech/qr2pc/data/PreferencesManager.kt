package com.yusuftech.qr2pc.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    companion object {
        val SERVER_IP = stringPreferencesKey("server_ip")
        val ALLOW_DUPLICATES = androidx.datastore.preferences.core.booleanPreferencesKey("allow_duplicates")
        val SOUND_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("vibration_enabled")
        val SPLIT_CHARACTER = stringPreferencesKey("split_character") // "None", "Enter", "Tab"
        val PAIRING_ID = stringPreferencesKey("pairing_id")
        val DATA_PROCESSING_MODE = stringPreferencesKey("data_processing_mode") // "None", "First N", "Last N", "Regex"
        val DATA_PROCESSING_VALUE = stringPreferencesKey("data_processing_value")
        val BATCH_MODE = androidx.datastore.preferences.core.booleanPreferencesKey("batch_mode")
        val SCAN_MODE = stringPreferencesKey("scan_mode") // "QR" or "TEXT"
        val APP_LANGUAGE = stringPreferencesKey("app_language") // "sys", "en", "bn"
    }

    val serverIp: Flow<String?> = context.dataStore.data.map { it[SERVER_IP] }
    val allowDuplicates: Flow<Boolean> = context.dataStore.data.map { it[ALLOW_DUPLICATES] ?: true }
    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { it[SOUND_ENABLED] ?: true }
    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { it[VIBRATION_ENABLED] ?: true }
    val splitCharacter: Flow<String> = context.dataStore.data.map { it[SPLIT_CHARACTER] ?: "Enter" }
    val pairingId: Flow<String> = context.dataStore.data.map { it[PAIRING_ID] ?: "0000" }
    val dataProcessingMode: Flow<String> = context.dataStore.data.map { it[DATA_PROCESSING_MODE] ?: "None" }
    val dataProcessingValue: Flow<String> = context.dataStore.data.map { it[DATA_PROCESSING_VALUE] ?: "" }
    val batchMode: Flow<Boolean> = context.dataStore.data.map { it[BATCH_MODE] ?: false }
    val scanMode: Flow<String> = context.dataStore.data.map { it[SCAN_MODE] ?: "QR" }
    val appLanguage: Flow<String> = context.dataStore.data.map { it[APP_LANGUAGE] ?: "sys" }

    suspend fun saveServerIp(ip: String) {
        context.dataStore.edit { it[SERVER_IP] = ip }
    }

    suspend fun setAllowDuplicates(allow: Boolean) {
        context.dataStore.edit { it[ALLOW_DUPLICATES] = allow }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[VIBRATION_ENABLED] = enabled }
    }

    suspend fun setSplitCharacter(character: String) {
        context.dataStore.edit { it[SPLIT_CHARACTER] = character }
    }

    suspend fun setPairingId(id: String) {
        context.dataStore.edit { it[PAIRING_ID] = id }
    }

    suspend fun setDataProcessingMode(mode: String) {
        context.dataStore.edit { it[DATA_PROCESSING_MODE] = mode }
    }

    suspend fun setDataProcessingValue(value: String) {
        context.dataStore.edit { it[DATA_PROCESSING_VALUE] = value }
    }

    suspend fun setBatchMode(enabled: Boolean) {
        context.dataStore.edit { it[BATCH_MODE] = enabled }
    }

    suspend fun setScanMode(mode: String) {
        context.dataStore.edit { it[SCAN_MODE] = mode }
    }

    suspend fun setAppLanguage(lang: String) {
        context.dataStore.edit { it[APP_LANGUAGE] = lang }
    }
}
