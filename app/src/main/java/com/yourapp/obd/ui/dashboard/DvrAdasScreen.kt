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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourapp.obd.data.bluetooth.ConnectionState
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
    val obdData           by viewModel.obdData.collectAsStateWithLifecycle()
    val lastAlert         by viewModel.lastAlert.collectAsStateWithLifecycle()
    val isRecording       by viewModel.isRecording.collectAsStateWithLifecycle()
    val calibration       by viewModel.adasCalibration.collectAsStateWithLifecycle()
    val connectionState   by viewModel.connectionState.collectAsStateWithLifecycle()
    val fcwDistanceM      by viewModel.fcwDistanceM.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    val laneColor = if (lastAlert is AdasAlert.LaneDeparture) Color(0xCCFF1744) else Color(0xBB00E676)
    val fcwAlert  = lastAlert as? AdasAlert.ForwardCollision
    val fcwColor  = when (fcwAlert?.level) {
        AlertLevel.DANGER  -> Color(0x66FF1744)
        AlertLevel.WARNING -> Color(0x66FF6D00)
        AlertLevel.CAUTION -> Color(0x44FFEA00)
        null               -> null
    }
    val maxDist = (calibration.dangerZoneM + calibration.warningZoneM + calibration.cautionZoneM).coerceAtLeast(1)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── 1. Камера ───────────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { pv -> viewModel.bindCamera(lifecycleOwner, pv) }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── 2. ADAS сетка ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val dangerT  = calibration.dangerZoneM.toFloat() / maxDist
                    val warningT = (calibration.dangerZoneM + calibration.warningZoneM).toFloat() / maxDist
                    val cautionT = (calibration.dangerZoneM + calibration.warningZoneM + calibration.cautionZoneM).toFloat() / maxDist
                    drawAdasGrid(
                        laneColor    = laneColor,
                        fcwColor     = fcwColor,
                        horizonY     = calibration.horizonPosition,
                        laneHalfW    = calibration.laneWidthPercent,
                        vpXRatio     = calibration.vanishingPointX,
                        dangerM      = calibration.dangerZoneM,
                        warningM     = calibration.warningZoneM,
                        cautionM     = calibration.cautionZoneM,
                        dangerT      = dangerT,
                        warningT     = warningT,
                        cautionT     = cautionT
                    )
                }
        )

        // ── 3. Алерт ─────────────────────────────────────────────────────────
        lastAlert?.let { alert ->
            AdasAlertBanner(
                alert    = alert,
                fcwDistanceM = if (alert is AdasAlert.ForwardCollision) fcwDistanceM else null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            )
        }

        // ── 4. Строка состояния OBD ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(DarkSurface.copy(alpha = 0.85f))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val btLabel = when (connectionState) {
                ConnectionState.CONNECTED -> "OBD: Подключено"
                ConnectionState.CONNECTING -> "OBD: Подключение..."
                ConnectionState.ERROR -> "OBD: Ошибка"
                ConnectionState.DISCONNECTED -> "OBD: Не подключено"
            }
            val btColor = when (connectionState) {
                ConnectionState.CONNECTED -> GreenOk
                ConnectionState.CONNECTING -> AlertYellow
                ConnectionState.ERROR -> AlertRed
                ConnectionState.DISCONNECTED -> Color.Gray
            }
            Text(btLabel, color = btColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text(if (isRecording) "● REC" else "○ STANDBY",
                color = if (isRecording) AlertRed else Color.Gray,
                fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        // ── 5. REC индикатор ─────────────────────────────────────────────────
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
                text       = if (isRecording) "REC" else "STANDBY",
                color      = Color.White,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ── 5. HUD справа ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HudCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ДИСТАНЦИЯ", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    val dist = fcwDistanceM
                    Text(
                        text       = if (dist != null) "${dist.toInt()} м" else "--",
                        color      = when {
                            dist == null -> Color.Gray
                            dist <= 5    -> AlertRed
                            dist <= 15   -> AlertOrange
                            else         -> GreenOk
                        },
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            HudCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("СКОРОСТЬ", color = Color.Gray, fontSize = 9.sp)
                    Text(
                        text       = "${obdData.speedKmh ?: 0}",
                        color      = AccentCyan,
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("км/ч", color = Color.Gray, fontSize = 9.sp)
                }
            }
            HudCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ОЖ", color = Color.Gray, fontSize = 9.sp)
                    val t = obdData.coolantTempC
                    Text(
                        text       = if (t != null) "${t}°C" else "--",
                        color      = when { t == null -> Color.Gray; t >= 100 -> AlertRed; t >= 90 -> AlertYellow; else -> GreenOk },
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            HudCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("СЕТЬ", color = Color.Gray, fontSize = 9.sp)
                    val v = obdData.voltageV
                    Text(
                        text       = if (v != null) "${"%.1f".format(v)}В" else "--",
                        color      = when { v == null -> Color.Gray; v < 11.5f -> AlertRed; v < 12.5f -> AlertYellow; else -> GreenOk },
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Полная ADAS сетка с калибруемыми параметрами:
 * - Точка схода (horizonY, vpXRatio)
 * - Ширина полосы (laneHalfW)
 * - Зоны: DANGER / WARNING / CAUTION / SAFE с метками дистанции
 * - Подсветка зоны при FCW алерте
 */
fun DrawScope.drawAdasGrid(
    laneColor:  Color,
    fcwColor:   Color?,
    horizonY:   Float,
    laneHalfW:  Float,
    vpXRatio:   Float,
    dangerM:    Int,
    warningM:   Int,
    cautionM:   Int,
    dangerT:    Float = 0.20f,
    warningT:   Float = 0.42f,
    cautionT:   Float = 0.68f
) {
    val w  = size.width
    val h  = size.height
    val vpX = w * vpXRatio
    val vpY = h * horizonY
    val vp  = Offset(vpX, vpY)

    val leftBase  = Offset(vpX - w * laneHalfW, h)
    val rightBase = Offset(vpX + w * laneHalfW, h)

    // t=0 => нижний край (близко), t=1 => точка схода (далеко)
    fun lp(t: Float) = Offset(leftBase.x  + (vpX - leftBase.x)  * t, h + (vpY - h) * t)
    fun rp(t: Float) = Offset(rightBase.x + (vpX - rightBase.x) * t, h + (vpY - h) * t)

    // Зоны (t границы) вычисляются из настроек дистанции
    data class Zone(val tNear: Float, val tFar: Float, val baseColor: Color, val label: String, val meters: Int)
    val zones = listOf(
        Zone(0.00f, dangerT, Color(0x66FF1744), "●  ${dangerM} м",  dangerM),
        Zone(dangerT, warningT, Color(0x55FF6D00), "●  ${warningM} м", warningM),
        Zone(warningT, cautionT, Color(0x44FFEA00), "●  ${cautionM} м", cautionM),
        Zone(cautionT, 1.00f, Color(0x2200E676), "БЕЗОПАСНО",         0)
    )

    // Заливка зон
    zones.forEach { z ->
        val nL = lp(z.tNear); val nR = rp(z.tNear)
        val fL = lp(z.tFar);  val fR = rp(z.tFar)
        val fillColor = if (fcwColor != null && z.tNear == 0.00f) fcwColor else z.baseColor
        val path = Path().apply {
            moveTo(nL.x, nL.y); lineTo(nR.x, nR.y)
            lineTo(fR.x, fR.y); lineTo(fL.x, fL.y); close()
        }
        drawPath(path, fillColor)
    }

    // Горизонтальные линии расстояний с подписями
    val distLines = listOf(
        Triple(dangerT, "${dangerM} м",  Color(0xCCFF1744)),
        Triple(warningT, "${warningM} м", Color(0xCCFF6D00)),
        Triple(cautionT, "${cautionM} м", Color(0xCCFFEA00))
    )
    distLines.forEach { (t, _, color) ->
        drawLine(color = color, start = lp(t), end = rp(t), strokeWidth = 2.5f, cap = StrokeCap.Round)
    }

    // Пунктирная центральная ось
    val dashCount = 12
    (0 until dashCount).forEach { i ->
        val t0 = i.toFloat() / dashCount
        val t1 = (i + 0.5f) / dashCount
        drawLine(
            color = Color(0x5500E676),
            start = Offset(vpX, h + (vpY - h) * t0),
            end   = Offset(vpX, h + (vpY - h) * t1),
            strokeWidth = 2f
        )
    }

    // Линии полосы (основные)
    drawLine(color = laneColor, start = leftBase,  end = vp, strokeWidth = 4f, cap = StrokeCap.Round)
    drawLine(color = laneColor, start = rightBase, end = vp, strokeWidth = 4f, cap = StrokeCap.Round)

    // Линия горизонта
    drawLine(color = Color(0x33FFFFFF), start = Offset(0f, vpY), end = Offset(w, vpY), strokeWidth = 1f)

    // Точка схода
    drawCircle(color = laneColor.copy(alpha = 0.8f), radius = 7f, center = vp)
    drawCircle(color = Color.White.copy(alpha = 0.6f), radius = 3f, center = vp)
}

@Composable
private fun HudCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.65f)),
        shape  = RoundedCornerShape(10.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) { content() }
    }
}

