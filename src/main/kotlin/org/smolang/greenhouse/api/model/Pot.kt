package org.smolang.greenhouse.api.model

class Pot (
    val potId: String,
    val plants: List<Plant>,
    val moistureSensor: MoistureSensor?,
    val nutrientSensor: NutrientSensor?,
    val pump: Pump
) {
}