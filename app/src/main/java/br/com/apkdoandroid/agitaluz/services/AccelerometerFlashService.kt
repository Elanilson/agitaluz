package br.com.apkdoandroid.agitaluz.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import br.com.apkdoandroid.agitaluz.MainActivity
import br.com.apkdoandroid.agitaluz.R
import br.com.apkdoandroid.agitaluz.util.ProximitySensorChecker

class AccelerometerFlashService : Service() {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null

    private val HORIZONTAL_THRESHOLD_ON = -6.0
    private val HORIZONTAL_THRESHOLD_OFF = 6.0
    private val REQUIRED_MOVEMENTS = 2
    private var RESET_TIME_THRESHOLD_MILLIS = 10000L
    private val FLASH_COOLDOWN_MILLIS = 500L

    private var lastXValue = 0.0F
    private var movementsCount = 0
    private var isFlashOn = false
    private var lastMovementTime = 0L
    private var lastFlashToggleTime = 0L

    private var flashToggleHandler: Handler? = null
    private var flashToggleRunnable: Runnable? = null

    private var vibrator: Vibrator? = null
    private val CHANNEL_ID = "ForegroundServiceChannel"
    var currentTime = SystemClock.elapsedRealtime()
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        notificacao_on()

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            Log.e("FlashService", "Flash não disponível neste dispositivo.")
            stopSelf()
            return
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager?.cameraIdList?.get(0)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (accelerometer != null) {
            sensorManager?.registerListener(
                accelerometerListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        } else {
            Log.e("FlashService", "Sensor de Acelerômetro não encontrado.")
            stopSelf()
        }

        flashToggleHandler = Handler()
    }

    private val accelerometerListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                currentTime = SystemClock.elapsedRealtime()
                val elapsedTimeSinceLastMovement = currentTime - lastMovementTime
                Log.d("FlashService", "MOVIMENTO-: ${movementsCount >= REQUIRED_MOVEMENTS}  - ${movementsCount} - ${REQUIRED_MOVEMENTS}")
                Log.d("FlashService", "MOVIMENTO: - ${(elapsedTimeSinceLastMovement >= 6000L &&  elapsedTimeSinceLastMovement <= 7000L ) && isFlashOn == false} = ${RESET_TIME_THRESHOLD_MILLIS} - ${elapsedTimeSinceLastMovement}")
                if ((elapsedTimeSinceLastMovement >= 6000L &&  elapsedTimeSinceLastMovement <= 7000L )  && isFlashOn == false) {
                        movementsCount = 0
                }

                if (x <= HORIZONTAL_THRESHOLD_ON && lastXValue > HORIZONTAL_THRESHOLD_OFF) {
                    lastMovementTime = currentTime
                    movementsCount++
                    checkAndToggleFlash()
                } else if (x >= HORIZONTAL_THRESHOLD_OFF && lastXValue < HORIZONTAL_THRESHOLD_ON) {
                    lastMovementTime = currentTime
                    movementsCount++
                    checkAndToggleFlash()
                }

                lastXValue = x
            }
        }
    }

    private fun checkAndToggleFlash() {

        val elapsedTimeSinceLastFlashToggle = currentTime - lastFlashToggleTime


        Log.d("FlashService", "MOVIMENTO: ${movementsCount >= REQUIRED_MOVEMENTS}  - ${movementsCount} - ${REQUIRED_MOVEMENTS}")
        if (movementsCount >= REQUIRED_MOVEMENTS && elapsedTimeSinceLastFlashToggle >= FLASH_COOLDOWN_MILLIS ||
            isFlashOn == true && elapsedTimeSinceLastFlashToggle > FLASH_COOLDOWN_MILLIS
        ) {
            toggleFlash(!isFlashOn)
            movementsCount = 0
            lastMovementTime = currentTime
            lastFlashToggleTime = currentTime
        }
    }

    private fun toggleFlash(turnOn: Boolean) {
        if (flashToggleHandler == null) {
            flashToggleHandler = Handler()
        }

        if (flashToggleRunnable != null) {
            flashToggleHandler?.removeCallbacks(flashToggleRunnable!!)
        }

        flashToggleRunnable = Runnable {
            try {
                cameraManager?.setTorchMode(cameraId!!, turnOn)
                isFlashOn = turnOn
                Log.d("FlashService", "Flash ${if (isFlashOn) "ligado" else "desligado"}")

                if (isFlashOn) {
                    //notificacao_off()
                    vibrate()
                }else{
                   // notificacao_on()
                }
            } catch (e: CameraAccessException) {
                e.printStackTrace()
                Log.e("FlashService", "Erro ao acessar a câmera: ${e.message}")
            }
        }

        flashToggleHandler?.post(flashToggleRunnable!!)
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
        // Intent para abrir a MainActivity
        val intent = Intent(this, MainActivity::class.java)
        var pendingIntent : PendingIntent? = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
             pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }else{
             pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title_lanterna_off) )
            .setContentText(getString(R.string.notification_text_stopped_lanterna))
            .setTicker("Lanterna ativada com agitação")
            .setSmallIcon(R.drawable.laterninha)
            .setContentIntent(pendingIntent)  // Define o PendingIntent
            .build()

        startForeground(1, notification)
    }
    private fun notificacao_off(){
        val updatedNotification : Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle( getString(R.string.notification_title_lanterna_on))
            .setContentText(getString(R.string.notification_text_running_lanterna))
            .setTicker("Lanterna ativada com agitação")
            .setSmallIcon(R.drawable.laterninha)
            .build()
        // Atualizar a notificação usando o mesmo ID
        startForeground(1, updatedNotification)
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(300)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(accelerometerListener)
    }
}
