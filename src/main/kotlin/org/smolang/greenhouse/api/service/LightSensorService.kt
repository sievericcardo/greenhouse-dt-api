package org.smolang.greenhouse.api.service

import org.apache.jena.query.ResultSet
import org.apache.jena.update.UpdateExecutionFactory
import org.apache.jena.update.UpdateFactory
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.smolang.greenhouse.api.model.LightSensor
import org.springframework.stereotype.Service

@Service
class LightSensorService (
    private val replConfig: REPLConfig,
    private val triplestoreProperties: TriplestoreProperties
) {

    private val tripleStore = triplestoreProperties.tripleStore
    private val prefix = triplestoreProperties.prefix
    private val ttlPrefix = triplestoreProperties.ttlPrefix
    private val repl = replConfig.repl()

    fun createSensor(request: CreateLightSensorRequest): LightSensor? {
        val query = """
            PREFIX : <$prefix>
            INSERT DATA {
                ast:lightSensor${request.sensorId} a :LightSensor ;
                    ast:sensorId ${request.sensorId} ;
                    ast:sensorProperty ${request.sensorProperty} .
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            return LightSensor(sensor.sensorId)
        } catch (e: Exception) {
            return null
        }
    }

    fun updateSensor(sensorId: String, request: UpdateLightSensorRequest): LightSensor? {
        val query = """
            PREFIX : <$prefix>
            DELETE {
                ast:lightSensor${sensorId} ?p ?o .
            }
            INSERT {
                ast:lightSensor${sensorId} a :LightSensor ;
                    ast:sensorId ${sensorId} ;
                    ast:sensorProperty ${request.sensorProperty} .
            }
            WHERE {
                ast:lightSensor${sensorId} ?p ?o .
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            return LightSensor(sensor.sensorId)
        } catch (e: Exception) {
            return null
        }
    }

    fun deleteSensor(sensorId: String): Boolean {
        val query = """
            PREFIX : <$prefix>
            DELETE {
                ast:lightSensor$sensorId ?p ?o .
            }
            WHERE {
                ast:lightSensor$sensorId ?p ?o .
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

    fun getSensor(sensorId: String): LightSensor? {
        val query = """
            SELECT ?sensorId ?sensorProperty ?lightLevel WHERE {
                ?obj a prog:LightSensor ;
                    prog:LightSensor_sensorId ?sensorId ;
                    prog:LightSensor_sensorProperty ?sensorProperty ;
                    prog:LightSensor_lightLevel ?lightLevel .
            }
        """.trimIndent()

        val result: ResultSet = repl.interpreter!!.query(query) ?: return null

        if (!result.hasNext()) {
            return null
        }

        val solution = result.next()
        val retrievedSensorId = solution.get("?sensorId").asLiteral().toString()
        val sensorProperty = solution.get("?sensorProperty").asLiteral().toString()
        val lightLevel = if (solution.contains("?lightLevel")) {
            solution.get("?lightLevel").asLiteral().toString().split("^^")[0].toDouble()
        } else null
        return LightSensor(retrievedSensorId, sensorProperty, lightLevel)
    }

    fun getAllSensors(): List<LightSensor> {
        val query = """
            SELECT ?sensorId ?sensorProperty ?lightLevel WHERE {
                ?obj a prog:LightSensor ;
                    prog:LightSensor_sensorId ?sensorId ;
                    prog:LightSensor_sensorProperty ?sensorProperty ;
                    prog:LightSensor_lightLevel ?lightLevel .
            }
        """.trimIndent()

        val result: ResultSet = repl.interpreter!!.query(query) ?: return emptyList()

        val sensors = mutableListOf<LightSensor>()
        while (result.hasNext()) {
            val solution = result.next()
            val sensorId = solution.get("?sensorId").asLiteral().toString()
            val sensorProperty = solution.get("?sensorProperty").asLiteral().toString()
            val lightLevel = if (solution.contains("?lightLevel")) {
                solution.get("?lightLevel").asLiteral().toString().split("^^")[0].toDouble()
            } else null
            sensors.add(LightSensor(sensorId, sensorProperty, lightLevel))
        }
        return sensors
    }
}
