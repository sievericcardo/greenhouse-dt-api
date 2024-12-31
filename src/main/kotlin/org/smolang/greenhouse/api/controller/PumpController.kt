package org.smolang.greenhouse.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.runtime.REPL
import no.uio.microobject.type.BaseType
import no.uio.microobject.type.STRINGTYPE
import org.apache.jena.query.QuerySolution
import org.apache.jena.query.ResultSet
import org.apache.jena.rdfconnection.RDFConnectionFactory
import org.apache.jena.update.*
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.Pump
import org.smolang.greenhouse.api.service.PumpService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.logging.Logger
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody


data class PumpRequest (
    val pumpGpioPin: Int,
    val pumpId: String,
    val modelName: String,
    val lifeTime: Int,
    val temperature: Double
)

@RestController
@RequestMapping("/api/pumps")
class PumpController (
    private val replConfig: REPLConfig,
    private val pumpService: PumpService
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

        val operatingPumps = pumpService.getOperatingPumps()
        val maintenancePumps = pumpService.getMaintenancePumps()
        val overheatingPumps = pumpService.getOverheatingPumps()
        val underheatingPumps = pumpService.getUnderheatingPumps()

        pumpsList.addAll(operatingPumps)
        pumpsList.addAll(maintenancePumps)
        pumpsList.addAll(overheatingPumps)
        pumpsList.addAll(underheatingPumps)

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
        val pumpsList = pumpService.getOperatingPumps()

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
        val pumpsList = pumpService.getMaintenancePumps()

        log.info("Pumps: $pumpsList")

        return ResponseEntity.ok(pumpsList)
    }

    @Operation(summary = "Retrieve the pump that are overheating")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the overheating pumps"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping("/overheating")
    fun getOverheatingPumps() : ResponseEntity<List<Pump>> {
        log.info("Getting all overheating pumps")

        val repl: REPL = replConfig.repl()
        val pumpsList = pumpService.getOverheatingPumps()

        log.info("Pumps: $pumpsList")

        return ResponseEntity.ok(pumpsList)
    }

    @Operation(summary = "Retrieve the pump that are underheating")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the underheating pumps"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping("/underheating")
    fun getUnderheatingPumps() : ResponseEntity<List<Pump>> {
        log.info("Getting all underheating pumps")

        val repl: REPL = replConfig.repl()
        val pumpsList = pumpService.getUnderheatingPumps()

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

        val tripleStoreHost = System.getenv("TRIPLESTORE_URL") ?: "localhost"
        val tripleStoreDataset = System.getenv("TRIPLESTORE_DATASET") ?: "ds"
        val tripleStore = "http://$tripleStoreHost:3030/$tripleStoreDataset"
        val prefix = System.getenv().getOrDefault("BASE_PREFIX_URI", "http://www.smolang.org/greenhouseDT#")

        val updatedPump = Pump(pumpRequest.pumpGpioPin, pumpRequest.pumpId, pumpRequest.modelName, pumpRequest.lifeTime, pumpRequest.temperature, PumpState.Unknown)
        log.info("Updated pump: $updatedPump")

        val updateQuery = """
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
            PREFIX ast: <$prefix>
            
            DELETE {
                ?pump ast:temperature ?oldTemperature .
            }
            INSERT {
                ?pump ast:temperature "${updatedPump.temperature}"^^xsd:double .
            }
            WHERE {
                ?pump a ast:Pump ;
                    ast:pumpGpioPin ${updatedPump.pumpGpioPin} ;
                    ast:pumpId "${updatedPump.pumpId}" ;
                    ast:temperature ?oldTemperature .
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
            repl.interpreter!!.getObjectNames("AssetModel")[0],
            "AssetModel",
            "reconfigureSingleModel",
            mapOf("mod" to LiteralExpr("\"pumps\"", STRINGTYPE)))
        repl.interpreter!!.evalCall(
            repl.interpreter!!.getObjectNames("AssetModel")[0],
            "AssetModel",
            "reclassifySingleModel",
            mapOf("mod" to LiteralExpr("\"pumps\"", STRINGTYPE)))

        return ResponseEntity.ok("Pump pressure updated")
    }
}