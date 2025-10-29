package org.smolang.greenhouse.api.service

import org.apache.jena.query.QuerySolution
import org.apache.jena.query.ResultSet
import org.apache.jena.update.UpdateExecutionFactory
import org.apache.jena.update.UpdateFactory
import org.apache.jena.update.UpdateProcessor
import org.apache.jena.update.UpdateRequest
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.ComponentsConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.smolang.greenhouse.api.model.Plant
import org.smolang.greenhouse.api.types.PlantMoistureState
import org.springframework.stereotype.Service

@Service
class PlantService (
    private val replConfig: REPLConfig,
    private val triplestoreProperties: TriplestoreProperties,
    private val componentsConfig: ComponentsConfig
) {

    private val tripleStore = triplestoreProperties.tripleStore
    private val prefix = triplestoreProperties.prefix
    private val ttlPrefix = triplestoreProperties.ttlPrefix
    private val repl = replConfig.repl()

    fun createPlant(plant: Plant): Boolean {
        val query = """
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
            PREFIX ast: <$prefix>
            INSERT DATA {
                ast:plant${plant.plantId} a ast:Plant ;
                    ast:plantId "${plant.plantId}" ;
                    ast:familyName "${plant.familyName}" ;
                    ast:moisture "${plant.moisture}"^^xsd:double .
            }
        """.trimIndent()

        val updateRequest: UpdateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = tripleStore + "/update"
        val updateProcessor: UpdateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            componentsConfig.addPlantToCache(plant)
        } catch (e: Exception) {
            return false
        }

        return true
    }

    fun getAllPlants() : List<Plant>? {
        // Return cached plants if available
        val cached = componentsConfig.getPlantCache()
        if (cached.isNotEmpty()) return cached.values.toList()
        val plants =
            """
             SELECT ?plantId ?familyName ?moisture ?healthState ?status ?moistureState WHERE {
                {
                    ?obj a prog:Plant ;
                        prog:Plant_plantId ?plantId ;
                        prog:Plant_familyNameOut ?familyName ;
                        prog:Plant_moistureOut ?moisture .
                    OPTIONAL { ?obj prog:Plant_healthState ?healthState }
                    OPTIONAL { ?obj prog:Plant_statusOut ?status }
                    BIND("unknown" AS ?moistureState)
                }
                UNION {
                    ?obj a prog:ThirstyPlant ;
                        prog:ThirstyPlant_plantId ?plantId ;
                        prog:ThirstyPlant_familyNameOut ?familyName ;
                        prog:ThirstyPlant_moistureOut ?moisture .
                    OPTIONAL { ?obj prog:ThirstyPlant_healthState ?healthState }
                    OPTIONAL { ?obj prog:ThirstyPlant_statusOut ?status }
                    BIND("thirsty" AS ?moistureState)
                }
                UNION {
                    ?obj a prog:MoistPlant ;
                        prog:MoistPlant_plantId ?plantId ;
                        prog:MoistPlant_familyNameOut ?familyName ;
                        prog:MoistPlant_moistureOut ?moisture .
                    OPTIONAL { ?obj prog:MoistPlant_healthState ?healthState }
                    OPTIONAL { ?obj prog:MoistPlant_statusOut ?status }
                    BIND("moist" AS ?moistureState)
                }
             }"""

        val result : ResultSet = repl.interpreter!!.query(plants)!!
        if (!result.hasNext()) {
            return null
        }

        val plantsList = mutableListOf<Plant>()

        while (result.hasNext()) {
            val solution : QuerySolution = result.next()
            val plantId = solution.get("?plantId").asLiteral().toString()
            val familyName = solution.get("?familyName").asLiteral().toString()
            val moisture = if (solution.contains("?moisture")) solution.get("?moisture").asLiteral().toString().split("^^")[0].toDouble() else null
//            val healthState = if (solution.contains("?healthState")) solution.get("?healthState").asLiteral().toString() else null
            val healthState = null
            val status = if (solution.contains("?status")) solution.get("?status").asLiteral().toString() else null
            val retrievedState = solution.get("?moistureState").asLiteral().toString().uppercase()
            val moistureState = when (retrievedState) {
                "thirst" -> PlantMoistureState.THIRSTY
                "moist" -> PlantMoistureState.MOIST
                "overwatered" -> PlantMoistureState.OVERWATERED
                else -> PlantMoistureState.UNKNOWN
            }

            plantsList.add(Plant(plantId, familyName, moisture, healthState, status, moistureState))
        }

        val uniquePlants = plantsList.distinctBy { it.plantId }
        plantsList.clear()
        plantsList.addAll(uniquePlants)

        return plantsList
    }

    fun getPlantByPlantId (plantId: String): Plant? {
        // Return cached plant if present
        componentsConfig.getPlantById(plantId)?.let { return it }
        val query = """
            SELECT DISTINCT ?familyName ?moisture ?healthState ?status ?moistureState WHERE {
                {
                    ?plant a prog:Plant ;
                        prog:Plant_plantId "$plantId" ;
                        prog:Plant_familyName ?familyName ;
                        prog:Plant_moisture ?moisture .
                    OPTIONAL { ?plant prog:Plant_healthState ?healthState }
                    OPTIONAL { ?plant prog:Plant_status ?status }
                    BIND("unknown" AS ?moistureState)
                }
                UNION {
                    ?plant a prog:ThirstyPlant ;
                        prog:ThirstyPlant_plantId "$plantId" ;
                        prog:ThirstyPlant_familyName ?familyName ;
                        prog:ThirstyPlant_moisture ?moisture .
                    OPTIONAL { ?plant prog:ThirstyPlant_healthState ?healthState }
                    OPTIONAL { ?plant prog:ThirstyPlant_status ?status }
                    BIND("thirsty" AS ?moistureState)
                }
                UNION {
                    ?plant a prog:MoistPlant ;
                        prog:MoistPlant_plantId "$plantId" ;
                        prog:MoistPlant_familyName ?familyName ;
                        prog:MoistPlant_moisture ?moisture .
                    OPTIONAL { ?plant prog:MoistPlant_healthState ?healthState }
                    OPTIONAL { ?plant prog:MoistPlant_status ?status }
                    BIND("moist" AS ?moistureState)
                }
            }
        """.trimIndent()

        val result : ResultSet = repl.interpreter!!.query(query)!!
        if (!result.hasNext()) {
            return null
        }

        val solution : QuerySolution = result.next()
        val familyName = solution.get("?familyName").asLiteral().toString()
        val moisture = solution.get("?moisture").asLiteral().toString().split("^^")[0].toDouble()
//        val healthState = if (solution.contains("?healthState")) solution.get("?healthState").asLiteral().toString() else null
        val healthState = null
        val status = if (solution.contains("?status")) solution.get("?status").asLiteral().toString() else null
        val retrievedState = solution.get("?moistureState").asLiteral().toString().uppercase()
        val moistureState = when (retrievedState) {
            "thirst" -> PlantMoistureState.THIRSTY
            "moist" -> PlantMoistureState.MOIST
            "overwatered" -> PlantMoistureState.OVERWATERED
            else -> PlantMoistureState.UNKNOWN
        }

        val plant = Plant(plantId, familyName, moisture, healthState, status, moistureState)
        componentsConfig.addPlantToCache(plant)
        return plant
    }

    fun updatePlant(plant: Plant, newMoisture: Double? = null, newHealthState: String? = null, newStatus: String? = null): Boolean {
        var setClause = ""
        var whereClause = """
            ?plant rdf:type ?t ;
                ast:plantId "${plant.plantId}" .
            ?t rdfs:subClassOf* ast:Plant .
        """

        // Build dynamic DELETE and INSERT clauses based on what's being updated
        var deleteClause = ""
        var insertClause = ""

        if (newMoisture != null) {
            deleteClause += "?plant ast:moisture \"${plant.moisture}\"^^xsd:double .\n"
            insertClause += "?plant ast:moisture \"$newMoisture\"^^xsd:double .\n"
        }

        if (newHealthState != null && plant.healthState != null) {
            deleteClause += "?plant ast:healthState \"${plant.healthState}\" .\n"
            insertClause += "?plant ast:healthState \"$newHealthState\" .\n"
        } else if (newHealthState != null && plant.healthState == null) {
            insertClause += "?plant ast:healthState \"$newHealthState\" .\n"
        }

        if (newStatus != null && plant.status != null) {
            deleteClause += "?plant ast:status \"${plant.status}\" .\n"
            insertClause += "?plant ast:status \"$newStatus\" .\n"
        } else if (newStatus != null && plant.status == null) {
            insertClause += "?plant ast:status \"$newStatus\" .\n"
        }

        if (deleteClause.isEmpty() && insertClause.isEmpty()) {
            return false // Nothing to update
        }

        val query = """
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX ast: <$prefix>
            ${if (deleteClause.isNotEmpty()) "DELETE {\n$deleteClause}" else ""}
            ${if (insertClause.isNotEmpty()) "INSERT {\n$insertClause}" else ""}
            WHERE {
                $whereClause
            }
        """.trimIndent()

        val updateRequest: UpdateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = tripleStore + "/update"
        val updateProcessor: UpdateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            // merge with cache if present
            val cached = componentsConfig.getPlantById(plant.plantId)
            val updatedPlant = if (cached == null) {
                Plant(
                    plant.plantId,
                    plant.familyName,
                    newMoisture ?: plant.moisture,
                    newHealthState ?: plant.healthState,
                    newStatus ?: plant.status,
                    plant.moistureState
                )
            } else {
                Plant(
                    cached.plantId,
                    newHealthState?.let { cached.familyName } ?: cached.familyName,
                    newMoisture ?: cached.moisture,
                    newHealthState ?: cached.healthState,
                    newStatus ?: cached.status,
                    cached.moistureState
                )
            }
            componentsConfig.addPlantToCache(updatedPlant)
        } catch (e: Exception) {
            return false
        }

        return true
    }

    fun deletePlant(plantId: String) : Boolean {
        val query = """
            PREFIX ast: <$prefix>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            DELETE {
                ?plant ?p ?o .
            }
            WHERE {
                ?plant rdf:type ?t ;
                    ast:plantId "$plantId" ;
                    ?p ?o .
                ?t rdfs:subClassOf* ast:Plant .
            }
        """.trimIndent()

        val updateRequest: UpdateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = tripleStore + "/update"
        val updateProcessor: UpdateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            componentsConfig.removePlantFromCache(plantId)
        } catch (e: Exception) {
            return false
        }

        return true
    }
}