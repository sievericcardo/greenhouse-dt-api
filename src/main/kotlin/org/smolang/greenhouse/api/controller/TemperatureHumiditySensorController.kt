package org.smolang.greenhouse.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.TemperatureHumiditySensor
import org.smolang.greenhouse.api.service.TemperatureHumiditySensorService
import org.smolang.greenhouse.api.types.CreateTemperatureHumiditySensorRequest
import org.smolang.greenhouse.api.types.UpdateTemperatureHumiditySensorRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.logging.Logger
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/api/sensors/temperature-humidity")
class TemperatureHumiditySensorController (
    private val replConfig: REPLConfig,
    private val temperatureHumiditySensorService: TemperatureHumiditySensorService
) {

    private val log: Logger = Logger.getLogger(TemperatureHumiditySensorController::class.java.name)

    @Operation(summary = "Create a new temperature and humidity sensor")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Temperature and humidity sensor created"),
        ApiResponse(responseCode = "400", description = "Invalid temperature and humidity sensor"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    @PostMapping(produces = ["application/json"])
    fun createTemperatureHumiditySensor(@SwaggerRequestBody(description = "Request to add a new temperature and humidity sensor") @RequestBody request: CreateTemperatureHumiditySensorRequest): ResponseEntity<String> {
        log.info("Creating temperature and humidity sensor $request")

        val sensor = temperatureHumiditySensorService.createSensor(request) ?: return ResponseEntity.badRequest().body("Failed to create temperature and humidity sensor")
        replConfig.regenerateSingleModel().invoke("temperatureHumiditySensors")

        return ResponseEntity.ok("Temperature and humidity sensor ${sensor.sensorId} created successfully")
    }

    @Operation(summary = "Retrieve a temperature and humidity sensor by ID")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the temperature and humidity sensor"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The temperature and humidity sensor you were trying to reach is not found")
    ])
    @GetMapping("/{sensorId}", produces = ["application/json"])
    fun getTemperatureHumiditySensorById(@PathVariable sensorId: String): ResponseEntity<TemperatureHumiditySensor> {
        log.info("Retrieving temperature and humidity sensor $sensorId")

        val sensor = temperatureHumiditySensorService.getSensor(sensorId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(sensor)
    }

    @Operation(summary = "Retrieve all temperature and humidity sensors")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved all temperature and humidity sensors"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping(produces = ["application/json"])
    fun getAllTemperatureHumiditySensors(): ResponseEntity<List<TemperatureHumiditySensor>> {
        log.info("Retrieving all temperature and humidity sensors")

        val sensors = temperatureHumiditySensorService.getAllSensors() ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(sensors)
    }

    @Operation(summary = "Update a temperature and humidity sensor")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Temperature and humidity sensor updated"),
        ApiResponse(responseCode = "400", description = "Invalid temperature and humidity sensor update request"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "Temperature and humidity sensor not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    @PatchMapping("/{sensorId}", produces = ["application/json"])
    fun updateTemperatureHumiditySensor(
        @PathVariable sensorId: String,
        @SwaggerRequestBody(description = "Request to update an existing temperature and humidity sensor") @RequestBody request: UpdateTemperatureHumiditySensorRequest
    ): ResponseEntity<String> {
        log.info("Updating temperature and humidity sensor with ID $sensorId")

        val sensor = temperatureHumiditySensorService.updateSensor(sensorId, request) ?: return ResponseEntity.badRequest().body("Failed to update temperature and humidity sensor")
        replConfig.regenerateSingleModel().invoke("temperatureHumiditySensors")

        return ResponseEntity.ok("Temperature and humidity sensor ${sensor.sensorId} updated successfully")
    }

    @Operation(summary = "Delete a temperature and humidity sensor")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Temperature and humidity sensor deleted"),
        ApiResponse(responseCode = "400", description = "Invalid temperature and humidity sensor ID"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "Temperature and humidity sensor not found"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    @DeleteMapping("/{sensorId}", produces = ["application/json"])
    fun deleteTemperatureHumiditySensor(@PathVariable sensorId: String): ResponseEntity<String> {
        log.info("Deleting temperature and humidity sensor with ID $sensorId")

        val deleted = temperatureHumiditySensorService.deleteSensor(sensorId)
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Temperature and humidity sensor $sensorId not found")
        }

        replConfig.regenerateSingleModel().invoke("temperatureHumiditySensors")
        return ResponseEntity.ok("Temperature and humidity sensor $sensorId deleted successfully")
    }
}