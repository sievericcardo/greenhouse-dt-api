package org.smolang.greenhouse.api.service

import org.apache.jena.query.ResultSet
import org.apache.jena.update.UpdateExecutionFactory
import org.apache.jena.update.UpdateFactory
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.slf4j.LoggerFactory
import org.smolang.greenhouse.api.model.*
import org.springframework.stereotype.Service

@Service
class GreenHouseService(
    private val replConfig: REPLConfig,
    private val triplestoreProperties: TriplestoreProperties
) {

    private val logger = LoggerFactory.getLogger(GreenHouseService::class.java)
    private val tripleStore = triplestoreProperties.tripleStore
    private val prefix = triplestoreProperties.prefix
    private val ttlPrefix = triplestoreProperties.ttlPrefix
    private val repl = replConfig.repl()

    fun createGreenHouse(greenhouseId: String): Boolean {
        val query = """
            PREFIX ast: <$prefix>
            
            INSERT DATA {
                ast:greenhouse$greenhouseId a ast:GreenHouse ;
                    ast:greenhouseId "$greenhouseId" .
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

    fun getAllGreenHouses(): List<GreenHouse>? {
        logger.debug("getAllGreenHouses: retrieving all greenhouses")
        val greenHousesQuery = """
            SELECT DISTINCT ?greenhouseId ?lightSensorId ?lightSensorProperty ?lightIntensity ?tempHumSensorId ?tempHumSensorProperty ?temperature ?humidity WHERE {
                ?greenhouseObj a prog:GreenHouse ;
                    prog:GreenHouse_greenhouseId ?greenhouseId .
                
                OPTIONAL {
                    ?greenhouseObj prog:GreenHouse_lightSensor ?lightSensorObj .
                    ?lightSensorObj prog:LightSensor_sensorId ?lightSensorId ;
                                   prog:LightSensor_sensorProperty ?lightSensorProperty .
                    OPTIONAL { ?lightSensorObj prog:LightSensor_lightIntensity ?lightIntensity }
                }
                
                OPTIONAL {
                    ?greenhouseObj prog:GreenHouse_temperatureHumiditySensor ?tempHumSensorObj .
                    ?tempHumSensorObj prog:TemperatureHumiditySensor_sensorId ?tempHumSensorId ;
                                     prog:TemperatureHumiditySensor_sensorProperty ?tempHumSensorProperty .
                    OPTIONAL { ?tempHumSensorObj prog:TemperatureHumiditySensor_temperature ?temperature }
                    OPTIONAL { ?tempHumSensorObj prog:TemperatureHumiditySensor_humidity ?humidity }
                }
            }
        """

        val result: ResultSet? = repl.interpreter!!.query(greenHousesQuery)
        if (result == null || !result.hasNext()) {
            logger.debug("getAllGreenHouses: no greenhouses found")
            return null
        }

        val greenHousesList = mutableListOf<GreenHouse>()

        while (result.hasNext()) {
            val solution = result.next()
            val greenhouseId = solution.get("?greenhouseId").asLiteral().toString()

            // Get sections for this greenhouse - simplified for now
            val sections = getSectionsByGreenHouseId(greenhouseId)

            // Get water buckets for this greenhouse - simplified for now
            val waterBuckets = getWaterBucketsByGreenHouseId(greenhouseId)

            // Build light sensor if present
            val lightSensor = if (solution.contains("?lightSensorId")) {
                val sensorId = solution.get("?lightSensorId").asLiteral().toString()
                val sensorProperty = solution.get("?lightSensorProperty").asLiteral().toString()
                val lightIntensity = if (solution.contains("?lightIntensity")) {
                    solution.get("?lightIntensity").asLiteral().toString().split("^^")[0].toDouble()
                } else null
                LightSensor(sensorId, sensorProperty, lightIntensity)
            } else {
                // Default light sensor
                LightSensor("default_light_$greenhouseId", "", null)
            }

            // Build temperature humidity sensor if present
            val temperatureHumiditySensor = if (solution.contains("?tempHumSensorId")) {
                val sensorId = solution.get("?tempHumSensorId").asLiteral().toString()
                val sensorProperty = solution.get("?tempHumSensorProperty").asLiteral().toString()
                val temperature = if (solution.contains("?temperature")) {
                    solution.get("?temperature").asLiteral().toString().split("^^")[0].toDouble()
                } else null
                val humidity = if (solution.contains("?humidity")) {
                    solution.get("?humidity").asLiteral().toString().split("^^")[0].toDouble()
                } else null
                TemperatureHumiditySensor(sensorId, sensorProperty, temperature, humidity)
            } else {
                // Default temperature humidity sensor
                TemperatureHumiditySensor("default_temphum_$greenhouseId", "", null, null)
            }

            greenHousesList.add(
                GreenHouse(
                    greenhouseId,
                    sections,
                    waterBuckets,
                    lightSensor,
                    temperatureHumiditySensor
                )
            )
        }

        logger.debug("getAllGreenHouses: retrieved ${greenHousesList.size} greenhouses")
        return greenHousesList
    }

    fun getGreenHouseById(greenhouseId: String): GreenHouse? {
        val greenHouseQuery = """
            SELECT DISTINCT ?lightSensorId ?lightSensorProperty ?lightIntensity ?tempHumSensorId ?tempHumSensorProperty ?temperature ?humidity WHERE {
                ?greenhouseObj a prog:GreenHouse ;
                    prog:GreenHouse_greenhouseId "$greenhouseId" .
                
                OPTIONAL {
                    ?greenhouseObj prog:GreenHouse_lightSensor ?lightSensorObj .
                    ?lightSensorObj prog:LightSensor_sensorId ?lightSensorId ;
                                   prog:LightSensor_sensorProperty ?lightSensorProperty .
                    OPTIONAL { ?lightSensorObj prog:LightSensor_lightIntensity ?lightIntensity }
                }
                
                OPTIONAL {
                    ?greenhouseObj prog:GreenHouse_temperatureHumiditySensor ?tempHumSensorObj .
                    ?tempHumSensorObj prog:TemperatureHumiditySensor_sensorId ?tempHumSensorId ;
                                     prog:TemperatureHumiditySensor_sensorProperty ?tempHumSensorProperty .
                    OPTIONAL { ?tempHumSensorObj prog:TemperatureHumiditySensor_temperature ?temperature }
                    OPTIONAL { ?tempHumSensorObj prog:TemperatureHumiditySensor_humidity ?humidity }
                }
            }
        """

        val result = repl.interpreter!!.query(greenHouseQuery)
        if (result == null || !result.hasNext()) {
            logger.debug("getGreenHouseById: greenhouse $greenhouseId not found")
            return null
        }

        val solution = result.next()

        // Get sections for this greenhouse - simplified for now
        val sections = getSectionsByGreenHouseId(greenhouseId)

        // Get water buckets for this greenhouse - simplified for now  
        val waterBuckets = getWaterBucketsByGreenHouseId(greenhouseId)

        // Build light sensor if present
        val lightSensor = if (solution.contains("?lightSensorId")) {
            val sensorId = solution.get("?lightSensorId").asLiteral().toString()
            val sensorProperty = solution.get("?lightSensorProperty").asLiteral().toString()
            val lightIntensity = if (solution.contains("?lightIntensity")) {
                solution.get("?lightIntensity").asLiteral().toString().split("^^")[0].toDouble()
            } else null
            LightSensor(sensorId, sensorProperty, lightIntensity)
        } else {
            // Default light sensor
            LightSensor("default_light_$greenhouseId", "", null)
        }

        // Build temperature humidity sensor if present
        val temperatureHumiditySensor = if (solution.contains("?tempHumSensorId")) {
            val sensorId = solution.get("?tempHumSensorId").asLiteral().toString()
            val sensorProperty = solution.get("?tempHumSensorProperty").asLiteral().toString()
            val temperature = if (solution.contains("?temperature")) {
                solution.get("?temperature").asLiteral().toString().split("^^")[0].toDouble()
            } else null
            val humidity = if (solution.contains("?humidity")) {
                solution.get("?humidity").asLiteral().toString().split("^^")[0].toDouble()
            } else null
            TemperatureHumiditySensor(sensorId, sensorProperty, temperature, humidity)
        } else {
            // Default temperature humidity sensor
            TemperatureHumiditySensor("default_temphum_$greenhouseId", "", null, null)
        }

        logger.debug("getGreenHouseById: retrieved greenhouse $greenhouseId")
        return GreenHouse(greenhouseId, sections, waterBuckets, lightSensor, temperatureHumiditySensor)
    }

    private fun getSectionsByGreenHouseId(greenhouseId: String): List<Section> {
        // Simplified implementation - return empty list for now
        // This should be implemented properly based on the SPARQL structure
        return emptyList()
    }

    private fun getWaterBucketsByGreenHouseId(greenhouseId: String): List<WaterBucket> {
        val waterBucketsQuery = """
            SELECT DISTINCT ?bucketId ?waterLevel WHERE {
                ?greenhouseObj a prog:GreenHouse ;
                    prog:GreenHouse_greenhouseId "$greenhouseId" ;
                    prog:GreenHouse_waterBuckets ?bucketObj .
                ?bucketObj prog:WaterBucket_bucketId ?bucketId ;
                          prog:WaterBucket_waterLevel ?waterLevel .
            }
        """

        val result: ResultSet? = repl.interpreter!!.query(waterBucketsQuery)
        if (result == null || !result.hasNext()) {
            return emptyList()
        }

        val waterBucketsList = mutableListOf<WaterBucket>()

        while (result.hasNext()) {
            val solution = result.next()
            val bucketId = solution.get("?bucketId").asLiteral().toString()
            val waterLevel = solution.get("?waterLevel").asLiteral().toString().split("^^")[0].toDouble()

            waterBucketsList.add(WaterBucket(bucketId, waterLevel))
        }

        return waterBucketsList
    }

    fun deleteGreenHouse(greenhouseId: String): Boolean {
        logger.info("deleteGreenHouse: deleting greenhouse $greenhouseId")
        val query = """
            PREFIX ast: <$prefix>
            
            DELETE {
                ast:greenhouse$greenhouseId ?p ?o .
                ?s ?p2 ast:greenhouse$greenhouseId .
            }
            WHERE {
                { ast:greenhouse$greenhouseId ?p ?o . }
                UNION
                { ?s ?p2 ast:greenhouse$greenhouseId . }
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            logger.info("deleteGreenHouse: deleted greenhouse $greenhouseId")
            return true
        } catch (e: Exception) {
            logger.error("deleteGreenHouse: failed to delete greenhouse $greenhouseId: ${e.message}", e)
            return false
        }
    }
}
