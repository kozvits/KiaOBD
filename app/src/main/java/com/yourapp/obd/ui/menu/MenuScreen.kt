package com.yourapp.obd.ui.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourapp.obd.ui.theme.AccentCyan
import com.yourapp.obd.ui.theme.DarkBackground
import com.yourapp.obd.ui.theme.DarkSurface

@Composable
fun MenuScreen(
    onNavigateToDtc: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToSettings: (section: String?) -> Unit,
    onNavigateToDvr: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Меню",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ExpandableSection(
            icon = Icons.Default.CameraAlt,
            title = "Видеорегистратор",
            accentColor = AccentCyan
        ) {
            MenuItem(
                icon = Icons.Default.Visibility,
                label = "Экран DVR",
                desc = "Камера, ADAS-сетка, HUD",
                onClick = onNavigateToDvr
            )
            MenuItem(
                icon = Icons.Default.Description,
                label = "Архив записей",
                desc = "Просмотр и удаление видео",
                onClick = onNavigateToPlayer
            )
        }

        ExpandableSection(
            icon = Icons.Default.Speed,
            title = "Приборная панель",
            accentColor = Color(0xFF00E676)
        ) {
            MenuItem(
                icon = Icons.Default.Speed,
                label = "Панель приборов",
                desc = "Аналоговые датчики, цифровые метрики",
                onClick = onNavigateToDashboard
            )
            MenuItem(
                icon = Icons.Default.Build,
                label = "Диагностика DTC",
                desc = "Коды ошибок ЭБУ",
                onClick = onNavigateToDtc
            )
        }

        ExpandableSection(
            icon = Icons.Default.Security,
            title = "ADAS",
            accentColor = Color(0xFFFF6D00)
        ) {
            MenuItem(
                icon = Icons.Default.Sensors,
                label = "Калибровка сетки",
                desc = "Горизонт, ширина полосы, зоны",
                onClick = { onNavigateToSettings("adas_calibration") }
            )
            MenuItem(
                icon = Icons.Default.ListAlt,
                label = "Чувствительность",
                desc = "Пороги срабатывания LDW/FCW",
                onClick = { onNavigateToSettings("adas_sensitivity") }
            )
            MenuItem(
                icon = Icons.Default.AirportShuttle,
                label = "Модули",
                desc = "Включение/отключение функций",
                onClick = { onNavigateToSettings("adas_modules") }
            )
        }

        ExpandableSection(
            icon = Icons.Default.Settings,
            title = "Система",
            accentColor = Color(0xFFFFC107)
        ) {
            MenuItem(
                icon = Icons.Default.Settings,
                label = "Все настройки",
                desc = "OBD-II, видео, SpeedCam, ADAS",
                onClick = { onNavigateToSettings(null) }
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ExpandableSection(
    icon: ImageVector,
    title: String,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    desc: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = desc,
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.DarkGray,
            modifier = Modifier.size(18.dp)
        )
    }
}
