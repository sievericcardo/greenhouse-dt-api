package org.smolang.greenhouse.api.service

import org.apache.jena.query.ResultSet
import org.apache.jena.update.UpdateExecutionFactory
import org.apache.jena.update.UpdateFactory
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.smolang.greenhouse.api.model.NutrientSensor
import org.smolang.greenhouse.api.types.CreateNutrientSensorRequest
import org.smolang.greenhouse.api.types.UpdateNutrientSensorRequest
import org.springframework.stereotype.Service

@Service
class NutrientSensorService (
    private val replConfig: REPLConfig,
    private val triplestoreProperties: TriplestoreProperties
) {

    private val tripleStore = triplestoreProperties.tripleStore
    private val prefix = triplestoreProperties.prefix
    private val ttlPrefix = triplestoreProperties.ttlPrefix
    private val repl = replConfig.repl()

    fun createSensor(request: CreateNutrientSensorRequest): NutrientSensor? {
        val query = """
            PREFIX ast: <$prefix>
            INSERT DATA {
                ast:nutrientSensor${request.sensorId} a :NutrientSensor ;
                    ast:sensorId ${request.sensorId} ;
                    ast:sensorProperty ${request.sensorProperty} .
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            return NutrientSensor(request.sensorId, request.sensorProperty)
        } catch (e: Exception) {
            return null
        }
    }

    fun updateSensor(sensorId: String, request: UpdateNutrientSensorRequest): NutrientSensor? {
        val query = """
            PREFIX ast: <$prefix>

            DELETE {
                ?sensor ast:sensorProperty ?oldProperty .
            }
            INSERT {
                ?sensor ast:sensorProperty ${request.sensorProperty} .
            }
            WHERE {
                ?sensor a ast:NutrientSensor ;
                    ast:sensorId "$sensorId" ;
                    ast:sensorProperty ?oldProperty .
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            return NutrientSensor(sensorId, request.sensorProperty)
        } catch (e: Exception) {
            return null
        }
    }

    fun deleteSensor(sensorId: String): Boolean {
        val query = """
            PREFIX ast: <$prefix>
            DELETE WHERE {
                ?sensor a ast:NutrientSensor ;
                    ast:sensorId "$sensorId" .
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        return try {
            updateProcessor.execute()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getSensor(sensorId: String): NutrientSensor? {
        val query = """
            SELECT ?sensorId ?sensorProperty ?nutrient WHERE {
                ?obj a prog:NutrientSensor ;
                    prog:NutrientSensor_sensorId ?sensorId ;
                    prog:NutrientSensor_sensorProperty ?sensorProperty ;
                    prog:NutrientSensor_nutrient ?nutrient .
            }
        """.trimIndent()

        val result: ResultSet = repl.interpreter!!.query(query) ?: return null

        if (!result.hasNext()) {
            return null
        }

        val solution = result.next()
        val sensorId = solution.get("?sensorId").asLiteral().toString()
        val sensorProperty = solution.get("?sensorProperty").asLiteral().toString()
        val nutrient = if (solution.contains("?nutrient")) {
            solution.get("?nutrient").asLiteral().toString().split("^^")[0].toDouble()
        } else null
        return NutrientSensor(sensorId, sensorProperty, nutrient)
    }

    fun getAllSensors(): List<NutrientSensor> {
        val query = """
            SELECT ?sensorId ?sensorProperty ?nutrient WHERE {
                ?obj a prog:NutrientSensor ;
                    prog:NutrientSensor_sensorId ?sensorId ;
                    prog:NutrientSensor_sensorProperty ?sensorProperty ;
                    prog:NutrientSensor_nutrient ?nutrient .
            }
        """.trimIndent()

        val result: ResultSet = repl.interpreter!!.query(query) ?: return emptyList()

        if (!result.hasNext()) {
            return emptyList()
        }

        val sensors = mutableListOf<NutrientSensor>()
        while (result.hasNext()) {
            val solution = result.next()
            val sensorId = solution.get("?sensorId").asLiteral().toString()
            val sensorProperty = solution.get("?sensorProperty").asLiteral().toString()
            val nutrient = if (solution.contains("?nutrient")) {
                solution.get("?nutrient").asLiteral().toString().split("^^")[0].toDouble()
            } else null
            sensors.add(NutrientSensor(sensorId, sensorProperty, nutrient))
        }
        return sensors
    }
}