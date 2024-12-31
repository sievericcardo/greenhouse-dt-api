package org.smolang.greenhouse.api.service

import org.apache.jena.query.QuerySolution
import org.apache.jena.query.ResultSet
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.smolang.greenhouse.api.controller.PumpState
import org.smolang.greenhouse.api.model.Pump
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
}