package org.smolang.greenhouse.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.MoistureSensor
import org.smolang.greenhouse.api.service.MoistureSensorService
import org.smolang.greenhouse.api.types.CreateMoistureSensorRequest
import org.smolang.greenhouse.api.types.UpdateMoistureSensorRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/api/sensors/moisture")
class MoistureSensorController(
    private val replConfig: REPLConfig,
    private val moistureSensorService: MoistureSensorService
) {

    private val log: Logger = LoggerFactory.getLogger(MoistureSensorController::class.java.name)

    @Operation(summary = "Create a new moisture sensor")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Moisture sensor created"),
            ApiResponse(responseCode = "400", description = "Invalid moisture sensor"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            ),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    @PostMapping(produces = ["application/json"])
    fun createMoistureSensor(@SwaggerRequestBody(description = "Request to add a new moisture sensor") @RequestBody request: CreateMoistureSensorRequest): ResponseEntity<String> {
        log.info("Creating moisture sensor $request")

        val sensor = moistureSensorService.createSensor(request) ?: return ResponseEntity.badRequest()
            .body("Failed to create moisture sensor")
        replConfig.regenerateSingleModel().invoke("moistureSensors")

        return ResponseEntity.ok("Moisture sensor ${sensor.sensorId} created successfully")
    }

    @Operation(summary = "Retrieve a moisture sensor by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved the moisture sensor"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            ),
            ApiResponse(responseCode = "404", description = "The moisture sensor you were trying to reach is not found")
        ]
    )
    @GetMapping("/{sensorId}", produces = ["application/json"])
    fun getMoistureSensorById(@PathVariable sensorId: String): ResponseEntity<MoistureSensor> {
        log.info("Retrieving moisture sensor $sensorId")

        val sensor = moistureSensorService.getSensor(sensorId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(sensor)
    }

    @Operation(summary = "Retrieve all moisture sensors")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved all moisture sensors"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            ),
            ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
        ]
    )
    @GetMapping(produces = ["application/json"])
    fun getAllMoistureSensors(): ResponseEntity<List<MoistureSensor>> {
        log.info("Retrieving all moisture sensors")

        val sensors = moistureSensorService.getAllSensors() ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(sensors)
    }

    @Operation(summary = "Update an existing moisture sensor")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Moisture sensor updated"),
            ApiResponse(responseCode = "400", description = "Invalid moisture sensor update request"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            ),
            ApiResponse(responseCode = "404", description = "Moisture sensor not found"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    @PatchMapping("/{sensorId}", produces = ["application/json"])
    fun updateMoistureSensor(
        @PathVariable sensorId: String,
        @SwaggerRequestBody(description = "Request to update an existing moisture sensor") @RequestBody request: UpdateMoistureSensorRequest
    ): ResponseEntity<String> {
        log.info("Updating moisture sensor $sensorId with request $request")

        val sensor = moistureSensorService.updateSensor(sensorId, request) ?: return ResponseEntity.badRequest()
            .body("Failed to update moisture sensor")
        replConfig.regenerateSingleModel().invoke("moistureSensors")

        return ResponseEntity.ok("Moisture sensor ${sensor.sensorId} updated successfully")
    }

    @Operation(summary = "Delete an existing moisture sensor")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Moisture sensor deleted"),
            ApiResponse(responseCode = "400", description = "Invalid moisture sensor ID"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            ),
            ApiResponse(responseCode = "404", description = "Moisture sensor not found")
        ]
    )
    @DeleteMapping("/{sensorId}", produces = ["application/json"])
    fun deleteMoistureSensor(@PathVariable sensorId: String): ResponseEntity<String> {
        log.info("Deleting moisture sensor $sensorId")

        if (!moistureSensorService.deleteSensor(sensorId)) {
            return ResponseEntity.badRequest().body("Failed to delete moisture sensor")
        }
        replConfig.regenerateSingleModel().invoke("moistureSensors")

        return ResponseEntity.ok("Moisture sensor $sensorId deleted successfully")
    }
}