package org.smolang.greenhouse.api.model

import org.smolang.greenhouse.api.types.PlantState

class Plant (
    val plantId: String,
    val idealMoisture: Double,
    val moisture: Double,
    val healthState: PlantState
) {
}