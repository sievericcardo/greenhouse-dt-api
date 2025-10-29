package org.smolang.greenhouse.api.service

import org.apache.jena.query.QuerySolution
import org.apache.jena.query.ResultSet
import org.apache.jena.update.UpdateExecutionFactory
import org.apache.jena.update.UpdateFactory
import org.apache.jena.update.UpdateProcessor
import org.apache.jena.update.UpdateRequest
import org.smolang.greenhouse.api.config.ComponentsConfig
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.smolang.greenhouse.api.model.Pump
import org.smolang.greenhouse.api.types.PumpState
import org.springframework.stereotype.Service

@Service
class PumpService(
    private val replConfig: REPLConfig,
    private val triplestoreProperties: TriplestoreProperties,
    private val componentsConfig: ComponentsConfig
) {

    private val tripleStore = triplestoreProperties.tripleStore
    private val prefix = triplestoreProperties.prefix
    private val ttlPrefix = triplestoreProperties.ttlPrefix
    private val repl = replConfig.repl()

    fun createPump(newPump: Pump): Boolean {
        val query = """
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
            PREFIX ast: <$prefix>
            
            INSERT DATA {
                ast:${newPump.actuatorId} a ast:Pump ;
                    ast:actuatorId "${newPump.actuatorId}" ;
                    ast:pumpChannel ${newPump.pumpChannel} .
            }
        """

        val updateRequest: UpdateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = tripleStore + "/update"
        val updateProcessor: UpdateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            componentsConfig.addPumpToCache(newPump)
        } catch (e: Exception) {
            return false
        }

        return true
    }

    fun getAllPumps(): List<Pump>? {
        // Return cached pumps if available
        val cached = componentsConfig.getPumpCache()
        if (cached.isNotEmpty()) return cached.values.toList()
        val pumpsList = mutableListOf<Pump>()
        val pumps =
            """
             SELECT DISTINCT ?actuatorId ?pumpChannel ?modelName ?lifeTime ?temperature ?pumpStatus WHERE {
                {
                    ?obj a prog:Pump ;
                        prog:Pump_actuatorId ?actuatorId ;
                        prog:Pump_pumpChannel ?pumpChannel .
                    OPTIONAL { ?obj prog:Pump_modelName ?modelName }
                    OPTIONAL { ?obj prog:Pump_lifeTime ?lifeTime }
                    OPTIONAL { ?obj prog:Pump_temperature ?temperature }
                    OPTIONAL { ?obj prog:Pump_pumpStatus ?pumpStatus }
                } UNION {
                    ?obj a prog:OperatingPump ;
                        prog:OperatingPump_actuatorId ?actuatorId ;
                        prog:OperatingPump_pumpChannel ?pumpChannel .
                    OPTIONAL { ?obj prog:OperatingPump_modelName ?modelName }
                    OPTIONAL { ?obj prog:OperatingPump_lifeTime ?lifeTime }
                    OPTIONAL { ?obj prog:OperatingPump_temperature ?temperature }
                    OPTIONAL { ?obj prog:OperatingPump_pumpStatus ?pumpStatus }
                } UNION {
                    ?obj a prog:MaintenancePump ;
                        prog:MaintenancePump_actuatorId ?actuatorId ;
                        prog:MaintenancePump_pumpChannel ?pumpChannel .
                    OPTIONAL { ?obj prog:MaintenancePump_modelName ?modelName }
                    OPTIONAL { ?obj prog:MaintenancePump_lifeTime ?lifeTime }
                    OPTIONAL { ?obj prog:MaintenancePump_temperature ?temperature }
                    OPTIONAL { ?obj prog:MaintenancePump_pumpStatus ?pumpStatus }
                } UNION {
                    ?obj a prog:OverheatingPump ;
                        prog:OverheatingPump_actuatorId ?actuatorId ;
                        prog:OverheatingPump_pumpChannel ?pumpChannel .
                    OPTIONAL { ?obj prog:OverheatingPump_modelName ?modelName }
                    OPTIONAL { ?obj prog:OverheatingPump_lifeTime ?lifeTime }
                    OPTIONAL { ?obj prog:OverheatingPump_temperature ?temperature }
                    OPTIONAL { ?obj prog:OverheatingPump_pumpStatus ?pumpStatus }
                } UNION {
                    ?obj a prog:UnderheatingPump ;
                        prog:UnderheatingPump_actuatorId ?actuatorId ;
                        prog:UnderheatingPump_pumpChannel ?pumpChannel .
                    OPTIONAL { ?obj prog:UnderheatingPump_modelName ?modelName }
                    OPTIONAL { ?obj prog:UnderheatingPump_lifeTime ?lifeTime }
                    OPTIONAL { ?obj prog:UnderheatingPump_temperature ?temperature }
                    OPTIONAL { ?obj prog:UnderheatingPump_pumpStatus ?pumpStatus }
                }
             }""".trimIndent()

        val result: ResultSet = repl.interpreter!!.query(pumps)!!

        if (!result.hasNext()) {
            return null
        }

        while (result.hasNext()) {
            val solution: QuerySolution = result.next()
            val actuatorId = solution.get("?actuatorId").asLiteral().toString()
            val pumpChannel = solution.get("?pumpChannel").asLiteral().toString().split("^^")[0].toInt()
            val modelName =
                if (solution.contains("?modelName")) solution.get("?modelName").asLiteral().toString() else null
            val lifeTime = if (solution.contains("?lifeTime")) solution.get("?lifeTime").asLiteral().toString()
                .split("^^")[0].toInt() else null
            val temperature = if (solution.contains("?temperature")) solution.get("?temperature").asLiteral().toString()
                .split("^^")[0].toDouble() else null
            val pumpStatus = if (solution.contains("?pumpStatus")) {
                try {
                    PumpState.valueOf(solution.get("?pumpStatus").asLiteral().toString())
                } catch (e: IllegalArgumentException) {
                    null
                }
            } else null

            pumpsList.add(Pump(actuatorId, pumpChannel, modelName, lifeTime, temperature, pumpStatus))
        }

        return pumpsList
    }

    fun getPumpByPumpId(pumpId: String): Pump? {
        componentsConfig.getPumpById(pumpId)?.let { return it }
        val pumps =
            """
             SELECT ?pumpChannel ?modelName ?lifeTime ?temperature ?pumpStatus WHERE {
                {
                    ?obj a prog:Pump ;
                        prog:Pump_actuatorId "$pumpId" ;
                        prog:Pump_pumpChannel ?pumpChannel .
                    OPTIONAL { ?obj prog:Pump_modelName ?modelName }
                    OPTIONAL { ?obj prog:Pump_lifeTime ?lifeTime }
                    OPTIONAL { ?obj prog:Pump_temperature ?temperature }
                    OPTIONAL { ?obj prog:Pump_pumpStatus ?pumpStatus }
                } UNION {
                    ?obj a prog:OperatingPump ;
                        prog:OperatingPump_actuatorId "$pumpId" ;
                        prog:OperatingPump_pumpChannel ?pumpChannel .
                    OPTIONAL { ?obj prog:OperatingPump_modelName ?modelName }
                    OPTIONAL { ?obj prog:OperatingPump_lifeTime ?lifeTime }
                    OPTIONAL { ?obj prog:OperatingPump_temperature ?temperature }
                    OPTIONAL { ?obj prog:OperatingPump_pumpStatus ?pumpStatus }
                } UNION {
                    ?obj a prog:MaintenancePump ;
                        prog:MaintenancePump_actuatorId "$pumpId" ;
                        prog:MaintenancePump_pumpChannel ?pumpChannel .
                    OPTIONAL { ?obj prog:MaintenancePump_modelName ?modelName }
                    OPTIONAL { ?obj prog:MaintenancePump_lifeTime ?lifeTime }
                    OPTIONAL { ?obj prog:MaintenancePump_temperature ?temperature }
                    OPTIONAL { ?obj prog:MaintenancePump_pumpStatus ?pumpStatus }
                } UNION {
                    ?obj a prog:OverheatingPump ;
                        prog:OverheatingPump_actuatorId "$pumpId" ;
                        prog:OverheatingPump_pumpChannel ?pumpChannel .
                    OPTIONAL { ?obj prog:OverheatingPump_modelName ?modelName }
                    OPTIONAL { ?obj prog:OverheatingPump_lifeTime ?lifeTime }
                    OPTIONAL { ?obj prog:OverheatingPump_temperature ?temperature }
                    OPTIONAL { ?obj prog:OverheatingPump_pumpStatus ?pumpStatus }
                } UNION {
                    ?obj a prog:UnderheatingPump ;
                        prog:UnderheatingPump_actuatorId "$pumpId" ;
                        prog:UnderheatingPump_pumpChannel ?pumpChannel .
                    OPTIONAL { ?obj prog:UnderheatingPump_modelName ?modelName }
                    OPTIONAL { ?obj prog:UnderheatingPump_lifeTime ?lifeTime }
                    OPTIONAL { ?obj prog:UnderheatingPump_temperature ?temperature }
                    OPTIONAL { ?obj prog:UnderheatingPump_pumpStatus ?pumpStatus }
                }
             }""".trimIndent()

        val result: ResultSet = repl.interpreter!!.query(pumps)!!

        if (!result.hasNext()) {
            return null
        }

        val solution: QuerySolution = result.next()
        val pumpChannel = solution.get("?pumpChannel").asLiteral().toString().split("^^")[0].toInt()
        val modelName =
            if (solution.contains("?modelName")) solution.get("?modelName").asLiteral().toString() else null
        val lifeTime = if (solution.contains("?lifeTime")) solution.get("?lifeTime").asLiteral().toString()
            .split("^^")[0].toInt() else null
        val temperature = if (solution.contains("?temperature")) solution.get("?temperature").asLiteral().toString()
            .split("^^")[0].toDouble() else null
        val pumpStatus = if (solution.contains("?pumpStatus")) {
            try {
                PumpState.valueOf(solution.get("?pumpStatus").asLiteral().toString())
            } catch (e: IllegalArgumentException) {
                null
            }
        } else null

        val pump = Pump(pumpId, pumpChannel, modelName, lifeTime, temperature, pumpStatus)
        // Update cache
        componentsConfig.addPumpToCache(pump)

        return pump
    }

    fun getPumpsByStatus(status: PumpState): List<Pump>? {
        val pumpsList = mutableListOf<Pump>()
        val pumps =
            """
             SELECT DISTINCT ?actuatorId ?pumpChannel ?modelName ?lifeTime ?temperature ?pumpStatus WHERE {
                ?obj a prog:Pump ;
                    prog:Pump_actuatorId ?actuatorId ;
                    prog:Pump_pumpChannel ?pumpChannel ;
                    prog:Pump_pumpStatus "$status" .
                OPTIONAL { ?obj prog:Pump_modelName ?modelName }
                OPTIONAL { ?obj prog:Pump_lifeTime ?lifeTime }
                OPTIONAL { ?obj prog:Pump_temperature ?temperature }
                OPTIONAL { ?obj prog:Pump_pumpStatus ?pumpStatus }
             }""".trimIndent()

        val result: ResultSet = repl.interpreter!!.query(pumps)!!

        if (!result.hasNext()) {
            return null
        }

        while (result.hasNext()) {
            val solution: QuerySolution = result.next()
            val actuatorId = solution.get("?actuatorId").asLiteral().toString()
            val pumpChannel = solution.get("?pumpChannel").asLiteral().toString().split("^^")[0].toInt()
            val modelName =
                if (solution.contains("?modelName")) solution.get("?modelName").asLiteral().toString() else null
            val lifeTime = if (solution.contains("?lifeTime")) solution.get("?lifeTime").asLiteral().toString()
                .split("^^")[0].toInt() else null
            val temperature = if (solution.contains("?temperature")) solution.get("?temperature").asLiteral().toString()
                .split("^^")[0].toDouble() else null
            val pumpStatus = if (solution.contains("?pumpStatus")) {
                try {
                    PumpState.valueOf(solution.get("?pumpStatus").asLiteral().toString())
                } catch (e: IllegalArgumentException) {
                    null
                }
            } else null

            pumpsList.add(Pump(actuatorId, pumpChannel, modelName, lifeTime, temperature, pumpStatus))
        }

        return pumpsList
    }

    fun getOperatingPumps(): List<Pump>? = getPumpsByStatus(PumpState.Operating)
    fun getMaintenancePumps(): List<Pump>? = getPumpsByStatus(PumpState.Maintenance)
    fun getOverheatingPumps(): List<Pump>? = getPumpsByStatus(PumpState.Overheating)
    fun getUnderheatingPumps(): List<Pump>? = getPumpsByStatus(PumpState.Underheating)

    fun updatePump(updatedPump: Pump): Boolean {
        var deleteClause = ""
        var insertClause = ""

        // Build dynamic update based on what fields are not null
        if (updatedPump.temperature != null) {
            deleteClause += "OPTIONAL { ?pump ast:temperature ?oldTemperature } .\n"
            insertClause += "?pump ast:temperature \"${updatedPump.temperature}\"^^xsd:double .\n"
        }

        if (updatedPump.lifeTime != null) {
            deleteClause += "OPTIONAL { ?pump ast:lifeTime ?oldLifeTime } .\n"
            insertClause += "?pump ast:lifeTime ${updatedPump.lifeTime} .\n"
        }

        if (updatedPump.modelName != null) {
            deleteClause += "OPTIONAL { ?pump ast:modelName ?oldModelName } .\n"
            insertClause += "?pump ast:modelName \"${updatedPump.modelName}\" .\n"
        }

        if (updatedPump.pumpStatus != null) {
            deleteClause += "OPTIONAL { ?pump ast:pumpStatus ?oldStatus } .\n"
            insertClause += "?pump ast:pumpStatus \"${updatedPump.pumpStatus}\" .\n"
        }

        if (deleteClause.isEmpty() && insertClause.isEmpty()) {
            return false // Nothing to update
        }

        val updateQuery = """
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
            PREFIX ast: <$prefix>
            
            DELETE {
                ${if (updatedPump.temperature != null) "?pump ast:temperature ?oldTemperature .\n" else ""}${if (updatedPump.lifeTime != null) "?pump ast:lifeTime ?oldLifeTime .\n" else ""}${if (updatedPump.modelName != null) "?pump ast:modelName ?oldModelName .\n" else ""}${if (updatedPump.pumpStatus != null) "?pump ast:pumpStatus ?oldStatus .\n" else ""}
            }
            INSERT {
                $insertClause
            }
            WHERE {
                ?pump a ast:Pump ;
                    ast:actuatorId "${updatedPump.actuatorId}" .
                $deleteClause
            }
        """

        val updateRequest: UpdateRequest = UpdateFactory.create(updateQuery)
        val fusekiEndpoint = tripleStore + "/update"
        val updateProcessor: UpdateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            // merge with cache if present
            val cached = componentsConfig.getPumpById(updatedPump.actuatorId)
            val merged = if (cached == null) {
                updatedPump
            } else {
                Pump(
                    cached.actuatorId,
                    updatedPump.pumpChannel.takeIf { it != 0 } ?: cached.pumpChannel,
                    updatedPump.modelName ?: cached.modelName,
                    updatedPump.lifeTime ?: cached.lifeTime,
                    updatedPump.temperature ?: cached.temperature,
                    updatedPump.pumpStatus ?: cached.pumpStatus
                )
            }
            componentsConfig.addPumpToCache(merged)
        } catch (e: Exception) {
            return false
        }

        return true
    }

    fun deletePump(actuatorId: String): Boolean {
        val deletePump = """
            PREFIX ast: <$prefix>
            
            DELETE {
                ?pump ?p ?o .
            }
            WHERE {
                ?pump a ast:Pump ;
                    ast:actuatorId "$actuatorId" ;
                    ?p ?o .
            }
        """

        val updateRequest: UpdateRequest = UpdateFactory.create(deletePump)
        val fusekiEndpoint = tripleStore + "/update"
        val updateProcessor: UpdateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            componentsConfig.removePumpFromCache(actuatorId)
        } catch (e: Exception) {
            return false
        }

        return true
    }
}