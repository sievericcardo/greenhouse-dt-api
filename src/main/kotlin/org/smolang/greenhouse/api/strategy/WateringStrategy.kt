package org.smolang.greenhouse.api.strategy

import org.smolang.greenhouse.api.model.Plant
import org.smolang.greenhouse.api.types.PlantMoistureState

/**
 * Strategy interface for determining watering duration based on plant moisture state
 */
interface WateringStrategy {
    /**
     * Calculate the watering duration in seconds for a given plant
     * @param plant The plant to water
     * @param moistureState The current moisture state of the plant
     * @return The watering duration in seconds
     */
    fun calculateWateringDuration(plant: Plant, moistureState: PlantMoistureState): Int
}
