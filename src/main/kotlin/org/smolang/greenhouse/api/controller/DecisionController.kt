package org.smolang.greenhouse.api.controller

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.smolang.greenhouse.api.service.DecisionService
import java.util.logging.Logger
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/decision")
class DecisionController (
    private val decisionService: DecisionService
) {

    private val log: Logger = Logger.getLogger(DecisionController::class.java.name)

//    @Scheduled(fixedRate = 3600000) // 3600000 milliseconds = 60 minutes
//    @Scheduled(fixedRate = 3) // 3600000 milliseconds = 60 minutes
    @Scheduled(cron = "*/5 * * * * *") // Execute every 5 second
    @Operation(summary = "Make a decision every 6 hours")
    fun makeDecision() {
        log.info( "Start decision process")
        val plantsToWater = decisionService.execSmol()
        decisionService.waterControl(plantsToWater)
        log.info( "End decision process")
    }
}