package org.smolang.greenhouse.api.service

import org.apache.jena.query.QuerySolution
import org.apache.jena.query.ResultSet
import org.apache.jena.update.UpdateExecutionFactory
import org.apache.jena.update.UpdateFactory
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.smolang.greenhouse.api.model.*
import org.smolang.greenhouse.api.types.PumpState
import org.springframework.stereotype.Service

@Service
class PotService (
    private val replConfig: REPLConfig,
    private val triplestoreProperties: TriplestoreProperties
) {

    private val tripleStore = triplestoreProperties.tripleStore
    private val prefix = triplestoreProperties.prefix
    private val ttlPrefix = triplestoreProperties.ttlPrefix
    private val repl = replConfig.repl()

    fun createPot (potId: String): Boolean {
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
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun getPots() : List<Pot>? {
        val potsQuery =
            """
             SELECT DISTINCT ?potId ?pumpId ?moistureSensorId ?moistureValue ?nutrientSensorId ?nutrientValue 
                             ?pumpChannel ?modelName ?lifeTime ?temperature ?pumpStatus
                             ?plant1Id ?plant2Id ?plant3Id ?plant4Id ?plant5Id WHERE {
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
                     ?moistureSensorObj prog:MoistureSensor_sensorId ?moistureSensorId ;
                                       prog:MoistureSensor_moisture ?moistureValue .
                 }
                 
                 OPTIONAL {
                     ?potObj prog:Pot_nutrientSensor ?nutrientSensorObj .
                     ?nutrientSensorObj prog:NutrientSensor_sensorId ?nutrientSensorId ;
                                       prog:NutrientSensor_nutrient ?nutrientValue .
                 }
                 
                 # Handle up to 5 plants per pot (adjust as needed)
                 OPTIONAL { ?potObj prog:Pot_plants ?plant1 . ?plant1 prog:Plant_plantId ?plant1Id }
                 OPTIONAL { ?potObj prog:Pot_plants ?plant2 . ?plant2 prog:Plant_plantId ?plant2Id . FILTER(?plant2 != ?plant1) }
                 OPTIONAL { ?potObj prog:Pot_plants ?plant3 . ?plant3 prog:Plant_plantId ?plant3Id . FILTER(?plant3 != ?plant1 && ?plant3 != ?plant2) }
                 OPTIONAL { ?potObj prog:Pot_plants ?plant4 . ?plant4 prog:Plant_plantId ?plant4Id . FILTER(?plant4 != ?plant1 && ?plant4 != ?plant2 && ?plant4 != ?plant3) }
                 OPTIONAL { ?potObj prog:Pot_plants ?plant5 . ?plant5 prog:Plant_plantId ?plant5Id . FILTER(?plant5 != ?plant1 && ?plant5 != ?plant2 && ?plant5 != ?plant3 && ?plant5 != ?plant4) }
             }"""

        val result: ResultSet? = repl.interpreter!!.query(potsQuery)
        val potsList = mutableListOf<Pot>()

        if (result == null || !result.hasNext()) {
            return null
        }

        while (result.hasNext()) {
            val solution = result.next()
            val potId = solution.get("?potId").asLiteral().toString()
            
            // Build plants list
            val plants = mutableListOf<Plant>()
            for (i in 1..5) {
                val plantIdVar = "?plant${i}Id"
                if (solution.contains(plantIdVar)) {
                    val plantId = solution.get(plantIdVar).asLiteral().toString()
                    // Get full plant details using separate query
                    getPlantById(plantId)?.let { plants.add(it) }
                }
            }
            
            // Build moisture sensor if present
            val moistureSensor = if (solution.contains("?moistureSensorId")) {
                val sensorId = solution.get("?moistureSensorId").asLiteral().toString()
                val moisture = solution.get("?moistureValue").asLiteral().toString().split("^^")[0].toDouble()
                MoistureSensor(sensorId, moisture)
            } else null
            
            // Build nutrient sensor if present
            val nutrientSensor = if (solution.contains("?nutrientSensorId")) {
                val sensorId = solution.get("?nutrientSensorId").asLiteral().toString()
                val nutrient = solution.get("?nutrientValue").asLiteral().toString().split("^^")[0].toDouble()
                NutrientSensor(sensorId, nutrient)
            } else null
            
            // Build pump
            val pump = if (solution.contains("?pumpId")) {
                val pumpId = solution.get("?pumpId").asLiteral().toString()
                val pumpChannel = solution.get("?pumpChannel").asLiteral().toString().toInt()
                val modelName = if (solution.contains("?modelName")) solution.get("?modelName").asLiteral().toString() else null
                val lifeTime = if (solution.contains("?lifeTime")) solution.get("?lifeTime").asLiteral().toString().toInt() else null
                val temperature = if (solution.contains("?temperature")) solution.get("?temperature").asLiteral().toString().split("^^")[0].toDouble() else null
                val pumpStatus = if (solution.contains("?pumpStatus")) {
                    try {
                        PumpState.valueOf(solution.get("?pumpStatus").asLiteral().toString())
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                } else null
                
                Pump(pumpId, pumpChannel, modelName, lifeTime, temperature, pumpStatus)
            } else {
                // Default pump if none found
                Pump("default_pump_$potId", 0, null, null, null, null)
            }

            potsList.add(Pot(potId, plants, moistureSensor, nutrientSensor, pump))
        }

        return potsList
    }

    private fun getPlantById(plantId: String): Plant? {
        val plantQuery = """
            SELECT DISTINCT ?moisture ?healthState ?status WHERE {
                ?plantObj a prog:Plant ;
                    prog:Plant_plantId "$plantId" ;
                    prog:Plant_moisture ?moisture .
                OPTIONAL { ?plantObj prog:Plant_healthState ?healthState }
                OPTIONAL { ?plantObj prog:Plant_status ?status }
            }
        """
        
        val result = repl.interpreter!!.query(plantQuery)
        if (result == null || !result.hasNext()) {
            return null
        }
        
        val solution = result.next()
        val moisture = solution.get("?moisture").asLiteral().toString().split("^^")[0].toDouble()
        val healthState = if (solution.contains("?healthState")) solution.get("?healthState").asLiteral().toString() else null
        val status = if (solution.contains("?status")) solution.get("?status").asLiteral().toString() else null
        
        return Plant(plantId, moisture, healthState, status)
    }

    fun getPotByPotId (id: String) : Pot? {
        val potsQuery =
            """
             SELECT DISTINCT ?potId ?pumpId ?moistureSensorId ?moistureValue ?nutrientSensorId ?nutrientValue 
                             ?pumpChannel ?modelName ?lifeTime ?temperature ?pumpStatus
                             ?plant1Id ?plant2Id ?plant3Id ?plant4Id ?plant5Id WHERE {
                 ?potObj a prog:Pot ;
                        prog:Pot_potId "$id" .
                 
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
                     ?moistureSensorObj prog:MoistureSensor_sensorId ?moistureSensorId ;
                                       prog:MoistureSensor_moisture ?moistureValue .
                 }
                 
                 OPTIONAL {
                     ?potObj prog:Pot_nutrientSensor ?nutrientSensorObj .
                     ?nutrientSensorObj prog:NutrientSensor_sensorId ?nutrientSensorId ;
                                       prog:NutrientSensor_nutrient ?nutrientValue .
                 }
                 
                 # Handle up to 5 plants per pot
                 OPTIONAL { ?potObj prog:Pot_plants ?plant1 . ?plant1 prog:Plant_plantId ?plant1Id }
                 OPTIONAL { ?potObj prog:Pot_plants ?plant2 . ?plant2 prog:Plant_plantId ?plant2Id . FILTER(?plant2 != ?plant1) }
                 OPTIONAL { ?potObj prog:Pot_plants ?plant3 . ?plant3 prog:Plant_plantId ?plant3Id . FILTER(?plant3 != ?plant1 && ?plant3 != ?plant2) }
                 OPTIONAL { ?potObj prog:Pot_plants ?plant4 . ?plant4 prog:Plant_plantId ?plant4Id . FILTER(?plant4 != ?plant1 && ?plant4 != ?plant2 && ?plant4 != ?plant3) }
                 OPTIONAL { ?potObj prog:Pot_plants ?plant5 . ?plant5 prog:Plant_plantId ?plant5Id . FILTER(?plant5 != ?plant1 && ?plant5 != ?plant2 && ?plant5 != ?plant3 && ?plant5 != ?plant4) }
             }"""

        val result: ResultSet? = repl.interpreter!!.query(potsQuery)

        if (result == null || !result.hasNext()) {
            return null
        }

        val solution = result.next()
        val potId = solution.get("?potId").asLiteral().toString()
        
        // Build plants list
        val plants = mutableListOf<Plant>()
        for (i in 1..5) {
            val plantIdVar = "?plant${i}Id"
            if (solution.contains(plantIdVar)) {
                val plantId = solution.get(plantIdVar).asLiteral().toString()
                getPlantById(plantId)?.let { plants.add(it) }
            }
        }
        
        // Build moisture sensor if present
        val moistureSensor = if (solution.contains("?moistureSensorId")) {
            val sensorId = solution.get("?moistureSensorId").asLiteral().toString()
            val moisture = solution.get("?moistureValue").asLiteral().toString().split("^^")[0].toDouble()
            MoistureSensor(sensorId, moisture)
        } else null
        
        // Build nutrient sensor if present
        val nutrientSensor = if (solution.contains("?nutrientSensorId")) {
            val sensorId = solution.get("?nutrientSensorId").asLiteral().toString()
            val nutrient = solution.get("?nutrientValue").asLiteral().toString().split("^^")[0].toDouble()
            NutrientSensor(sensorId, nutrient)
        } else null
        
        // Build pump
        val pump = if (solution.contains("?pumpId")) {
            val pumpId = solution.get("?pumpId").asLiteral().toString()
            val pumpChannel = solution.get("?pumpChannel").asLiteral().toString().toInt()
            val modelName = if (solution.contains("?modelName")) solution.get("?modelName").asLiteral().toString() else null
            val lifeTime = if (solution.contains("?lifeTime")) solution.get("?lifeTime").asLiteral().toString().toInt() else null
            val temperature = if (solution.contains("?temperature")) solution.get("?temperature").asLiteral().toString().split("^^")[0].toDouble() else null
            val pumpStatus = if (solution.contains("?pumpStatus")) {
                try {
                    PumpState.valueOf(solution.get("?pumpStatus").asLiteral().toString())
                } catch (e: IllegalArgumentException) {
                    null
                }
            } else null
            
            Pump(pumpId, pumpChannel, modelName, lifeTime, temperature, pumpStatus)
        } else {
            // Default pump if none found
            Pump("default_pump_$potId", 0, null, null, null, null)
        }

        return Pot(potId, plants, moistureSensor, nutrientSensor, pump)
    }

    fun deletePot(potId: String) : Boolean {
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
            return true
        } catch (e: Exception) {
            return false
        }
    }
}