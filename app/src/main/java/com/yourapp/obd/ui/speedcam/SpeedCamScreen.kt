package com.yourapp.obd.ui.speedcam

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
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
import com.yourapp.obd.data.speedcam.SpeedCamConstants
import com.yourapp.obd.ui.theme.AccentCyan
import com.yourapp.obd.ui.theme.AlertRed
import com.yourapp.obd.ui.theme.DarkBackground
import com.yourapp.obd.ui.theme.DarkSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedCamScreen(
    onBack: () -> Unit,
    viewModel: SpeedCamViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isUpdating by viewModel.isUpdating.collectAsStateWithLifecycle()
    val isRollingBack by viewModel.isRollingBack.collectAsStateWithLifecycle()
    val resultMessage by viewModel.resultMessage.collectAsStateWithLifecycle()

    LaunchedEffect(resultMessage) {
        resultMessage?.let {
            viewModel.clearResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сигнатурный радар и SpeedCam", color = Color.White) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Секция: статус и статистика ──
            item(key = "status") {
                SectionCard("Состояние базы") {
                    if (state.totalCameras > 0) {
                        Text(
                            "Активных камер в базе: ${state.totalCameras}",
                            color = AccentCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    if (state.lastUpdateTimestamp > 0L) {
                        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                            .format(Date(state.lastUpdateTimestamp))
                        Text(
                            "Последнее обновление: $dateStr",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            "База ещё не обновлялась",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Типы камер: стационарные скорости, красный свет, средняя скорость, передвижные",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            // ── Секция: источники данных ──
            item(key = "sources") {
                SectionCard("Источники данных") {
                    Text(
                        "URL для загрузки баз камер контроля скорости. " +
                        "Поддерживаются JSON, CSV и OSM Overpass API.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    PresetSelector(
                        presets = SpeedCamConstants.PRESETS,
                        onSelectPreset = { index -> viewModel.applyPreset(index) },
                        onAutoDetect = { viewModel.applyCountryPreset() }
                    )

                    Spacer(Modifier.height(8.dp))
                    UrlInput("Источник 1", state.url1) { viewModel.setUrl(1, it) }
                    Spacer(Modifier.height(8.dp))
                    UrlInput("Источник 2", state.url2) { viewModel.setUrl(2, it) }
                    Spacer(Modifier.height(8.dp))
                    UrlInput("Источник 3", state.url3) { viewModel.setUrl(3, it) }
                }
            }

            // ── Секция: обновление ──
            item(key = "update") {
                SectionCard("Обновление") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Автообновление ежедневно в 03:00",
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = state.autoUpdate,
                            onCheckedChange = { viewModel.setAutoUpdate(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentCyan,
                                checkedTrackColor = AccentCyan.copy(alpha = 0.4f)
                            )
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.updateDatabases() },
                        enabled = !isUpdating && !isRollingBack,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCyan,
                            disabledContainerColor = Color.DarkGray
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Обновление...", color = Color.White)
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Обновить базу камер", color = Color.White)
                        }
                    }

                    resultMessage?.let { msg ->
                        if (msg.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                msg,
                                color = if (msg.startsWith("Ошибка")) AlertRed else AccentCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (state.rollbackAvailable) {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.rollbackUpdate() },
                            enabled = !isRollingBack && !isUpdating,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B0000),
                                disabledContainerColor = Color.DarkGray
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isRollingBack) {
                                CircularProgressIndicator(
                                    Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Откат...", color = Color.White)
                            } else {
                                Text("↩ Откатить последнее обновление", color = Color.White)
                            }
                        }
                    }
                }
            }

            // ── Секция: история обновлений ──
            if (state.updateHistory.isNotEmpty()) {
                item(key = "history") {
                    SectionCard("История обновлений") {
                        state.updateHistory.take(10).forEach { item ->
                            val dateStr = SimpleDateFormat(
                                "dd.MM.yyyy HH:mm",
                                Locale.getDefault()
                            ).format(Date(item.timestamp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    dateStr,
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    modifier = Modifier.width(90.dp)
                                )
                                Text(
                                    item.summary,
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // ── Секция: описание типов ──
            item(key = "types") {
                SectionCard("Типы камер") {
                    CameraTypeRow("SPEED", "Стационарная камера скорости", AccentCyan)
                    CameraTypeRow("REDLIGHT", "Камера проезда на красный", Color(0xFFFF6D00))
                    CameraTypeRow("AVERAGE", "Камера средней скорости", Color(0xFF00E676))
                    CameraTypeRow("MOBILE", "Передвижная камера", Color(0xFFFFC107))
                    CameraTypeRow("TOLL", "Камера оплаты проезда", Color(0xFF9C27B0))
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Компоненты ───────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                color = AccentCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun UrlInput(label: String, value: String, onValueChange: (String) -> Unit) {
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
private fun PresetSelector(
    presets: List<SpeedCamConstants.SourcePreset>,
    onSelectPreset: (Int) -> Unit,
    onAutoDetect: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .background(DarkSurface, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Выберите предустановку...",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ExpandMore,
                    null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(DarkSurface)
                    .widthIn(max = 360.dp)
            ) {
                presets.forEachIndexed { index, preset ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(preset.name, color = Color.White, fontSize = 13.sp)
                                Text(preset.description, color = Color.Gray, fontSize = 10.sp)
                            }
                        },
                        onClick = {
                            expanded = false
                            onSelectPreset(index)
                        }
                    )
                }
            }
        }
        Button(
            onClick = onAutoDetect,
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                Icons.Default.Sensors,
                null,
                tint = AccentCyan,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Авто", color = AccentCyan, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CameraTypeRow(code: String, label: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$code — $label",
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}
