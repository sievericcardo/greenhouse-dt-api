package org.smolang.greenhouse.api.service

import org.apache.jena.query.ResultSet
import org.apache.jena.update.UpdateExecutionFactory
import org.apache.jena.update.UpdateFactory
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.smolang.greenhouse.api.model.LightSwitch
import org.smolang.greenhouse.api.types.CreateLightSwitchRequest
import org.springframework.stereotype.Service

@Service
class LightSwitchService (
    private val replConfig: REPLConfig,
    private val triplestoreProperties: TriplestoreProperties
) {

    private val tripleStore = triplestoreProperties.tripleStore
    private val prefix = triplestoreProperties.prefix
    private val ttlPrefix = triplestoreProperties.ttlPrefix
    private val repl = replConfig.repl()

    fun createLightSwitch(request: CreateLightSwitchRequest): LightSwitch? {
        val query = """
            PREFIX ast: <$prefix>
            INSERT DATA {
                ast:lightSwitch${request.actuatorId} a :LightSwitch ;
                    ast:actuatorId ${request.actuatorId} .
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            return LightSwitch(request.actuatorId)
        } catch (e: Exception) {
            return null
        }
    }

    fun deleteLightSwitch(lightSwitchId: String): Boolean {
        val query = """
            PREFIX ast: <$prefix>
            DELETE WHERE {
                ast:lightSwitch$lightSwitchId ?p ?o .
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

    fun getLightSwitch(lightSwitchId: String): LightSwitch? {
        val query = """
            SELECT ?lightSwitchId ?lightIntensity WHERE {
                ?obj a prog:LightSwitch ;
                    prog:LightSwitch_actuatorId ?lightSwitchId ;
                    prog:LightSwitch_lightIntensity ?lightIntensity .
            }
        """.trimIndent()

        val result: ResultSet = repl.interpreter!!.query(query) ?: return null

        if (!result.hasNext()) {
            return null
        }

        val solution = result.next()
        val lightSwitchId = solution.get("?lightSwitchId").asLiteral().toString()
        val lightIntensity = if (solution.contains("?lightIntensity")) {
            solution.get("?lightIntensity").asLiteral().toString().split("^^")[0].toDouble()
        } else null
        return LightSwitch(lightSwitchId, lightIntensity)
    }

    fun getAllLightSwitches(): List<LightSwitch> {
        val query = """
            SELECT ?lightSwitchId ?lightIntensity WHERE {
                ?obj a prog:LightSwitch ;
                    prog:LightSwitch_actuatorId ?lightSwitchId ;
                    prog:LightSwitch_lightIntensity ?lightIntensity .
            }
        """.trimIndent()

        val result: ResultSet = repl.interpreter!!.query(query) ?: return emptyList()

        if (!result.hasNext()) {
            return emptyList()
        }

        val switches = mutableListOf<LightSwitch>()
        while (result.hasNext()) {
            val solution = result.next()
            val lightSwitchId = solution.get("?lightSwitchId").asLiteral().toString()
            val lightIntensity = if (solution.contains("?lightIntensity")) {
                solution.get("?lightIntensity").asLiteral().toString().split("^^")[0].toDouble()
            } else null
            switches.add(LightSwitch(lightSwitchId, lightIntensity))
        }
        return switches
    }
}