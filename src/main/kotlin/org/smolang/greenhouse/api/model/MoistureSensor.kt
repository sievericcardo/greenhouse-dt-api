package org.smolang.greenhouse.api.model

class MoistureSensor(
    sensorId: String,
    sensorProperty: String? = null,
    accuracy: Double? = null,
    val moisture: Double? = null
) : Sensor(sensorId, sensorProperty, accuracy) {
    override fun toString(): String {
        return "MoistureSensor(sensorId='$sensorId', moisture=$moisture)"
    }
}