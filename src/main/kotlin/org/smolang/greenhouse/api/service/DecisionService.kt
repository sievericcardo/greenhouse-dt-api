package org.smolang.greenhouse.api.service

import jakarta.jms.JMSException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.smolang.greenhouse.api.config.EnvironmentConfig
import org.smolang.greenhouse.api.config.QueueConfig
import org.smolang.greenhouse.api.config.REPLConfig
import org.springframework.stereotype.Service

@Service
class DecisionService(
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
    private val log: Logger = LoggerFactory.getLogger(DecisionService::class.java.name)

    @Throws(JMSException::class)
    fun waterControl() {
        log.info("Starting water control")
        // list of pump pins to activate with their watering duration
        // Triple: (pumpPin, pumpId, wateringDuration)
        val pumpsToActivate: MutableList<Triple<Int, Int, Int>> = mutableListOf()

        // Get all plants (pots are reachable via plant.pot)
        val plants = plantService.getAllPlants()

        if (plants.isNullOrEmpty()) {
            log.info("No plants found - skipping watering")
            return
        }

        // Use the active watering strategy to compute per-plant durations
        val activeStrategy = wateringStrategyLoader.getActiveStrategy()

        // Group plants by pot id - multiple plants can belong to the same pot
        val plantsByPot = plants.groupBy { it.pot.potId }

        // For each pot, compute the watering durations for all its plants and pick the minimum
        for ((potId, potPlants) in plantsByPot) {
            if (potPlants.isEmpty()) continue

            val pot = potPlants.first().pot
            val pump = pot.pump

            // Compute durations per plant using the active strategy
            val durations = potPlants.map { plant ->
                try {
                    activeStrategy.calculateWateringDuration(plant, plant.moistureState)
                } catch (e: Exception) {
                    log.error("Failed to calculate watering duration for plant ${plant.plantId}: ${e.message}")
                    0
                }
            }

            val pumpDuration = durations.filter { it > 0 }.minOrNull() ?: durations.minOrNull() ?: 0

            if (pumpDuration <= 0) {
                log.info("Calculated pump duration <= 0 for pot $potId - skipping pump activation")
                continue
            }

            // Convert actuator id to Int if possible
            val actuatorIdInt = try {
                pump.actuatorId.toInt()
            } catch (e: NumberFormatException) {
                log.error("Pump actuatorId is not a valid integer: ${pump.actuatorId} - skipping pump for pot $potId")
                continue
            }

            pumpsToActivate.add(Triple(pump.pumpChannel, actuatorIdInt, pumpDuration))
            log.info("Scheduled pump for pot $potId -> channel=${pump.pumpChannel}, actuator=${actuatorIdInt}, duration=${pumpDuration}")
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
                    log.error("Failed to publish message for pump ${pump.second}: ${e.message}")
                    e.printStackTrace()
                }
            }
        } else {
            log.warn("MODE is not 'remote' (current: '$mode') - skipping message publishing. Set MODE environment variable to 'remote' to enable actuator messages.")
        }
    }
}
