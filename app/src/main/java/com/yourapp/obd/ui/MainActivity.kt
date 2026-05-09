package com.yourapp.obd.ui

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.yourapp.obd.service.DrivingForegroundService
import com.yourapp.obd.ui.theme.KiaOBDTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val serviceIntent = Intent(this, DrivingForegroundService::class.java)
        startForegroundService(serviceIntent)

        setContent {
            KiaOBDTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KiaOBDNavHost()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
