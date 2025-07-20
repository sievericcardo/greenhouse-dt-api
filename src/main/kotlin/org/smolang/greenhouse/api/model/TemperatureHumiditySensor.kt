package org.smolang.greenhouse.api.model

class TemperatureHumiditySensor(
    sensorId: String,
    val temperature: Double,
    val humidity: Double
) : Sensor(sensorId) {
    override fun toString(): String {
        return "TemperatureHumiditySensor(sensorId='$sensorId', temperature=$temperature, humidity=$humidity)"
    }
}