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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourapp.obd.ui.theme.AccentCyan
import com.yourapp.obd.ui.theme.AlertOrange
import com.yourapp.obd.ui.theme.AlertRed
import com.yourapp.obd.ui.theme.AlertYellow
import com.yourapp.obd.ui.theme.DarkBackground
import com.yourapp.obd.ui.theme.DarkSurface
import com.yourapp.obd.ui.theme.GreenOk
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
        updateResult?.let { snackbarHostState.showSnackbar(it); viewModel.clearUpdateResult() }
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
                Snackbar(snackbarData = data, containerColor = DarkSurface, contentColor = Color.White)
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
            // ── Bluetooth ──────────────────────────────────────────────────
            SectionCard("Bluetooth OBD-II") {
                BluetoothDeviceSelector(state.selectedDeviceAddress, viewModel)
            }

            // ── Видеорегистратор ───────────────────────────────────────────
            SectionCard("Видеорегистратор") {
                DropdownSetting(
                    label   = "Разрешение видео",
                    current = state.videoResolution,
                    options = listOf("FHD" to "1080p Full HD", "HD" to "720p HD", "SD" to "480p SD"),
                    onSelect = { viewModel.setVideoResolution(it) }
                )
                Spacer(Modifier.height(8.dp))
                DropdownSetting(
                    label   = "Длительность ролика",
                    current = state.segmentDurationMin.toString(),
                    options = listOf("1" to "1 мин","3" to "3 мин","5" to "5 мин","10" to "10 мин","15" to "15 мин"),
                    onSelect = { viewModel.setSegmentDurationMin(it.toInt()) }
                )
                Spacer(Modifier.height(8.dp))
                DropdownSetting(
                    label   = "Размер буфера",
                    current = state.bufferSizeGb.toString(),
                    options = listOf("1" to "1 ГБ","2" to "2 ГБ","4" to "4 ГБ","8" to "8 ГБ","16" to "16 ГБ"),
                    onSelect = { viewModel.setBufferSizeGb(it.toInt()) }
                )
            }

            // ── Базы SpeedCam ──────────────────────────────────────────────
            SectionCard("Базы камер SpeedCam") {
                Text("Источники баз данных камер контроля скорости",
                    color = Color.Gray, fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp))
                UrlInputField("Источник 1", state.speedcamUrl1) { viewModel.setSpeedcamUrl(1, it) }
                Spacer(Modifier.height(8.dp))
                UrlInputField("Источник 2", state.speedcamUrl2) { viewModel.setSpeedcamUrl(2, it) }
                Spacer(Modifier.height(8.dp))
                UrlInputField("Источник 3", state.speedcamUrl3) { viewModel.setSpeedcamUrl(3, it) }
                Spacer(Modifier.height(12.dp))
                if (state.speedcamLastUpdate > 0L) {
                    Text(
                        "Последнее обновление: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(state.speedcamLastUpdate))}",
                        color = Color.Gray, fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Button(
                    onClick  = { viewModel.updateSpeedcamDatabases() },
                    enabled  = !isUpdating,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan, disabledContainerColor = Color.DarkGray),
                    shape    = RoundedCornerShape(10.dp)
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Обновление...", color = Color.White)
                    } else {
                        Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Обновить базы", color = Color.White)
                    }
                }
            }

            // ── Калибровка ADAS ────────────────────────────────────────────
            SectionCard("Калибровка ADAS") {
                Text(
                    "Настройте положение горизонта и ширину полосы под вашу камеру. " +
                    "Установите дистанции зон безопасности в метрах.",
                    color = Color.Gray, fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Горизонт (точка схода)
                SliderSetting(
                    label    = "Горизонт (точка схода)",
                    value    = state.adasHorizonPercent,
                    valueStr = "${"%.0f".format(state.adasHorizonPercent)}% от верха",
                    min      = 25f,
                    max      = 65f,
                    color    = AccentCyan,
                    onValueChangeFinished = { viewModel.setAdasHorizon(it) }
                )
                Spacer(Modifier.height(4.dp))

                // Ширина полосы
                SliderSetting(
                    label    = "Ширина полосы",
                    value    = state.adasLaneWidthPercent,
                    valueStr = "${"%.0f".format(state.adasLaneWidthPercent)}% ширины экрана",
                    min      = 10f,
                    max      = 45f,
                    color    = GreenOk,
                    onValueChangeFinished = { viewModel.setAdasLaneWidth(it) }
                )
                Spacer(Modifier.height(12.dp))

                // Зоны дистанций
                Text("Зоны дистанций", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))

                DistanceZoneRow("ОПАСНОСТЬ", state.adasDangerDistM,  AlertRed,    1..15)  { viewModel.setAdasDangerDist(it) }
                DistanceZoneRow("ВНИМАНИЕ",  state.adasWarningDistM, AlertOrange, 5..25)  { viewModel.setAdasWarningDist(it) }
                DistanceZoneRow("ОСТОРОЖНО", state.adasCautionDistM, AlertYellow, 10..40) { viewModel.setAdasCautionDist(it) }
                DistanceZoneRow("БЕЗОПАСНО", state.adasSafeDistM,    GreenOk,    20..60) { viewModel.setAdasSafeDist(it) }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick  = { viewModel.resetAdasCalibration() },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray)
                ) {
                    Text("Сбросить калибровку по умолчанию", color = Color.Gray, fontSize = 13.sp)
                }
            }

            // ── ADAS модули ────────────────────────────────────────────────
            SectionCard("ADAS — Чувствительность") {
                DropdownSetting(
                    label   = "Чувствительность",
                    current = state.adasSensitivity,
                    options = listOf("LOW" to "Низкая","MEDIUM" to "Средняя","HIGH" to "Высокая"),
                    onSelect = { viewModel.setAdasSensitivity(it) }
                )
            }

            SectionCard("ADAS — Модули") {
                SettingsSwitch("LDW — Выезд из полосы",             state.ldwEnabled)        { viewModel.setLdwEnabled(it) }
                SettingsSwitch("FCW — Предупреждение о столкновении", state.fcwEnabled)       { viewModel.setFcwEnabled(it) }
                SettingsSwitch("Детекция знаков скорости",           state.signEnabled)       { viewModel.setSignEnabled(it) }
                SettingsSwitch("DMS — Усталость водителя",           state.dmsEnabled)        { viewModel.setDmsEnabled(it) }
                SettingsSwitch("Детекция пешеходов",                 state.pedestrianEnabled) { viewModel.setPedestrianEnabled(it) }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Компоненты ─────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = DarkSurface),
        shape    = RoundedCornerShape(12.dp)
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
    valueStr: String,
    min: Float,
    max: Float,
    color: Color,
    onValueChangeFinished: (Float) -> Unit
) {
    var sliderVal by remember(value) { mutableStateOf(value) }
    Column {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(label, color = Color.White, fontSize = 13.sp)
            Text(valueStr, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Slider(
            value          = sliderVal,
            onValueChange  = { sliderVal = it },
            onValueChangeFinished = { onValueChangeFinished(sliderVal) },
            valueRange     = min..max,
            modifier       = Modifier.fillMaxWidth(),
            colors         = SliderDefaults.colors(
                thumbColor       = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.25f)
            )
        )
    }
}

