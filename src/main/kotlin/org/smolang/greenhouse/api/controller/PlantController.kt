package org.smolang.greenhouse.api.controller

import io.swagger.annotations.ApiParam
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.Plant
import org.smolang.greenhouse.api.service.PlantService
import org.smolang.greenhouse.api.service.PotService
import org.smolang.greenhouse.api.types.CreatePlantRequest
import org.smolang.greenhouse.api.types.PlantMoistureState
import org.smolang.greenhouse.api.types.UpdatePlantRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/api/plants")
class PlantController(
    private val replConfig: REPLConfig,
    private val plantService: PlantService,
    private val potService: PotService
) {

    private val log: Logger = LoggerFactory.getLogger(PlantController::class.java.name)

    @Operation(summary = "Create a plant")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully created the plant"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            ),
            ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
        ]
    )
    @PostMapping(produces = ["application/json"])
    fun createPlant(@SwaggerRequestBody(description = "Plant to be created") @RequestBody createPlantRequest: CreatePlantRequest): ResponseEntity<Boolean> {
        log.info("Creating a plant")

        val pot = potService.getPotByPotId(createPlantRequest.potId) ?: run {
            log.error("Pot with ID ${createPlantRequest.potId} not found")
            return ResponseEntity.badRequest().build()
        }

        val plant = Plant(
            createPlantRequest.plantId,
            createPlantRequest.familyName,
            pot,
            null,
            null,
            createPlantRequest.status,
            PlantMoistureState.UNKNOWN
        )

        if (!plantService.createPlant(plant)) {
            log.error("Plant not created")
            return ResponseEntity.badRequest().build()
        }

        log.info("Plant created")
        replConfig.regenerateSingleModel().invoke("plants")

        return ResponseEntity.ok(true)
    }

    @Operation(summary = "Retrieve the plants")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved the plants"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            ),
            ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
        ]
    )
    @GetMapping(produces = ["application/json"])
    fun getPlants(): ResponseEntity<List<Plant>> {
        log.info("Getting all plants")

        val plantsList = plantService.getAllPlants() ?: return ResponseEntity.notFound().build()

        log.info("Plants: $plantsList")

        return ResponseEntity.ok(plantsList)
    }

    @Operation(summary = "Retrieve a plant")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved the plant"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            ),
            ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
        ]
    )
    @GetMapping("/{plantId}", produces = ["application/json"])
    fun getPlant(
        @ApiParam(
            value = "Plant ID",
            required = true
        ) @Valid @PathVariable plantId: String
    ): ResponseEntity<Plant> {
        log.info("Getting plant with ID $plantId")
        val plant = plantService.getPlantByPlantId(plantId) ?: return ResponseEntity.notFound().build()
        log.info("Plant: $plant")

        return ResponseEntity.ok(plant)
    }

    @Operation(summary = "Update a plant")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully updated the plant"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            ),
            ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
        ]
    )
    @PatchMapping("/{plantId}", produces = ["application/json"])
    fun updatePlant(
        @ApiParam(value = "Plant ID", required = true) @Valid @PathVariable plantId: String,
        @SwaggerRequestBody(description = "Plant to be updated") @RequestBody updatePlantRequest: UpdatePlantRequest
    ): ResponseEntity<Plant> {
        log.info("Updating a plant")

        val plant = plantService.getPlantByPlantId(plantId) ?: return ResponseEntity.notFound().build()

        if (plant.moisture == null || plant.healthState == null) {
            log.error("Plant moisture or health state is null")
            return ResponseEntity.badRequest().build()
        }

        if (!plantService.updatePlant(plant, plant.moisture, plant.healthState, updatePlantRequest.newStatus)) {
            log.error("Plant not updated")
            return ResponseEntity.badRequest().build()
        }

        log.info("Plant updated")
        replConfig.regenerateSingleModel().invoke("plants")
        val updatedPlant = plantService.getPlantByPlantId(plantId) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(updatedPlant)
    }

    @Operation(summary = "Update the plant model")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully updated the plant model"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            ),
            ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
        ]
    )
    @PatchMapping(produces = ["application/json"])
    fun updatePlantModel(): ResponseEntity<String> {
        log.info("Updating a plant model")

        replConfig.reclassifySingleModel().invoke("plants")

        return ResponseEntity.ok("Plant model updated")
    }

    @Operation(summary = "Delete a plant")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully deleted the plant"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            ),
            ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
        ]
    )
    @DeleteMapping(produces = ["application/json"])
    fun deletePlant(
        @ApiParam(
            value = "Plant ID",
            required = true
        ) @Valid @PathVariable plantId: String
    ): ResponseEntity<Boolean> {
        log.info("Deleting a plant")

        if (!plantService.deletePlant(plantId)) {
            log.error("Plant not deleted")
            return ResponseEntity.badRequest().build()
        }

        log.info("Plant deleted")
        replConfig.regenerateSingleModel().invoke("plants")

        return ResponseEntity.ok(true)
    }
}