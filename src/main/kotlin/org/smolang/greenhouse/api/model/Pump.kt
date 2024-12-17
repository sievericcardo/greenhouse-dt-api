package org.smolang.greenhouse.api.model

import org.smolang.greenhouse.api.controller.PumpState

class Pump (
    val pumpGpioPin: Int,
    val pumpId: String,
    val temperature: Double,
    val pumpStatus: PumpState
) {
}