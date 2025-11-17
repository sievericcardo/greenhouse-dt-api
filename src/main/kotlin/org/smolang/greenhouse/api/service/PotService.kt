package org.smolang.greenhouse.api.service

import org.apache.jena.query.ResultSet
import org.apache.jena.update.UpdateExecutionFactory
import org.apache.jena.update.UpdateFactory
import org.slf4j.LoggerFactory
import org.smolang.greenhouse.api.config.ComponentsConfig
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.smolang.greenhouse.api.model.Pot
import org.smolang.greenhouse.api.model.Pump
import org.springframework.stereotype.Service

@Service
class PotService(
    private val replConfig: REPLConfig,
    private val triplestoreProperties: TriplestoreProperties,
    private val componentsConfig: ComponentsConfig,
    private val moistureSensorService: MoistureSensorService,
    private val nutrientSensorService: NutrientSensorService,
    private val pumpService: PumpService
) {

    private val logger = LoggerFactory.getLogger(PotService::class.java)

    private val tripleStore = triplestoreProperties.tripleStore
    private val prefix = triplestoreProperties.prefix
    private val ttlPrefix = triplestoreProperties.ttlPrefix
    private val repl = replConfig.repl()

    fun createPot(potId: String): Boolean {
        logger.info("createPot: creating pot $potId")
        val query = """
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
            PREFIX ast: <$prefix>
            
            INSERT DATA {
                ast:pot$potId a ast:Pot ;
                    ast:potId "$potId" .
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            // add minimal pot representation to cache
            val defaultPump = Pump("default_pump_$potId", 0, null, null, null, null)
            val pot = Pot(potId, null, null, defaultPump)
            componentsConfig.addPotToCache(pot)
            logger.info("createPot: created pot $potId")
            return true
        } catch (e: Exception) {
            logger.error("createPot: failed to create pot $potId: ${e.message}", e)
            return false
        }
    }

    fun getPots(): List<Pot>? {
        logger.debug("getPots: retrieving pots")
        // Return cached pots if available
        val cached = componentsConfig.getPotCache()
        if (cached.isNotEmpty()) return cached.values.toList()
        // Basic pot info: only ids for sensors and pump. Plants will be queried per-pot and resolved via PlantService.
        val potsQuery =
            """
             SELECT ?potId ?pumpId ?moistureSensorId ?nutrientSensorId WHERE {
                 ?potObj a prog:Pot ;
                        prog:Pot_potId ?potId ;
                        prog:Pot_pump ?pumpObj .
                        
                 {
                    ?pumpObj a prog:Pump;
                        prog:Pump_actuatorId ?pumpId .
                 } UNION {
                    ?pumpObj a prog:OperatingPump ;
                        prog:OperatingPump_actuatorId ?pumpId .
                } UNION {
                    ?pumpObj a prog:MaintenancePump ;
                        prog:MaintenancePump_actuatorId ?pumpId .
                } UNION {
                    ?pumpObj a prog:OverheatingPump ;
                        prog:OverheatingPump_actuatorId ?pumpId .
                } UNION {
                    ?pumpObj a prog:UnderheatingPump ;
                        prog:UnderheatingPump_actuatorId ?pumpId .
                }

                 OPTIONAL {
                     ?potObj prog:Pot_moistureSensor ?moistureSensorObj .
                     ?moistureSensorObj prog:MoistureSensor_sensorId ?moistureSensorId .
                 }

                 OPTIONAL {
                     ?potObj prog:Pot_nutrientSensor ?nutrientSensorObj .
                     ?nutrientSensorObj prog:NutrientSensor_sensorId ?nutrientSensorId .
                 }
             }"""

        val result: ResultSet? = repl.interpreter!!.query(potsQuery)
        val potsList = mutableListOf<Pot>()

        if (result == null || !result.hasNext()) {
            logger.debug("getPots: no pots found")
            return null
        }

        while (result.hasNext()) {
            val solution = result.next()
            val potId = solution.get("?potId").asLiteral().toString()

            // Build moisture sensor if present (use MoistureSensorService to retrieve full sensor)
            val moistureSensor = if (solution.contains("?moistureSensorId")) {
                val sensorId = solution.get("?moistureSensorId").asLiteral().toString()
                try {
                    moistureSensorService.getSensor(sensorId)
                } catch (e: Exception) {
                    logger.warn("Failed to resolve moisture sensor $sensorId for pot $potId: ${e.message}")
                    null
                }
            } else null

            // Build nutrient sensor if present
            val nutrientSensor = if (solution.contains("?nutrientSensorId")) {
                val sensorId = solution.get("?nutrientSensorId").asLiteral().toString()
                try {
                    nutrientSensorService.getSensor(sensorId)
                } catch (e: Exception) {
                    logger.warn("Failed to resolve nutrient sensor $sensorId for pot $potId: ${e.message}")
                    null
                }
            } else null

            val pumpId = solution.get("?pumpId").asLiteral().toString()
            val pump = componentsConfig.getPumpById(pumpId) ?: pumpService.getPumpByPumpId(pumpId)

            if (pump == null) {
                // Missing pump is critical for the pot representation. Skip this pot and log a warning.
                logger.warn("Skipping pot $potId because pump was not found (pot query may be incomplete)")
                continue
            }

            val pot = Pot(potId, moistureSensor, nutrientSensor, pump)
            // populate cache
            componentsConfig.addPotToCache(pot)
            potsList.add(pot)
        }

        logger.debug("getPots: retrieved ${potsList.size} pots")
        return potsList
    }

    fun getPotByPotId(id: String): Pot? {
        logger.debug("getPotByPotId: retrieving pot $id")
        // Try cache first
        componentsConfig.getPotById(id)?.let { return it }

        val potsQuery =
            """
             SELECT DISTINCT ?pumpId ?moistureSensorId ?nutrientSensorId WHERE {
                 ?potObj a prog:Pot ;
                        prog:Pot_potId "$id" ;
                        prog:Pot_pump ?pumpObj .
                 
                 {
                    ?pumpObj a prog:Pump;
                        prog:Pump_actuatorId ?pumpId .
                 } UNION {
                    ?pumpObj a prog:OperatingPump ;
                        prog:OperatingPump_actuatorId ?pumpId .
                } UNION {
                    ?pumpObj a prog:MaintenancePump ;
                        prog:MaintenancePump_actuatorId ?pumpId .
                } UNION {
                    ?pumpObj a prog:OverheatingPump ;
                        prog:OverheatingPump_actuatorId ?pumpId .
                } UNION {
                    ?pumpObj a prog:UnderheatingPump ;
                        prog:UnderheatingPump_actuatorId ?pumpId .
                }

                 OPTIONAL {
                     ?potObj prog:Pot_moistureSensor ?moistureSensorObj .
                     ?moistureSensorObj prog:MoistureSensor_sensorId ?moistureSensorId .
                 }

                 OPTIONAL {
                     ?potObj prog:Pot_nutrientSensor ?nutrientSensorObj .
                     ?nutrientSensorObj prog:NutrientSensor_sensorId ?nutrientSensorId .
                 }
             }"""

        val result: ResultSet? = repl.interpreter!!.query(potsQuery)

        if (result == null || !result.hasNext()) {
            logger.debug("getPotByPotId: pot $id not found")
            return null
        }

        val solution = result.next()

        // Build moisture sensor if present
        val moistureSensor = if (solution.contains("?moistureSensorId")) {
            val sensorId = solution.get("?moistureSensorId").asLiteral().toString()
            moistureSensorService.getSensor(sensorId)
        } else null

        // Build nutrient sensor if present
        val nutrientSensor = if (solution.contains("?nutrientSensorId")) {
            val sensorId = solution.get("?nutrientSensorId").asLiteral().toString()
            nutrientSensorService.getSensor(sensorId)
        } else null

        // Build pump
        val pumpId = solution.get("?pumpId").asLiteral().toString()
        val pump = componentsConfig.getPumpById(pumpId) ?: pumpService.getPumpByPumpId(pumpId)!!

        val pot = Pot(id, moistureSensor, nutrientSensor, pump)
        // populate cache
        componentsConfig.addPotToCache(pot)
        logger.debug("getPotByPotId: retrieved pot $id")
        return pot
    }

    fun deletePot(potId: String): Boolean {
        logger.info("deletePot: deleting pot $potId")
        val query = """
            PREFIX ast: <$prefix>
            
            DELETE {
                ast:pot$potId ?p ?o .
                ?s ?p2 ast:pot$potId .
            }
            WHERE {
                { ast:pot$potId ?p ?o . }
                UNION
                { ?s ?p2 ast:pot$potId . }
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            componentsConfig.removePotFromCache(potId)
            logger.info("deletePot: deleted pot $potId")
            return true
        } catch (e: Exception) {
            logger.error("deletePot: failed to delete pot $potId: ${e.message}", e)
            return false
        }
    }
}