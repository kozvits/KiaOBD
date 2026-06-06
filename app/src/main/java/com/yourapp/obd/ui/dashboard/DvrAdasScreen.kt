package com.yourapp.obd.ui.dashboard

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.yourapp.obd.ui.theme.GreenOk

@Composable
fun DvrAdasScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val obdData          by viewModel.obdData.collectAsStateWithLifecycle()
    val lastAlert        by viewModel.lastAlert.collectAsStateWithLifecycle()
    val fcwDistanceM     by viewModel.fcwDistanceM.collectAsStateWithLifecycle()
    val isRecording      by viewModel.isRecording.collectAsStateWithLifecycle()
    val connectionState  by viewModel.connectionState.collectAsStateWithLifecycle()
    val calibration      by viewModel.adasCalibration.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    val laneColor = if (lastAlert is AdasAlert.LaneDeparture) Color(0xCCFF1744) else Color(0xBB00E676)
    val activeFcwLevel = resolveFcwLevel(
        distanceM = fcwDistanceM,
        dangerM = calibration.dangerZoneM,
        warningM = calibration.warningZoneM,
        cautionM = calibration.cautionZoneM,
        alertLevel = (lastAlert as? AdasAlert.ForwardCollision)?.level
    )

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
                    drawAdasGrid(
                        laneColor       = laneColor,
                        activeFcwLevel  = activeFcwLevel,
                        obstacleDistM   = fcwDistanceM,
                        horizonY        = calibration.horizonPosition,
                        laneHalfW       = calibration.laneWidthPercent,
                        vpXRatio        = calibration.vanishingPointX,
                        dangerM         = calibration.dangerZoneM,
                        warningM        = calibration.warningZoneM,
                        cautionM        = calibration.cautionZoneM
                    )
                }
        )

        // ── 3. Алерт внизу ──────────────────────────────────────────────────
        lastAlert?.let { alert ->
            AdasAlertBanner(
                alert    = alert,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }

        // ── 4. REC индикатор (тап — старт/стоп записи) ──────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clickable {
                    if (isRecording) viewModel.stopRecording()
                    else viewModel.startRecording()
                }
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
                    Text("ДИСТ.", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    val dist = fcwDistanceM
                    Text(
                        text = if (dist != null) "${dist.toInt()} м" else "--",
                        color = when (activeFcwLevel) {
                            AlertLevel.DANGER  -> AlertRed
                            AlertLevel.WARNING -> AlertOrange
                            AlertLevel.CAUTION -> AlertYellow
                            null               -> GreenOk
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
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

        // ── 6. OBD статус ─────────────────────────────────────────────────────
        ObdStatusLine(
            connectionState = connectionState,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 4.dp)
        )
    }
}

private fun resolveFcwLevel(
    distanceM: Float?,
    dangerM: Int,
    warningM: Int,
    cautionM: Int,
    alertLevel: AlertLevel?
): AlertLevel? {
    if (distanceM != null) {
        return when {
            distanceM <= dangerM -> AlertLevel.DANGER
            distanceM <= warningM -> AlertLevel.WARNING
            distanceM <= cautionM -> AlertLevel.CAUTION
            else -> null
        }
    }
    return alertLevel
}

private fun alertLevelColor(level: AlertLevel, alpha: Float): Color = when (level) {
    AlertLevel.DANGER  -> Color(0x66FF1744).copy(alpha = alpha)
    AlertLevel.WARNING -> Color(0x66FF6D00).copy(alpha = alpha)
    AlertLevel.CAUTION -> Color(0x66FFEA00).copy(alpha = alpha)
}

/**
 * ADAS-сетка с калибруемыми зонами и динамической подсветкой по дистанции до препятствия.
 */
fun DrawScope.drawAdasGrid(
    laneColor:      Color,
    activeFcwLevel: AlertLevel?,
    obstacleDistM:  Float?,
    horizonY:       Float,
    laneHalfW:      Float,
    vpXRatio:       Float,
    dangerM:        Int,
    warningM:       Int,
    cautionM:       Int
) {
    val w  = size.width
    val h  = size.height
    val vpX = w * vpXRatio
    val vpY = h * horizonY
    val vp  = Offset(vpX, vpY)

    val leftBase  = Offset(vpX - w * laneHalfW, h)
    val rightBase = Offset(vpX + w * laneHalfW, h)

    fun lp(t: Float) = Offset(leftBase.x  + (vpX - leftBase.x)  * t, h + (vpY - h) * t)
    fun rp(t: Float) = Offset(rightBase.x + (vpX - rightBase.x) * t, h + (vpY - h) * t)

    fun metersToT(meters: Float): Float =
        (meters / cautionM.coerceAtLeast(1)).coerceIn(0.02f, 1f)

    val tDanger  = metersToT(dangerM.toFloat())
    val tWarning = metersToT(warningM.toFloat())
    val tCaution = metersToT(cautionM.toFloat())

    data class Zone(
        val tNear: Float,
        val tFar: Float,
        val baseColor: Color,
        val level: AlertLevel?
    )
    val zones = listOf(
        Zone(0.00f,    tDanger,  Color(0x66FF1744), AlertLevel.DANGER),
        Zone(tDanger,  tWarning, Color(0x55FF6D00), AlertLevel.WARNING),
        Zone(tWarning, tCaution, Color(0x44FFEA00), AlertLevel.CAUTION),
        Zone(tCaution, 1.00f,    Color(0x2200E676), null)
    )

    zones.forEach { z ->
        val nL = lp(z.tNear); val nR = rp(z.tNear)
        val fL = lp(z.tFar);  val fR = rp(z.tFar)
        val highlighted = activeFcwLevel != null && z.level == activeFcwLevel
        val fillColor = if (highlighted) {
            alertLevelColor(activeFcwLevel!!, 0.95f)
        } else {
            z.baseColor
        }
        val path = Path().apply {
            moveTo(nL.x, nL.y); lineTo(nR.x, nR.y)
            lineTo(fR.x, fR.y); lineTo(fL.x, fL.y); close()
        }
        drawPath(path, fillColor)
    }

    listOf(
        Triple(tDanger,  Color(0xCCFF1744)),
        Triple(tWarning, Color(0xCCFF6D00)),
        Triple(tCaution, Color(0xCCFFEA00))
    ).forEach { (t, color) ->
        drawLine(color = color, start = lp(t), end = rp(t), strokeWidth = 2.5f, cap = StrokeCap.Round)
    }

    obstacleDistM?.let { dist ->
        val t = metersToT(dist.coerceIn(0f, cautionM.toFloat()))
        val left = lp(t)
        val right = rp(t)
        val markerColor = when (activeFcwLevel) {
            AlertLevel.DANGER  -> Color(0xFFFF1744)
            AlertLevel.WARNING -> Color(0xFFFF6D00)
            AlertLevel.CAUTION -> Color(0xFFFFEA00)
            null               -> Color.White
        }
        drawLine(
            color = markerColor,
            start = left,
            end = right,
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )
        val center = Offset((left.x + right.x) / 2f, (left.y + right.y) / 2f)
        drawCircle(color = markerColor, radius = 8f, center = center)
        drawCircle(color = Color.White, radius = 4f, center = center)
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
