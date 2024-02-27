package br.com.apkdoandroid.agitaluz.broadcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import br.com.apkdoandroid.agitaluz.services.AccelerometerFlashService
import br.com.apkdoandroid.agitaluz.services.LanternaService

class LanternaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("Test", "Receive : ${intent.action}")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val intent = Intent(context, AccelerometerFlashService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }


    }
}