package com.yourapp.obd.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.hiltViewModel
import com.yourapp.obd.ui.theme.*
import com.yourapp.obd.domain.model.OBDData

@Composable
fun DashboardScreen(
    onNavigateToDtc: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val obdData by viewModel.obdData.collectAsStateWithLifecycle()
    var speedPriority by remember { mutableStateOf("OBD") } // OBD vs GPS

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- ЗАГОЛОВОК И ПРИОРИТЕТ СКОРОСТИ ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "БОРТОВОЙ КОМПЬЮТЕР",
                    color = AccentCyan,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // Переключатель приоритета скорости
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Скорость: ", color = Color.Gray, fontSize = 12.sp)
                    SegmentedButton(
                        selectedItem = speedPriority,
                        options = listOf("OBD", "GPS"),
                        onItemSelected = { speedPriority = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- ГЛАВНЫЙ ИНДИКАТОР (СПИДОМЕТР) ---
            MainSpeedometer(
                speed = obdData.speedKmh ?: 0,
                priority = speedPriority,
                modifier = Modifier.fillMaxWidth().height(250.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- РАЗДЕЛ ТЕЛЕМЕТРИИ (СЕТКА ПАРАМЕТРОВ) ---
            Text(
                text = "ТЕЛЕМЕТРИЯ ECU",
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            TelemetryGrid(obdData)

            Spacer(modifier = Modifier.height(24.dp))

            // --- GPS И ТРИП-КОМПЬЮТЕР ---
            TripComputerSection()

            Spacer(modifier = Modifier.height(32.dp))

            // --- КНОПКИ БЫСТРОГО ДОСТУПА ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionButton(Icons.Default.BugReport, "DTC", onNavigateToDtc)
                QuickActionButton(Icons.Default.PlayArrow, "ВИДЕО", onNavigateToPlayer)
                QuickActionButton(Icons.Default.Settings, "НАСТРОЙКИ", onNavigateToSettings)
            }
        }
    }
}

@Composable
fun MainSpeedometer(speed: Int, priority: String, modifier: Modifier) {
    Box(
        modifier = modifier.background(DarkSurface, androidx.compose.foundation.shape.RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = speed.toString(),
                color = AccentCyan,
                fontSize = 80.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "км/ч",
                color = Color.Gray,
                fontSize = 18.sp
            )
            Text(
                text = "Источник: $priority",
                color = Color.DarkGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun TelemetryGrid(obdData: OBDData) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("RPM", obdData.rpm?.toString() ?: "--", Modifier.weight(1f))
            MetricCard("Темп ОЖ", "${obdData.coolantTempC ?: "--"}°C", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Нагрузка", "${obdData.mapKpa ?: "--"} кПа", Modifier.weight(1f))
            MetricCard("Топливо", "${obdData.fuelLevelPercent?.let { "%.0f".format(it) } ?: "--"}%", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("УОЗ", "${obdData.timingAdvanceDeg ?: "--"}°", Modifier.weight(1f))
            MetricCard("Воздух", "${obdData.intakeAirTempC ?: "--"}°C", Modifier.weight(1f))
        }
    }
}

@Composable
fun TripComputerSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("ТРИП-КОМПЬЮТЕР", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TripItem("Дистанция", "0.0 км")
                TripItem("Время", "00:00:00")
                TripItem("Расход", "0.0 л/100км")
            }
        }
    }
}

@Composable
fun TripItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.Gray, fontSize = 10.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, color = Color.Gray, fontSize = 10.sp)
            Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun SegmentedButton(selectedItem: String, options: List<String>, onItemSelected: (String) -> Unit) {
    Row {
        options.forEach { option ->
            Button(
                onClick = { onItemSelected(option) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedItem == option) AccentCyan else Color.DarkGray
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Text(option, color = Color.White, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun QuickActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 12.sp)
        }
    }
}
