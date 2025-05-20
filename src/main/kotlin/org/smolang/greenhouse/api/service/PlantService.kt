package org.smolang.greenhouse.api.service

import org.apache.jena.query.QuerySolution
import org.apache.jena.query.ResultSet
import org.apache.jena.update.UpdateExecutionFactory
import org.apache.jena.update.UpdateFactory
import org.apache.jena.update.UpdateProcessor
import org.apache.jena.update.UpdateRequest
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.smolang.greenhouse.api.model.Plant
import org.springframework.stereotype.Service

@Service
class PlantService (
    private val replConfig: REPLConfig,
    private val triplestoreProperties: TriplestoreProperties
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
                $prefix:plant${plant.plantId} a ast:Plant ;
                    $prefix:idealMoisture ${plant.idealMoisture} ;
                    $prefix:status ${plant.status} .
            }
        """.trimIndent()

        val updateRequest: UpdateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = tripleStore + "/update"
        val updateProcessor: UpdateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
        } catch (e: Exception) {
            return false
        }

        return true
    }

    fun getAllPlants() : List<Plant>? {
        val plants =
            """
             SELECT DISTINCT ?plantId ?idealMoisture ?moisture ?healthState ?status WHERE {
                ?obj a prog:Plant ;
                    prog:Plant_plantId ?plantId ;
                    prog:Plant_idealMoisture ?idealMoisture ;
                    prog:Plant_moisture ?moisture ;
                    prog:Plant_healthState ?healthState ;
                    prog:Plant_status ?status .
             }"""

        val result : ResultSet = repl.interpreter!!.query(plants)!!
        if (!result.hasNext()) {
            return null
        }

        val plantsList = mutableListOf<Plant>()

        while (result.hasNext()) {
            val solution : QuerySolution = result.next()
            val plantId = solution.get("?plantId").asLiteral().toString()
            val idealMoisture = solution.get("?idealMoisture").asLiteral().toString().split("^^")[0].toDouble()
            val moisture = solution.get("?moisture").asLiteral().toString().split("^^")[0].toDouble()
            val healthState = solution.get("?healthState").asLiteral().toString()
            val status = solution.get("?status").asLiteral().toString()

            plantsList.add(Plant(plantId, idealMoisture, moisture, healthState, status))
        }

        return plantsList
    }

    fun getPlantByPlantId (plantId: String): Plant? {
        val query = """
            SELECT DISTINCT ?idealMoisture ?moisture ?healthState ?status WHERE {
                ?plant a prog:Plant ;
                    prog:Plant_plantId  "$plantId" ;
                    prog:Plant_idealMoisture ?idealMoisture ;
                    prog:Plant_moisture ?moisture ;
                    prog:Plant_healthState ?healthState ;
                    prog:Plant_status ?status .
            }
        """.trimIndent()

        val result : ResultSet = repl.interpreter!!.query(query)!!
        if (!result.hasNext()) {
            return null
        }

        val solution : QuerySolution = result.next()
        val idealMoisture = solution.get("?idealMoisture").asLiteral().toString().split("^^")[0].toDouble()
        val moisture = solution.get("?moisture").asLiteral().toString().split("^^")[0].toDouble()
        val healthState = solution.get("?healthState").asLiteral().toString()
        val status = solution.get("?status").asLiteral().toString()

        return Plant(plantId, idealMoisture, moisture, healthState, status)
    }

    fun updatePlant(plant: Plant, newIdealMoisture: Double, newStatus: String): Boolean {
        val query = """
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
            PREFIX ast: <$prefix>
            DELETE {
                ?plant ast:idealMoisture ${plant.idealMoisture} .
                ?plant ast:status ${plant.status} .
            }
            INSERT {
                ?plant ast:idealMoisture $newIdealMoisture .
                ?plant ast:status $newStatus .
            }
            WHERE {
                ?plant a ast:Plant ;
                    ast:plantId "${plant.plantId}" ;
                    ast:idealMoisture ${plant.idealMoisture} ;
                    ast:moisture ${plant.moisture} ;
                    ast:healthState ${plant.healthState} ;
                    ast:status ${plant.status} .
            }
        """.trimIndent()

        val updateRequest: UpdateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = tripleStore + "/update"
        val updateProcessor: UpdateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
        } catch (e: Exception) {
            return false
        }

        return true
    }

    fun deletePlant(plantId: String) : Boolean {
        val query = """
            PREFIX ast: <$prefix>
            DELETE {
                ?plant a ast:Plant ;
                    ast:plantId "$plantId" ;
                    ast:idealMoisture ?idealMoisture ;
                    ast:moisture ?moisture ;
                    ast:healthState ?healthState .
            }
            WHERE {
                ?plant a ast:Plant ;
                    ast:plantId "$plantId" .
            }
        """.trimIndent()

        val updateRequest: UpdateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = tripleStore + "/update"
        val updateProcessor: UpdateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
        } catch (e: Exception) {
            return false
        }

        return true
    }
}