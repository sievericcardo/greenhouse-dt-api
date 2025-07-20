package org.smolang.greenhouse.api.model

import java.io.Serializable

open class Sensor(
    val sensorId: String,
) : Serializable {
    override fun toString(): String {
        return "Sensor(sensorId='$sensorId')"
    }
}