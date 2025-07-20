package org.smolang.greenhouse.api.model

class MoistureSensor(
    sensorId: String,
    val moisture: Double
) : Sensor(sensorId) {
    override fun toString(): String {
        return "MoistureSensor(sensorId='$sensorId', moisture=$moisture)"
    }
}