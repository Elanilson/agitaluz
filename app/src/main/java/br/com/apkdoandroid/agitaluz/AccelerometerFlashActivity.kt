package br.com.apkdoandroid.agitaluz


import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import br.com.apkdoandroid.agitaluz.services.AccelerometerFlashService

class AccelerometerFlashActivity : AppCompatActivity() {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null

    // Ajuste este valor conforme necessário para controlar a sensibilidade do movimento
    private val HORIZONTAL_THRESHOLD_ON = -8.0
    private val HORIZONTAL_THRESHOLD_OFF = 8.0
    private val REQUIRED_MOVEMENTS = 2
    private val RESET_TIME_THRESHOLD_MILLIS = 10000L // 20 segundos
    private val FLASH_COOLDOWN_MILLIS = 500L // 3 segundos

    private var lastXValue = 0.0F
    private var movementsCount = 0
    private var isFlashOn = false
    private var lastMovementTime = 0L
    private var lastFlashToggleTime = 0L

    private var flashToggleHandler: Handler? = null
    private var flashToggleRunnable: Runnable? = null

    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accelerometer_flash)

        val meuServico = Intent(this, AccelerometerFlashService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(meuServico)
        }else{
            startService(meuServico)
        }

       /* if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            Log.e("FlashActivity", "Flash não disponível neste dispositivo.")
            finish()
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
            Log.e("FlashActivity", "Sensor de Acelerômetro não encontrado.")
            finish()
        }

        flashToggleHandler = Handler()*/
    }

    private val accelerometerListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]

                // Log.d("FlashActivity", "Eixo X: $x")

                if (x <= HORIZONTAL_THRESHOLD_ON && lastXValue > HORIZONTAL_THRESHOLD_OFF) {
                    // Mudança de direção para a esquerda
                    movementsCount++
                    checkAndToggleFlash()
                } else if (x >= HORIZONTAL_THRESHOLD_OFF && lastXValue < HORIZONTAL_THRESHOLD_ON) {
                    // Mudança de direção para a direita
                    movementsCount++
                    checkAndToggleFlash()
                }

                lastXValue = x
            }
        }
    }

    private fun checkAndToggleFlash() {
        val currentTime = SystemClock.elapsedRealtime()
        val elapsedTimeSinceLastMovement = currentTime - lastMovementTime
        val elapsedTimeSinceLastFlashToggle = currentTime - lastFlashToggleTime

        //  Log.d("FlashActivity", "TEMPO: ${elapsedTimeSinceLastFlashToggle } > ${RESET_TIME_THRESHOLD_MILLIS }")
        //  Log.d("FlashActivity", "TEMPO: ${elapsedTimeSinceLastFlashToggle >= FLASH_COOLDOWN_MILLIS}")
        //  Log.d("FlashActivity", "TEMPO: ${elapsedTimeSinceLastFlashToggle} - ${FLASH_COOLDOWN_MILLIS}")
        Log.d("FlashActivity", "MOVIMENTO: ${movementsCount >= REQUIRED_MOVEMENTS}  - ${movementsCount} - ${REQUIRED_MOVEMENTS}")
        if (movementsCount >= REQUIRED_MOVEMENTS && elapsedTimeSinceLastFlashToggle >= FLASH_COOLDOWN_MILLIS ||
            isFlashOn == true && elapsedTimeSinceLastFlashToggle > FLASH_COOLDOWN_MILLIS) {
            toggleFlash(!isFlashOn)
            movementsCount = 0 // Reinicia o contador após acionar o flash
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
                Log.d("FlashActivity", "Flash ${if (isFlashOn) "ligado" else "desligado"}")

                if (isFlashOn) {
                    // Vibração ao ligar o flash
                    vibrate()
                }
            } catch (e: CameraAccessException) {
                e.printStackTrace()
                Log.e("FlashActivity", "Erro ao acessar a câmera: ${e.message}")
            }
        }

        flashToggleHandler?.post(flashToggleRunnable!!)
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(300)
        }
    }


    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(accelerometerListener)
    }

    override fun onResume() {
        super.onResume()
        if (accelerometer != null) {
            sensorManager?.registerListener(
                accelerometerListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }
}