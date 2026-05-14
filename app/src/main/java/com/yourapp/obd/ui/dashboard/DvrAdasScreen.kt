package com.yourapp.obd.ui.dashboard

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.hiltViewModel
import com.yourapp.obd.ui.theme.*
import com.yourapp.obd.domain.model.OBDData

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
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Привязка камеры происходит через ViewModel/Repository
                    // Для полной реализации нужно передать LifecycleOwner
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
                    .padding(bottom = 40.dp)
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
    }
}

@Composable
private fun AdasAlertOverlay(alert la: com.yourapp.obd.domain.model.AdasAlert, modifier: Modifier) {
    // Reusing the implementation from DashboardScreen for consistency
    val (text, color) = when (alert) {
        is com.yourapp.obd.domain.model.AdasAlert.LaneDeparture -> "⚠ ВЫЕЗД ИЗ ПОЛОСЫ ${alert.direction}" to AlertYellow
        is com.yourapp.obd.domain.model.AdasAlert.ForwardCollision -> when (alert.level) {
            com.yourapp.obd.domain.model.AlertLevel.DANGER -> "🔴 ОПАСНОСТЬ СТОЛКНОВЕНИЯ!" to AlertRed
            com.yourapp.obd.domain.model.AlertLevel.WARNING -> "🟠 ВНИМАНИЕ! Авто близко" to AlertOrange
            com.yourapp.obd.domain.model.AlertLevel.CAUTION -> "🟡 Сократи дистанцию" to AlertYellow
        }
        is com.yourapp.obd.domain.model.AdasAlert.SpeedLimitExceeded -> "🚫 Превышение: ${alert.actualKmh}/${alert.limitKmh} км/ч" to AlertRed
        is com.yourapp.obd.domain.model.AdasAlert.DriverFatigue -> "😴 УСТАЛОСТЬ ВОДИТЕЛЯ!" to AlertRed
        is com.yourapp.obd.domain.model.AdasAlert.DriverDistracted -> "👁 ОТВЛЕЧЕНИЕ ВОДИТЕЛЯ!" to AlertOrange
        is com.yourapp.obd.domain.model.AdasAlert.PedestrianDetected -> "🚶 ПЕШЕХОД НА ДОРОГЕ!" to AlertYellow
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
