package org.smolang.greenhouse.api.service

import org.apache.jena.query.ResultSet
import org.apache.jena.update.UpdateExecutionFactory
import org.apache.jena.update.UpdateFactory
import org.smolang.greenhouse.api.config.ComponentsConfig
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.slf4j.LoggerFactory
import org.smolang.greenhouse.api.model.LightSensor
import org.smolang.greenhouse.api.types.CreateLightSensorRequest
import org.smolang.greenhouse.api.types.UpdateLightSensorRequest
import org.springframework.stereotype.Service

@Service
class LightSensorService(
    private val replConfig: REPLConfig,
    private val triplestoreProperties: TriplestoreProperties,
    private val componentsConfig: ComponentsConfig
) {

    private val logger = LoggerFactory.getLogger(LightSensorService::class.java)
    private val tripleStore = triplestoreProperties.tripleStore
    private val prefix = triplestoreProperties.prefix
    private val ttlPrefix = triplestoreProperties.ttlPrefix
    private val repl = replConfig.repl()

    fun createSensor(request: CreateLightSensorRequest): LightSensor? {
        val query = """
            PREFIX ast: <$prefix>
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
            val sensor = LightSensor(request.sensorId)
            componentsConfig.addLightSensorToCache(sensor)
            return sensor
        } catch (e: Exception) {
            return null
        }
    }

    fun updateSensor(sensorId: String, request: UpdateLightSensorRequest): LightSensor? {
        val query = """
            PREFIX ast: <$prefix>
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
            // merge with cache if present
            val cached = componentsConfig.getLightSensorById(sensorId)
            val sensor = if (cached == null) {
                LightSensor(sensorId)
            } else {
                // create new instance merging known fields (sensorProperty is not used on LightSensor class currently)
                LightSensor(sensorId)
            }
            componentsConfig.addLightSensorToCache(sensor)
            return sensor
        } catch (e: Exception) {
            return null
        }
    }

    fun deleteSensor(sensorId: String): Boolean {
        val query = """
            PREFIX ast: <$prefix>
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
            componentsConfig.removeLightSensorFromCache(sensorId)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun getSensor(sensorId: String): LightSensor? {
        // Return cached sensor if available
        componentsConfig.getLightSensorById(sensorId)?.let { return it }
        val query = """
            SELECT ?sensorId ?sensorProperty ?lightLevel WHERE {
                ?obj a prog:LightSensor ;
                    prog:LightSensor_sensorId ?sensorId ;
                    prog:LightSensor_sensorProperty ?sensorProperty .
                OPTIONAL { ?obj prog:LightSensor_lightLevel ?lightLevel }
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
        val sensor = LightSensor(retrievedSensorId, sensorProperty, lightLevel)
        componentsConfig.addLightSensorToCache(sensor)
        return sensor
    }

    fun getAllSensors(): List<LightSensor> {
        // Return cached sensors if available
        val cached = componentsConfig.getLightSensorCache()
        if (cached.isNotEmpty()) return cached.values.toList()
        val query = """
            SELECT ?sensorId ?sensorProperty ?lightLevel WHERE {
                ?obj a prog:LightSensor ;
                    prog:LightSensor_sensorId ?sensorId ;
                    prog:LightSensor_sensorProperty ?sensorProperty .
                OPTIONAL { ?obj prog:LightSensor_lightLevel ?lightLevel }
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
