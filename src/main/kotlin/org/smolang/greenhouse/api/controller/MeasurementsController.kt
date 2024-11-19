package org.smolang.greenhouse.api.controller

import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.logging.Logger

@RestController
@RequestMapping("/api/measurements")
class MeasurementsController {

    private val log: Logger = Logger.getLogger(MeasurementsController::class.java.name)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Operation(summary = "Retrieve the measurements for the plants")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the measurements"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping("/plants")
    fun getPlantsMeasurements() : ResponseEntity<Map<String, List<Pair<Instant, Double>>>> = runBlocking {
        log.info("Getting all plants measurements")

        val influxUrl = System.getenv().getOrDefault("INFLUX_URL", "http://localhost:8086")
        val influxToken = System.getenv().getOrDefault("INFLUX_TOKEN", "my-token")
        val influxBucket = System.getenv().getOrDefault("INFLUX_BUCKET", "GreenHouse")
        val influxOrg = System.getenv().getOrDefault("INFLUX_ORG", "UiO")

        val fluxQuery = """
            from(bucket: "${influxBucket}")
              |> range(start: -1h)
              |> filter(fn: (r) => r["_measurement"] == "ast:pot")
              |> filter(fn: (r) => r["_field"] == "moisture")
              |> yield(name: "mean")
              |> keep(columns: ["_time", "_value", "plant_id"])
        """

        val influxDBClient = InfluxDBClientKotlinFactory
            .create(influxUrl, influxToken.toCharArray(), influxOrg)

        val results = influxDBClient.getQueryKotlinApi().query(fluxQuery)
        val plantsMeasurements = mutableMapOf<String, List<Pair<Instant, Double>>>()

        results.consumeEach { record ->
            val measurementTime = record.getValueByKey("_time") as Instant
            val plantId = record.getValueByKey("plant_id") as String
            val moisture = record.getValueByKey("_value") as Double
            plantsMeasurements[plantId] = plantsMeasurements.getOrDefault(plantId, listOf()) + listOf(Pair(measurementTime, moisture))
        }

        influxDBClient.close()
        log.info("Plants measurements: $plantsMeasurements")

        return@runBlocking ResponseEntity.ok(plantsMeasurements)
    }
}