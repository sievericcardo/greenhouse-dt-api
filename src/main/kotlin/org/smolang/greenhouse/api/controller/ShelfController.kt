package org.smolang.greenhouse.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.uio.microobject.runtime.REPL
import org.apache.jena.query.QuerySolution
import org.apache.jena.query.ResultSet
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.Shelf
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Logger

@RestController
@RequestMapping("/api/shelves")
class ShelfController (
    private val replConfig: REPLConfig
) {

    private val log: Logger = Logger.getLogger(ShelfController::class.java.name)

    @Operation(summary = "Retrieve the shelves")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the shelves"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping("/all")
    fun getShelves() : ResponseEntity<List<Shelf>> {
        log.info("Getting all shelves")

        val repl: REPL = replConfig.repl()

        val shelves =
            """
             SELECT * WHERE {
                ?obj a prog:Shelf ;
                    prog:Shelf_shelfFloor ?shelfFloor  .
             }"""

        val result: ResultSet = repl.interpreter!!.query(shelves)!!
        val shelvesList = mutableListOf<Shelf>()

        while (result.hasNext()) {
            val solution: QuerySolution = result.next()
            val shelfFloor = solution.get("?shelfFloor").asLiteral().toString()

            shelvesList.add(Shelf(shelfFloor))
        }

        log.info("Shelves: $shelvesList")

        return ResponseEntity.ok(shelvesList)
    }
}