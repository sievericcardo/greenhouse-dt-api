package org.smolang.greenhouse.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.uio.microobject.runtime.REPL
import org.apache.jena.query.QuerySolution
import org.apache.jena.query.ResultSet
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.Plant
import org.smolang.greenhouse.api.service.PlantService
import org.smolang.greenhouse.api.types.PlantState
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Logger

@RestController
@RequestMapping("/api/plants")
class PlantController (
    private val replConfig: REPLConfig,
    private val plantService: PlantService
) {

    private val log : Logger = Logger.getLogger(PlantController::class.java.name)

    @Operation(summary = "Retrieve the plants")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the plants"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping("/all")
    fun getPlants() : ResponseEntity<List<Plant>> {
        log.info("Getting all plants")
        val plants = mutableListOf<Plant>()

        val healthyPlants = plantService.getHealthyPlants() ?: emptyList()
        val unhealthyPlants = plantService.getUnhealthyPlants() ?: emptyList()
        val deadPlants = plantService.getDeadPlants() ?: emptyList()

        plants.addAll(healthyPlants)
        plants.addAll(unhealthyPlants)
        plants.addAll(deadPlants)

        log.info("Plants: $plants")

        return ResponseEntity.ok(plants)
    }

    @PostMapping("/update-model")
    fun updateModel() : ResponseEntity<String> {
        log.info("Updating the model")

        val repl : REPL = replConfig.repl()
        replConfig.reclassifySingleModel().invoke("plants")

        return ResponseEntity.ok("Model updated")
    }
}