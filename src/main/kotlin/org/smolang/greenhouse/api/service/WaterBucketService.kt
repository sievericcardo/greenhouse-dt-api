package org.smolang.greenhouse.api.service

import org.apache.jena.query.ResultSet
import org.apache.jena.update.UpdateExecutionFactory
import org.apache.jena.update.UpdateFactory
import org.smolang.greenhouse.api.config.ComponentsConfig
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.slf4j.LoggerFactory
import org.smolang.greenhouse.api.model.WaterBucket
import org.springframework.stereotype.Service

@Service
class WaterBucketService(
    private val replConfig: REPLConfig,
    private val triplestoreProperties: TriplestoreProperties,
    private val componentsConfig: ComponentsConfig
) {

    private val logger = LoggerFactory.getLogger(WaterBucketService::class.java)
    private val tripleStore = triplestoreProperties.tripleStore
    private val prefix = triplestoreProperties.prefix
    private val ttlPrefix = triplestoreProperties.ttlPrefix
    private val repl = replConfig.repl()

    fun createWaterBucket(bucketId: String): Boolean {
        logger.info("createWaterBucket: creating water bucket $bucketId")
        val query = """
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
            PREFIX ast: <$prefix>
            
            INSERT DATA {
                ast:bucket$bucketId a ast:WaterBucket ;
                    ast:bucketId "$bucketId" .
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            componentsConfig.addWaterBucketToCache(WaterBucket(bucketId, null))
            logger.info("createWaterBucket: created water bucket $bucketId")
            return true
        } catch (e: Exception) {
            logger.error("createWaterBucket: failed to create water bucket $bucketId: ${e.message}", e)
            return false
        }
    }

    fun getWaterBucketsByGreenHouseId(greenhouseId: String): List<WaterBucket>? {
        logger.debug("getWaterBucketsByGreenHouseId: retrieving buckets for greenhouse $greenhouseId")
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
            logger.debug("getWaterBucketsByGreenHouseId: no buckets found for greenhouse $greenhouseId")
            return null
        }

        val waterBucketsList = mutableListOf<WaterBucket>()

        while (result.hasNext()) {
            val solution = result.next()
            val bucketId = solution.get("?bucketId").asLiteral().toString()
            val waterLevel = solution.get("?waterLevel").asLiteral().toString().split("^^")[0].toDouble()

            waterBucketsList.add(WaterBucket(bucketId, waterLevel))
        }

        logger.debug("getWaterBucketsByGreenHouseId: retrieved ${waterBucketsList.size} buckets for greenhouse $greenhouseId")
        return waterBucketsList
    }

    fun getAllWaterBuckets(): List<WaterBucket>? {
        logger.debug("getAllWaterBuckets: retrieving all water buckets")
        // Return cached water buckets if available
        val cached = componentsConfig.getWaterBucketCache()
        if (cached.isNotEmpty()) return cached.values.toList()
        val waterBucketsQuery = """
            SELECT DISTINCT ?bucketId ?waterLevel WHERE {
                ?bucketObj a prog:WaterBucket ;
                    prog:WaterBucket_bucketId ?bucketId ;
                    prog:WaterBucket_waterLevel ?waterLevel .
            }
        """

        val result: ResultSet? = repl.interpreter!!.query(waterBucketsQuery)
        if (result == null || !result.hasNext()) {
            logger.debug("getAllWaterBuckets: no water buckets found")
            return null
        }

        val waterBucketsList = mutableListOf<WaterBucket>()

        while (result.hasNext()) {
            val solution = result.next()
            val bucketId = solution.get("?bucketId").asLiteral().toString()
            val waterLevel = solution.get("?waterLevel").asLiteral().toString().split("^^")[0].toDouble()

            waterBucketsList.add(WaterBucket(bucketId, waterLevel))
        }

        logger.debug("getAllWaterBuckets: retrieved ${waterBucketsList.size} water buckets")
        return waterBucketsList
    }

    fun getWaterBucketById(bucketId: String): WaterBucket? {
        logger.debug("getWaterBucketById: retrieving bucket $bucketId")
        // Try cache first
        componentsConfig.getWaterBucketById(bucketId)?.let { return it }
        val waterBucketQuery = """
            SELECT DISTINCT ?waterLevel WHERE {
                ?bucketObj a prog:WaterBucket ;
                    prog:WaterBucket_bucketId "$bucketId" ;
                    prog:WaterBucket_waterLevel ?waterLevel .
            }
        """

        val result = repl.interpreter!!.query(waterBucketQuery)
        if (result == null || !result.hasNext()) {
            logger.debug("getWaterBucketById: bucket $bucketId not found")
            return null
        }

        val solution = result.next()
        val waterLevel = solution.get("?waterLevel").asLiteral().toString().split("^^")[0].toDouble()

        val bucket = WaterBucket(bucketId, waterLevel)
        componentsConfig.addWaterBucketToCache(bucket)
        logger.debug("getWaterBucketById: retrieved bucket $bucketId")
        return bucket
    }

    fun updateWaterBucket(bucketId: String, newWaterLevel: Double): WaterBucket? {
        logger.info("updateWaterBucket: updating bucket $bucketId -> new level $newWaterLevel")
        val query = """
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
            PREFIX ast: <$prefix>
            
            DELETE {
                ?bucket ast:waterLevel ?oldLevel .
            }
            INSERT {
                ?bucket ast:waterLevel "$newWaterLevel"^^xsd:double .
            }
            WHERE {
                ?bucket a ast:WaterBucket ;
                    ast:bucketId "$bucketId" ;
                    ast:waterLevel ?oldLevel .
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            val bucket = WaterBucket(bucketId, newWaterLevel)
            componentsConfig.addWaterBucketToCache(bucket)
            logger.info("updateWaterBucket: updated bucket $bucketId")
            return bucket
        } catch (e: Exception) {
            logger.error("updateWaterBucket: failed to update bucket $bucketId: ${e.message}", e)
            return null
        }
    }

    fun deleteWaterBucket(bucketId: String): Boolean {
        logger.info("deleteWaterBucket: deleting bucket $bucketId")
        val query = """
            PREFIX ast: <$prefix>
            
            DELETE {
                ast:bucket$bucketId ?p ?o .
                ?s ?p2 ast:bucket$bucketId .
            }
            WHERE {
                { ast:bucket$bucketId ?p ?o . }
                UNION
                { ?s ?p2 ast:bucket$bucketId . }
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            componentsConfig.removeWaterBucketFromCache(bucketId)
            logger.info("deleteWaterBucket: deleted bucket $bucketId")
            return true
        } catch (e: Exception) {
            logger.error("deleteWaterBucket: failed to delete bucket $bucketId: ${e.message}", e)
            return false
        }
    }
}
