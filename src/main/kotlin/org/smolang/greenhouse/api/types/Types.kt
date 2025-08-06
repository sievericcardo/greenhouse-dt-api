package org.smolang.greenhouse.api.types

data class CreatePlantRequest (
    val plantId: String,
    val familyName: String,
    val status: String? = null
)

data class UpdatePlantRequest (
    val newStatus: String? = null
)

data class CreatePumpRequest(
    val actuatorId: String,
    val pumpChannel: Int,
    val modelName: String? = null,
    val lifeTime: Int? = null,
    val temperature: Double? = null
)

data class UpdatePumpRequest (
    val actuatorId: String? = null,
    val pumpChannel: Int? = null,
    val modelName: String? = null,
    val lifeTime: Int? = null,
    val temperature: Double? = null
)

data class DeletePumpRequest (
    val actuatorId: String
)

data class CreateLightSwitchRequest(
    val actuatorId: String
)

data class CreateGreenHouseRequest (
    val greenhouseId: String,
    val sections: List<String>,
    val waterBuckets: List<String>,
    val lightSensor: String? = null,
    val thSensor: String? = null,
)

data class CreateSectionRequest (
    val sectionId: String,
    val pots: List<String>
)

data class UpdateSectionRequest (
    val pots: List<String>?
)

data class CreateWaterBucketRequest (
    val bucketId: String
)

data class UpdateWaterBucketRequest (
    val bucketId: String
)

data class CreatePotRequest (
    val potId: String,
    val plants: List<String>,
    val moistureSensor: String? = null,
    val nutrientSensor: String? = null,
    val pump: String
)

data class UpdatePotRequest (
    val plants: List<String>? = null,
    val moistureSensor: String? = null,
    val nutrientSensor: String? = null,
    val pump: String? = null
)

data class CreateMoistureSensorRequest (
    val sensorId: String,
    val sensorProperty: String,
)

data class UpdateMoistureSensorRequest (
    val sensorProperty: String?,
)

data class CreateNutrientSensorRequest (
    val sensorId: String,
    val sensorProperty: String,
)

data class UpdateNutrientSensorRequest (
    val sensorProperty: String?,
)

data class CreateLightSensorRequest (
    val sensorId: String,
    val sensorProperty: String,
)

data class UpdateLightSensorRequest (
    val sensorProperty: String?,
)

data class CreateTemperatureHumiditySensorRequest (
    val sensorId: String,
    val sensorProperty: String,
)

data class UpdateTemperatureHumiditySensorRequest (
    val sensorProperty: String?,
)
