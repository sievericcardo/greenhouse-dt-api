package org.smolang.greenhouse.api.model

class NutrientSensor(
    sensorId: String,
    sensorProperty: String? = null,
    val nutrient: Double? = null
) : Sensor(sensorId, sensorProperty) {
    override fun toString(): String {
        return "NutrientSensor(sensorId='$sensorId', nutrient=$nutrient)"
    }
}