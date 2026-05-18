package com.yourapp.obd.ui.dashboard

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import com.yourapp.obd.ui.theme.GreenOk

@Composable
fun DvrAdasScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val obdData       by viewModel.obdData.collectAsStateWithLifecycle()
    val lastAlert     by viewModel.lastAlert.collectAsStateWithLifecycle()
    val isRecording   by viewModel.isRecording.collectAsStateWithLifecycle()
    val calibration   by viewModel.adasCalibration.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    val laneColor = if (lastAlert is AdasAlert.LaneDeparture) Color(0xCCFF1744) else Color(0xBB00E676)
    val fcwLevel  = (lastAlert as? AdasAlert.ForwardCollision)?.level

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── 1. Камера ──────────────────────────────────────────────────────
        AndroidView(
            factory = { ctx -> PreviewView(ctx).also { viewModel.bindCamera(lifecycleOwner, it) } },
            modifier = Modifier.fillMaxSize()
        )

        // ── 2. ADAS сетка с калибровкой ────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxSize().drawBehind {
                drawAdasGrid(
                    laneColor    = laneColor,
                    fcwLevel     = fcwLevel,
                    calibration  = calibration
                )
            }
        )

        // ── 3. Алерт ────────────────────────────────────────────────────────
        lastAlert?.let {
            AdasAlertBanner(it, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
        }

        // ── 4. REC ──────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopStart).padding(12.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Box(Modifier.size(10.dp).background(if (isRecording) AlertRed else Color.Gray, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(if (isRecording) "REC" else "STANDBY", color = Color.White,
                fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // ── 5. HUD справа ───────────────────────────────────────────────────
        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HudCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("РАДАР", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("АКТИВЕН", color = GreenOk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            HudCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("СКОРОСТЬ", color = Color.Gray, fontSize = 9.sp)
                    Text("${obdData.speedKmh ?: 0}", color = AccentCyan,
                        fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text("км/ч", color = Color.Gray, fontSize = 9.sp)
                }
            }
            HudCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ОЖ", color = Color.Gray, fontSize = 9.sp)
                    val t = obdData.coolantTempC
                    Text(if (t != null) "${t}°C" else "--",
                        color = when { t == null -> Color.Gray; t >= 100 -> AlertRed; t >= 90 -> AlertYellow; else -> GreenOk },
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            HudCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("СЕТЬ", color = Color.Gray, fontSize = 9.sp)
                    val v = obdData.voltageV
                    Text(if (v != null) "${"%.1f".format(v)}В" else "--",
                        color = when { v == null -> Color.Gray; v < 11.5f -> AlertRed; v < 12.5f -> AlertYellow; else -> GreenOk },
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun DrawScope.drawAdasGrid(
    laneColor: Color,
    fcwLevel: AlertLevel?,
    calibration: AdasCalibration
) {
    val w     = size.width
    val h     = size.height
    val vpX   = w * 0.5f
    val vpY   = h * (calibration.horizonPct / 100f)
    val halfB = w * (calibration.laneWidthPct / 100f)

    val leftBase  = Offset(vpX - halfB, h)
    val rightBase = Offset(vpX + halfB, h)
    val vp        = Offset(vpX, vpY)

    fun lp(t: Float) = Offset(leftBase.x  + (vp.x - leftBase.x) * t, leftBase.y + (vp.y - leftBase.y) * t)
    fun rp(t: Float) = Offset(rightBase.x + (vp.x - rightBase.x) * t, rightBase.y + (vp.y - rightBase.y) * t)

    // Вычисляем t-позиции зон из метров (линейная интерполяция 0м..safeDistM)
    val maxDist = calibration.safeDistM.toFloat()
    fun tFrom(m: Int) = 1f - (m.toFloat() / maxDist).coerceIn(0f, 1f)

    val tDanger  = tFrom(calibration.dangerDistM)
    val tWarning = tFrom(calibration.warningDistM)
    val tCaution = tFrom(calibration.cautionDistM)

    // Цвет зоны FCW
    val dangerFill  = if (fcwLevel == AlertLevel.DANGER)  Color(0xAAFF1744) else Color(0x44FF1744)
    val warningFill = if (fcwLevel == AlertLevel.WARNING) Color(0xAAFF6D00) else Color(0x33FF6D00)
    val cautionFill = if (fcwLevel == AlertLevel.CAUTION) Color(0xAAFFEA00) else Color(0x22FFEA00)
    val safeFill    = Color(0x1500E676)

    // Заливка зон
    data class Z(val tFar: Float, val tNear: Float, val color: Color)
    listOf(
        Z(0f,       tDanger,  dangerFill),
        Z(tDanger,  tWarning, warningFill),
        Z(tWarning, tCaution, cautionFill),
        Z(tCaution, 1f,       safeFill)
    ).forEach { z ->
        val path = Path().apply {
            moveTo(lp(z.tNear).x, lp(z.tNear).y)
            lineTo(rp(z.tNear).x, rp(z.tNear).y)
            lineTo(rp(z.tFar).x,  rp(z.tFar).y)
            lineTo(lp(z.tFar).x,  lp(z.tFar).y)
            close()
        }
        drawPath(path, z.color)
    }

    // Дистанционные линии с метками
    listOf(
        Triple(tDanger,  "${calibration.dangerDistM} м",  Color(0xBBFF1744)),
        Triple(tWarning, "${calibration.warningDistM} м", Color(0xBBFF6D00)),
        Triple(tCaution, "${calibration.cautionDistM} м", Color(0xBBFFEA00)),
        Triple(1f,       "${calibration.safeDistM} м",    Color(0xBB00E676))
    ).forEach { (t, _, color) ->
        val l = lp(t); val r = rp(t)
        drawLine(color, l, r, strokeWidth = 2f, cap = StrokeCap.Round)
        // Тики на линиях полос
        drawLine(color, Offset(l.x - 12f, l.y), Offset(l.x + 12f, l.y), strokeWidth = 3f)
        drawLine(color, Offset(r.x - 12f, r.y), Offset(r.x + 12f, r.y), strokeWidth = 3f)
    }

    // Линии полосы
    drawLine(laneColor, leftBase,  vp, strokeWidth = 5f, cap = StrokeCap.Round)
    drawLine(laneColor, rightBase, vp, strokeWidth = 5f, cap = StrokeCap.Round)

    // Горизонт
    drawLine(Color(0x44FFFFFF), Offset(0f, vpY), Offset(w, vpY), strokeWidth = 1f)

    // Маркер точки схода
    drawCircle(laneColor,        radius = 8f, center = vp)
    drawCircle(Color(0xAAFFFFFF), radius = 3f, center = vp)
}

@Composable
private fun HudCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.65f)),
        shape  = RoundedCornerShape(10.dp)
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) { content() }
    }
}

