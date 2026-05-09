package com.yourapp.obd.ui.settings

import android.Manifest
import android.bluetooth.BluetoothDevice
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.settingsState.collectAsStateWithLifecycle()

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
            SectionCard(title = "Bluetooth OBD-II") {
                BluetoothDeviceSelector(
                    selectedAddress = state.selectedDeviceAddress,
                    viewModel = viewModel
                )
            }

            SectionCard(title = "Видеорегистратор") {
                BufferSizeSelector(
                    currentGb = state.bufferSizeGb,
                    onSelect = { viewModel.setBufferSizeGb(it) }
                )
            }

            SectionCard(title = "ADAS — Чувствительность") {
                SensitivitySelector(
                    current = state.adasSensitivity,
                    onSelect = { viewModel.setAdasSensitivity(it) }
                )
            }

            SectionCard(title = "ADAS — Модули") {
                SettingsSwitch("LDW — Выезд из полосы", state.ldwEnabled) { viewModel.setLdwEnabled(it) }
                SettingsSwitch("FCW — Предупреждение о столкновении", state.fcwEnabled) { viewModel.setFcwEnabled(it) }
                SettingsSwitch("Детекция знаков скорости", state.signEnabled) { viewModel.setSignEnabled(it) }
                SettingsSwitch("DMS — Усталость водителя", state.dmsEnabled) { viewModel.setDmsEnabled(it) }
                SettingsSwitch("Детекция пешеходов", state.pedestrianEnabled) { viewModel.setPedestrianEnabled(it) }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = AccentCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun BluetoothDeviceSelector(
    selectedAddress: String,
    viewModel: SettingsViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }

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
        Text("Устройство", color = Color.White, fontSize = 14.sp)
        Text(
            text = if (selectedAddress.isBlank()) "Не выбрано" else selectedAddress,
            color = Color.Gray,
            fontSize = 12.sp
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
            DropdownMenuItem(
                text = {
                    Column {
                        try {
                            Text(device.name ?: "Неизвестно", color = Color.White, fontSize = 14.sp)
                        } catch (_: SecurityException) {}
                        Text(device.address, color = Color.Gray, fontSize = 11.sp)
                    }
                },
                onClick = {
                    viewModel.selectDevice(device.address)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun BufferSizeSelector(currentGb: Int, onSelect: (Int) -> Unit) {
    val options = listOf(1, 2, 4, 8, 16)
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Размер буфера", color = Color.White, fontSize = 14.sp)
        Text("$currentGb ГБ", color = Color.Gray, fontSize = 12.sp)
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.background(DarkSurface)
    ) {
        options.forEach { gb ->
            DropdownMenuItem(
                text = { Text("$gb ГБ", color = Color.White) },
                onClick = { onSelect(gb); expanded = false }
            )
        }
    }
}

@Composable
private fun SensitivitySelector(current: String, onSelect: (String) -> Unit) {
    val options = listOf("LOW", "MEDIUM", "HIGH")
    val labels = mapOf("LOW" to "Низкая", "MEDIUM" to "Средняя", "HIGH" to "Высокая")
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Чувствительность", color = Color.White, fontSize = 14.sp)
        Text(labels[current] ?: current, color = Color.Gray, fontSize = 12.sp)
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.background(DarkSurface)
    ) {
        options.forEach { opt ->
            DropdownMenuItem(
                text = { Text(labels[opt] ?: opt, color = Color.White) },
                onClick = { onSelect(opt); expanded = false }
            )
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
