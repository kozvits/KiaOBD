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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourapp.obd.ui.theme.AccentCyan
import com.yourapp.obd.ui.theme.DarkBackground
import com.yourapp.obd.ui.theme.DarkSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    scrollToSection: String? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state          by viewModel.settingsState.collectAsStateWithLifecycle()
    val isUpdating     by viewModel.isUpdatingSpeedcam.collectAsStateWithLifecycle()
    val isRollingBack  by viewModel.isRollingBack.collectAsStateWithLifecycle()
    val updateResult   by viewModel.speedcamUpdateResult.collectAsStateWithLifecycle()
    val snackbar       = remember { SnackbarHostState() }
    val listState      = rememberLazyListState()

    val sectionIndex = when (scrollToSection) {
        "adas_calibration" -> 3
        "adas_sensitivity" -> 4
        "adas_modules"     -> 5
        else -> null
    }

    LaunchedEffect(updateResult) {
        updateResult?.let { snackbar.showSnackbar(it); viewModel.clearUpdateResult() }
    }

    LaunchedEffect(sectionIndex) {
        if (sectionIndex != null) {
            listState.animateScrollToItem(sectionIndex)
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
            SnackbarHost(snackbar) { data ->
                Snackbar(snackbarData = data, containerColor = DarkSurface, contentColor = Color.White)
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "bluetooth") {
                SectionCard("Bluetooth OBD-II") {
                    BluetoothDeviceSelector(state.selectedDeviceAddress, viewModel)
                }
            }

            item(key = "video") {
                SectionCard("Видеорегистратор") {
                    DropdownSetting("Разрешение видео", state.videoResolution,
                        listOf("FHD" to "1080p Full HD", "HD" to "720p HD", "SD" to "480p SD")
                    ) { viewModel.setVideoResolution(it) }
                    Spacer(Modifier.height(8.dp))
                    DropdownSetting("Длительность ролика", state.segmentDurationMin.toString(),
                        listOf("1" to "1 мин","3" to "3 мин","5" to "5 мин","10" to "10 мин","15" to "15 мин")
                    ) { viewModel.setSegmentDurationMin(it.toInt()) }
                    Spacer(Modifier.height(8.dp))
                    DropdownSetting("Размер буфера", state.bufferSizeGb.toString(),
                        listOf("1" to "1 ГБ","2" to "2 ГБ","4" to "4 ГБ","8" to "8 ГБ","16" to "16 ГБ")
                    ) { viewModel.setBufferSizeGb(it.toInt()) }
                }
            }

            item(key = "speedcam") {
                SectionCard("Базы камер SpeedCam") {
                    Text("Настройки камер SpeedCam перенесены в отдельное меню:",
                        color = Color.Gray, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Меню → Сигнатурный радар → База камер SpeedCam",
                        color = AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    if (state.speedcamTotalCameras > 0) {
                        Text("В базе: ${state.speedcamTotalCameras} камер",
                            color = Color.White, fontSize = 12.sp)
                    }
                    if (state.speedcamLastUpdate > 0L) {
                        Text(
                            "Последнее обновление: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(state.speedcamLastUpdate))}",
                            color = Color.Gray, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    SettingsSwitch(
                        label = "Автообновление ежедневно в 03:00",
                        checked = state.speedcamAutoUpdate,
                        onCheckedChange = { viewModel.setSpeedcamAutoUpdate(it) }
                    )
                }
            }

            item(key = "adas_calibration") {
                SectionCard("Калибровка ADAS") {
                    Text(
                        "Настройте параметры сетки под реальный вид дороги с камеры вашего автомобиля",
                        color = Color.Gray, fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    SliderSetting(
                        label = "Линия горизонта",
                        value = state.horizonPosition,
                        range = 0.25f..0.65f,
                        displayValue = "${"%.0f".format(state.horizonPosition * 100)}% от верха",
                        onValueChange = { viewModel.setHorizonPosition(it) }
                    )

                    Spacer(Modifier.height(8.dp))

                    SliderSetting(
                        label = "Ширина полосы",
                        value = state.laneWidthPercent,
                        range = 0.10f..0.45f,
                        displayValue = "${"%.0f".format(state.laneWidthPercent * 100)}% ширины экрана",
                        onValueChange = { viewModel.setLaneWidthPercent(it) }
                    )

                    Spacer(Modifier.height(8.dp))

                    SliderSetting(
                        label = "Точка схода (горизонт X)",
                        value = state.vanishingPointX,
                        range = 0.25f..0.75f,
                        displayValue = "${"%.0f".format(state.vanishingPointX * 100)}% от левого края",
                        onValueChange = { viewModel.setVanishingPointX(it) }
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Зоны расстояний", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    DropdownSetting("Зона ОПАСНОСТЬ (красная)", state.dangerZoneM.toString(),
                        listOf("3" to "3 м","5" to "5 м","7" to "7 м","10" to "10 м")
                    ) { viewModel.setDangerZoneM(it.toInt()) }
                    Spacer(Modifier.height(8.dp))

                    DropdownSetting("Зона ВНИМАНИЕ (оранжевая)", state.warningZoneM.toString(),
                        listOf("8" to "8 м","10" to "10 м","15" to "15 м","20" to "20 м")
                    ) { viewModel.setWarningZoneM(it.toInt()) }
                    Spacer(Modifier.height(8.dp))

                    DropdownSetting("Зона ОСТОРОЖНО (жёлтая)", state.cautionZoneM.toString(),
                        listOf("15" to "15 м","20" to "20 м","25" to "25 м","30" to "30 м")
                    ) { viewModel.setCautionZoneM(it.toInt()) }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.setHorizonPosition(0.42f)
                            viewModel.setLaneWidthPercent(0.28f)
                            viewModel.setVanishingPointX(0.5f)
                            viewModel.setDangerZoneM(5)
                            viewModel.setWarningZoneM(10)
                            viewModel.setCautionZoneM(20)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2D2D)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Сбросить к умолчаниям", color = Color.Gray)
                    }
                }
            }

            item(key = "adas_sensitivity") {
                SectionCard("ADAS — Чувствительность") {
                    DropdownSetting("Чувствительность", state.adasSensitivity,
                        listOf("LOW" to "Низкая","MEDIUM" to "Средняя","HIGH" to "Высокая")
                    ) { viewModel.setAdasSensitivity(it) }
                }
            }

            item(key = "adas_modules") {
                SectionCard("ADAS — Модули") {
                    SettingsSwitch("LDW — Выезд из полосы",              state.ldwEnabled)            { viewModel.setLdwEnabled(it) }
                    SettingsSwitch("FCW — Предупреждение о столкновении", state.fcwEnabled)            { viewModel.setFcwEnabled(it) }
                    SettingsSwitch("Детекция знаков скорости",            state.signEnabled)           { viewModel.setSignEnabled(it) }
                    SettingsSwitch("DMS — Усталость водителя",            state.dmsEnabled)            { viewModel.setDmsEnabled(it) }
                    SettingsSwitch("Детекция пешеходов",                  state.pedestrianEnabled)     { viewModel.setPedestrianEnabled(it) }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Переиспользуемые компоненты ───────────────────────────────────────────────

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = AccentCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 13.sp)
            Text(displayValue, color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = AccentCyan,
                activeTrackColor = AccentCyan,
                inactiveTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
private fun DropdownSetting(
    label: String,
    current: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val display = options.firstOrNull { it.first == current }?.second ?: current
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 13.sp)
            Text(display, color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkSurface)) {
            options.forEach { (value, disp) ->
                DropdownMenuItem(
                    text = { Text(disp, color = if (value == current) AccentCyan else Color.White) },
                    onClick = { onSelect(value); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun BluetoothDeviceSelector(selectedAddress: String, viewModel: SettingsViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try { devices = viewModel.getPairedDevices(); expanded = true }
                    catch (_: SecurityException) {}
                }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Устройство ELM327", color = Color.White, fontSize = 13.sp)
            Text(
                if (selectedAddress.isBlank()) "Не выбрано" else selectedAddress,
                color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkSurface)) {
            if (devices.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Нет сопряжённых устройств", color = Color.Gray) },
                    onClick = { expanded = false }
                )
            }
            devices.forEach { dev ->
                val name = try { dev.name ?: "Неизвестно" } catch (_: SecurityException) { "Неизвестно" }
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(name, color = Color.White, fontSize = 13.sp)
                            Text(dev.address, color = Color.Gray, fontSize = 11.sp)
                        }
                    },
                    onClick = { viewModel.selectDevice(dev.address); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentCyan, checkedTrackColor = AccentCyan.copy(alpha = 0.4f)))
    }
}
