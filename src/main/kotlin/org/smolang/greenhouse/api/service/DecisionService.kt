package org.smolang.greenhouse.api.service

import jakarta.jms.JMSException
import org.apache.jena.query.ResultSet
import org.smolang.greenhouse.api.config.EnvironmentConfig
import org.smolang.greenhouse.api.config.QueueConfig
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.types.PlantMoistureState
import org.springframework.stereotype.Service
import java.util.logging.Logger

@Service
class DecisionService (
    private val replConfig: REPLConfig,
    private val environmentConfig: EnvironmentConfig,
    private val queueConfig: QueueConfig,
    private val messagePublisher: MessagePublisher,
    private val plantService: PlantService,
    private val potService: PotService,
    private val pumpService: PumpService,
    private val wateringStrategyLoader: WateringStrategyLoader
) {

    private val repl = replConfig.repl()
    private val log : Logger = Logger.getLogger(DecisionService::class.java.name)

    @Throws(JMSException::class)
    fun waterControl() {
        log.info("Starting water control")
        // list of pump pins to activate with their watering duration
        // Triple: (pumpPin, pumpId, wateringDuration)
        val pumpsToActivate: MutableList<Triple<Int, Int, Int>> = mutableListOf()

        // Get all pots (each pot contains plants and a pump)
        val pots = potService.getPots() ?: emptyList()

        if (pots.isEmpty()) {
            log.info("No pots found - nothing to water")
            return
        }

        // Load all configured strategies details (durations per moisture state)
        val allStrategies = wateringStrategyLoader.getAllStrategyDetails()

        for (pot in pots) {
            try {
                val pump = pot.pump

                // For each plant in the pot compute the min duration across all strategies
                val durationPlants = pot.plants.mapNotNull { plant ->
                    try {
                        val stateKey = when (plant.moistureState) {
                            PlantMoistureState.THIRSTY -> "thirsty"
                            PlantMoistureState.MOIST -> "moist"
                            PlantMoistureState.OVERWATERED -> "overwatered"
                            else -> "unknown"
                        }

                        // collect durations for this plant from every strategy
                        val durations = allStrategies.mapNotNull { (_, details) ->
                            val durationsMap = details["durations"] as? Map<*, *>
                            when (val value = durationsMap?.get(stateKey)) {
                                is Int -> value
                                is Number -> value.toInt()
                                is String -> value.toIntOrNull()
                                else -> null
                            }
                        }

                        // min across strategies for this plant (or 0 if none)
                        durations.minOrNull() ?: 0
                    } catch (e: Exception) {
                        log.warning("Failed to compute durations for plant ${plant.plantId}: ${e.message}")
                        null
                    }
                }

                // Determine the pump duration for the pot: minimum across plants' minima
                val pumpDuration = durationPlants.minOrNull() ?: 0

                if (pumpDuration > 0) {
                    // pump.pumpChannel used as gpio pin; actuatorId parsed to Int for pump id
                    try {
                        pumpsToActivate.add(Triple(pump.pumpChannel, pump.actuatorId.toInt(), pumpDuration))
                        log.info("Added pump ${pump.actuatorId} (pin ${pump.pumpChannel}) with duration $pumpDuration")
                    } catch (nfe: NumberFormatException) {
                        log.warning("Pump actuatorId is not an integer: ${pump.actuatorId}")
                    }
                }
            } catch (e: Exception) {
                log.warning("Error processing pot ${pot.potId}: ${e.message}")
            }
        }

        log.info("Processed pots. Start watering: ${pumpsToActivate.size} pumps to activate")
        startWaterActuator(pumpsToActivate)

        log.info("End watering")
        return
    }

    @Throws(JMSException::class)
    private fun startWaterActuator(pumpPinsToActivate: List<Triple<Int, Int, Int>>) {
        log.info("Start water actuator - Pumps to activate: ${pumpPinsToActivate.size}")

        if (pumpPinsToActivate.isEmpty()) {
            log.info("No pumps to activate - skipping")
            return
        }
        
        // We are not going to connect to any machines if we are in local
        val mode = environmentConfig.getOrDefault("MODE", "remote")
        log.info("MODE environment variable: $mode")
        
        if (mode == "remote") {
            for (pump in pumpPinsToActivate) {
                try {
                    val command = "[WATER]${pump.first} ${pump.third}"
                    log.info("Water cmd: $command")
                    log.info("Water pump: ${pump.second}")
                    messagePublisher.publish("actuator.${pump.second}.water", command)
                    log.info("Message published successfully to actuator.${pump.second}.water")
                } catch (e: Exception) {
                    log.severe("Failed to publish message for pump ${pump.second}: ${e.message}")
                    e.printStackTrace()
                }
            }
        } else {
            log.warning("MODE is not 'remote' (current: '$mode') - skipping message publishing. Set MODE environment variable to 'remote' to enable actuator messages.")
        }
    }
}
