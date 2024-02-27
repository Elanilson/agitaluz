package br.com.apkdoandroid.agitaluz


import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import br.com.apkdoandroid.agitaluz.services.LanternaService
import android.content.Context
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import br.com.apkdoandroid.agitaluz.broadcasts.LanternaReceiver
import br.com.apkdoandroid.agitaluz.services.AccelerometerFlashService
import br.com.apkdoandroid.agitaluz.util.Permissao


class MainActivity : AppCompatActivity() {
    private var temPermissionNotificacao = false
    private val permissoes = listOf(Manifest.permission.POST_NOTIFICATIONS)
    private var isFlashOn = false
    private var lastUpdate: Long = 0
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private lateinit var myReceiver: LanternaReceiver
    // Ajuste este valor conforme necessário para controlar a sensibilidade do movimento
    private val MOVEMENT_THRESHOLD = 20
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

       temPermissionNotificacao = Permissao.requisitarPermissoes(this,permissoes,1)
        myReceiver = LanternaReceiver()

       // val meuServico = Intent(this, LanternaService::class.java)
        val meuServico = Intent(this, AccelerometerFlashService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(meuServico)
        }else{
            startService(meuServico)
        }

        IntentFilter().apply {
            addAction(Intent.ACTION_BOOT_COMPLETED)
        }.also {
            registerReceiver(myReceiver,it)
        }



        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
            Log.e("Accelerometer", "Acelerômetro não disponível neste dispositivo.")
            return
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer != null) {
            sensorManager?.registerListener(
                accelerometerListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        } else {
            Log.e("Accelerometer", "Sensor de Acelerômetro não encontrado.")
        }
    }

    private val accelerometerListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                Log.d("Accelerometer", "Eixo X: $x, Eixo Y: $y, Eixo Z: $z")
            }
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