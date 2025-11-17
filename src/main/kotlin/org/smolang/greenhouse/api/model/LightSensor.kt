package org.smolang.greenhouse.api.model

class LightSensor(
    sensorId: String,
    sensorProperty: String? = null,
    val lightIntensity: Double? = null,
) : Sensor(sensorId) {
    override fun toString(): String {
        return "LightSensor(sensorId='$sensorId', light intensity=$lightIntensity)"
    }
}