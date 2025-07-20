package org.smolang.greenhouse.api.model

class LightSensor (
    sensorId: String,
    val lightIntensity: Double,
) : Sensor(sensorId) {
    override fun toString(): String {
        return "LightSensor(sensorId='$sensorId', light intensity=$lightIntensity)"
    }
}