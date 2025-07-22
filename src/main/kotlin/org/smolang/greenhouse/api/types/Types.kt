package org.smolang.greenhouse.api.types

data class CreatePlantRequest (
    val plantId: String,
    val moisture: Double,
    val healthState: String? = null,
    val status: String? = null
)

data class UpdatePlantRequest (
    val newMoisture: Double? = null,
    val newHealthState: String? = null,
    val newStatus: String? = null
)

data class CreatePotRequest (
    val potId: String
)

data class UpdatePotRequest (
    val potId: String
    // Add other fields as needed
)

data class CreatePumpRequest(
    val actuatorId: String,
    val pumpChannel: Int,
    val modelName: String? = null,
    val lifeTime: Int? = null,
    val temperature: Double? = null
)

data class UpdatePumpRequest (
    val actuatorId: String,
    val pumpChannel: Int? = null,
    val modelName: String? = null,
    val lifeTime: Int? = null,
    val temperature: Double? = null
)

data class DeletePumpRequest (
    val actuatorId: String
)

data class CreateGreenHouseRequest (
    val greenhouseId: String
)

data class CreateSectionRequest (
    val sectionId: String
)

data class CreateWaterBucketRequest (
    val bucketId: String,
    val waterLevel: Double
)

data class UpdateWaterBucketRequest (
    val bucketId: String,
    val newWaterLevel: Double
)