@Composable
private fun AdasAlertBanner(alert: AdasAlert, fcwDistanceM: Float?, modifier: Modifier) {
    val (text, color) = when (alert) {
        is AdasAlert.LaneDeparture      -> "ВЫЕЗД ИЗ ПОЛОСЫ: ${alert.direction}" to AlertYellow
        is AdasAlert.ForwardCollision   -> {
            val distText = if (fcwDistanceM != null) "  •  ${fcwDistanceM.toInt()} м" else ""
            when (alert.level) {
                AlertLevel.DANGER  -> "ОПАСНОСТЬ СТОЛКНОВЕНИЯ!$distText" to AlertRed
                AlertLevel.WARNING -> "ВНИМАНИЕ!$distText"              to AlertOrange
                AlertLevel.CAUTION -> "$distText"                        to AlertYellow
            }
        }
        is AdasAlert.SpeedLimitExceeded -> "Превышение: ${alert.actualKmh}/${alert.limitKmh} км/ч" to AlertRed
        is AdasAlert.DriverFatigue      -> "УСТАЛОСТЬ ВОДИТЕЛЯ!"  to AlertRed
        is AdasAlert.DriverDistracted   -> "ОТВЛЕЧЕНИЕ!"          to AlertOrange
        is AdasAlert.PedestrianDetected -> "ПЕШЕХОД!"             to AlertYellow
    }
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.92f)),
        shape    = RoundedCornerShape(14.dp)
    ) {
        Text(
            text       = text,
            color      = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize   = 17.sp,
            modifier   = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
        )
    }
}
