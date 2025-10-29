package org.smolang.greenhouse.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.LightSensor
import org.smolang.greenhouse.api.service.LightSensorService
import org.smolang.greenhouse.api.types.CreateLightSensorRequest
import org.smolang.greenhouse.api.types.UpdateLightSensorRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.slf4j.Logger
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/api/sensors/light")
class LightSensorController (
    private val replConfig: REPLConfig,
    private val lightSensorService: LightSensorService
) {

    private val log: Logger = LoggerFactory.getLogger(LightSensorController::class.java.name)

    @Operation(summary = "Create a new light sensor")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Light sensor created"),
        ApiResponse(responseCode = "400", description = "Invalid light sensor"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    @PostMapping(produces = ["application/json"])
    fun createLightSensor(@SwaggerRequestBody(description = "Request to add a new light sensor") @RequestBody request: CreateLightSensorRequest) : ResponseEntity<String> {
        log.info("Creating light sensor $request")

        val sensor = lightSensorService.createSensor(request) ?: return ResponseEntity.badRequest().body("Failed to create light sensor")
        replConfig.regenerateSingleModel().invoke("lightSensors")

        return ResponseEntity.ok("Light sensor ${sensor.sensorId} created successfully")
    }

    @Operation(summary = "Update an existing light sensor")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Light sensor updated"),
        ApiResponse(responseCode = "400", description = "Invalid light sensor update request"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "Light sensor not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    @PatchMapping("/{sensorId}", produces = ["application/json"])
    fun updateLightSensor(
        @PathVariable sensorId: String,
        @SwaggerRequestBody(description = "Request to update an existing light sensor") @RequestBody request: UpdateLightSensorRequest
    ): ResponseEntity<String> {
        log.info("Updating light sensor with ID $sensorId")

        val sensor = lightSensorService.updateSensor(sensorId, request) ?: return ResponseEntity.badRequest().body("Failed to update light sensor")
        replConfig.regenerateSingleModel().invoke("lightSensors")

        return ResponseEntity.ok("Light sensor ${sensor.sensorId} updated successfully")
    }

    @Operation(summary = "Retrieve a light sensor by ID")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the light sensor"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The light sensor you were trying to reach is not found")
    ])
    @GetMapping("/{sensorId}", produces = ["application/json"])
    fun getLightSensorById(@PathVariable sensorId: String): ResponseEntity<LightSensor> {
        log.info("Getting light sensor by ID: $sensorId")

        val sensor = lightSensorService.getSensor(sensorId) ?: return ResponseEntity.notFound().build()

        log.info("Light sensor: $sensor")

        return ResponseEntity.ok(sensor)
    }

    @Operation(summary = "Retrieve all light sensors")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved all light sensors"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping(produces = ["application/json"])
    fun getAllLightSensors(): ResponseEntity<List<LightSensor>> {
        log.info("Getting all light sensors")
        val sensors = lightSensorService.getAllSensors()
        return ResponseEntity.ok(sensors)
    }

    @Operation(summary = "Delete a light sensor")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully deleted the light sensor"),
        ApiResponse(responseCode = "401", description = "You are not authorized to delete the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The light sensor you were trying to delete is not found")
    ])
    @DeleteMapping("/{sensorId}", produces = ["application/json"])
    fun deleteLightSensor(@PathVariable sensorId: String): ResponseEntity<String> {
        log.info("Deleting light sensor with ID: $sensorId")

        if (!lightSensorService.deleteSensor(sensorId)) {
            return ResponseEntity.notFound().build()
        }
        replConfig.regenerateSingleModel().invoke("lightSensors")

        return ResponseEntity.ok("Light sensor $sensorId deleted successfully")
    }
}