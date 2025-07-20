package org.smolang.greenhouse.api.model

class NuetrientSensor(
    sensorId: String,
    val nutrient: Double
) : Sensor(sensorId) {
    override fun toString(): String {
        return "NutrientSensor(sensorId='$sensorId', nutrient=$nutrient)"
    }
}