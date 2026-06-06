package com.yourapp.obd.ui.speedcam

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.obd.data.speedcam.SpeedCamConstants
import com.yourapp.obd.data.speedcam.SpeedCamNotificationHelper
import com.yourapp.obd.data.speedcam.SpeedCamRepository
import com.yourapp.obd.data.speedcam.SpeedCamSourceProvider
import com.yourapp.obd.data.speedcam.SpeedCamUpdateLogEntity
import com.yourapp.obd.data.speedcam.SpeedCamUpdateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpeedCamScreenState(
    val url1: String = "",
    val url2: String = "",
    val url3: String = "",
    val autoUpdate: Boolean = true,
    val lastUpdateTimestamp: Long = 0L,
    val totalCameras: Int = 0,
    val rollbackAvailable: Boolean = false,
    val updateHistory: List<SpeedCamUpdateHistoryItem> = emptyList()
)

data class SpeedCamUpdateHistoryItem(
    val timestamp: Long,
    val summary: String,
    val newCount: Int,
    val removedCount: Int,
    val modifiedCount: Int,
    val totalActive: Int,
    val rollbackAvailable: Boolean
)

@HiltViewModel
class SpeedCamViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val speedCamRepository: SpeedCamRepository
) : ViewModel() {

    companion object {
        val KEY_URL1 = stringPreferencesKey("speedcam_url1")
        val KEY_URL2 = stringPreferencesKey("speedcam_url2")
        val KEY_URL3 = stringPreferencesKey("speedcam_url3")
        val KEY_LAST_UPD = stringPreferencesKey("speedcam_last_update")
        val KEY_AUTO_UPD = booleanPreferencesKey("speedcam_auto_update")
    }

    private val _state = MutableStateFlow(SpeedCamScreenState())
    val state: StateFlow<SpeedCamScreenState> = _state.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    private val _isRollingBack = MutableStateFlow(false)
    val isRollingBack: StateFlow<Boolean> = _isRollingBack.asStateFlow()

    private val _resultMessage = MutableStateFlow<String?>(null)
    val resultMessage: StateFlow<String?> = _resultMessage.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                _state.value = _state.value.copy(
                    url1 = prefs[KEY_URL1] ?: "",
                    url2 = prefs[KEY_URL2] ?: "",
                    url3 = prefs[KEY_URL3] ?: "",
                    autoUpdate = prefs[KEY_AUTO_UPD] ?: true,
                    lastUpdateTimestamp = prefs[KEY_LAST_UPD]?.toLongOrNull() ?: 0L
                )
            }
        }
        viewModelScope.launch {
            refreshDbState()
        }
        viewModelScope.launch {
            speedCamRepository.lastUpdateResult.collect { result ->
                if (result != null) {
                    _resultMessage.value = result
                    refreshDbState()
                }
            }
        }
    }

    private suspend fun refreshDbState() {
        try {
            val history = speedCamRepository.getUpdateHistory()
            val snapshotInfo = speedCamRepository.rollbackManager.getSnapshotInfo()
            val totalActive = speedCamRepository.getTotalActive()
            _state.value = _state.value.copy(
                totalCameras = totalActive,
                rollbackAvailable = snapshotInfo.available,
                updateHistory = history.map { it.toHistoryItem() }
            )
        } catch (_: Exception) { }
    }

    private fun SpeedCamUpdateLogEntity.toHistoryItem() = SpeedCamUpdateHistoryItem(
        timestamp = timestamp,
        summary = summary,
        newCount = newCameras,
        removedCount = removedCameras,
        modifiedCount = modifiedCameras,
        totalActive = totalActive,
        rollbackAvailable = rollbackAvailable
    )

    fun setUrl(index: Int, url: String) {
        viewModelScope.launch {
            dataStore.edit {
                when (index) {
                    1 -> it[KEY_URL1] = url
                    2 -> it[KEY_URL2] = url
                    3 -> it[KEY_URL3] = url
                }
            }
        }
    }

    fun applyPreset(index: Int) {
        val preset = SpeedCamConstants.PRESETS.getOrNull(index) ?: return
        viewModelScope.launch {
            dataStore.edit {
                it[KEY_URL1] = preset.url1
                it[KEY_URL2] = preset.url2
                it[KEY_URL3] = preset.url3
            }
        }
    }

    fun applyCountryPreset() {
        val countryIso = SpeedCamSourceProvider.extractCountryIso()
        val overpassUrl = if (countryIso != null) {
            SpeedCamSourceProvider.buildOverpassUrl(countryIso)
        } else {
            SpeedCamSourceProvider.buildOverpassUrl()
        }
        viewModelScope.launch {
            dataStore.edit {
                it[KEY_URL1] = overpassUrl
                it[KEY_URL2] = ""
                it[KEY_URL3] = ""
            }
        }
    }

    fun setAutoUpdate(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_AUTO_UPD] = enabled }
            if (enabled) {
                SpeedCamUpdateWorker.schedule(context)
            } else {
                SpeedCamUpdateWorker.cancel(context)
            }
        }
    }

    fun updateDatabases() {
        viewModelScope.launch {
            _isUpdating.value = true
            _resultMessage.value = null
            val s = state.value
            val urls = listOf(s.url1, s.url2, s.url3).filter { it.isNotBlank() }
            if (urls.isEmpty()) {
                _resultMessage.value = "Нет источников для обновления"
                _isUpdating.value = false
                return@launch
            }
            val stats = speedCamRepository.updateFromSources(urls)
            dataStore.edit { it[KEY_LAST_UPD] = stats.timestamp.toString() }
            _resultMessage.value = stats.summary
            _isUpdating.value = false
            refreshDbState()
            if (!stats.isError) {
                SpeedCamNotificationHelper.notifyUpdateSuccess(context, stats.summary)
            }
        }
    }

    fun rollbackUpdate() {
        viewModelScope.launch {
            _isRollingBack.value = true
            val stats = speedCamRepository.rollbackLastUpdate()
            refreshDbState()
            _resultMessage.value = if (!stats.isError) {
                "Откат выполнен успешно. Всего камер: ${stats.totalActive}"
            } else {
                "Откат недоступен — нет снапшота"
            }
            _isRollingBack.value = false
        }
    }

    fun clearResult() { _resultMessage.value = null }
}
