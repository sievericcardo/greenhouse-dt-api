package org.smolang.greenhouse.api.model

class Pump (
    val pumpGpioPin: Int,
    val pumpId: String,
    val waterPressure: Double
) {
}