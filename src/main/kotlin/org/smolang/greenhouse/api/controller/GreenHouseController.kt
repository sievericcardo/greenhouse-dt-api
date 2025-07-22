package org.smolang.greenhouse.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.GreenHouse
import org.smolang.greenhouse.api.service.GreenHouseService
import org.smolang.greenhouse.api.types.CreateGreenHouseRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.logging.Logger
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/api/greenhouses")
class GreenHouseController (
    private val replConfig: REPLConfig,
    private val greenHouseService: GreenHouseService
) {

    private val log: Logger = Logger.getLogger(GreenHouseController::class.java.name)

    @Operation(summary = "Create a new greenhouse")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Greenhouse created"),
        ApiResponse(responseCode = "400", description = "Invalid greenhouse"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    @PostMapping(produces = ["application/json"])
    fun createGreenHouse(@SwaggerRequestBody(description = "Request to add a new greenhouse") @RequestBody request: CreateGreenHouseRequest) : ResponseEntity<String> {
        log.info("Creating greenhouse $request")

        if(!greenHouseService.createGreenHouse(request.greenhouseId)) {
            return ResponseEntity.badRequest().body("Failed to create greenhouse")
        }
        replConfig.regenerateSingleModel().invoke("greenhouses")

        return ResponseEntity.ok("Greenhouse ${request.greenhouseId} created successfully")
    }

    @Operation(summary = "Retrieve all greenhouses")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the greenhouses"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping(produces = ["application/json"])
    fun getGreenHouses() : ResponseEntity<List<GreenHouse>> {
        log.info("Getting all greenhouses")

        val greenhouses = greenHouseService.getAllGreenHouses() ?: return ResponseEntity.noContent().build()

        log.info("Greenhouses: $greenhouses")

        return ResponseEntity.ok(greenhouses)
    }

    @Operation(summary = "Retrieve a greenhouse")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the greenhouse"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping("/{greenhouseId}", produces = ["application/json"])
    fun getGreenHouseById(@PathVariable greenhouseId: String) : ResponseEntity<GreenHouse> {
        log.info("Getting greenhouse by ID: $greenhouseId")

        val greenhouse = greenHouseService.getGreenHouseById(greenhouseId) ?: return ResponseEntity.notFound().build()

        log.info("Greenhouse: $greenhouse")

        return ResponseEntity.ok(greenhouse)
    }

    @Operation(summary = "Delete a greenhouse")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully deleted the greenhouse"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @DeleteMapping("/{greenhouseId}")
    fun deleteGreenHouse(@PathVariable greenhouseId: String) : ResponseEntity<Boolean> {
        log.info("Deleting greenhouse: $greenhouseId")

        if (!greenHouseService.deleteGreenHouse(greenhouseId)) {
            log.severe("Greenhouse not deleted")
            return ResponseEntity.badRequest().build()
        }

        log.info("Greenhouse deleted")
        replConfig.regenerateSingleModel().invoke("greenhouses")

        return ResponseEntity.ok(true)
    }
}
