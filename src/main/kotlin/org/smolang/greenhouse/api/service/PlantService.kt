package org.smolang.greenhouse.api.service

import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.smolang.greenhouse.api.model.Plant
import org.smolang.greenhouse.api.types.PlantState
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

    fun getHealthyPlants(): List<Plant>? {
        val plantList = mutableListOf<Plant>()
        val plants =
            """
             SELECT DISTINCT ?plantId ?idealMoisture ?moisture ?healthState WHERE {
                ?obj a prog:Plant ;
                    prog:Plant_plantId ?plantId ;
                    prog:Plant_idealMoisture ?idealMoisture ;
                    prog:Plant_moisture ?moisture ;
                    prog:Plant_healthState ?state .
                ?state a prog:GoodHealthState ;
                    prog:GoodHealthState_name ?healthState .
             }""".trimIndent()

        val result = repl.interpreter!!.query(plants) ?: return null

        while (result.hasNext()) {
            val solution = result.next()
            val plantId = solution.get("?plantId").asLiteral().toString()
            val idealMoisture = solution.get("?idealMoisture").asLiteral().toString().split("^^")[0].toDouble()
            val moisture = solution.get("?moisture").asLiteral().toString().split("^^")[0].toDouble()
            val healthState = PlantState.Healthy

            plantList.add(Plant(plantId, idealMoisture, moisture, healthState))
        }

        return plantList
    }

    fun getUnhealthyPlants(): List<Plant>? {
        val plantList = mutableListOf<Plant>()
        val plants =
            """
             SELECT DISTINCT ?plantId ?idealMoisture ?moisture ?healthState WHERE {
                ?obj a prog:Plant ;
                    prog:Plant_plantId ?plantId ;
                    prog:Plant_idealMoisture ?idealMoisture ;
                    prog:Plant_moisture ?moisture ;
                    prog:Plant_healthState ?state .
                ?state a prog:BadHealthState ;
                    prog:BadHealthState_name ?healthState .
             }""".trimIndent()

        val result = repl.interpreter!!.query(plants) ?: return null

        while (result.hasNext()) {
            val solution = result.next()
            val plantId = solution.get("?plantId").asLiteral().toString()
            val idealMoisture = solution.get("?idealMoisture").asLiteral().toString().split("^^")[0].toDouble()
            val moisture = solution.get("?moisture").asLiteral().toString().split("^^")[0].toDouble()
            val healthState = PlantState.Unhealthy

            plantList.add(Plant(plantId, idealMoisture, moisture, healthState))
        }

        return plantList
    }

    fun getDeadPlants(): List<Plant>? {
        val plantList = mutableListOf<Plant>()
        val plants =
            """
             SELECT DISTINCT ?plantId ?idealMoisture ?moisture ?healthState WHERE {
                ?obj a prog:Plant ;
                    prog:Plant_plantId ?plantId ;
                    prog:Plant_idealMoisture ?idealMoisture ;
                    prog:Plant_moisture ?moisture ;
                    prog:Plant_healthState ?state .
                ?state a prog:DeadHealthState ;
                    prog:DeadHealthState_name ?healthState .
             }""".trimIndent()

        val result = repl.interpreter!!.query(plants) ?: return null

        while (result.hasNext()) {
            val solution = result.next()
            val plantId = solution.get("?plantId").asLiteral().toString()
            val idealMoisture = solution.get("?idealMoisture").asLiteral().toString().split("^^")[0].toDouble()
            val moisture = solution.get("?moisture").asLiteral().toString().split("^^")[0].toDouble()
            val healthState = PlantState.Dead

            plantList.add(Plant(plantId, idealMoisture, moisture, healthState))
        }

        return plantList
    }
}