@Composable
private fun AdasAlertBanner(alert: AdasAlert, modifier: Modifier) {
    val (text, color) = when (alert) {
        is AdasAlert.LaneDeparture      -> "ВЫЕЗД ИЗ ПОЛОСЫ: ${alert.direction}" to AlertYellow
        is AdasAlert.ForwardCollision   -> when (alert.level) {
            AlertLevel.DANGER  -> "ОПАСНОСТЬ СТОЛКНОВЕНИЯ!" to AlertRed
            AlertLevel.WARNING -> "ВНИМАНИЕ! Авто близко"   to AlertOrange
            AlertLevel.CAUTION -> "Сократи дистанцию"       to AlertYellow
        }
        is AdasAlert.SpeedLimitExceeded -> "Превышение: ${alert.actualKmh}/${alert.limitKmh} км/ч" to AlertRed
        is AdasAlert.DriverFatigue      -> "УСТАЛОСТЬ ВОДИТЕЛЯ!"  to AlertRed
        is AdasAlert.DriverDistracted   -> "ОТВЛЕЧЕНИЕ!"          to AlertOrange
        is AdasAlert.PedestrianDetected -> "ПЕШЕХОД!"             to AlertYellow
    }
    Card(modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.92f)),
        shape  = RoundedCornerShape(14.dp)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp))
    }
}
