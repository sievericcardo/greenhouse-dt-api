package org.smolang.greenhouse.api.service

import org.apache.jena.query.ResultSet
import org.apache.jena.update.UpdateExecutionFactory
import org.apache.jena.update.UpdateFactory
import org.smolang.greenhouse.api.config.ComponentsConfig
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.slf4j.LoggerFactory
import org.smolang.greenhouse.api.model.NutrientSensor
import org.smolang.greenhouse.api.types.CreateNutrientSensorRequest
import org.smolang.greenhouse.api.types.UpdateNutrientSensorRequest
import org.springframework.stereotype.Service

@Service
class NutrientSensorService(
    private val replConfig: REPLConfig,
    private val triplestoreProperties: TriplestoreProperties,
    private val componentsConfig: ComponentsConfig
) {

    private val logger = LoggerFactory.getLogger(NutrientSensorService::class.java)

    private val tripleStore = triplestoreProperties.tripleStore
    private val prefix = triplestoreProperties.prefix
    private val ttlPrefix = triplestoreProperties.ttlPrefix
    private val repl = replConfig.repl()

    fun createSensor(request: CreateNutrientSensorRequest): NutrientSensor? {
        logger.info("createSensor: creating nutrient sensor ${request.sensorId}")
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
            val sensor = NutrientSensor(request.sensorId, request.sensorProperty)
            componentsConfig.addNutrientSensorToCache(sensor)
            logger.info("createSensor: created nutrient sensor ${request.sensorId}")
            return sensor
        } catch (e: Exception) {
            logger.error("createSensor: failed to create nutrient sensor ${request.sensorId}: ${e.message}", e)
            return null
        }
    }

    fun updateSensor(sensorId: String, request: UpdateNutrientSensorRequest): NutrientSensor? {
        logger.info("updateSensor: updating nutrient sensor $sensorId")
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
            // merge with cache if present
            val cached = componentsConfig.getNutrientSensorById(sensorId)
            val sensor = if (cached == null) {
                NutrientSensor(sensorId, request.sensorProperty)
            } else {
                NutrientSensor(cached.sensorId, request.sensorProperty ?: cached.sensorProperty, cached.nutrient)
            }
            componentsConfig.addNutrientSensorToCache(sensor)
            logger.info("updateSensor: updated nutrient sensor $sensorId")
            return sensor
        } catch (e: Exception) {
            logger.error("updateSensor: failed to update nutrient sensor $sensorId: ${e.message}", e)
            return null
        }
    }

    fun deleteSensor(sensorId: String): Boolean {
        logger.info("deleteSensor: deleting nutrient sensor $sensorId")
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
            componentsConfig.removeNutrientSensorFromCache(sensorId)
            logger.info("deleteSensor: deleted nutrient sensor $sensorId")
            true
        } catch (e: Exception) {
            logger.error("deleteSensor: failed to delete nutrient sensor $sensorId: ${e.message}", e)
            false
        }
    }

    fun getSensor(sensorId: String): NutrientSensor? {
        logger.debug("getSensor: retrieving nutrient sensor $sensorId")
        // Return cached sensor if present
        componentsConfig.getNutrientSensorById(sensorId)?.let { return it }
        // Restrict query to the requested sensorId to avoid returning an unrelated sensor
        val query = """
            SELECT ?sensorId ?sensorProperty ?nutrient WHERE {
                ?obj a prog:NutrientSensor ;
                    prog:NutrientSensor_sensorId "$sensorId" ;
                    prog:NutrientSensor_sensorProperty ?sensorProperty .
                OPTIONAL { ?obj prog:NutrientSensor_nutrient ?nutrient }
                BIND("$sensorId" AS ?sensorId)
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
        val sensor = NutrientSensor(sensorId, sensorProperty, nutrient)
        componentsConfig.addNutrientSensorToCache(sensor)
        logger.debug("getSensor: retrieved nutrient sensor $sensorId")
        return sensor
    }

    fun getAllSensors(): List<NutrientSensor> {
        logger.debug("getAllSensors: retrieving all nutrient sensors")
        // Return cached sensors if available
        val cached = componentsConfig.getNutrientSensorCache()
        if (cached.isNotEmpty()) return cached.values.toList()
        val query = """
            SELECT ?sensorId ?sensorProperty ?nutrient WHERE {
                ?obj a prog:NutrientSensor ;
                    prog:NutrientSensor_sensorId ?sensorId ;
                    prog:NutrientSensor_sensorProperty ?sensorProperty .
                OPTIONAL { ?obj prog:NutrientSensor_nutrient ?nutrient }
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
            val sensor = NutrientSensor(sensorId, sensorProperty, nutrient)
            // populate cache
            componentsConfig.addNutrientSensorToCache(sensor)
            sensors.add(sensor)
        }
        logger.debug("getAllSensors: retrieved ${sensors.size} nutrient sensors")
        return sensors
    }
}