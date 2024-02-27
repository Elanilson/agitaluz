package br.com.apkdoandroid.agitaluz.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.util.Log

class ProximitySensorChecker(private val context: Context) {

    private val TAG = "ProximitySensorChecker"
    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val proximitySensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    fun isProximityWithinDistance(distanceThreshold: Float, callback: (Boolean) -> Unit) {
        if (proximitySensor != null) {
            val proximityListener = ProximityListener(distanceThreshold, callback)
            sensorManager?.registerListener(
                proximityListener,
                proximitySensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )

            // Aguarda um tempo antes de chamar o callback
            Handler().postDelayed({
                sensorManager?.unregisterListener(proximityListener)
                proximityListener.checkDistance()
            }, 1000)
        } else {
            // Dispositivo não possui sensor de proximidade, retorna true
            Log.d(TAG, "Dispositivo sem sensor de proximidade. Retornando true.")
            callback(true)
        }
    }

    private inner class ProximityListener(
        private val distanceThreshold: Float,
        private val callback: (Boolean) -> Unit
    ) : SensorEventListener {
        private var proximityValue: Float? = null

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                proximityValue = event.values[0]
            }
        }

        fun checkDistance() {
            if (proximityValue != null) {
                Log.d("FlashService", "Distância do sensor de proximidade: $proximityValue")
                callback(proximityValue!! <= distanceThreshold)
            } else {
                Log.e("FlashService", "Nenhuma leitura do sensor de proximidade.")
                callback(true) // Se não houver leitura, assume-se como dentro da distância
            }
        }
    }
}
