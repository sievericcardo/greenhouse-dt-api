package org.smolang.greenhouse.api.service

import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.runtime.REPL
import no.uio.microobject.type.STRINGTYPE
import org.apache.jena.query.QuerySolution
import org.apache.jena.query.ResultSet
import org.apache.jena.update.UpdateExecutionFactory
import org.apache.jena.update.UpdateFactory
import org.apache.jena.update.UpdateProcessor
import org.apache.jena.update.UpdateRequest
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.smolang.greenhouse.api.controller.PumpState
import org.smolang.greenhouse.api.model.Pump
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class PumpService (
    private val replConfig: REPLConfig,
    private val triplestoreProperties: TriplestoreProperties
) {

    private val tripleStore = triplestoreProperties.tripleStore
    private val prefix = triplestoreProperties.prefix
    private val ttlPrefix = triplestoreProperties.ttlPrefix
    private val repl = replConfig.repl()

    fun getOperatingPumps() : List<Pump> {
        val pumpsList = mutableListOf<Pump>()
        val pumps =
            """
             SELECT DISTINCT ?pumpGpioPin ?pumpId ?modelName ?pumpLifeTime ?temperature WHERE {
                ?obj a prog:OperatingPump ;
                    prog:OperatingPump_pumpGpioPin ?pumpGpioPin ;
                    prog:OperatingPump_pumpId ?pumpId ;
                    prog:OperatingPump_modelNameOut ?modelName ;
                    prog:OperatingPump_pumpLifeTimeOut ?pumpLifeTime ;
                    prog:OperatingPump_temperatureOut ?temperature .
             }""".trimIndent()

        val result: ResultSet = repl.interpreter!!.query(pumps)!!

        while (result.hasNext()) {
            val solution: QuerySolution = result.next()
            val pumpGpioPin = solution.get("?pumpGpioPin").asLiteral().toString().split("^^")[0].toInt()
            val pumpId = solution.get("?pumpId").asLiteral().toString()
            val modelName = solution.get("?modelName").asLiteral().toString()
            val lifeTime = solution.get("?pumpLifeTime").asLiteral().toString().split("^^")[0].toInt()
            val temperature = solution.get("?temperature").asLiteral().toString().split("^^")[0].toDouble()

            pumpsList.add(Pump(pumpGpioPin, pumpId, modelName, lifeTime, temperature, PumpState.Operational))
        }

        return pumpsList
    }

    fun getMaintenancePumps() : List<Pump> {
        val pumpsList = mutableListOf<Pump>()
        val pumps =
            """
             SELECT DISTINCT ?pumpGpioPin ?pumpId ?modelName ?pumpLifeTime ?temperature WHERE {
                ?obj a prog:MaintenancePump ;
                    prog:MaintenancePump_pumpGpioPin ?pumpGpioPin ;
                    prog:MaintenancePump_pumpId ?pumpId ;
                    prog:MaintenancePump_modelNameOut ?modelName ;
                    prog:MaintenancePump_pumpLifeTimeOut ?pumpLifeTime ;
                    prog:MaintenancePump_temperatureOut ?temperature .
             }""".trimIndent()

        val result: ResultSet = repl.interpreter!!.query(pumps)!!

        while (result.hasNext()) {
            val solution: QuerySolution = result.next()
            val pumpGpioPin = solution.get("?pumpGpioPin").asLiteral().toString().split("^^")[0].toInt()
            val pumpId = solution.get("?pumpId").asLiteral().toString()
            val modelName = solution.get("?modelName").asLiteral().toString()
            val lifeTime = solution.get("?pumpLifeTime").asLiteral().toString().split("^^")[0].toInt()
            val temperature = solution.get("?temperature").asLiteral().toString().split("^^")[0].toDouble()

            pumpsList.add(Pump(pumpGpioPin, pumpId, modelName, lifeTime, temperature, PumpState.Maintenance))
        }

        return pumpsList
    }

    fun getOverheatingPumps() : List<Pump> {
        val pumpsList = mutableListOf<Pump>()
        val pumps =
            """
             SELECT DISTINCT ?pumpGpioPin ?pumpId ?modelName ?pumpLifeTime ?temperature WHERE {
                ?obj a prog:OverheatingPump ;
                    prog:OverheatingPump_pumpGpioPin ?pumpGpioPin ;
                    prog:OverheatingPump_pumpId ?pumpId ;
                    prog:OverheatingPump_modelNameOut ?modelName ;
                    prog:OverheatingPump_pumpLifeTimeOut ?pumpLifeTime ;
                    prog:OverheatingPump_temperatureOut ?temperature .
             }""".trimIndent()

        val result: ResultSet = repl.interpreter!!.query(pumps)!!

        while (result.hasNext()) {
            val solution: QuerySolution = result.next()
            val pumpGpioPin = solution.get("?pumpGpioPin").asLiteral().toString().split("^^")[0].toInt()
            val pumpId = solution.get("?pumpId").asLiteral().toString()
            val modelName = solution.get("?modelName").asLiteral().toString()
            val lifeTime = solution.get("?pumpLifeTime").asLiteral().toString().split("^^")[0].toInt()
            val temperature = solution.get("?temperature").asLiteral().toString().split("^^")[0].toDouble()

            pumpsList.add(Pump(pumpGpioPin, pumpId, modelName, lifeTime, temperature, PumpState.Overheating))
        }

        return pumpsList
    }

    fun getUnderheatingPumps() : List<Pump> {
        val pumpsList = mutableListOf<Pump>()
        val pumps =
            """
             SELECT DISTINCT ?pumpGpioPin ?pumpId ?modelName ?pumpLifeTime ?temperature WHERE {
                ?obj a prog:UnderheatingPump ;
                    prog:UnderheatingPump_pumpGpioPin ?pumpGpioPin ;
                    prog:UnderheatingPump_pumpId ?pumpId ;
                    prog:UnderheatingPump_modelNameOut ?modelName ;
                    prog:UnderheatingPump_pumpLifeTimeOut ?pumpLifeTime ;
                    prog:UnderheatingPump_temperatureOut ?temperature .
             }""".trimIndent()

        val result: ResultSet = repl.interpreter!!.query(pumps)!!

        while (result.hasNext()) {
            val solution: QuerySolution = result.next()
            val pumpGpioPin = solution.get("?pumpGpioPin").asLiteral().toString().split("^^")[0].toInt()
            val pumpId = solution.get("?pumpId").asLiteral().toString()
            val modelName = solution.get("?modelName").asLiteral().toString()
            val lifeTime = solution.get("?pumpLifeTime").asLiteral().toString().split("^^")[0].toInt()
            val temperature = solution.get("?temperature").asLiteral().toString().split("^^")[0].toDouble()

            pumpsList.add(Pump(pumpGpioPin, pumpId, modelName, lifeTime, temperature, PumpState.Underheating))
        }

        return pumpsList
    }

    fun updatePump(updatedPump: Pump) : Boolean {
        val updateQuery = """
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
            PREFIX ast: <$prefix>
            
            DELETE {
                ?pump ast:temperature ?oldTemperature .
                ?pump ast:pumpLifeTime ?oldLifeTime .
            }
            INSERT {
                ?pump ast:temperature "${updatedPump.temperature}"^^xsd:double .
                ?pump ast:pumpLifeTime ${updatedPump.lifeTime} .
            }
            WHERE {
                ?pump a ast:Pump ;
                    ast:pumpGpioPin ${updatedPump.pumpGpioPin} ;
                    ast:pumpId "${updatedPump.pumpId}" ;
                    ast:temperature ?oldTemperature ;
                    ast:pumpLifeTime ?oldLifeTime .
            }
        """

        val updateRequest: UpdateRequest = UpdateFactory.create(updateQuery)
        val fusekiEndpoint = tripleStore + "/update"
        val updateProcessor: UpdateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
        } catch (e: Exception) {
            return false
        }

        return true
    }
}