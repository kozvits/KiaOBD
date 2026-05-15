package com.yourapp.obd.ui.dashboard

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourapp.obd.domain.model.AdasAlert
import com.yourapp.obd.domain.model.AlertLevel
import com.yourapp.obd.ui.theme.AccentCyan
import com.yourapp.obd.ui.theme.AlertOrange
import com.yourapp.obd.ui.theme.AlertRed
import com.yourapp.obd.ui.theme.AlertYellow
import com.yourapp.obd.ui.theme.DarkSurface
import com.yourapp.obd.ui.theme.GreenOk

@Composable
fun DvrAdasScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val obdData by viewModel.obdData.collectAsStateWithLifecycle()
    val lastAlert by viewModel.lastAlert.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Видеопоток камеры — bindCamera вызывается при создании PreviewView
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    // Привязываем камеру сразу при создании View
                    viewModel.bindCamera(lifecycleOwner, previewView)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Оверлей ADAS
        lastAlert?.let { alert ->
            AdasAlertOverlay(
                alert = alert,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp)
            )
        }

        // HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(if (isRecording) AlertRed else Color.Gray, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRecording) "REC" else "STANDBY",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Text(
                text = "${obdData.speedKmh ?: 0} км/ч",
                color = AccentCyan,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp)
            )
        }

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
        shape = RoundedCornerShape(12.dp)
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
        is AdasAlert.LaneDeparture      -> "ВЫЕЗД ИЗ ПОЛОСЫ ${alert.direction}" to AlertYellow
        is AdasAlert.ForwardCollision   -> when (alert.level) {
            AlertLevel.DANGER  -> "ОПАСНОСТЬ СТОЛКНОВЕНИЯ!" to AlertRed
            AlertLevel.WARNING -> "ВНИМАНИЕ! Авто близко" to AlertOrange
            AlertLevel.CAUTION -> "Сократи дистанцию" to AlertYellow
        }
        is AdasAlert.SpeedLimitExceeded -> "Превышение: ${alert.actualKmh}/${alert.limitKmh} км/ч" to AlertRed
        is AdasAlert.DriverFatigue      -> "УСТАЛОСТЬ ВОДИТЕЛЯ!" to AlertRed
        is AdasAlert.DriverDistracted   -> "ОТВЛЕЧЕНИЕ ВОДИТЕЛЯ!" to AlertOrange
        is AdasAlert.PedestrianDetected -> "ПЕШЕХОД НА ДОРОГЕ!" to AlertYellow
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
