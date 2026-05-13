package com.yourapp.obd.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DashboardScreen(
    onNavigateToDtc: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val obdData by viewModel.obdData.collectAsStateWithLifecycle()
    val lastAlert by viewModel.lastAlert.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Hidden camera preview for CameraX initialization
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier
                .size(1.dp)
                .align(Alignment.TopStart)
        ).also { previewViewCompose ->
            DisposableEffect(Unit) {
                val previewView = previewViewCompose.findViewById<PreviewView>(androidx.camera.view.R.id.preview_view)
                    ?: return@DisposableEffect onDispose {}
                
                val lifecycleOwner = previewView.findViewTreeLifecycleOwner() 
                    ?: return@DisposableEffect onDispose {}
                
                viewModel.bindCamera(lifecycleOwner, previewView)
                
                // Check camera permission and start recording if granted
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    viewModel.startRecording()
                }
                
                onDispose {
                    viewModel.stopRecording()
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            StatusBar(
                connectionState = connectionState,
                isRecording = isRecording,
                onNavigateToDtc = onNavigateToDtc,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToPlayer = onNavigateToPlayer
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                GaugesSection(
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
        }

        lastAlert?.let { alert ->
            AdasAlertOverlay(
                alert = alert,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun StatusBar(
    connectionState: ConnectionState,
    isRecording: Boolean,
    onNavigateToDtc: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPlayer: () -> Unit
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
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onNavigateToDtc) {
            Text("DTC", color = AccentCyan, fontSize = 12.sp)
        }
        TextButton(onClick = onNavigateToPlayer) {
            Text("ВИДЕО", color = AccentCyan, fontSize = 12.sp)
        }
        TextButton(onClick = onNavigateToSettings) {
            Text("НАСТРОЙКИ", color = AccentCyan, fontSize = 12.sp)
        }
    }
}

@Composable
private fun GaugesSection(obdData: OBDData, modifier: Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnalogGauge(
            value = obdData.speedKmh?.toFloat() ?: 0f,
            maxValue = 180f,
            label = "км/ч",
            unit = "",
            color = AccentCyan,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(0.9f)
        )
        AnalogGauge(
            value = obdData.rpm?.toFloat() ?: 0f,
            maxValue = 8000f,
            label = "RPM",
            unit = "×1000",
            color = AlertOrange,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(0.9f)
        )
    }
}

@Composable
private fun AnalogGauge(
    value: Float,
    maxValue: Float,
    label: String,
    unit: String,
    color: Color,
    modifier: Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize(0.9f)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = minOf(size.width, size.height) / 2f - 16.dp.toPx()
            val startAngle = 150f
            val sweepRange = 240f

            drawArc(
                color = Color.DarkGray,
                startAngle = startAngle,
                sweepAngle = sweepRange,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )

            val fraction = (value / maxValue).coerceIn(0f, 1f)
            val valueSweep = sweepRange * fraction
            if (fraction > 0f) {
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = valueSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            drawTickMarks(center, radius, startAngle, sweepRange, color)
            drawNeedle(center, radius * 0.75f, startAngle + valueSweep, color)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (label == "км/ч") "${value.toInt()}" else "${(value / 1000f).let { "%.1f".format(it) }}",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = label, color = color, fontSize = 12.sp)
        }
    }
}

private fun DrawScope.drawTickMarks(
    center: Offset,
    radius: Float,
    startAngle: Float,
    sweepRange: Float,
    color: Color
) {
    val tickCount = 8
    for (i in 0..tickCount) {
        val angle = Math.toRadians((startAngle + sweepRange * i / tickCount).toDouble())
        val outerX = center.x + radius * cos(angle).toFloat()
        val outerY = center.y + radius * sin(angle).toFloat()
        val innerX = center.x + (radius - 16.dp.toPx()) * cos(angle).toFloat()
        val innerY = center.y + (radius - 16.dp.toPx()) * sin(angle).toFloat()
        drawLine(
            color = color.copy(alpha = 0.6f),
            start = Offset(innerX, innerY),
            end = Offset(outerX, outerY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

private fun DrawScope.drawNeedle(center: Offset, length: Float, angleDeg: Float, color: Color) {
    val angle = Math.toRadians(angleDeg.toDouble())
    val tipX = center.x + length * cos(angle).toFloat()
    val tipY = center.y + length * sin(angle).toFloat()
    drawLine(
        color = color,
        start = center,
        end = Offset(tipX, tipY),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawCircle(color = Color.White, radius = 6.dp.toPx(), center = center)
}

@Composable
private fun DigitalMetricsSection(obdData: OBDData, modifier: Modifier) {
    Column(
        modifier = modifier.padding(4.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MetricCard("ОЖ", obdData.coolantTempC?.let { "$it°C" } ?: "--", AccentCyan)
            MetricCard("MAP", obdData.mapKpa?.let { "$it кПа" } ?: "--", AccentCyan)
            MetricCard("Дрос.", obdData.throttlePercent?.let { "${"%.0f".format(it)}%" } ?: "--", AccentCyan)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MetricCard("Топл.", obdData.fuelLevelPercent?.let { "${"%.0f".format(it)}%" } ?: "--", AccentCyan)
            MetricCard("УОЗ", obdData.timingAdvanceDeg?.let { "${"%.1f".format(it)}°" } ?: "--", AccentCyan)
            MetricCard("ТВЗ", obdData.intakeAirTempC?.let { "$it°C" } ?: "--", AccentCyan)
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, color: Color) {
    Card(
        modifier = Modifier
            .width(100.dp)
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
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
        )
    }
}
