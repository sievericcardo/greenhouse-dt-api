package org.smolang.greenhouse.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.uio.microobject.runtime.REPL
import org.apache.jena.query.QuerySolution
import org.apache.jena.query.ResultSet
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.Plant
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Logger

@RestController
@RequestMapping("/api/plants")
class PlantController (
    val replConfig: REPLConfig
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

        val repl : REPL = replConfig.repl()

        val plants =
            """
             SELECT * WHERE {
                ?obj a prog:Plant ;
                    prog:Plant_plantId ?plantId ;
                    prog:Plant_idealMoisture ?idealMoisture ;
                    prog:Plant_healthState ?healthState .
             }"""

        val result : ResultSet = repl.interpreter!!.query(plants)!!
        val plantsList = mutableListOf<Plant>()

        while (result.hasNext()) {
            val solution : QuerySolution = result.next()
            val plantId = solution.get("?plantId").asLiteral().toString()
            val idealMoisture = solution.get("?idealMoisture").asLiteral().toString().split("^^")[0].toDouble()
            val healthState = solution.get("?healthState").asLiteral().toString()

            plantsList.add(Plant(plantId, idealMoisture, healthState))
        }

        log.info("Plants: $plantsList")

        return ResponseEntity.ok(plantsList)
    }
}