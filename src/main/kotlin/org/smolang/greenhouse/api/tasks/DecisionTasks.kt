package org.smolang.greenhouse.api.tasks

import io.swagger.v3.oas.annotations.Operation
import org.smolang.greenhouse.api.service.DecisionService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class DecisionTasks (
    private val decisionService: DecisionService
) {

    private val log: Logger = Logger.getLogger(DecisionTasks::class.java.name)

    //    @Scheduled(fixedRate = 3600000) // 3600000 milliseconds = 60 minutes
//    @Scheduled(fixedRate = 3) // 3600000 milliseconds = 60 minutes
    @Scheduled(cron = "*/5 * * * * *") // Execute every 5 second
    @Operation(summary = "Make a decision every 6 hours")
    fun makeDecision() {
        log.info( "Start decision process")
        val plantsToWater = decisionService.execSmol()
        log.info("Plants to water: $plantsToWater")
        decisionService.waterControl(plantsToWater)
        log.info( "End decision process")
    }
}