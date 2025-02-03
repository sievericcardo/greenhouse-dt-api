package org.smolang.greenhouse.api.service

import jakarta.jms.JMSException
import org.apache.jena.query.ResultSet
import org.smolang.greenhouse.api.config.EnvironmentConfig
import org.smolang.greenhouse.api.config.QueueConfig
import org.smolang.greenhouse.api.config.REPLConfig
import java.util.logging.Logger


class DecisionService (
    private val replConfig: REPLConfig,
    private val environmentConfig: EnvironmentConfig,
    private val queueConfig: QueueConfig,
    private val messagePublisher: MessagePublisher
) {

    private val repl = replConfig.repl()
    private val log : Logger = Logger.getLogger(DecisionService::class.java.name)

    fun execSmol(): ResultSet {
        checkNotNull(repl.interpreter)
        repl.interpreter!!.evalCall(
            repl.interpreter!!.getObjectNames("AssetModel")[0],
            "AssetModel",
            "decision"
        )

        val needWaterQuery: String =
            """
            PREFIX prog: <https://github.com/Edkamb/SemanticObjects/Program#>
            
            SELECT DISTINCT ?plantId ?pumpGpioPin ?pumpId 
            WHERE { ?plantToWater prog:Decision_plantId ?plantId ; 
                prog:Decision_pumpGpioPin ?pumpGpioPin ; 
                prog:Decision_pumpId ?pumpId . 
            }
        """.trimIndent()

        val plantsToWater: ResultSet = repl.interpreter!!.query(needWaterQuery)!!;
        return plantsToWater
    }

    @Throws(JMSException::class)
    fun waterControl(plantsToWater: ResultSet) {
        // list of pump pins to activate
        val pumps: MutableList<Pair<Int, Int>> = mutableListOf()

        while (plantsToWater.hasNext()) {
            val plantToWater = plantsToWater.next()
            val plantId = plantToWater["?plantId"].asLiteral().toString()
            log.info("Plant to water: $plantId")
            // Split the content I retrieve for pumpPin based on the delimiter ^^ and take the first element
            val pumpPin = plantToWater["?pumpGpioPin"].asLiteral().toString().split("\\^\\^".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()[0]
            //      String pumpPin = plantToWater.get("?pumpGpioPin").asLiteral().toString();
            val pumpId = plantToWater["?pumpId"].asLiteral().toString()
            log.info("Pump Pin: $pumpPin Pump Id: $pumpId")

            // Add the List of the two elements into the pumps List
            pumps.add(Pair(pumpPin.toInt(), pumpId.toInt()))
        }
        log.info("Start watering")
        startWaterActuator(pumps)

        log.info("End watering")
    }

    @Throws(JMSException::class)
    private fun startWaterActuator(pumpPinsToActivate: List<Pair<Int, Int>>) {
        log.info("Start water actuator")

        if (pumpPinsToActivate.isNotEmpty()) {
            // We are not going to connect to any machines if we are in local
            val mode = environmentConfig.getOrDefault("MODE", "local")
            if (mode== "remote") {
                for (pumps in pumpPinsToActivate) {
                    try {
                        val command = "[WATER]" + pumps.first + " 2"
                        log.info("Water cmd: $command")
                        log.info("Water pump: " + pumps.second)
                        messagePublisher.publish("actuator." + pumps.second + ".water", command)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}