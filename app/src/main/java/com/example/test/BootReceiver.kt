package com.example.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed, starting services...")

            // Start SMS Service
            val smsIntent = Intent(context, SmsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(smsIntent)
            } else {
                context.startService(smsIntent)
            }

            // Start Heartbeat Service
            val heartbeatIntent = Intent(context, HeartbeatService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(heartbeatIntent)
            } else {
                context.startService(heartbeatIntent)
            }
        }
    }
}