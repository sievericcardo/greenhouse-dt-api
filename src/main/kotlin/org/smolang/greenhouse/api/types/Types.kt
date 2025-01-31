package org.smolang.greenhouse.api.types

data class CreatePlantRequest (
    val plantId: String,
    val idealMoisture: Double
)

data class UpdatePlantRequest (
    val plantId: String,
    val idealMoistureNew: Double?
)

data class DeletePlantRequest (
    val plantId: String
)

data class CreatePumpRequest(
    val pumpGpioPin: Int,
    val pumpId: String,
    val modelName: String,
    val lifeTime: Int,
    val temperature: Double
)

data class UpdatePumpRequest (
    val pumpGpioPin: Int,
    val pumpId: String,
    val modelName: String,
    val lifeTime: Int,
    val temperature: Double
)

data class DeletePumpRequest (
    val pumpId: String
)
