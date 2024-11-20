package org.smolang.greenhouse.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.smolang.greenhouse.api.config.EnvironmentConfig
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Logger

@RestController
@RequestMapping("/api")
class HomeController (
    private val environment: EnvironmentConfig
) {

    private val log : Logger = Logger.getLogger(HomeController::class.java.name)

    @GetMapping("/status")
    fun status(): String {
        log.info("Application is running")
        return "Application is running"
    }

    @Operation(summary = "Set the application in demo mode")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully set the application in demo mode")
    ])
    @PostMapping("/set-demo")
    fun setDemoMode(): ResponseEntity<String> {
        val currentDemoMode = environment.getOrDefault("DEMO", "false").toBoolean()
        val value = if (currentDemoMode) "demo" else "live"
        val newValue = if (!currentDemoMode) "live" else "demo"
        log.info("Setting the application in ${value} mode")

        environment.set("DEMO", (!currentDemoMode).toString())
        return ResponseEntity.ok("Application is now in ${newValue} mode")
    }
}