@Composable
private fun DistanceZoneRow(
    label: String,
    value: Int,
    color: Color,
    range: IntRange,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = 6.dp),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .size(10.dp)
                    .background(color, androidx.compose.foundation.shape.CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(label, color = Color.White, fontSize = 13.sp)
            }
            Text("$value м", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkSurface)) {
            range.step(if (range.last > 30) 5 else if (range.last > 15) 2 else 1).forEach { m ->
                DropdownMenuItem(
                    text    = { Text("$m м", color = if (m == value) color else Color.White) },
                    onClick = { onSelect(m); expanded = false }
                )
            }
        }
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
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }.padding(vertical = 8.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 14.sp)
            Text(display, color = AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkSurface)) {
            options.forEach { (v, d) ->
                DropdownMenuItem(
                    text    = { Text(d, color = if (v == current) AccentCyan else Color.White) },
                    onClick = { onSelect(v); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun UrlInputField(label: String, value: String, onValueChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value         = text,
        onValueChange = { text = it },
        label         = { Text(label, fontSize = 12.sp) },
        placeholder   = { Text("https://...", color = Color.DarkGray, fontSize = 12.sp) },
        modifier      = Modifier.fillMaxWidth(),
        singleLine    = true,
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = AccentCyan, unfocusedBorderColor = Color.DarkGray,
            focusedLabelColor    = AccentCyan, unfocusedLabelColor  = Color.Gray,
            focusedTextColor     = Color.White, unfocusedTextColor  = Color.White,
            cursorColor          = AccentCyan
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onValueChange(text) }),
        textStyle       = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
    )
}

@Composable
private fun BluetoothDeviceSelector(selectedAddress: String, viewModel: SettingsViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var devices  by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    Box {
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable { try { devices = viewModel.getPairedDevices(); expanded = true } catch (_: SecurityException) {} }
                .padding(vertical = 8.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically
        ) {
            Text("Устройство ELM327", color = Color.White, fontSize = 14.sp)
            Text(if (selectedAddress.isBlank()) "Не выбрано" else selectedAddress,
                color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkSurface)) {
            if (devices.isEmpty()) {
                DropdownMenuItem(text = { Text("Нет сопряжённых устройств", color = Color.Gray) },
                    onClick = { expanded = false })
            }
            devices.forEach { dev ->
                val name = try { dev.name ?: "Неизвестно" } catch (_: SecurityException) { "Неизвестно" }
                DropdownMenuItem(
                    text = { Column {
                        Text(name, color = Color.White, fontSize = 13.sp)
                        Text(dev.address, color = Color.Gray, fontSize = 11.sp)
                    }},
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
        Arrangement.SpaceBetween, Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = AccentCyan,
                checkedTrackColor  = AccentCyan.copy(alpha = 0.4f)
            ))
    }
}
