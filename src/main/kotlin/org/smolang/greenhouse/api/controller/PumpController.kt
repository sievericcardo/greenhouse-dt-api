package org.smolang.greenhouse.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.uio.microobject.runtime.REPL
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.Pump
import org.smolang.greenhouse.api.service.PumpService
import org.smolang.greenhouse.api.types.PumpState
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.logging.Logger
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody
import org.smolang.greenhouse.api.types.CreatePumpRequest
import org.smolang.greenhouse.api.types.UpdatePumpRequest
import org.smolang.greenhouse.api.types.DeletePumpRequest

@RestController
@RequestMapping("/api/pumps")
class PumpController (
    private val replConfig: REPLConfig,
    private val pumpService: PumpService
) {

    private val log: Logger = Logger.getLogger(PumpController::class.java.name)

    @Operation(summary = "Create a new pump")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully created the pump"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @PostMapping("/create")
    fun createPump(@SwaggerRequestBody(description = "Pump to be created") @RequestBody pumpRequest: CreatePumpRequest) : ResponseEntity<String> {
        log.info("Creating a new pump")

        val newPump = Pump(pumpRequest.pumpGpioPin, pumpRequest.pumpId, pumpRequest.modelName, pumpRequest.lifeTime, pumpRequest.temperature, PumpState.Unknown)
        log.info("New pump: $newPump")

        if (!pumpService.createPump(newPump)) {
            return ResponseEntity.badRequest().body("Failed to create pump")
        }

        replConfig.regenerateSingleModel().invoke("pumps")

        return ResponseEntity.ok("Pump created")
    }

    @Operation(summary = "Retrieve the pumps")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the pumps"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping("/retrieve")
    fun getPumps() : ResponseEntity<List<Pump>> {
        log.info("Getting all pumps")

        val repl: REPL = replConfig.repl()
        val pumpsList = mutableListOf<Pump>()

        val operatingPumps = pumpService.getOperatingPumps()
        val maintenancePumps = pumpService.getMaintenancePumps()
        val overheatingPumps = pumpService.getOverheatingPumps()
        val underheatingPumps = pumpService.getUnderheatingPumps()

        operatingPumps?.let { pumpsList.addAll(it) }
        maintenancePumps?.let { pumpsList.addAll(it) }
        overheatingPumps?.let { pumpsList.addAll(it) }
        underheatingPumps?.let { pumpsList.addAll(it) }

        if (pumpsList.isEmpty()) {
            return ResponseEntity.notFound().build()
        }

        log.info("Pumps: $pumpsList")

        return ResponseEntity.ok(pumpsList)
    }

    @Operation(summary = "Retrieve the pump that are operating")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the operating pumps"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping("/operating")
    fun getOperatingPumps() : ResponseEntity<List<Pump>> {
        log.info("Getting all operating pumps")

        val repl: REPL = replConfig.repl()
        val pumpsList = pumpService.getOperatingPumps() ?: return ResponseEntity.notFound().build()

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
        val pumpsList = pumpService.getMaintenancePumps() ?: return ResponseEntity.notFound().build()

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
        val pumpsList = pumpService.getOverheatingPumps() ?: return ResponseEntity.notFound().build()

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
        val pumpsList = pumpService.getUnderheatingPumps() ?: return ResponseEntity.notFound().build()

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
    @PatchMapping("/update")
    fun updatePump(@SwaggerRequestBody(description = "Pump to be updated") @RequestBody pumpRequest: UpdatePumpRequest) : ResponseEntity<String> {
        log.info("Updating pump pressure")

        val updatedPump = Pump(pumpRequest.pumpGpioPin, pumpRequest.pumpId, pumpRequest.modelName, pumpRequest.lifeTime, pumpRequest.temperature, PumpState.Unknown)
        log.info("Updated pump: $updatedPump")

        if (!pumpService.updatePump(updatedPump)) {
            return ResponseEntity.badRequest().body("Failed to update pump")
        }

        replConfig.regenerateSingleModel().invoke("pumps")

        return ResponseEntity.ok("Pump updated")
    }

    @Operation(summary = "Update pressure to multiple pumps")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully updated the pumps pressure"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @PatchMapping("/update-multil")
    fun updateMultiplePumps(@SwaggerRequestBody(description = "Pumps to be updated") @RequestBody pumpRequests: List<UpdatePumpRequest>) : ResponseEntity<String> {
        log.info("Updating pumps pressure")

        val updatedPumps = pumpRequests.map { Pump(it.pumpGpioPin, it.pumpId, it.modelName, it.lifeTime, it.temperature, PumpState.Unknown) }
        log.info("Updated pumps: $updatedPumps")

        updatedPumps.forEach() {
            if (!pumpService.updatePump(it)) {
                return ResponseEntity.badRequest().body("Failed to update pump")
            }
        }

        replConfig.regenerateSingleModel().invoke("pumps")

        return ResponseEntity.ok("Pumps updated")
    }

    @Operation(summary = "Delete a pump")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully deleted the pump"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @DeleteMapping("/delete")
    fun deletePump(@SwaggerRequestBody(description = "Pump to be deleted") @RequestBody pumpRequest: DeletePumpRequest) : ResponseEntity<String> {
        log.info("Deleting a pump")

        if (!pumpService.deletePump(pumpRequest.pumpId)) {
            return ResponseEntity.badRequest().body("Failed to delete pump")
        }

        replConfig.regenerateSingleModel().invoke("pumps")

        return ResponseEntity.ok("Pump deleted")
    }
}