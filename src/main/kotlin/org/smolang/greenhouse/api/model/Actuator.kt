package org.smolang.greenhouse.api.model

import java.io.Serializable

open class Actuator(
    val actuatorId: String,
) : Serializable {
    override fun toString(): String {
        return "Actuator(actuatorId='$actuatorId')"
    }
}