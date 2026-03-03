package org.smolang.greenhouse.api.model

class LightSensor(
    sensorId: String,
    sensorProperty: String? = null,
    accuracy: Double? = null,
    val lightIntensity: Double? = null,
) : Sensor(sensorId, sensorProperty, accuracy) {
    override fun toString(): String {
        return "LightSensor(sensorId='$sensorId', light intensity=$lightIntensity)"
    }
}