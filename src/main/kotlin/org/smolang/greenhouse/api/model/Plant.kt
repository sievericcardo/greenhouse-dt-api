package org.smolang.greenhouse.api.model

class Plant (
    val plantId: String,
    val idealMoisture: Double,
    val moisture: Double,
    val healthState: String,
    val status: String
) {
}