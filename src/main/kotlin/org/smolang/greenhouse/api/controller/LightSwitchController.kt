package org.smolang.greenhouse.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.LightSwitch
import org.smolang.greenhouse.api.service.LightSwitchService
import org.smolang.greenhouse.api.types.CreateLightSwitchRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.logging.Logger
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/api/actuators/light")
class LightSwitchController (
    private val replConfig: REPLConfig,
    private val lightSensorService: LightSensorService
) {

    private val log: Logger = Logger.getLogger(LightSwitchController::class.java.name)

    @Operation(summary = "Create a new light switch")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Light switch created"),
        ApiResponse(responseCode = "400", description = "Invalid light switch"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    @PostMapping(produces = ["application/json"])
    fun createLightSwitch(@SwaggerRequestBody(description = "Request to add a new light switch") @RequestBody request: CreateLightSwitchRequest) : ResponseEntity<String> {
        log.info("Creating light switch $request")

        val lightSwitch = lightSensorService.createLightSwitch(request) ?: return ResponseEntity.badRequest().body("Failed to create light switch")
        replConfig.regenerateSingleModel().invoke("lightSwitches")

        return ResponseEntity.ok("Light switch ${lightSwitch.lightSwitchId} created successfully")
    }

    @Operation(summary = "Retrieve a light switch by ID")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the light switch"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The light switch you were trying to reach is not found")
    ])
    @GetMapping("/{lightSwitchId}", produces = ["application/json"])
    fun getLightSwitchById(@PathVariable lightSwitchId: String): ResponseEntity<LightSwitch> {
        log.info("Getting light switch by ID: $lightSwitchId")

        val lightSwitch = lightSensorService.getLightSwitch(lightSwitchId) ?: return ResponseEntity.notFound().build()

        log.info("Light switch: $lightSwitch")

        return ResponseEntity.ok(lightSwitch)
    }

    @Operation(summary = "Retrieve all light switches")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the light switches"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping(produces = ["application/json"])
    fun getLightSwitches(): ResponseEntity<List<LightSwitch>> {
        log.info("Getting all light switches")

        val lightSwitches = lightSensorService.getAllLightSwitches() ?: return ResponseEntity.noContent().build()

        log.info("Light switches: $lightSwitches")

        return ResponseEntity.ok(lightSwitches)
    }

    @Operation(summary = "Delete an existing light switch")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Light switch deleted"),
        ApiResponse(responseCode = "400", description = "Invalid light switch ID"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "Light switch not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    @DeleteMapping("/{lightSwitchId}", produces = ["application/json"])
    fun deleteLightSwitch(@PathVariable lightSwitchId: String): ResponseEntity<String> {
        log.info("Deleting light switch with ID $lightSwitchId")

        if (!lightSensorService.deleteLightSwitch(lightSwitchId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Failed to delete light switch")
        }
        replConfig.regenerateSingleModel().invoke("lightSwitches")

        return ResponseEntity.ok("Light switch $lightSwitchId deleted successfully")
    }
}