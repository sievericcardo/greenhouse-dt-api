package org.smolang.greenhouse.api.model

class TemperatureHumiditySensor(
    sensorId: String,
    sensorProperty: String? = null,
    val temperature: Double? = null,
    val humidity: Double? = null
) : Sensor(sensorId, sensorProperty) {
    override fun toString(): String {
        return "TemperatureHumiditySensor(sensorId='$sensorId', temperature=$temperature, humidity=$humidity)"
    }
}