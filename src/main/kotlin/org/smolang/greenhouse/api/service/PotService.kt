package org.smolang.greenhouse.api.service

import org.apache.jena.query.ResultSet
import org.apache.jena.update.UpdateExecutionFactory
import org.apache.jena.update.UpdateFactory
import org.smolang.greenhouse.api.config.ComponentsConfig
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.smolang.greenhouse.api.model.Plant
import org.smolang.greenhouse.api.model.Pot
import org.smolang.greenhouse.api.model.Pump
import org.springframework.stereotype.Service

@Service
class PotService(
    private val replConfig: REPLConfig,
    private val triplestoreProperties: TriplestoreProperties,
    private val componentsConfig: ComponentsConfig,
    private val plantService: PlantService,
    private val moistureSensorService: MoistureSensorService,
    private val nutrientSensorService: NutrientSensorService,
    private val pumpService: PumpService
) {

    private val tripleStore = triplestoreProperties.tripleStore
    private val prefix = triplestoreProperties.prefix
    private val ttlPrefix = triplestoreProperties.ttlPrefix
    private val repl = replConfig.repl()

    fun createPot(potId: String): Boolean {
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
            val pot = Pot(potId, emptyList(), null, null, defaultPump)
            componentsConfig.addPotToCache(pot)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun getPots(): List<Pot>? {
        // Return cached pots if available
        val cached = componentsConfig.getPotCache()
        if (cached.isNotEmpty()) return cached.values.toList()
        // Basic pot info: only ids for sensors and pump. Plants will be queried per-pot and resolved via PlantService.
        val potsQuery =
            """
             SELECT DISTINCT ?potId ?pumpId ?pumpChannel ?modelName ?lifeTime ?temperature ?pumpStatus ?moistureSensorId ?nutrientSensorId WHERE {
                 ?potObj a prog:Pot ;
                        prog:Pot_potId ?potId .

                 OPTIONAL {
                     ?potObj prog:Pot_pump ?pumpObj .
                     ?pumpObj prog:Pump_actuatorId ?pumpId ;
                              prog:Pump_pumpChannel ?pumpChannel .
                     OPTIONAL { ?pumpObj prog:Pump_modelName ?modelName }
                     OPTIONAL { ?pumpObj prog:Pump_lifeTime ?lifeTime }
                     OPTIONAL { ?pumpObj prog:Pump_temperature ?temperature }
                     OPTIONAL { ?pumpObj prog:Pump_pumpStatus ?pumpStatus }
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
            return null
        }

        while (result.hasNext()) {
            val solution = result.next()
            val potId = solution.get("?potId").asLiteral().toString()

            // Build plants list by querying the pot's plants list and resolving each plant via PlantService
            val plants = mutableListOf<Plant>()
            val plantsQuery = """
                SELECT ?plantId WHERE {
                    ?pot a prog:Pot ; prog:Pot_potId "$potId" .
                    ?pot prog:Pot_plants ?plantObj .
                    ?plantObj prog:Plant_plantId ?plantId .
                }
            """.trimIndent()
            val plantsResult = repl.interpreter!!.query(plantsQuery)
            if (plantsResult != null) {
                while (plantsResult.hasNext()) {
                    val plantSol = plantsResult.next()
                    val plantId = plantSol.get("?plantId").asLiteral().toString()
                    plantService.getPlantByPlantId(plantId)?.let { plants.add(it) }
                }
            }

            // Build moisture sensor if present (use MoistureSensorService to retrieve full sensor)
            val moistureSensor = if (solution.contains("?moistureSensorId")) {
                val sensorId = solution.get("?moistureSensorId").asLiteral().toString()
                moistureSensorService.getSensor(sensorId)
            } else null

            // Build nutrient sensor if present
            val nutrientSensor = if (solution.contains("?nutrientSensorId")) {
                val sensorId = solution.get("?nutrientSensorId").asLiteral().toString()
                nutrientSensorService.getSensor(sensorId)
            } else null

            // Build pump: try cache first, then PumpService
            val pump = if (solution.contains("?pumpId")) {
                val pumpId = solution.get("?pumpId").asLiteral().toString()
                // try components cache
                componentsConfig.getPumpById(pumpId) ?: run {
                    // fallback to searching pumps via PumpService
                    pumpService.getPumpByPumpId(pumpId) ?: return null
                }
            } else {
                return null
            }

            potsList.add(Pot(potId, plants, moistureSensor, nutrientSensor, pump))
        }

        return potsList
    }

    fun getPotByPotId(id: String): Pot? {
        // Try cache first
        componentsConfig.getPotById(id)?.let { return it }

        val potsQuery =
            """
             SELECT DISTINCT ?potId ?pumpId ?moistureSensorId ?nutrientSensorId WHERE {
                 ?potObj a prog:Pot ;
                        prog:Pot_potId "$id" .

                 OPTIONAL {
                     ?potObj prog:Pot_pump ?pumpObj .
                     ?pumpObj prog:Pump_actuatorId ?pumpId .
                     OPTIONAL { ?pumpObj prog:Pump_modelName ?modelName }
                     OPTIONAL { ?pumpObj prog:Pump_lifeTime ?lifeTime }
                     OPTIONAL { ?pumpObj prog:Pump_temperature ?temperature }
                     OPTIONAL { ?pumpObj prog:Pump_pumpStatus ?pumpStatus }
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
            return null
        }

        val solution = result.next()
        val potId = solution.get("?potId").asLiteral().toString()

        // Build plants list (query list and resolve each plant)
        val plants = mutableListOf<Plant>()
        val plantsQuery = """
            SELECT ?plantId WHERE {
                ?pot a prog:Pot ; prog:Pot_potId "$potId" .
                ?pot prog:Pot_plants ?plantObj .
                ?plantObj prog:Plant_plantId ?plantId .
            }
        """.trimIndent()
        val plantsResult = repl.interpreter!!.query(plantsQuery)
        if (plantsResult != null) {
            while (plantsResult.hasNext()) {
                val plantSol = plantsResult.next()
                val plantId = plantSol.get("?plantId").asLiteral().toString()
                plantService.getPlantByPlantId(plantId)?.let { plants.add(it) }
            }
        }

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
        val pump = if (solution.contains("?pumpId")) {
            val pumpId = solution.get("?pumpId").asLiteral().toString()
            pumpService.getPumpByPumpId(pumpId) ?: return null
        } else {
            return null
        }

        return Pot(potId, plants, moistureSensor, nutrientSensor, pump)
    }

    fun deletePot(potId: String): Boolean {
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
            return true
        } catch (e: Exception) {
            return false
        }
    }
}