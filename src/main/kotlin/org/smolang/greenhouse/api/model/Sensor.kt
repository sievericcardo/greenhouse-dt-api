package org.smolang.greenhouse.api.model

import java.io.Serializable

open class Sensor(
    val sensorId: String,
    val sensorProperty: String? = null,
    val accuracy: Double? = null,
) : Serializable {
    override fun toString(): String {
        return "Sensor(sensorId='$sensorId')"
    }
}