package org.smolang.greenhouse.api.model

import java.io.Serializable

class Sensor(
    val sensorId: String,
) : Serializable {
    override fun toString(): String {
        return "Sensor(sensorId='$sensorId')"
    }
}