package org.smolang.greenhouse.api.service

import org.apache.jena.query.ResultSet
import org.apache.jena.update.UpdateExecutionFactory
import org.apache.jena.update.UpdateFactory
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.smolang.greenhouse.api.model.Pot
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

    fun createPot (potId: String, shelfFloor: String, potPosition: String, pumpId: String, plantId: String): Boolean {
        val query = """
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
            PREFIX ast: <$prefix>
            
            INSERT DATA {
                ast:pot$potId a ast:Pot ;
                    ast:potId "$potId" ;
                    ast:shelfFloor "$shelfFloor" ;
                    ast:potPosition "$potPosition" ;
                    ast:pumpId ?pump ;
                    ast:plantId ?plant .
            }
            WHERE {
                ?pump a ast:Pump ;
                    ast:pumpId "$pumpId" .
                ?plant a ast:Plant ;
                    ast:plantId "$plantId" .
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
             SELECT DISTINCT ?potId ?shelfFloor ?potPosition ?pumpId ?plantId WHERE {
                 ?obj a prog:Pot ;
                        prog:Pot_id ?potId ;
                        prog:Pot_shelfFloor ?shelfFloor ;
                        prog:Pot_potPosition ?potPosition ;
                        prog:Pot_pumpId ?pumpId ;
                        prog:Pot_plantId ?plantId .
             }"""

        val result: ResultSet? = repl.interpreter!!.query(potsQuery)
        val potsList = mutableListOf<Pot>()

        if (result == null || !result.hasNext()) {
            return null
        }

        while (result.hasNext()) {
            val solution = result.next()
            val potId = solution.get("?potId").asLiteral().toString()
            val shelfFloor = solution.get("?shelfFloor").asLiteral().toString()
            val potPosition = solution.get("?potPosition").asLiteral().toString()
            val pumpId = solution.get("?pumpId").asLiteral().toString()
            val plantId = solution.get("?plantId").asLiteral().toString()

            potsList.add(Pot(potId, shelfFloor, potPosition, pumpId, plantId))
        }

        return potsList
    }

    fun getPotByPotId (id: String) : Pot? {
        val potsQuery =
            """
             SELECT DISTINCT ?potId ?shelfFloor ?potPosition ?pumpId ?plantId WHERE {
                 ?obj a prog:Pot ;
                        prog:Pot_id "$id" ;
                        prog:Pot_shelfFloor ?shelfFloor ;
                        prog:Pot_potPosition ?potPosition ;
                        prog:Pot_pumpId ?pumpId ;
                        prog:Pot_plantId ?plantId .
             }"""

        val result: ResultSet? = repl.interpreter!!.query(potsQuery)
        val potsList = mutableListOf<Pot>()

        if (result == null || !result.hasNext()) {
            return null
        }

        while (result.hasNext()) {
            val solution = result.next()
            val potId = solution.get("?potId").asLiteral().toString()
            val shelfFloor = solution.get("?shelfFloor").asLiteral().toString()
            val potPosition = solution.get("?potPosition").asLiteral().toString()
            val pumpId = solution.get("?pumpId").asLiteral().toString()
            val plantId = solution.get("?plantId").asLiteral().toString()

            potsList.add(Pot(potId, shelfFloor, potPosition, pumpId, plantId))
        }

        return potsList.firstOrNull()
    }

    fun deletePot(potId: String) : Boolean {
        val query = """
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
            PREFIX ast: <$prefix>
            
            DELETE {
                ast:pot$potId a ast:Pot ;
                    ast:potId "$potId" ;
                    ast:shelfFloor ?shelfFloor ;
                    ast:potPosition ?potPosition ;
                    ast:pumpId ?pumpId ;
                    ast:plantId ?plantId .
            }
            WHERE {
                ast:pot$potId a ast:Pot ;
                    ast:potId "$potId" ;
                    ast:shelfFloor ?shelfFloor ;
                    ast:potPosition ?potPosition ;
                    ast:pumpId ?pumpId ;
                    ast:plantId ?plantId .
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