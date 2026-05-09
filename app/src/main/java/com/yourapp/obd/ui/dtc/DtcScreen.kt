package com.yourapp.obd.ui.dtc

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourapp.obd.domain.model.DTCCode
import com.yourapp.obd.domain.model.DtcSeverity
import com.yourapp.obd.ui.theme.AlertOrange
import com.yourapp.obd.ui.theme.AlertRed
import com.yourapp.obd.ui.theme.DarkBackground
import com.yourapp.obd.ui.theme.DarkSurface
import com.yourapp.obd.ui.theme.GreenOk
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DtcScreen(
    onBack: () -> Unit,
    viewModel: DtcViewModel = hiltViewModel()
) {
    val codes by viewModel.dtcCodes.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val showDialog by viewModel.showClearDialog.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.readDtcCodes() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Коды ошибок DTC", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.readDtcCodes() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.requestClearDtc() },
                containerColor = AlertRed
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Сбросить ошибки", tint = Color.White)
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (codes.isEmpty()) {
                Text(
                    text = "Ошибок не обнаружено ✓",
                    color = GreenOk,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(codes, key = { it.code }) { code ->
                        DtcCodeCard(code)
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissClearDialog() },
                title = { Text("Сбросить ошибки?", color = Color.White) },
                text = { Text("Все коды ошибок будут удалены из ЭБУ и базы данных.", color = Color.Gray) },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmClearDtc() }) {
                        Text("Сбросить", color = AlertRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissClearDialog() }) {
                        Text("Отмена", color = Color.Gray)
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}

@Composable
private fun DtcCodeCard(code: DTCCode) {
    val severityColor = when (code.severity) {
        DtcSeverity.LOW -> GreenOk
        DtcSeverity.MEDIUM -> AlertOrange
        DtcSeverity.HIGH -> AlertRed
    }
    val severityLabel = when (code.severity) {
        DtcSeverity.LOW -> "P0xxx"
        DtcSeverity.MEDIUM -> "P1xxx"
        DtcSeverity.HIGH -> "B/C/U"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(severityColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = code.code,
                        color = severityColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = severityLabel, color = Color.Gray, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = code.description, color = Color.White, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        .format(Date(code.timestamp)),
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}
