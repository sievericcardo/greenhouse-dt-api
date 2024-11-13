package org.smolang.greenhouse.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.uio.microobject.runtime.REPL
import org.apache.jena.query.QuerySolution
import org.apache.jena.query.ResultSet
import org.apache.jena.rdfconnection.RDFConnectionFactory
import org.apache.jena.update.*
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.Pump
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.logging.Logger
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody


data class PumpRequest (
    val pumpGpioPin: Int,
    val pumpId: String,
    val waterPressure: Double
)

@RestController
@RequestMapping("/api/pumps")
class PumpController (
    private val replConfig: REPLConfig
) {

    private val log: Logger = Logger.getLogger(PumpController::class.java.name)

    @Operation(summary = "Retrieve the pumps")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the pumps"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping("/all")
    fun getPumps() : ResponseEntity<List<Pump>> {
        log.info("Getting all pumps")

        val repl: REPL = replConfig.repl()
        val pumpsList = mutableListOf<Pump>()

        val pumps =
            """
             SELECT * WHERE {
                ?obj a prog:Pump ;
                    prog:Pump_pumpGpioPin ?pumpGpioPin ;
                    prog:Pump_pumpId ?pumpId ;
                    domain:models ?x .
                        ?x domain:waterPressure ?waterPressure .
             }"""

        val result: ResultSet = repl.interpreter!!.query(pumps)!!

        while (result.hasNext()) {
            val solution: QuerySolution = result.next()
            val pumpGpioPin = solution.get("?pumpGpioPin").asLiteral().toString().split("^^")[0].toInt()
            val pumpId = solution.get("?pumpId").asLiteral().toString()
            val waterPressure = solution.get("?waterPressure").asLiteral().toString().split("^^")[0].toDouble()

            pumpsList.add(Pump(pumpGpioPin, pumpId, waterPressure, PumpState.Unknown))
        }

        val operatingPumps =
            """
             SELECT * WHERE {
                ?obj a prog:OperatingPump ;
                    prog:OperatingPump_pumpGpioPin ?pumpGpioPin ;
                    prog:OperatingPump_pumpId ?pumpId ;
                    domain:models ?x .
                        ?x domain:waterPressure ?waterPressure .
             }"""

        val opertingResult: ResultSet = repl.interpreter!!.query(operatingPumps)!!

        while (opertingResult.hasNext()) {
            val solution: QuerySolution = opertingResult.next()
            val pumpGpioPin = solution.get("?pumpGpioPin").asLiteral().toString().split("^^")[0].toInt()
            val pumpId = solution.get("?pumpId").asLiteral().toString()
            val waterPressure = solution.get("?waterPressure").asLiteral().toString().split("^^")[0].toDouble()

            pumpsList.add(Pump(pumpGpioPin, pumpId, waterPressure, PumpState.Operational))
        }

        val maintenancePumps =
            """
             SELECT * WHERE {
                ?obj a prog:MaintenancePump ;
                    prog:MaintenancePump_pumpGpioPin ?pumpGpioPin ;
                    prog:MaintenancePump_pumpId ?pumpId ;
                    domain:models ?x .
                        ?x domain:waterPressure ?waterPressure .
             }"""

        val maintenanceResult: ResultSet = repl.interpreter!!.query(maintenancePumps)!!

        while (maintenanceResult.hasNext()) {
            val solution: QuerySolution = maintenanceResult.next()
            val pumpGpioPin = solution.get("?pumpGpioPin").asLiteral().toString().split("^^")[0].toInt()
            val pumpId = solution.get("?pumpId").asLiteral().toString()
            val waterPressure = solution.get("?waterPressure").asLiteral().toString().split("^^")[0].toDouble()

            pumpsList.add(Pump(pumpGpioPin, pumpId, waterPressure, PumpState.Maintenance))
        }

        log.info("Pumps: $pumpsList")

        return ResponseEntity.ok(pumpsList)
    }

    @Operation(summary = "Retrieve the pump that are operational")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the operational pumps"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping("/operational")
    fun getOperationalPumps() : ResponseEntity<List<Pump>> {
        log.info("Getting all operational pumps")

        val repl: REPL = replConfig.repl()
        val pumpsList = mutableListOf<Pump>()

        val pumps =
            """
             SELECT * WHERE {
                ?obj a prog:OperatingPump ;
                    prog:OperatingPump_pumpGpioPin ?pumpGpioPin ;
                    prog:OperatingPump_pumpId ?pumpId ;
                    domain:models ?x .
                        ?x domain:waterPressure ?waterPressure .
             }"""

        val result: ResultSet = repl.interpreter!!.query(pumps)!!

        while (result.hasNext()) {
            val solution: QuerySolution = result.next()
            val pumpGpioPin = solution.get("?pumpGpioPin").asLiteral().toString().split("^^")[0].toInt()
            val pumpId = solution.get("?pumpId").asLiteral().toString()
            val waterPressure = solution.get("?waterPressure").asLiteral().toString().split("^^")[0].toDouble()

            pumpsList.add(Pump(pumpGpioPin, pumpId, waterPressure, PumpState.Operational))
        }

        log.info("Pumps: $pumpsList")

        return ResponseEntity.ok(pumpsList)
    }

    @Operation(summary = "Retrieve the pump that are in maintenance")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the pumps in maintenance"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping("/maintenance")
    fun getMaintenancePumps() : ResponseEntity<List<Pump>> {
        log.info("Getting all pumps in maintenance")

        val repl: REPL = replConfig.repl()
        val pumpsList = mutableListOf<Pump>()

        val pumps =
            """
             SELECT * WHERE {
                ?obj a prog:MaintenancePump ;
                    prog:MaintenancePump_pumpGpioPin ?pumpGpioPin ;
                    prog:MaintenancePump_pumpId ?pumpId ;
                    domain:models ?x .
                        ?x domain:waterPressure ?waterPressure .
             }"""

        val result: ResultSet = repl.interpreter!!.query(pumps)!!

        while (result.hasNext()) {
            val solution: QuerySolution = result.next()
            val pumpGpioPin = solution.get("?pumpGpioPin").asLiteral().toString().split("^^")[0].toInt()
            val pumpId = solution.get("?pumpId").asLiteral().toString()
            val waterPressure = solution.get("?waterPressure").asLiteral().toString().split("^^")[0].toDouble()

            pumpsList.add(Pump(pumpGpioPin, pumpId, waterPressure, PumpState.Maintenance))
        }

        log.info("Pumps: $pumpsList")

        return ResponseEntity.ok(pumpsList)
    }

    @Operation(summary = "Update pressure to a pump")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully updated the pump pressure"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @PostMapping("/update")
    fun updatePumpPressure(@SwaggerRequestBody(description = "Pump to be updated") @RequestBody pumpRequest: PumpRequest) : ResponseEntity<String> {
        log.info("Updating pump pressure")

        val tripleStore =System.getenv().getOrDefault("TRIPLESTORE_URL", "http://localhost:3030/ds")
        val prefix = System.getenv().getOrDefault("BASE_PREFIX_URI", "http://www.smolang.org/greenhouseDT#")

        val updatedPump = Pump(pumpRequest.pumpGpioPin, pumpRequest.pumpId, pumpRequest.waterPressure, PumpState.Unknown)

        val updateQuery = """
            PREFIX ast: <$prefix>
            
            DELETE {
                ?pump ast:waterPressure ?oldWaterPressure .
            }
            INSERT {
                ?pump ast:waterPressure ${updatedPump.waterPressure}^^xsd:double .
            }
            WHERE {
                ?pump a ast:Pump ;
                    ast:pumpGpioPin ${updatedPump.pumpGpioPin} ;
                    ast:pumpId "${updatedPump.pumpId}" ;
                    ast:waterPressure ?oldWaterPressure .
            }
        """

        val updateRequest: UpdateRequest = UpdateFactory.create(updateQuery)
        val fusekiEndpoint = tripleStore + "/update"
        val updateProcessor: UpdateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body("Error: the update query could not be executed.")
        }

        val repl: REPL = replConfig.repl()
        repl.interpreter!!.tripleManager.regenerateTripleStoreModel()
        repl.interpreter!!.evalCall(
            repl.interpreter!!.getObjectNames("AssetModel").get(0),
            "AssetModel",
            "reconfigure")
        repl.interpreter!!.evalCall(
            repl.interpreter!!.getObjectNames("AssetModel").get(0),
            "AssetModel",
            "reclassify")

        return ResponseEntity.ok("Pump pressure updated")
    }
}