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
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun getWaterBucketsByGreenHouseId(greenhouseId: String): List<WaterBucket>? {
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
            return null
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

    fun getAllWaterBuckets(): List<WaterBucket>? {
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
            return null
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

    fun getWaterBucketById(bucketId: String): WaterBucket? {
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
            return null
        }

        val solution = result.next()
        val waterLevel = solution.get("?waterLevel").asLiteral().toString().split("^^")[0].toDouble()

        val bucket = WaterBucket(bucketId, waterLevel)
        componentsConfig.addWaterBucketToCache(bucket)
        return bucket
    }

    fun updateWaterBucket(bucketId: String, newWaterLevel: Double): WaterBucket? {
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
            return bucket
        } catch (e: Exception) {
            return null
        }
    }

    fun deleteWaterBucket(bucketId: String): Boolean {
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
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
