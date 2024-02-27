package br.com.apkdoandroid.agitaluz.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.app.NotificationCompat
import br.com.apkdoandroid.agitaluz.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LanternaService : Service(), SensorEventListener  {
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastAcceleration: Float = 0f
    private val shakeThreshold = 10f  // Ajuste conforme necessário
    private var lanternaAtivada = false
    private val CHANNEL_ID = "ForegroundServiceChannel"
    private var lastShakeTime: Long = 20
    private val debounceInterval: Long = 100 // Intervalo de debounce em milissegundos

    override fun onCreate() {
        createNotificationChannel()
        notificacao_on()
        super.onCreate()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager?

        if (sensorManager != null) {
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            if (accelerometer != null) {
                sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            } else {
                stopSelf()  // Encerra o serviço se o sensor não estiver disponível
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val accelerationX = event.values[0]
            val accelerationY = event.values[1]

            // Ignorar a aceleração no eixo Z (vertical)
            val acceleration = calculateHorizontalAcceleration(accelerationX, accelerationY)


           if(acceleration >= 25F){
               Log.d("ShakeService", "Aceleração: $acceleration")
           }

            if (isShaking(acceleration)) {
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastShakeTime >= debounceInterval) {
                    // Tempo suficiente desde a última agitação, trata a agitação
                    if (acceleration >= 200F) {
                        toggleFlashlight()
                    }

                    lastShakeTime = currentTime
                }
            }

            lastAcceleration = acceleration
        }
    }

    // Função para calcular a aceleração horizontal
    private fun calculateHorizontalAcceleration(x: Float, y: Float): Float {
        return Math.sqrt((x * x + y * y).toDouble()).toFloat()
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Não é necessário implementar neste exemplo
    }

    private fun calculateAcceleration(x: Float, y: Float, z: Float): Float {
        return Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    }

    private fun isShaking(acceleration: Float): Boolean {
        val delta = acceleration - lastAcceleration
         return Math.abs(delta) > shakeThreshold
       // return acceleration > 40
    }

    private fun toggleFlashlight() {
        if (isFlashlightAvailable()) {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
                if(lanternaAtivada == false){
                    lanternaAtivada = true
                    // Ligar a lanterna
                    cameraManager.setTorchMode(cameraId, lanternaAtivada)
                    Log.d("ShakeService", "Lanterna ligada")
                    notificacao_off()

                }else{
                    lanternaAtivada = false
                    // Desligar a lanterna
                    cameraManager.setTorchMode(cameraId, lanternaAtivada)
                    Log.d("ShakeService", "Lanterna desligada")
                    notificacao_on()
                }

        } else {
            Log.e("ShakeService", "Lanterna não disponível neste dispositivo")
        }
    }


    private fun isFlashlightAvailable(): Boolean {
        return applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "AgitaLuz",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
    private fun notificacao_on(){
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title_lanterna_off) )
            .setContentText(getString(R.string.notification_text_stopped_lanterna))
            .setTicker("Lanterna ativada com agitação")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .build()

        startForeground(1, notification)
    }
    private fun notificacao_off(){
        val updatedNotification : Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle( getString(R.string.notification_title_lanterna_on))
            .setContentText(getString(R.string.notification_text_running_lanterna))
            .setTicker("Lanterna ativada com agitação")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .build()
        // Atualizar a notificação usando o mesmo ID
        startForeground(1, updatedNotification)
    }

}
