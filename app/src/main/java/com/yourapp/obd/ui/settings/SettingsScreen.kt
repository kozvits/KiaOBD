package com.yourapp.obd.ui.settings

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourapp.obd.ui.theme.AccentCyan
import com.yourapp.obd.ui.theme.AlertRed
import com.yourapp.obd.ui.theme.DarkBackground
import com.yourapp.obd.ui.theme.DarkSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.settingsState.collectAsStateWithLifecycle()
    val isUpdating by viewModel.isUpdatingSpeedcam.collectAsStateWithLifecycle()
    val updateResult by viewModel.speedcamUpdateResult.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(updateResult) {
        updateResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUpdateResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = DarkSurface,
                    contentColor = Color.White
                )
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Bluetooth OBD-II ─────────────────────────────────────────────
            SectionCard(title = "Bluetooth OBD-II") {
                BluetoothDeviceSelector(
                    selectedAddress = state.selectedDeviceAddress,
                    viewModel = viewModel
                )
            }

            // ── Видеорегистратор ─────────────────────────────────────────────
            SectionCard(title = "Видеорегистратор") {
                DropdownSetting(
                    label = "Разрешение видео",
                    current = state.videoResolution,
                    options = listOf("FHD" to "1080p Full HD", "HD" to "720p HD", "SD" to "480p SD"),
                    onSelect = { viewModel.setVideoResolution(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                DropdownSetting(
                    label = "Длительность ролика",
                    current = state.segmentDurationMin.toString(),
                    options = listOf("1" to "1 мин", "3" to "3 мин", "5" to "5 мин", "10" to "10 мин", "15" to "15 мин"),
                    onSelect = { viewModel.setSegmentDurationMin(it.toInt()) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                DropdownSetting(
                    label = "Размер буфера",
                    current = state.bufferSizeGb.toString(),
                    options = listOf("1" to "1 ГБ", "2" to "2 ГБ", "4" to "4 ГБ", "8" to "8 ГБ", "16" to "16 ГБ"),
                    onSelect = { viewModel.setBufferSizeGb(it.toInt()) }
                )
            }

            // ── Базы SpeedCam ────────────────────────────────────────────────
            SectionCard(title = "Базы камер SpeedCam") {
                Text(
                    text = "Источники обновления баз данных камер контроля скорости",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                UrlInputField(
                    label = "Источник 1",
                    value = state.speedcamUrl1,
                    onValueChange = { viewModel.setSpeedcamUrl(1, it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                UrlInputField(
                    label = "Источник 2",
                    value = state.speedcamUrl2,
                    onValueChange = { viewModel.setSpeedcamUrl(2, it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                UrlInputField(
                    label = "Источник 3",
                    value = state.speedcamUrl3,
                    onValueChange = { viewModel.setSpeedcamUrl(3, it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Последнее обновление
                if (state.speedcamLastUpdate > 0L) {
                    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        .format(Date(state.speedcamLastUpdate))
                    Text(
                        text = "Последнее обновление: $dateStr",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Кнопка обновить
                Button(
                    onClick = { viewModel.updateSpeedcamDatabases() },
                    enabled = !isUpdating,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan,
                        disabledContainerColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Обновление...", color = Color.White)
                    } else {
                        Icon(Icons.Default.Refresh, null, tint = Color.White,
                            modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Обновить базы", color = Color.White)
                    }
                }
            }

            // ── ADAS Чувствительность ────────────────────────────────────────
            SectionCard(title = "ADAS — Чувствительность") {
                DropdownSetting(
                    label = "Чувствительность",
                    current = state.adasSensitivity,
                    options = listOf("LOW" to "Низкая", "MEDIUM" to "Средняя", "HIGH" to "Высокая"),
                    onSelect = { viewModel.setAdasSensitivity(it) }
                )
            }

            // ── ADAS Модули ──────────────────────────────────────────────────
            SectionCard(title = "ADAS — Модули") {
                SettingsSwitch("LDW — Выезд из полосы", state.ldwEnabled) { viewModel.setLdwEnabled(it) }
                SettingsSwitch("FCW — Предупреждение о столкновении", state.fcwEnabled) { viewModel.setFcwEnabled(it) }
                SettingsSwitch("Детекция знаков скорости", state.signEnabled) { viewModel.setSignEnabled(it) }
                SettingsSwitch("DMS — Усталость водителя", state.dmsEnabled) { viewModel.setDmsEnabled(it) }
                SettingsSwitch("Детекция пешеходов", state.pedestrianEnabled) { viewModel.setPedestrianEnabled(it) }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Компоненты ────────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = AccentCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DropdownSetting(
    label: String,
    current: String,
    options: List<Pair<String, String>>,  // value to displayLabel
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayLabel = options.firstOrNull { it.first == current }?.second ?: current

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 14.sp)
            Text(displayLabel, color = AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkSurface)
        ) {
            options.forEach { (value, display) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            display,
                            color = if (value == current) AccentCyan else Color.White
                        )
                    },
                    onClick = { onSelect(value); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun UrlInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var text by remember(value) { mutableStateOf(value) }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text("https://...", color = Color.DarkGray, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentCyan,
            unfocusedBorderColor = Color.DarkGray,
            focusedLabelColor = AccentCyan,
            unfocusedLabelColor = Color.Gray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = AccentCyan
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onValueChange(text) }),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
    )
}

@Composable
private fun BluetoothDeviceSelector(
    selectedAddress: String,
    viewModel: SettingsViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        devices = viewModel.getPairedDevices()
                        expanded = true
                    } catch (_: SecurityException) {}
                }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Устройство ELM327", color = Color.White, fontSize = 14.sp)
            Text(
                text = if (selectedAddress.isBlank()) "Не выбрано" else selectedAddress,
                color = AccentCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkSurface)
        ) {
            if (devices.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Нет сопряжённых устройств", color = Color.Gray) },
                    onClick = { expanded = false }
                )
            }
            devices.forEach { device ->
                val name = try { device.name ?: "Неизвестно" } catch (_: SecurityException) { "Неизвестно" }
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(name, color = Color.White, fontSize = 13.sp)
                            Text(device.address, color = Color.Gray, fontSize = 11.sp)
                        }
                    },
                    onClick = { viewModel.selectDevice(device.address); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentCyan,
                checkedTrackColor = AccentCyan.copy(alpha = 0.4f)
            )
        )
    }
}
