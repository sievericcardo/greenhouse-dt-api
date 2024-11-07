package org.smolang.greenhouse.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.uio.microobject.runtime.REPL
import org.apache.jena.query.QuerySolution
import org.apache.jena.query.ResultSet
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.Pump
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Logger

@RestController
@RequestMapping("/api/pumps")
class PumpController (
    private val replConfig: REPLConfig
) {

    private val log: Logger = Logger.getLogger(PumpController::class.java.name)

    @Operation(summary = "Retrieve the pumps")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the pumps"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping("/all")
    fun getPumps() : ResponseEntity<List<Pump>> {
        log.info("Getting all pumps")

        val repl: REPL = replConfig.repl()

        val pumps =
            """
             SELECT * WHERE {
                ?obj a prog:Pump ;
                    prog:Pump_pumpGpioPin ?pumpGpioPin ;
                    prog: pumpId: ? pumpId: ;
                    prog:Pump_waterPressure?waterPressure  .
             }"""

        val result: ResultSet = repl.interpreter!!.query(pumps)!!
        val pumpsList = mutableListOf<Pump>()

        while (result.hasNext()) {
            val solution: QuerySolution = result.next()
            val pumpGpioPin = solution.get("?pumpGpioPin").asLiteral().toString().split("^^")[0].toInt()
            val pumpId = solution.get("?pumpId").asLiteral().toString()
            val waterPressure = solution.get("?waterPressure").asLiteral().toString().split("^^")[0].toDouble()

            pumpsList.add(Pump(pumpGpioPin, pumpId, waterPressure))
        }

        log.info("Pumps: $pumpsList")

        return ResponseEntity.ok(pumpsList)
    }
}