package org.smolang.greenhouse.api.service

import com.sksamuel.hoplite.ConfigLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.smolang.greenhouse.api.config.MoistureDurations
import org.smolang.greenhouse.api.config.StrategyDefinition
import org.smolang.greenhouse.api.config.WateringStrategyConfig
import org.smolang.greenhouse.api.model.Plant
import org.smolang.greenhouse.api.strategy.WateringStrategy
import org.smolang.greenhouse.api.types.PlantMoistureState
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Service that loads and manages watering strategies from a YAML configuration file.
 * Supports runtime reloading without application restart.
 */
@Service
class WateringStrategyLoader {

    private val log: Logger = LoggerFactory.getLogger(WateringStrategyLoader::class.java.name)
    private val configResourcePath = "/watering-strategies.yml"
    private val configFilePath = "src/main/resources/watering-strategies.yml"
    private val lock = ReentrantReadWriteLock()

    private var config: WateringStrategyConfig = WateringStrategyConfig()
    private var activeStrategyName: String = "default"

    init {
        loadConfiguration()
    }

    /**
     * Load or reload the configuration from the YAML file
     */
    fun loadConfiguration(): Boolean {
        return try {
            lock.write {
                // Try to load from classpath first (for JAR execution)
                val resourceUrl = javaClass.getResource(configResourcePath)
                config = if (resourceUrl != null) {
                    log.info("Loading configuration from classpath: $configResourcePath")
                    ConfigLoader().loadConfigOrThrow(configResourcePath)
                } else {
                    // Fallback to file system (for development)
                    val file = File(configFilePath)
                    if (!file.exists()) {
                        log.warn("Configuration file not found at: $configFilePath. Using defaults.")
                        return false
                    }
                    log.info("Loading configuration from file: $configFilePath")
                    ConfigLoader().loadConfigOrThrow(configFilePath)
                }

                activeStrategyName = config.activeStrategy
                log.info("Loaded watering strategies configuration. Active strategy: $activeStrategyName")
                log.info("Available strategies: ${config.strategies.keys.joinToString(", ")}")
            }
            true
        } catch (e: Exception) {
            log.error("Failed to load configuration: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Save the current configuration to the YAML file
     */
    fun saveConfiguration(): Boolean {
        return try {
            val file = File(configFilePath)
            lock.read {
                // Convert config to YAML manually since Hoplite is primarily for loading
                val yamlContent = buildYamlString(config)
                file.writeText(yamlContent)
                log.info("Saved watering strategies configuration")
            }
            true
        } catch (e: Exception) {
            log.error("Failed to save configuration: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Build YAML string from configuration
     */
    private fun buildYamlString(config: WateringStrategyConfig): String {
        val yaml = StringBuilder()
        yaml.appendLine("activeStrategy: ${config.activeStrategy}")
        yaml.appendLine("strategies:")
        config.strategies.forEach { (key, strategy) ->
            yaml.appendLine("  $key:")
            yaml.appendLine("    name: ${strategy.name}")
            yaml.appendLine("    description: ${strategy.description}")
            yaml.appendLine("    durations:")
            yaml.appendLine("      thirsty: ${strategy.durations.thirsty}")
            yaml.appendLine("      moist: ${strategy.durations.moist}")
            yaml.appendLine("      overwatered: ${strategy.durations.overwatered}")
            yaml.appendLine("      unknown: ${strategy.durations.unknown}")
        }
        return yaml.toString()
    }

    /**
     * Get the current active strategy
     */
    fun getActiveStrategy(): WateringStrategy {
        return lock.read {
            val strategyDef = config.strategies[activeStrategyName]
                ?: config.strategies["default"]
                ?: throw IllegalStateException("No strategy found: $activeStrategyName")

            ConfigurableWateringStrategy(strategyDef)
        }
    }

    /**
     * Set the active strategy by name
     */
    fun setActiveStrategy(strategyName: String): Boolean {
        return lock.write {
            if (!config.strategies.containsKey(strategyName)) {
                log.warn("Strategy not found: $strategyName")
                return@write false
            }

            activeStrategyName = strategyName
            // Update the config to persist
            config = config.copy(activeStrategy = strategyName)
            saveConfiguration()

            log.info("Active strategy changed to: $strategyName")
            true
        }
    }

    /**
     * Get all available strategy names and descriptions
     */
    fun getAllStrategies(): Map<String, String> {
        return lock.read {
            config.strategies.mapValues { (_, def) -> def.description }
        }
    }

    /**
     * Get detailed information about all strategies
     */
    fun getAllStrategyDetails(): Map<String, Map<String, Any>> {
        return lock.read {
            config.strategies.mapValues { (key, def) ->
                mapOf(
                    "name" to def.name,
                    "description" to def.description,
                    "durations" to mapOf(
                        "thirsty" to def.durations.thirsty,
                        "moist" to def.durations.moist,
                        "overwatered" to def.durations.overwatered,
                        "unknown" to def.durations.unknown
                    ),
                    "isActive" to (key == activeStrategyName)
                )
            }
        }
    }

    /**
     * Get the name of the currently active strategy
     */
    fun getActiveStrategyName(): String {
        return lock.read { activeStrategyName }
    }

    /**
     * Get the active strategy definition
     */
    fun getActiveStrategyDefinition(): StrategyDefinition? {
        return lock.read {
            config.strategies[activeStrategyName]
        }
    }

    /**
     * Get watering duration for a specific plant based on the active strategy
     */
    fun getWateringDurationForPlant(plant: Plant): Int {
        val strategy = getActiveStrategy()
        return strategy.calculateWateringDuration(plant, plant.moistureState)
    }

    /**
     * Add or update a strategy in the configuration
     */
    fun addOrUpdateStrategy(
        strategyKey: String, name: String, description: String,
        thirstyDuration: Int, moistDuration: Int,
        overwateredDuration: Int, unknownDuration: Int
    ): Boolean {
        return try {
            lock.write {
                val newStrategy = StrategyDefinition(
                    name = name,
                    description = description,
                    durations = MoistureDurations(
                        thirsty = thirstyDuration,
                        moist = moistDuration,
                        overwatered = overwateredDuration,
                        unknown = unknownDuration
                    )
                )

                val updatedStrategies = config.strategies.toMutableMap()
                updatedStrategies[strategyKey] = newStrategy
                config = config.copy(strategies = updatedStrategies)

                saveConfiguration()
                log.info("Strategy '$strategyKey' added/updated successfully")
                true
            }
        } catch (e: Exception) {
            log.error("Failed to add/update strategy: ${e.message}")
            false
        }
    }

    /**
     * Delete a strategy from the configuration
     */
    fun deleteStrategy(strategyKey: String): Boolean {
        return lock.write {
            if (strategyKey == activeStrategyName) {
                log.warn("Cannot delete active strategy: $strategyKey")
                return@write false
            }

            if (!config.strategies.containsKey(strategyKey)) {
                log.warn("Strategy not found: $strategyKey")
                return@write false
            }

            val updatedStrategies = config.strategies.toMutableMap()
            updatedStrategies.remove(strategyKey)
            config = config.copy(strategies = updatedStrategies)

            saveConfiguration()
            log.info("Strategy '$strategyKey' deleted successfully")
            true
        }
    }
}

/**
 * Internal strategy implementation that uses configuration
 */
private class ConfigurableWateringStrategy(
    private val strategyDef: StrategyDefinition
) : WateringStrategy {

    override fun calculateWateringDuration(plant: Plant, moistureState: PlantMoistureState): Int {
        return when (moistureState) {
            PlantMoistureState.THIRSTY -> strategyDef.durations.thirsty
            PlantMoistureState.MOIST -> strategyDef.durations.moist
            PlantMoistureState.OVERWATERED -> strategyDef.durations.overwatered
            PlantMoistureState.UNKNOWN -> strategyDef.durations.unknown
        }
    }
}
