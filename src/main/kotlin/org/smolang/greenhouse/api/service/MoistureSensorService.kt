package org.smolang.greenhouse.api.service

import org.apache.jena.query.ResultSet
import org.apache.jena.update.UpdateExecutionFactory
import org.apache.jena.update.UpdateFactory
import org.smolang.greenhouse.api.config.ComponentsConfig
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.slf4j.LoggerFactory
import org.smolang.greenhouse.api.model.MoistureSensor
import org.smolang.greenhouse.api.types.CreateMoistureSensorRequest
import org.smolang.greenhouse.api.types.UpdateMoistureSensorRequest
import org.springframework.stereotype.Service

@Service
class MoistureSensorService(
    private val replConfig: REPLConfig,
    private val triplestoreProperties: TriplestoreProperties,
    private val componentsConfig: ComponentsConfig
) {

    private val logger = LoggerFactory.getLogger(MoistureSensorService::class.java)
    private val tripleStore = triplestoreProperties.tripleStore
    private val prefix = triplestoreProperties.prefix
    private val ttlPrefix = triplestoreProperties.ttlPrefix
    private val repl = replConfig.repl()

    fun createSensor(request: CreateMoistureSensorRequest): MoistureSensor? {
        logger.info("createSensor: creating moisture sensor ${request.sensorId}")
        val query = """
            PREFIX ast: <$prefix>
            INSERT DATA {
                ast:moistureSensor${request.sensorId} a ast:MoistureSensor ;
                    ast:sensorId ${request.sensorId} ;
                    ast:sensorProperty ${request.sensorProperty} .
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            val sensor = MoistureSensor(request.sensorId, request.sensorProperty)
            componentsConfig.addMoistureSensorToCache(sensor)
            logger.info("createSensor: created moisture sensor ${request.sensorId}")
            return sensor
        } catch (e: Exception) {
            logger.error("createSensor: failed to create moisture sensor ${request.sensorId}: ${e.message}", e)
            return null
        }
    }

    fun updateSensor(sensorId: String, request: UpdateMoistureSensorRequest): MoistureSensor? {
        logger.info("updateSensor: updating moisture sensor $sensorId")
        val query = """
            PREFIX ast: <$prefix>
            DELETE {
                ?sensor ast:sensorProperty ?oldProperty .
            }
            INSERT {
                ?sensor ast:sensorProperty ${request.sensorProperty} .
            }
            WHERE {
                ?sensor a ast:MoistureSensor ;
                    ast:sensorId "$sensorId" ;
                    ast:sensorProperty ?oldProperty .
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            // merge with cache if present
            val cached = componentsConfig.getMoistureSensorById(sensorId)
            val sensor = if (cached == null) {
                MoistureSensor(sensorId, request.sensorProperty)
            } else {
                // keep cached moisture value, update property
                MoistureSensor(cached.sensorId, request.sensorProperty ?: cached.sensorProperty, cached.moisture)
            }
            componentsConfig.addMoistureSensorToCache(sensor)
            logger.info("updateSensor: updated moisture sensor $sensorId")
            return sensor
        } catch (e: Exception) {
            logger.error("updateSensor: failed to update moisture sensor $sensorId: ${e.message}", e)
            return null
        }
    }

    fun deleteSensor(sensorId: String): Boolean {
        logger.info("deleteSensor: deleting moisture sensor $sensorId")
        val query = """
            PREFIX ast: <$prefix>
            DELETE WHERE {
                ast:moistureSensor$sensorId ?p ?o .
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        return try {
            updateProcessor.execute()
            componentsConfig.removeMoistureSensorFromCache(sensorId)
            logger.info("deleteSensor: deleted moisture sensor $sensorId")
            true
        } catch (e: Exception) {
            logger.error("deleteSensor: failed to delete moisture sensor $sensorId: ${e.message}", e)
            false
        }
    }

    fun getSensor(sensorId: String): MoistureSensor? {
        logger.debug("getSensor: retrieving moisture sensor $sensorId")
        // Return cached sensor if present
        componentsConfig.getMoistureSensorById(sensorId)?.let { return it }
        val query = """
            SELECT ?sensorId ?sensorProperty ?moisture WHERE {
                ?obj a prog:MoistureSensor ;
                    prog:MoistureSensor_sensorId ?sensorId ;
                    prog:MoistureSensor_sensorProperty ?sensorProperty .
                OPTIONAL { ?obj prog:MoistureSensor_moisture ?moisture }
            }
        """.trimIndent()

        val result: ResultSet = repl.interpreter!!.query(query) ?: return null

        if (!result.hasNext()) {
            return null
        }

        val solution = result.next()
        val sensorId = solution.get("?sensorId").asLiteral().toString()
        val sensorProperty = solution.get("?sensorProperty").asLiteral().toString()
        val moisture = if (solution.contains("?moisture")) {
            solution.get("?moisture").asLiteral().toString().split("^^")[0].toDouble()
        } else null
        val sensor = MoistureSensor(sensorId, sensorProperty, moisture)
        componentsConfig.addMoistureSensorToCache(sensor)
        logger.debug("getSensor: retrieved moisture sensor $sensorId")
        return sensor
    }

    fun getAllSensors(): List<MoistureSensor> {
        logger.debug("getAllSensors: retrieving all moisture sensors")
        // Return cached sensors if available
        val cached = componentsConfig.getMoistureSensorCache()
        if (cached.isNotEmpty()) return cached.values.toList()
        val query = """
            SELECT ?sensorId ?sensorProperty ?moisture WHERE {
                ?obj a prog:MoistureSensor ;
                    prog:MoistureSensor_sensorId ?sensorId ;
                    prog:MoistureSensor_sensorProperty ?sensorProperty .
                OPTIONAL { ?obj prog:MoistureSensor_moisture ?moisture }
            }
        """.trimIndent()

        val result: ResultSet = repl.interpreter!!.query(query) ?: return emptyList()

        val sensors = mutableListOf<MoistureSensor>()
        while (result.hasNext()) {
            val solution = result.next()
            val sensorId = solution.get("?sensorId").asLiteral().toString()
            val sensorProperty = solution.get("?sensorProperty").asLiteral().toString()
            val moisture = if (solution.contains("?moisture")) {
                solution.get("?moisture").asLiteral().toString().split("^^")[0].toDouble()
            } else null
            sensors.add(MoistureSensor(sensorId, sensorProperty, moisture))
        }
        logger.debug("getAllSensors: retrieved ${sensors.size} moisture sensors")
        return sensors
    }
}
