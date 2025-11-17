package org.smolang.greenhouse.api.config

import org.smolang.greenhouse.api.model.*
import org.springframework.context.annotation.Configuration

typealias LightSensorCache = MutableMap<String, LightSensor>
typealias MoistureSensorCache = MutableMap<String, MoistureSensor>
typealias NutrientSensorCache = MutableMap<String, NutrientSensor>
typealias TemperatureHumiditySensorCache = MutableMap<String, TemperatureHumiditySensor>
typealias PlantCache = MutableMap<String, Plant>
typealias PotCache = MutableMap<String, Pot>
typealias PumpCache = MutableMap<String, Pump>
typealias SectionCache = MutableMap<String, Section>
typealias WaterBucketCache = MutableMap<String, WaterBucket>

@Configuration
open class ComponentsConfig {

    private val lightSensorCache: LightSensorCache = mutableMapOf()
    private val moistureSensorCache: MoistureSensorCache = mutableMapOf()
    private val nutrientSensorCache: NutrientSensorCache = mutableMapOf()
    private val temperatureHumiditySensorCache: TemperatureHumiditySensorCache = mutableMapOf()
    private val plantCache: PlantCache = mutableMapOf()
    private val potCache: PotCache = mutableMapOf()
    private val pumpCache: PumpCache = mutableMapOf()
    private val sectionCache: SectionCache = mutableMapOf()
    private val waterBucketCache: WaterBucketCache = mutableMapOf()

    open fun getLightSensorCache(): LightSensorCache {
        return lightSensorCache
    }

    open fun getLightSensorById(sensorId: String): LightSensor? {
        return lightSensorCache[sensorId]
    }

    open fun addLightSensorToCache(sensor: LightSensor): LightSensor {
        lightSensorCache[sensor.sensorId] = sensor
        return sensor
    }

    open fun removeLightSensorFromCache(sensorId: String): LightSensor? {
        return lightSensorCache.remove(sensorId)
    }

    open fun getMoistureSensorCache(): MoistureSensorCache {
        return moistureSensorCache
    }

    open fun getMoistureSensorById(sensorId: String): MoistureSensor? {
        return moistureSensorCache[sensorId]
    }

    open fun addMoistureSensorToCache(sensor: MoistureSensor): MoistureSensor {
        moistureSensorCache[sensor.sensorId] = sensor
        return sensor
    }

    open fun removeMoistureSensorFromCache(sensorId: String): MoistureSensor? {
        return moistureSensorCache.remove(sensorId)
    }

    open fun getNutrientSensorCache(): NutrientSensorCache {
        return nutrientSensorCache
    }

    open fun getNutrientSensorById(sensorId: String): NutrientSensor? {
        return nutrientSensorCache[sensorId]
    }

    open fun addNutrientSensorToCache(sensor: NutrientSensor): NutrientSensor {
        nutrientSensorCache[sensor.sensorId] = sensor
        return sensor
    }

    open fun removeNutrientSensorFromCache(sensorId: String): NutrientSensor? {
        return nutrientSensorCache.remove(sensorId)
    }

    open fun getTemperatureHumiditySensorCache(): TemperatureHumiditySensorCache {
        return temperatureHumiditySensorCache
    }

    open fun getTemperatureHumiditySensorById(sensorId: String): TemperatureHumiditySensor? {
        return temperatureHumiditySensorCache[sensorId]
    }

    open fun addTemperatureHumiditySensorToCache(sensor: TemperatureHumiditySensor): TemperatureHumiditySensor {
        temperatureHumiditySensorCache[sensor.sensorId] = sensor
        return sensor
    }

    open fun removeTemperatureHumiditySensorFromCache(sensorId: String): TemperatureHumiditySensor? {
        return temperatureHumiditySensorCache.remove(sensorId)
    }

    open fun getPlantCache(): PlantCache {
        return plantCache
    }

    open fun getPlantById(plantId: String): Plant? {
        return plantCache[plantId]
    }

    open fun addPlantToCache(plant: Plant): Plant {
        plantCache[plant.plantId] = plant
        return plant
    }

    open fun removePlantFromCache(plantId: String): Plant? {
        return plantCache.remove(plantId)
    }

    open fun clearPlantCache() {
        plantCache.clear()
    }

    open fun getPotCache(): PotCache {
        return potCache
    }

    open fun getPotById(potId: String): Pot? {
        return potCache[potId]
    }

    open fun addPotToCache(pot: Pot): Pot {
        potCache[pot.potId] = pot
        return pot
    }

    open fun removePotFromCache(potId: String): Pot? {
        return potCache.remove(potId)
    }

    open fun getPumpCache(): PumpCache {
        return pumpCache
    }

    open fun getPumpById(pumpId: String): Pump? {
        return pumpCache[pumpId]
    }

    open fun addPumpToCache(pump: Pump): Pump {
        pumpCache[pump.actuatorId] = pump
        return pump
    }

    open fun removePumpFromCache(pumpId: String): Pump? {
        return pumpCache.remove(pumpId)
    }

    open fun getSectionCache(): SectionCache {
        return sectionCache
    }

    open fun getSectionById(sectionId: String): Section? {
        return sectionCache[sectionId]
    }

    open fun addSectionToCache(section: Section): Section {
        sectionCache[section.sectionId] = section
        return section
    }

    open fun removeSectionFromCache(sectionId: String): Section? {
        return sectionCache.remove(sectionId)
    }

    open fun getWaterBucketCache(): WaterBucketCache {
        return waterBucketCache
    }

    open fun getWaterBucketById(bucketId: String): WaterBucket? {
        return waterBucketCache[bucketId]
    }

    open fun addWaterBucketToCache(bucket: WaterBucket): WaterBucket {
        waterBucketCache[bucket.bucketId] = bucket
        return bucket
    }

    open fun removeWaterBucketFromCache(bucketId: String): WaterBucket? {
        return waterBucketCache.remove(bucketId)
    }
}