package org.smolang.greenhouse.api.controller

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import com.influxdb.client.write.Point
import com.opencsv.CSVReader
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.smolang.greenhouse.api.config.EnvironmentConfig
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.FileReader
import java.time.Instant

@RestController
@RequestMapping("/api/measurements")
class MeasurementsController(
    private val environment: EnvironmentConfig
) {

    private val log: Logger = LoggerFactory.getLogger(MeasurementsController::class.java.name)
    private val influxHost = environment.getOrDefault("INFLUX_URL", "localhost")
    private val influxUrl = "http://$influxHost:8086"
    private val influxToken = environment.getOrDefault("INFLUX_TOKEN", "my-token")
    private val influxBucket = environment.getOrDefault("INFLUX_BUCKET", "GreenHouse")
    private val influxOrg = environment.getOrDefault("INFLUX_ORG", "UiO")

    @OptIn(ExperimentalCoroutinesApi::class)
    @Operation(summary = "Retrieve the measurements for the plants")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved the measurements"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            ),
            ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
        ]
    )
    @GetMapping("/plants")
    fun getPlantsMeasurements(): ResponseEntity<Map<String, List<Pair<Instant, Double>>>> = runBlocking {
        log.info("Getting all plants measurements")

        val demoVar = environment.getOrDefault("DEMO", "false")
        val demo: Boolean = demoVar.equals("true", ignoreCase = true)

        var fluxQuery = ""
        if (!demo) {
            fluxQuery = """
                from(bucket: "${influxBucket}")
                  |> range(start: -1h)
                  |> filter(fn: (r) => r["_measurement"] == "ast:pot")
                  |> filter(fn: (r) => r["_field"] == "moisture")
                  |> yield(name: "mean")
                  |> keep(columns: ["_time", "_value", "plant_id"])
            """
        } else {
//            fluxQuery = """
//                from(bucket: "${influxBucket}")
//                  |> range(start: 2024-10-28T21:00:00Z, stop: 2024-10-28T22:00:00Z)
//                  |> filter(fn: (r) => r["_measurement"] == "ast:pot")
//                  |> filter(fn: (r) => r["_field"] == "moisture")
//                  |> yield(name: "mean")
//                  |> keep(columns: ["_time", "_value", "plant_id"])
//               """
            fluxQuery = """
                from(bucket: "${influxBucket}")
                  |> range(start: 2024-11-12T17:00:00Z, stop: 2024-11-12T18:00:00Z)
                  |> filter(fn: (r) => r["_measurement"] == "ast:pot")
                  |> filter(fn: (r) => r["_field"] == "moisture")
                  |> yield(name: "mean")
                  |> keep(columns: ["_time", "_value", "plant_id"])
               """
        }

        val influxDBClient = InfluxDBClientKotlinFactory
            .create(influxUrl, influxToken.toCharArray(), influxOrg)

        val results = influxDBClient.getQueryKotlinApi().query(fluxQuery)
        val plantsMeasurements = mutableMapOf<String, MutableList<Pair<Instant, Double>>>()

        results.consumeEach { record ->
            val measurementTime = Instant.parse(record.getValueByKey("_time").toString())
            val plantId = record.getValueByKey("plant_id").toString()
            val moisture = record.getValueByKey("_value").toString().toDouble()
            val measurement = Pair(measurementTime, moisture)

            plantsMeasurements.computeIfAbsent(plantId) { mutableListOf() }.apply {
                if (!contains(measurement)) add(measurement)
            }
        }

        // Convert the lists before returning the response
        val plantsMeasurementsList = plantsMeasurements.mapValues { it.value.toList() }

        influxDBClient.close()
        log.info("Plants measurements: $plantsMeasurements")

        return@runBlocking ResponseEntity.ok(plantsMeasurementsList)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Operation(summary = "Retrieve the measurements for the shelf")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved the measurements"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            ),
            ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
        ]
    )
    @GetMapping("/shelves")
    fun getShelfMeasurements(): ResponseEntity<Map<String, Map<String, List<Pair<Instant, Double>>>>> = runBlocking {
        log.info("Getting all plants measurements")

        val demoVar = environment.getOrDefault("DEMO", "false")
        val demo: Boolean = demoVar.equals("true", ignoreCase = true)

        var fluxQuery = ""
        if (!demo) {
            fluxQuery = """
                from(bucket: "${influxBucket}")
                  |> range(start: -1h)
                  |> filter(fn: (r) => r["_measurement"] == "ast:shelf")
                  |> filter(fn: (r) => r["_field"] == "humidity" or r["_field"] == "temperature")
                  |> yield(name: "mean")
                  |> keep(columns: ["_time", "_value", "_field", "shelf_floor"])
            """
        } else {
//            fluxQuery = """
//                from(bucket: "${influxBucket}")
//                  |> range(start: 2024-10-28T21:00:00Z, stop: 2024-10-28T22:00:00Z)
//                  |> filter(fn: (r) => r["_measurement"] == "ast:shelf")
//                  |> filter(fn: (r) => r["_field"] == "humidity" or r["_field"] == "temperature")
//                  |> yield(name: "mean")
//                  |> keep(columns: ["_time", "_value", "_field", "shelf_floor"])
//               """
            fluxQuery = """
                from(bucket: "${influxBucket}")
                  |> range(start: 2024-11-12T17:00:00Z, stop: 2024-11-12T18:00:00Z)
                  |> filter(fn: (r) => r["_measurement"] == "ast:shelf")
                  |> filter(fn: (r) => r["_field"] == "humidity" or r["_field"] == "temperature")
                  |> yield(name: "mean")
                  |> keep(columns: ["_time", "_value", "_field", "shelf_floor"])
               """
        }

        val influxDBClient = InfluxDBClientKotlinFactory
            .create(influxUrl, influxToken.toCharArray(), influxOrg)

        val results = influxDBClient.getQueryKotlinApi().query(fluxQuery)
        val shelfMeasurements = mutableMapOf<String, Map<String, List<Pair<Instant, Double>>>>()
        val singleMeasurements = mutableMapOf<String, List<Pair<Instant, Double>>>()

        results.consumeEach { record ->
            val measurementTime = record.getValueByKey("_time") as Instant
            val shelfId = record.getValueByKey("shelf_floor") as String
            val measurementType = record.getValueByKey("_field") as String
            val moisture = record.getValueByKey("_value") as Double
            singleMeasurements[measurementType] = singleMeasurements.getOrDefault(measurementType, listOf()) + listOf(
                Pair(measurementTime, moisture)
            )
            shelfMeasurements[shelfId] = singleMeasurements
        }

        influxDBClient.close()
        log.info("Plants measurements: $shelfMeasurements")

        return@runBlocking ResponseEntity.ok(shelfMeasurements)
    }

    @Operation(summary = "Inject the demo measurements for the plants")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved the measurements"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            ),
            ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
        ]
    )
    @PostMapping("/inject-demo-data")
    fun injectDemoData(): ResponseEntity<String> = runBlocking {
        // Insert all the data from the csv file
        val filePath = "src/main/resources/demo-data.csv"
        val influxHost = environment.getOrDefault("INFLUX_URL", "localhost")
        val influxUrl = "http://$influxHost:8086"
        val influxToken = environment.getOrDefault("INFLUX_TOKEN", "my-token")
        val influxBucket = environment.getOrDefault("INFLUX_BUCKET", "GreenHouse")
        val influxOrg = environment.getOrDefault("INFLUX_ORG", "UiO")

        val influxDBClient = InfluxDBClientKotlinFactory
            .create(influxUrl, influxToken.toCharArray(), influxOrg)
        val writeApi = influxDBClient.getWriteKotlinApi()

        val csvReader = CSVReader(FileReader(filePath))

        try {
            val headers = csvReader.readNext() // First row contains headers
            val rows = csvReader.readAll()

            rows.forEach { row ->
                val point = Point.measurement("_measurement")

                headers.forEachIndexed { index, column ->
                    when (column.lowercase()) {
                        "_time", "_start", "_stop" -> {
                            // Convert timestamp fields to Instant
                            val timeValue = Instant.parse(row[index]) // Adjust format if needed
                            point.time(timeValue, WritePrecision.NS)
                        }

                        "_field" -> point.addField(column, row[index].toDouble())
                        "_value" -> point.addField(column, row[index].toDouble())
                        "group_position" -> point.addTag(column, row[index].toString())
                        "plant_id" -> point.addTag(column, row[index].toString())
                        "pot_position" -> point.addTag(column, row[index].toString())
                        "shelf_floor" -> point.addTag(column, row[index].toString())
                        else -> point.addTag(column, row[index]) // Assume non-numeric values are tags
                    }
                }

                // Write the point to InfluxDB
                writeApi.writePoint(point, influxBucket, influxOrg)
            }
        } finally {
            csvReader.close()
            environment.set("DEMO", "true")
        }

        return@runBlocking ResponseEntity.ok("Data injected")
    }
}