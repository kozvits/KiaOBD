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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── 1. Камера ────────────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { pv ->
                    viewModel.bindCamera(lifecycleOwner, pv)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── 2. ADAS — полупрозрачные линии полосы (заглушка-визуализация) ───
        AdasLaneOverlay(
            modifier = Modifier.fillMaxSize()
        )

        // ── 3. ADAS алерт внизу по центру ────────────────────────────────────
        lastAlert?.let { alert ->
            AdasAlertBanner(
                alert = alert,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }

        // ── 4. REC индикатор — верхний левый ─────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (isRecording) AlertRed else Color.Gray, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isRecording) "REC" else "STANDBY",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ── 5. HUD панель справа: радар + скорость + ОЖ + напряжение ─────────
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Радар
            HudCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("РАДАР", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("АКТИВЕН", color = GreenOk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Скорость
            HudCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("СКОРОСТЬ", color = Color.Gray, fontSize = 9.sp)
                    Text(
                        text = "${obdData.speedKmh ?: 0}",
                        color = AccentCyan,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("км/ч", color = Color.Gray, fontSize = 9.sp)
                }
            }

            // Температура ОЖ
            HudCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ОЖ", color = Color.Gray, fontSize = 9.sp)
                    val temp = obdData.coolantTempC
                    val tempColor = when {
                        temp == null -> Color.Gray
                        temp >= 100  -> AlertRed
                        temp >= 90   -> AlertYellow
                        else         -> GreenOk
                    }
                    Text(
                        text = if (temp != null) "${temp}°C" else "--°C",
                        color = tempColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Напряжение бортовой сети
            HudCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("БОРТ. СЕТЬ", color = Color.Gray, fontSize = 9.sp)
                    val v = obdData.voltageV
                    val vColor = when {
                        v == null    -> Color.Gray
                        v < 11.5f    -> AlertRed
                        v < 12.5f    -> AlertYellow
                        else         -> GreenOk
                    }
                    Text(
                        text = if (v != null) "${"%.1f".format(v)}В" else "--В",
                        color = vColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HudCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            content()
        }
    }
}

/** Рисует полупрозрачные линии полос — ADAS визуализация */
@Composable
private fun AdasLaneOverlay(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val vanishY = h * 0.45f
        val lineColor = Color(0x9900E676)
        // Левая линия полосы
        drawLine(
            color = lineColor,
            start = androidx.compose.ui.geometry.Offset(w * 0.25f, h),
            end = androidx.compose.ui.geometry.Offset(w * 0.42f, vanishY),
            strokeWidth = 4f
        )
        // Правая линия полосы
        drawLine(
            color = lineColor,
            start = androidx.compose.ui.geometry.Offset(w * 0.75f, h),
            end = androidx.compose.ui.geometry.Offset(w * 0.58f, vanishY),
            strokeWidth = 4f
        )
    }
}

@Composable
private fun AdasAlertBanner(alert: AdasAlert, modifier: Modifier) {
    val (text, color) = when (alert) {
        is AdasAlert.LaneDeparture      -> "⚠ ВЫЕЗД ИЗ ПОЛОСЫ: ${alert.direction}" to AlertYellow
        is AdasAlert.ForwardCollision   -> when (alert.level) {
            AlertLevel.DANGER  -> "🔴 ОПАСНОСТЬ СТОЛКНОВЕНИЯ!" to AlertRed
            AlertLevel.WARNING -> "🟠 ВНИМАНИЕ! Авто близко" to AlertOrange
            AlertLevel.CAUTION -> "🟡 Сократи дистанцию" to AlertYellow
        }
        is AdasAlert.SpeedLimitExceeded -> "🚫 Превышение: ${alert.actualKmh}/${alert.limitKmh} км/ч" to AlertRed
        is AdasAlert.DriverFatigue      -> "😴 УСТАЛОСТЬ ВОДИТЕЛЯ!" to AlertRed
        is AdasAlert.DriverDistracted   -> "👁 ОТВЛЕЧЕНИЕ!" to AlertOrange
        is AdasAlert.PedestrianDetected -> "🚶 ПЕШЕХОД!" to AlertYellow
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
        )
    }
}
