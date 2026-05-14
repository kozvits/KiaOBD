package com.yourapp.obd.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.hiltViewModel
import com.yourapp.obd.ui.theme.*
import com.yourapp.obd.domain.model.AdasAlert
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun DvrAdasScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val obdData by viewModel.obdData.collectAsStateWithLifecycle()
    val lastAlert by viewModel.lastAlert.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Видеопоток камеры
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Оверлей ADAS (Алерты)
        lastAlert?.let { alert ->
            AdasAlertOverlay(
                alert = alert,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp)
            )
        }

        // 3. HUD - Индикаторы состояния
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Индикатор записи
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(if (isRecording) AlertRed else Color.Gray, androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRecording) "REC" else "STANDBY",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Текущая скорость (HUD)
            Text(
                text = "${obdData.speedKmh ?: 0} км/ч",
                color = AccentCyan,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp)
            )
        }
        
        // 4. Виджет анти-радара (Заглушка/UI часть)
        RadarWidget(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
    }
}

@Composable
fun RadarWidget(modifier: Modifier) {
    Card(
        modifier = modifier.size(width = 120.dp, height = 60.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.7f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("РАДАР", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("АКТИВЕН", color = GreenOk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdasAlertOverlay(alert: AdasAlert, modifier: Modifier) {
    val (text, color) = when (alert) {
        is AdasAlert.LaneDeparture -> "⚠ ВЫЕЗД ИЗ ПОЛОСЫ ${alert.direction}" to AlertYellow
        is AdasAlert.ForwardCollision -> when (alert.level) {
            AlertLevel.DANGER -> "🔴 ОПАСНОСТЬ СТОЛКНОВЕНИЯ!" to AlertRed
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
        )
    }
}
