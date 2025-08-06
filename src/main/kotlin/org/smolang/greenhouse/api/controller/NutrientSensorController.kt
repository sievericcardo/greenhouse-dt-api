package org.smolang.greenhouse.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.NutrientSensor
import org.smolang.greenhouse.api.service.NutrientSensorService
import org.smolang.greenhouse.api.types.CreateNutrientSensorRequest
import org.smolang.greenhouse.api.types.UpdateNutrientSensorRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.logging.Logger
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/api/sensors/nutrient")
class NutrientSensorController (
    private val replConfig: REPLConfig,
    private val nutrientSensorService: NutrientSensorService
) {

    private val log: Logger = Logger.getLogger(NutrientSensorController::class.java.name)

    @Operation(summary = "Create a new nutrient sensor")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Nutrient sensor created"),
        ApiResponse(responseCode = "400", description = "Invalid nutrient sensor"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    @PostMapping(produces = ["application/json"])
    fun createNutrientSensor(@SwaggerRequestBody(description = "Request to add a new nutrient sensor") @RequestBody request: CreateNutrientSensorRequest): ResponseEntity<String> {
        log.info("Creating nutrient sensor $request")

        val sensor = nutrientSensorService.createSensor(request) ?: return ResponseEntity.badRequest().body("Failed to create nutrient sensor")
        replConfig.regenerateSingleModel().invoke("nutrientSensors")

        return ResponseEntity.ok("Nutrient sensor ${sensor.sensorId} created successfully")
    }

    @Operation(summary = "Retrieve a nutrient sensor by ID")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the nutrient sensor"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The nutrient sensor you were trying to reach is not found")
    ])
    @GetMapping("/{sensorId}", produces = ["application/json"])
    fun getNutrientSensorById(@PathVariable sensorId: String): ResponseEntity<NutrientSensor> {
        log.info("Retrieving nutrient sensor $sensorId")

        val sensor = nutrientSensorService.getSensor(sensorId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(sensor)
    }

    @Operation(summary = "Retrieve all nutrient sensors")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved all nutrient sensors"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping(produces = ["application/json"])
    fun getAllNutrientSensors(): ResponseEntity<List<NutrientSensor>> {
        log.info("Retrieving all nutrient sensors")

        val sensors = nutrientSensorService.getAllSensors() ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(sensors)
    }

    @Operation(summary = "Update an existing nutrient sensor")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Nutrient sensor updated"),
        ApiResponse(responseCode = "400", description = "Invalid nutrient sensor update request"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "Nutrient sensor not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    @PatchMapping("/{sensorId}", produces = ["application/json"])
    fun updateNutrientSensor(
        @PathVariable sensorId: String,
        @SwaggerRequestBody(description = "Request to update an existing nutrient sensor") @RequestBody request: UpdateNutrientSensorRequest
    ): ResponseEntity<String> {
        log.info("Updating nutrient sensor with ID $sensorId")

        val sensor = nutrientSensorService.updateSensor(sensorId, request) ?: return ResponseEntity.badRequest().body("Failed to update nutrient sensor")
        replConfig.regenerateSingleModel().invoke("nutrientSensors")

        return ResponseEntity.ok("Nutrient sensor ${sensor.sensorId} updated successfully")
    }

    @Operation(summary = "Delete an existing nutrient sensor")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Nutrient sensor deleted"),
        ApiResponse(responseCode = "400", description = "Invalid nutrient sensor ID"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "Nutrient sensor not found")
    ])
    @DeleteMapping("/{sensorId}", produces = ["application/json"])
    fun deleteNutrientSensor(@PathVariable sensorId: String): ResponseEntity<String> {
        log.info("Deleting nutrient sensor with ID $sensorId")

        if (!nutrientSensorService.deleteSensor(sensorId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nutrient sensor $sensorId not found")
        }
        replConfig.regenerateSingleModel().invoke("nutrientSensors")

        return ResponseEntity.ok("Nutrient sensor $sensorId deleted successfully")
    }
}