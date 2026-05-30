package com.yourapp.obd

import android.app.Application
import com.yourapp.obd.data.speedcam.SpeedCamNotificationHelper
import com.yourapp.obd.data.speedcam.SpeedCamUpdateWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KiaOBDApp : Application() {

    override fun onCreate() {
        super.onCreate()
        SpeedCamNotificationHelper.createChannel(this)
        SpeedCamUpdateWorker.schedule(this)
    }
}
