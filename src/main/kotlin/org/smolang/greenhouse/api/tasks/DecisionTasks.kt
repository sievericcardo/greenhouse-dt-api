package org.smolang.greenhouse.api.tasks

import io.swagger.v3.oas.annotations.Operation
import org.smolang.greenhouse.api.service.DecisionService
import org.smolang.greenhouse.api.service.MessagePublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
class DecisionTasks (
    private val decisionService: DecisionService,
    private val messagePublisher: MessagePublisher,
) {

    private val log: Logger = Logger.getLogger(DecisionTasks::class.java.name)

    //    @Scheduled(fixedRate = 3600000) // 3600000 milliseconds = 60 minutes
//    @Scheduled(fixedRate = 3) // 3600000 milliseconds = 60 minutes
    @Scheduled(cron = "0 0/3 * * * ?") // Execute every 3 minutes for testing purposes
//    @Scheduled(cron = "0 0 */6 * * *") // Execute every 6 hours
    @Operation(summary = "Make a decision every 6 hours")
    fun makeDecision() {
        log.info( "Start decision process")
        decisionService.waterControl()
        log.info( "End decision process")
    }

    // Schedule every 3 minutes
//    @Scheduled(cron = "0 0/3 * * * ?") // Execute every 3 minutes
    @Operation(summary = "Make a decision every 6 hours")
    fun promptQueue() {
        log.info( "Alive message")
        messagePublisher.publish("controller.keepalive","SMOL Scheduler is alive: ${System.currentTimeMillis()}")
        log.info( "End message")
    }

//    @Scheduled(cron = "* * * * * *") // Execute every 3 minutes
    @Operation(summary = "Make a decision every 6 hours")
    fun spamMsg() {
        log.info( "Alive message")
        for (i in 1..4) {
            log.info( "Sending message to actuator.$i.water")
            messagePublisher.publish("actuator.$i.water","[WATER]18 2")
        }
        // wait 3 hours
        Thread.sleep(1000 * 60 * 3) // 3 minutes
        log.info( "End message")
    }
}