package org.smolang.greenhouse.api.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.smolang.greenhouse.api.service.*
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Initializes components caches by calling getAll / get functions on services after
 * the Spring context is refreshed. This avoids circular injection into REPLConfig.
 */
@Component
class ComponentsCacheInitializer(
    private val lightSensorService: LightSensorService,
    private val moistureSensorService: MoistureSensorService,
    private val nutrientSensorService: NutrientSensorService,
    private val temperatureHumiditySensorService: TemperatureHumiditySensorService,
    private val plantService: PlantService,
    private val potService: PotService,
    private val pumpService: PumpService,
    private val sectionService: SectionService,
    private val waterBucketService: WaterBucketService,
    private val componentsConfig: ComponentsConfig
) {

    private val log: Logger = LoggerFactory.getLogger(ComponentsCacheInitializer::class.java.name)

    @EventListener(ContextRefreshedEvent::class)
    fun onContextRefreshed(event: ContextRefreshedEvent) {
        log.info("ComponentsCacheInitializer: populating component caches...")

        try {
            // Sensors
            lightSensorService.getAllSensors()
            moistureSensorService.getAllSensors()
            nutrientSensorService.getAllSensors()
            temperatureHumiditySensorService.getAllSensors()

            // Plants, pots, pumps, sections, water buckets
            waterBucketService.getAllWaterBuckets()
            pumpService.getAllPumps()
            potService.getPots()
            plantService.getAllPlants()
            sectionService.getAllSections()

            log.info("ComponentsCacheInitializer: caches populated (hopefully)")
        } catch (e: Exception) {
            log.warn("ComponentsCacheInitializer: error while populating caches: ${e.message}")
        }
    }
}
