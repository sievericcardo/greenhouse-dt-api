package org.smolang.greenhouse.api.model

import org.smolang.greenhouse.api.types.PumpState

class Pump (
    actuatorId: String,
    val pumpChannel: Int,
    val modelName: String?,
    val lifeTime: Int?,
    val temperature: Double?,
    val pumpStatus: PumpState?
) : Actuator(actuatorId) {
    override fun toString(): String {
        return "Pump(actuatorId='$actuatorId', pumpGpioPin=$pumpGpioPin, modelName='$modelName', lifeTime=$lifeTime, temperature=$temperature, pumpStatus=$pumpStatus)"
    }
}