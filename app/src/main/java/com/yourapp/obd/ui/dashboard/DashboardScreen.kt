package com.yourapp.obd.ui.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourapp.obd.data.bluetooth.ConnectionState
import com.yourapp.obd.domain.model.AdasAlert
import com.yourapp.obd.domain.model.AlertLevel
import com.yourapp.obd.domain.model.OBDData
import com.yourapp.obd.ui.theme.AccentCyan
import com.yourapp.obd.ui.theme.AlertOrange
import com.yourapp.obd.ui.theme.AlertRed
import com.yourapp.obd.ui.theme.AlertYellow
import com.yourapp.obd.ui.theme.DarkBackground
import com.yourapp.obd.ui.theme.DarkSurface
import com.yourapp.obd.ui.theme.GreenOk

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val obdData by viewModel.obdData.collectAsStateWithLifecycle()
    val lastAlert by viewModel.lastAlert.collectAsStateWithLifecycle()
    val currentTrip by viewModel.currentTrip.collectAsStateWithLifecycle()
    val obdAlert by viewModel.obdAlert.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            StatusBar(
                connectionState = connectionState,
                isRecording = isRecording
            )

            // ── OBD Alert ─────────────────────────────────────
            obdAlert?.let { alert ->
                ObdAlertBanner(alert = alert)
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp)
            ) {
                DigitalGaugesSection(
                    obdData = obdData,
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                )
                Spacer(modifier = Modifier.width(8.dp))
                DigitalMetricsSection(
                    obdData = obdData,
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                )
            }

            // ── Текущая поездка ───────────────────────────────
            TripInfoBar(trip = currentTrip)
        }

        lastAlert?.let { alert ->
            AdasAlertOverlay(
                alert = alert,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }

        ObdStatusLine(
            connectionState = connectionState,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 6.dp)
        )
    }
}

@Composable
private fun StatusBar(
    connectionState: ConnectionState,
    isRecording: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recBlink"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val btColor = when (connectionState) {
            ConnectionState.CONNECTED -> GreenOk
            ConnectionState.CONNECTING -> AlertYellow
            ConnectionState.ERROR -> AlertRed
            ConnectionState.DISCONNECTED -> Color.Gray
        }
        Icon(
            imageVector = if (connectionState == ConnectionState.CONNECTED)
                Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
            contentDescription = null,
            tint = btColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = if (isRecording) Icons.Default.Videocam else Icons.Default.VideocamOff,
            contentDescription = null,
            tint = if (isRecording) AlertRed.copy(alpha = alpha) else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ObdAlertBanner(alert: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                alert,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DigitalGaugesSection(obdData: OBDData, modifier: Modifier) {
    Column(
        modifier = modifier.padding(4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DigitalGauge(
            value = obdData.speedKmh ?: 0,
            unit = "км/ч",
            label = "СКОРОСТЬ",
            color = AccentCyan,
            large = true,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        DigitalGauge(
            value = obdData.rpm ?: 0,
            unit = "RPM",
            label = "ОБОРОТЫ",
            color = AlertOrange,
            large = false,
            modifier = Modifier.weight(0.6f).fillMaxWidth()
        )
    }
}

@Composable
private fun DigitalGauge(
    value: Int,
    unit: String,
    label: String,
    color: Color,
    large: Boolean,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = Color.Gray,
                fontSize = if (large) 13.sp else 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(if (large) 8.dp else 4.dp))
            Text(
                text = "$value",
                color = color,
                fontSize = if (large) 64.sp else 36.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = unit,
                color = color.copy(alpha = 0.7f),
                fontSize = if (large) 16.sp else 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DigitalMetricsSection(obdData: OBDData, modifier: Modifier) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(Modifier.weight(1f)) {
                MetricCard("ОЖ", obdData.coolantTempC?.let { "$it°C" } ?: "--", AccentCyan)
            }
            Box(Modifier.weight(1f)) {
                MetricCard("MAP", obdData.mapKpa?.let { "$it кПа" } ?: "--", AccentCyan)
            }
            Box(Modifier.weight(1f)) {
                MetricCard("Дрос.", obdData.throttlePercent?.let { "${"%.0f".format(it)}%" } ?: "--", AccentCyan)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(Modifier.weight(1f)) {
                MetricCard("Топл.", obdData.fuelLevelPercent?.let { "${"%.0f".format(it)}%" } ?: "--", AccentCyan)
            }
            Box(Modifier.weight(1f)) {
                MetricCard("УОЗ", obdData.timingAdvanceDeg?.let { "${"%.1f".format(it)}°" } ?: "--", AccentCyan)
            }
            Box(Modifier.weight(1f)) {
                MetricCard("ТВЗ", obdData.intakeAirTempC?.let { "$it°C" } ?: "--", AccentCyan)
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, color: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = label, color = Color.Gray, fontSize = 10.sp)
            Text(
                text = value,
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TripInfoBar(trip: CurrentTripInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (trip.distanceKm > 0 || trip.durationMinutes > 0) {
                TripStatItem(Icons.Default.Route, formatDist(trip.distanceKm), "км")
                TripStatItem(Icons.Default.Timer, "${trip.durationMinutes}", "мин")
                TripStatItem(Icons.Default.Speed, "${trip.avgSpeedKmh}", "км/ч ср")
            } else {
                Text(
                    "Нет данных поездки",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun TripStatItem(icon: ImageVector, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = AccentCyan, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(2.dp))
        Text(label, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
private fun ObdStatusLine(
    connectionState: ConnectionState,
    modifier: Modifier
) {
    val (text, color) = when (connectionState) {
        ConnectionState.CONNECTED -> "OBD: Подключено" to GreenOk
        ConnectionState.CONNECTING -> "OBD: Подключение..." to AlertYellow
        ConnectionState.ERROR -> "OBD: Ошибка" to AlertRed
        ConnectionState.DISCONNECTED -> "OBD: Отключён" to Color.Gray
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun AdasAlertOverlay(alert: AdasAlert, modifier: Modifier) {
    val (text, color) = when (alert) {
        is AdasAlert.LaneDeparture -> "⚠ ВЫЕЗД ИЗ ПОЛОСЫ ${alert.direction}" to AlertYellow
        is AdasAlert.ForwardCollision -> when (alert.level) {
            AlertLevel.DANGER -> "🔴 ОПАСНОСТЬ СТОЛКНОВЕНИЯ! ${alert.ttcSeconds.toInt()}с" to AlertRed
            AlertLevel.WARNING -> "🟠 ВНИМАНИЕ! Авто близко" to AlertOrange
            AlertLevel.CAUTION -> "🟡 Сократи дистанцию" to AlertYellow
        }
        is AdasAlert.SpeedLimitExceeded -> "🚫 Превышение: ${alert.actualKmh}/${alert.limitKmh} км/ч" to AlertRed
        is AdasAlert.DriverFatigue -> "😴 УСТАЛОСТЬ ВОДИТЕЛЯ!" to AlertRed
        is AdasAlert.DriverDistracted -> "👁 ОТВЛЕЧЕНИЕ ВОДИТЕЛЯ!" to AlertOrange
        is AdasAlert.PedestrianDetected -> "🚶 ПЕШЕХОД НА ДОРОГЕ!" to AlertYellow
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
        )
    }
}

private fun formatDist(km: Float): String {
    return if (km >= 100f) "%.0f".format(km)
    else "%.1f".format(km)
}
