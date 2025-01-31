package org.smolang.greenhouse.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody
import no.uio.microobject.runtime.REPL
import org.apache.jena.query.QuerySolution
import org.apache.jena.query.ResultSet
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.Plant
import org.smolang.greenhouse.api.service.PlantService
import org.springframework.http.ResponseEntity
import java.util.logging.Logger
import org.smolang.greenhouse.api.types.CreatePlantRequest
import org.smolang.greenhouse.api.types.UpdatePlantRequest
import org.smolang.greenhouse.api.types.DeletePlantRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/plants")
class PlantController (
    private val replConfig: REPLConfig,
    private val plantService: PlantService
) {

    private val log : Logger = Logger.getLogger(PlantController::class.java.name)

    @Operation(summary = "Create a plant")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully created the plant"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @PostMapping("/create")
    fun createPlant(@SwaggerRequestBody(description = "Plant to be created") @RequestBody createPlantRequest: CreatePlantRequest) : ResponseEntity<Boolean> {
        log.info("Creating a plant")

        val plant = Plant(
            createPlantRequest.plantId,
            createPlantRequest.idealMoisture,
            0.0,
            "bad"
        )

        if (!plantService.createPlant(plant)) {
            log.severe("Plant not created", )
            return ResponseEntity.badRequest().build()
        }

        log.info("Plant created")

        return ResponseEntity.ok(true)
    }

    @Operation(summary = "Retrieve the plants")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the plants"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping("/retrieve")
    fun getPlants() : ResponseEntity<List<Plant>> {
        log.info("Getting all plants")

        val plantsList = plantService.getAllPlants() ?: return ResponseEntity.notFound().build()

        log.info("Plants: $plantsList")

        return ResponseEntity.ok(plantsList)
    }

    @Operation(summary = "Update a plant")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully updated the plant"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @PatchMapping("/update")
    fun updatePlant(@SwaggerRequestBody(description = "Plant to be updated") @RequestBody updatePlantRequest: UpdatePlantRequest) : ResponseEntity<Boolean> {
        log.info("Updating a plant")

        val plant = plantService.getPlantByPlantId(updatePlantRequest.plantId) ?: return ResponseEntity.notFound().build()
        val moisture = updatePlantRequest.idealMoistureNew ?: plant.idealMoisture

        if (!plantService.updatePlant(plant, moisture)) {
            log.severe("Plant not updated")
            return ResponseEntity.badRequest().build()
        }

        log.info("Plant updated")

        return ResponseEntity.ok(true)
    }

    @Operation(summary = "Delete a plant")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully deleted the plant"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @DeleteMapping("/delete")
    fun deletePlant(@SwaggerRequestBody(description = "Plant to be deleted") @RequestBody deletePlantRequest: DeletePlantRequest) : ResponseEntity<Boolean> {
        log.info("Deleting a plant")

        if (!plantService.deletePlant(deletePlantRequest.plantId)) {
            log.severe("Plant not deleted")
            return ResponseEntity.badRequest().build()
        }

        log.info("Plant deleted")

        return ResponseEntity.ok(true)
    }
}