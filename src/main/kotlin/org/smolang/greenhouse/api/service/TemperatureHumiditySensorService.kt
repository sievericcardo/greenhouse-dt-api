package org.smolang.greenhouse.api.service

import org.apache.jena.query.ResultSet
import org.apache.jena.update.UpdateExecutionFactory
import org.apache.jena.update.UpdateFactory
import org.smolang.greenhouse.api.config.ComponentsConfig
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.slf4j.LoggerFactory
import org.smolang.greenhouse.api.model.TemperatureHumiditySensor
import org.smolang.greenhouse.api.types.CreateTemperatureHumiditySensorRequest
import org.smolang.greenhouse.api.types.UpdateTemperatureHumiditySensorRequest
import org.springframework.stereotype.Service

@Service
class TemperatureHumiditySensorService(
    private val replConfig: REPLConfig,
    private val triplestoreProperties: TriplestoreProperties,
    private val componentsConfig: ComponentsConfig
) {

    private val logger = LoggerFactory.getLogger(TemperatureHumiditySensorService::class.java)

    private val tripleStore = triplestoreProperties.tripleStore
    private val prefix = triplestoreProperties.prefix
    private val ttlPrefix = triplestoreProperties.ttlPrefix
    private val repl = replConfig.repl()

    fun createSensor(request: CreateTemperatureHumiditySensorRequest): TemperatureHumiditySensor? {
        logger.info("createSensor: creating temp/humidity sensor ${request.sensorId}")
        val query = """
            PREFIX ast: <$prefix>
            INSERT DATA {
                ast:humidityTemperatureSensor${request.sensorId} a :TemperatureHumiditySensor ;
                    ast:sensorId ${request.sensorId} ;
                    ast:sensorProperty ${request.sensorProperty} .
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            val sensor = TemperatureHumiditySensor(request.sensorId)
            componentsConfig.addTemperatureHumiditySensorToCache(sensor)
            logger.info("createSensor: created temp/humidity sensor ${request.sensorId}")
            return sensor
        } catch (e: Exception) {
            logger.error("createSensor: failed to create temp/humidity sensor ${request.sensorId}: ${e.message}", e)
            return null
        }
    }

    fun updateSensor(sensorId: String, request: UpdateTemperatureHumiditySensorRequest): TemperatureHumiditySensor? {
        logger.info("updateSensor: updating temp/humidity sensor $sensorId")
        val query = """
            PREFIX ast: <$prefix>

            DELETE {
                ast:humidityTemperatureSensor${sensorId} ?p ?o .
            }
            INSERT {
                ast:humidityTemperatureSensor${sensorId} a ast:TemperatureHumiditySensor ;
                    ast:sensorId ${sensorId} ;
                    ast:sensorProperty ${request.sensorProperty} .
            }
            WHERE {
                ast:humidityTemperatureSensor${sensorId} ?p ?o .
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            // merge with cache if present
            val cached = componentsConfig.getTemperatureHumiditySensorById(sensorId)
            val sensor = if (cached == null) {
                TemperatureHumiditySensor(sensorId, request.sensorProperty, null, null)
            } else {
                TemperatureHumiditySensor(
                    cached.sensorId,
                    request.sensorProperty ?: cached.sensorProperty,
                    cached.temperature,
                    cached.humidity
                )
            }
            componentsConfig.addTemperatureHumiditySensorToCache(sensor)
            logger.info("updateSensor: updated temp/humidity sensor $sensorId")
            return sensor
        } catch (e: Exception) {
            logger.error("updateSensor: failed to update temp/humidity sensor $sensorId: ${e.message}", e)
            return null
        }
    }

    fun deleteSensor(sensorId: String): Boolean {
        logger.info("deleteSensor: deleting temp/humidity sensor $sensorId")
        val query = """
            PREFIX ast: <$prefix>

            DELETE {
                ast:humidityTemperatureSensor$sensorId ?p ?o .
            }
            WHERE {
                ast:humidityTemperatureSensor$sensorId ?p ?o .
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            componentsConfig.removeTemperatureHumiditySensorFromCache(sensorId)
            logger.info("deleteSensor: deleted temp/humidity sensor $sensorId")
            return true
        } catch (e: Exception) {
            logger.error("deleteSensor: failed to delete temp/humidity sensor $sensorId: ${e.message}", e)
            return false
        }
    }

    fun getSensor(sensorId: String): TemperatureHumiditySensor? {
        logger.debug("getSensor: retrieving temp/humidity sensor $sensorId")
        // Return cached sensor if present
        componentsConfig.getTemperatureHumiditySensorById(sensorId)?.let { return it }
        val query = """
            SELECT ?sensorId ?sensorProperty ?temperature ?humidity WHERE {
                ?obj a prog:TemperatureHumiditySensor ;
                    prog:TemperatureHumiditySensor_sensorId ?sensorId ;
                    prog:TemperatureHumiditySensor_sensorProperty ?sensorProperty .
                OPTIONAL { ?obj prog:TemperatureHumiditySensor_temperature ?temperature }
                OPTIONAL { ?obj prog:TemperatureHumiditySensor_humidity ?humidity }
            }
        """.trimIndent()

        val result: ResultSet? = repl.interpreter!!.query(query)
        if (result == null || !result.hasNext()) {
            return null
        }

        val solution = result.next()
        val retrievedSensorId = solution.get("?sensorId").asLiteral().toString()
        val sensorProperty = solution.get("?sensorProperty").asLiteral().toString()
        val temperature = if (solution.contains("?temperature")) {
            solution.get("?temperature").asLiteral().toString().split("^^")[0].toDouble()
        } else null
        val humidity = if (solution.contains("?humidity")) {
            solution.get("?humidity").asLiteral().toString().split("^^")[0].toDouble()
        } else null
        val sensor = TemperatureHumiditySensor(retrievedSensorId, sensorProperty, temperature, humidity)
        componentsConfig.addTemperatureHumiditySensorToCache(sensor)
        logger.debug("getSensor: retrieved temp/humidity sensor $sensorId")
        return sensor
    }

    fun getAllSensors(): List<TemperatureHumiditySensor> {
        logger.debug("getAllSensors: retrieving all temp/humidity sensors")
        // Return cached sensors if available
        val cached = componentsConfig.getTemperatureHumiditySensorCache()
        if (cached.isNotEmpty()) return cached.values.toList()
        val query = """
            SELECT ?sensorId ?sensorProperty ?temperature ?humidity WHERE {
                ?obj a prog:TemperatureHumiditySensor ;
                    prog:TemperatureHumiditySensor_sensorId ?sensorId ;
                    prog:TemperatureHumiditySensor_sensorProperty ?sensorProperty .
                OPTIONAL { ?obj prog:TemperatureHumiditySensor_temperature ?temperature }
                OPTIONAL { ?obj prog:TemperatureHumiditySensor_humidity ?humidity }
            }
        """.trimIndent()

        val result: ResultSet? = repl.interpreter!!.query(query)
        if (result == null || !result.hasNext()) {
            return emptyList()
        }

        val sensors = mutableListOf<TemperatureHumiditySensor>()
        while (result.hasNext()) {
            val solution = result.next()
            val sensorId = solution.get("?sensorId").asLiteral().toString()
            val sensorProperty = solution.get("?sensorProperty").asLiteral().toString()
            val temperature = if (solution.contains("?temperature")) {
                solution.get("?temperature").asLiteral().toString().split("^^")[0].toDouble()
            } else null
            val humidity = if (solution.contains("?humidity")) {
                solution.get("?humidity").asLiteral().toString().split("^^")[0].toDouble()
            } else null
            sensors.add(TemperatureHumiditySensor(sensorId, sensorProperty, temperature, humidity))
        }
        logger.debug("getAllSensors: retrieved ${sensors.size} temp/humidity sensors")
        return sensors
    }
}