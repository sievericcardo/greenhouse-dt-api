package org.smolang.greenhouse.api.model

class LightSwitch(
    actuatorId: String,
    val isOn: Boolean
) : Actuator(actuatorId) {
    override fun toString(): String {
        return "LightSwitch(actuatorId='$actuatorId', isOn=$isOn)"
    }
}
