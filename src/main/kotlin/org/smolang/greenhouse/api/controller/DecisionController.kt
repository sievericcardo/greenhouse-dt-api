package org.smolang.greenhouse.api.controller

import io.swagger.annotations.ApiParam
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.smolang.greenhouse.api.service.DecisionService
import org.smolang.greenhouse.api.service.PlantService
import org.smolang.greenhouse.api.service.WateringStrategyLoader
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/api/decision")
class DecisionController(
    private val decisionService: DecisionService,
    private val wateringStrategyLoader: WateringStrategyLoader,
    private val plantService: PlantService
) {

    private val log: Logger = LoggerFactory.getLogger(DecisionController::class.java.name)

    //    @Scheduled(fixedRate = 3600000) // 3600000 milliseconds = 60 minutes
//    @Scheduled(fixedRate = 3) // 3600000 milliseconds = 60 minutes
//    @Scheduled(cron = "*/5 * * * * *") // Execute every 5 second
//    @Operation(summary = "Make a decision every 6 hours")
    fun makeDecision() {
        log.info("Start decision process")
        decisionService.waterControl()
        log.info("End decision process")
    }

    @Operation(summary = "Set watering strategy")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully set watering strategy"),
            ApiResponse(responseCode = "400", description = "Invalid strategy name"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            )
        ]
    )
    @PostMapping("/strategy/{strategyName}")
    fun setWateringStrategy(
        @ApiParam(value = "Strategy name (e.g., default, conservative, aggressive)", required = true)
        @PathVariable strategyName: String
    ): ResponseEntity<Map<String, String>> {
        log.info("Setting watering strategy to: $strategyName")

        if (!wateringStrategyLoader.setActiveStrategy(strategyName)) {
            log.warn("Invalid strategy name: $strategyName")
            val availableStrategies = wateringStrategyLoader.getAllStrategies().keys.joinToString(", ")
            return ResponseEntity.badRequest()
                .body(
                    mapOf(
                        "error" to "Invalid strategy name",
                        "provided" to strategyName,
                        "available" to availableStrategies
                    )
                )
        }

        return ResponseEntity.ok(
            mapOf(
                "message" to "Watering strategy set successfully",
                "strategy" to strategyName
            )
        )
    }

    @Operation(summary = "Get available watering strategies")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved strategies"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            )
        ]
    )
    @GetMapping("/strategies")
    fun getAvailableStrategies(): ResponseEntity<Map<String, Any>> {
        log.info("Getting available watering strategies")

        val strategiesDetails = wateringStrategyLoader.getAllStrategyDetails()
        val activeStrategy = wateringStrategyLoader.getActiveStrategyName()

        return ResponseEntity.ok(
            mapOf(
                "strategies" to strategiesDetails,
                "activeStrategy" to activeStrategy
            )
        )
    }

    @Operation(summary = "Get watering strategies applied to plants from the current set strategy")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved plant with strategies"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            )
        ]
    )
    @GetMapping("/plant-strategies")
    fun getAvailablePlantStrategies(): ResponseEntity<Map<String, Any>> {
        log.info("Getting watering strategies for all plants")

        val plants = plantService.getAllPlants() ?: emptyList()
        val activeStrategyName = wateringStrategyLoader.getActiveStrategyName()
        val strategyDefinition = wateringStrategyLoader.getActiveStrategyDefinition()

        val plantStrategies = plants.map { plant ->
            val wateringDuration = wateringStrategyLoader.getWateringDurationForPlant(plant)
            PlantStrategyResponse(
                plantId = plant.plantId,
                familyName = plant.familyName,
                moistureState = plant.moistureState.name,
                wateringDuration = wateringDuration,
                strategyName = activeStrategyName
            )
        }

        return ResponseEntity.ok(
            mapOf(
                "activeStrategy" to activeStrategyName,
                "strategyDetails" to mapOf(
                    "name" to (strategyDefinition?.name ?: ""),
                    "description" to (strategyDefinition?.description ?: ""),
                    "durations" to mapOf(
                        "thirsty" to (strategyDefinition?.durations?.thirsty ?: 0),
                        "moist" to (strategyDefinition?.durations?.moist ?: 0),
                        "overwatered" to (strategyDefinition?.durations?.overwatered ?: 0),
                        "unknown" to (strategyDefinition?.durations?.unknown ?: 0)
                    )
                ),
                "plantStrategies" to plantStrategies
            )
        )
    }

    @Operation(summary = "Reload strategies from configuration file")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully reloaded configuration"),
            ApiResponse(responseCode = "500", description = "Failed to reload configuration"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            )
        ]
    )
    @PostMapping("/reload")
    fun reloadConfiguration(): ResponseEntity<Map<String, String>> {
        log.info("Reloading watering strategies configuration")

        if (!wateringStrategyLoader.loadConfiguration()) {
            return ResponseEntity.status(500)
                .body(mapOf("error" to "Failed to reload configuration"))
        }

        return ResponseEntity.ok(
            mapOf(
                "message" to "Configuration reloaded successfully",
                "activeStrategy" to wateringStrategyLoader.getActiveStrategyName()
            )
        )
    }

    @Operation(summary = "Add or update a custom watering strategy")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully added/updated strategy"),
            ApiResponse(responseCode = "400", description = "Invalid request"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            )
        ]
    )
    @PostMapping("/strategies")
    fun addOrUpdateStrategy(
        @SwaggerRequestBody(description = "Strategy configuration")
        @RequestBody strategyRequest: StrategyRequest
    ): ResponseEntity<Map<String, String>> {
        log.info("Adding/updating strategy: ${strategyRequest.key}")

        if (!wateringStrategyLoader.addOrUpdateStrategy(
                strategyRequest.key,
                strategyRequest.name,
                strategyRequest.description,
                strategyRequest.durations.thirsty,
                strategyRequest.durations.moist,
                strategyRequest.durations.overwatered,
                strategyRequest.durations.unknown
            )
        ) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Failed to add/update strategy"))
        }

        return ResponseEntity.ok(
            mapOf(
                "message" to "Strategy added/updated successfully",
                "key" to strategyRequest.key
            )
        )
    }

    @Operation(summary = "Delete a custom watering strategy")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully deleted strategy"),
            ApiResponse(responseCode = "400", description = "Cannot delete strategy"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            )
        ]
    )
    @PostMapping("/strategies/{strategyKey}/delete")
    fun deleteStrategy(
        @ApiParam(value = "Strategy key to delete", required = true)
        @PathVariable strategyKey: String
    ): ResponseEntity<Map<String, String>> {
        log.info("Deleting strategy: $strategyKey")

        if (!wateringStrategyLoader.deleteStrategy(strategyKey)) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Cannot delete strategy (may be active or not found)"))
        }

        return ResponseEntity.ok(
            mapOf(
                "message" to "Strategy deleted successfully",
                "key" to strategyKey
            )
        )
    }
}

data class PlantStrategyResponse(
    val plantId: String,
    val familyName: String,
    val moistureState: String,
    val wateringDuration: Int,
    val strategyName: String
)

data class StrategyRequest(
    val key: String,
    val name: String,
    val description: String,
    val durations: DurationsRequest
)

data class DurationsRequest(
    val thirsty: Int,
    val moist: Int,
    val overwatered: Int,
    val unknown: Int
)