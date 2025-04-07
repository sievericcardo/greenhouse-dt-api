package org.smolang.greenhouse.api.controller

import io.swagger.annotations.ApiParam
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import no.uio.microobject.runtime.REPL
import org.apache.jena.query.QuerySolution
import org.apache.jena.query.ResultSet
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.Pot
import org.smolang.greenhouse.api.service.PotService
import org.springframework.http.ResponseEntity
import java.util.logging.Logger
import org.smolang.greenhouse.api.types.CreatePotRequest
import org.smolang.greenhouse.api.types.UpdatePotRequest
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/api/pots")
class PotController (
    private val replConfig: REPLConfig,
    private val potService: PotService
) {

    private val log : Logger = Logger.getLogger(PotController::class.java.name)

    @Operation(summary = "Create a new pot")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Pot created"),
        ApiResponse(responseCode = "400", description = "Invalid pot"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    @PostMapping(produces= ["application/json"])
    fun createPot(@SwaggerRequestBody(description = "Request to add a new pot") @RequestBody request: CreatePotRequest) : ResponseEntity<Pot> {
        log.info("Creating pot $request")

        if(!potService.createPot(request.potId, request.shelfFloor, request.potPosition, request.pumpId, request.plantId)) {
            return ResponseEntity.badRequest().build()
        }
        replConfig.regenerateSingleModel().invoke("pots")

        return ResponseEntity.ok(Pot(request.potId, request.shelfFloor, request.potPosition, request.pumpId, request.plantId))
    }

    @Operation(summary = "Retrieve the pots")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the pots"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping(produces= ["application/json"])
    fun getPots() : ResponseEntity<List<Pot>> {
        log.info("Getting all pots")

        val pots = potService.getPots()?: return ResponseEntity.noContent().build()

        log.info("Pots: $pots")

        return ResponseEntity.ok(pots)
    }

    @Operation(summary = "Retrieve a pot")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Pot found"),
        ApiResponse(responseCode = "400", description = "Invalid pot"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    @GetMapping("/{potId}", produces= ["application/json"])
    fun getPot(@ApiParam(value = "Plant ID", required = true) @Valid @PathVariable potId: String) : ResponseEntity<Pot> {
        log.info("Getting pot $potId")

        val pot = potService.getPotByPotId(potId) ?: return ResponseEntity.badRequest().build()

        return ResponseEntity.ok(pot)
    }

    @Operation(summary = "Delete a pot")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Pot deleted"),
        ApiResponse(responseCode = "400", description = "Invalid pot"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    @DeleteMapping("/{potId}", produces= ["application/json"])
    fun deletePot(@ApiParam(value = "Pot ID", required = true) @Valid @PathVariable potId: String) : ResponseEntity<String> {
        log.info("Deleting pot $potId")

        if (potService.getPotByPotId(potId) == null) {
            return ResponseEntity.notFound().build()
        }
        if (!potService.deletePot(potId)) {
            return ResponseEntity.badRequest().build()
        }
        replConfig.regenerateSingleModel().invoke("pots")

        return ResponseEntity.ok("Pot deleted")
    }
}