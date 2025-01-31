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

data class CreatePotRequest (
    val shelfFloor: String,
    val potPosition: String,
    val pumpId: String,
    val plantId: String
)

data class UpdatePotRequest (
    val shelfFloor: String,
    val potPosition: String,
    val pumpId: String,
    val plantId: String,
    val newShelfFloor: String?,
    val newPotPosition: String?,
    val newPumpId: String?,
    val newPlantId: String?
)

data class DeletePotRequest (
    val shelfFloor: String,
    val potPosition: String,
    val pumpId: String,
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
