package org.smolang.greenhouse.api.model

import org.smolang.greenhouse.api.types.PlantMoistureState

class Plant(
    val plantId: String,
    val familyName: String,
    val moisture: Double?,
    val healthState: String?,
    val status: String?,
    val moistureState: PlantMoistureState
)