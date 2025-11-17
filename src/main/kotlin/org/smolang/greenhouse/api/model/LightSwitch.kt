package org.smolang.greenhouse.api.model

class LightSwitch(
    actuatorId: String,
    val lightIntensity: Double? = null,
    val isOn: Boolean? = null
) : Actuator(actuatorId) {
    override fun toString(): String {
        return "LightSwitch(actuatorId='$actuatorId', isOn=$isOn)"
    }
}
