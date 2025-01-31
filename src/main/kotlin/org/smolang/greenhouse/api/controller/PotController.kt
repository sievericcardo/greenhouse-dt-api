package org.smolang.greenhouse.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.uio.microobject.runtime.REPL
import org.apache.jena.query.QuerySolution
import org.apache.jena.query.ResultSet
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.Pot
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Logger
import org.smolang.greenhouse.api.types.CreatePotRequest
import org.smolang.greenhouse.api.types.UpdatePotRequest

@RestController
@RequestMapping("/api/pots")
class PotController (
    private val replConfig: REPLConfig
) {

    private val log : Logger = Logger.getLogger(PotController::class.java.name)

    @Operation(summary = "Retrieve the pots")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the pots"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping("/retrieve")
    fun getPots() : ResponseEntity<List<Pot>> {
        log.info("Getting all pots")

        val repl : REPL = replConfig.repl()

        val pots =
            """
             SELECT DISTINCT ?shelfFloor ?potPosition ?pumpId ?plantId WHERE {
                ?obj a prog:Pot ;
                    prog:Pot_shelfFloor ?shelfFloor ;
                    prog:Pot_potPosition ?potPosition ;
                    prog:Pot_pumpId ?pumpId ;
                    prog:Pot_plantId ?plantId .
             }"""

        val result : ResultSet = repl.interpreter!!.query(pots)!!
        val potsList = mutableListOf<Pot>()

        while (result.hasNext()) {
            val solution : QuerySolution = result.next()
            val shelfFloor = solution.get("?shelfFloor").asLiteral().toString()
            val potPosition = solution.get("?potPosition").asLiteral().toString()
            val pumpId = solution.get("?pumpId").asLiteral().toString()
            val plantId = solution.get("?plantId").asLiteral().toString()

            potsList.add(Pot(shelfFloor, potPosition, pumpId, plantId))
        }

        log.info("Pots: $potsList")

        return ResponseEntity.ok(potsList)
    }
}