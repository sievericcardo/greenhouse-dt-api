package org.smolang.greenhouse.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.WaterBucket
import org.smolang.greenhouse.api.service.WaterBucketService
import org.smolang.greenhouse.api.types.CreateWaterBucketRequest
import org.smolang.greenhouse.api.types.UpdateWaterBucketRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/api/waterbuckets")
class WaterBucketController (
    private val replConfig: REPLConfig,
    private val waterBucketService: WaterBucketService
) {

    private val log: Logger = LoggerFactory.getLogger(WaterBucketController::class.java.name)

    @Operation(summary = "Create a new water bucket")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Water bucket created"),
        ApiResponse(responseCode = "400", description = "Invalid water bucket"),
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "500", description = "Internal server error")
    ])
    @PostMapping(produces = ["application/json"])
    fun createWaterBucket(@SwaggerRequestBody(description = "Request to add a new water bucket") @RequestBody request: CreateWaterBucketRequest) : ResponseEntity<String> {
        log.info("Creating water bucket $request")

        if(!waterBucketService.createWaterBucket(request.bucketId)) {
            return ResponseEntity.badRequest().body("Failed to create water bucket")
        }
        replConfig.regenerateSingleModel().invoke("waterbuckets")

        return ResponseEntity.ok("Water bucket ${request.bucketId} created successfully")
    }

    @Operation(summary = "Retrieve all water buckets")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the water buckets"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping(produces = ["application/json"])
    fun getWaterBuckets() : ResponseEntity<List<WaterBucket>> {
        log.info("Getting all water buckets")

        val waterBuckets = waterBucketService.getAllWaterBuckets() ?: return ResponseEntity.noContent().build()

        log.info("Water buckets: $waterBuckets")

        return ResponseEntity.ok(waterBuckets)
    }

    @Operation(summary = "Retrieve a water bucket")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully retrieved the water bucket"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @GetMapping("/{bucketId}", produces = ["application/json"])
    fun getWaterBucketById(@PathVariable bucketId: String) : ResponseEntity<WaterBucket> {
        log.info("Getting water bucket by ID: $bucketId")

        val waterBucket = waterBucketService.getWaterBucketById(bucketId) ?: return ResponseEntity.notFound().build()

        log.info("Water bucket: $waterBucket")

        return ResponseEntity.ok(waterBucket)
    }

    @Operation(summary = "Update a water bucket")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully updated the water bucket"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @PatchMapping("/{bucketId}", produces = ["application/json"])
    fun updateWaterBucket(@PathVariable bucketId: String,
                          @SwaggerRequestBody(description = "Water bucket to be updated") @RequestBody updateRequest: UpdateWaterBucketRequest) : ResponseEntity<WaterBucket> {
        return ResponseEntity.notFound().build()
//        log.info("Updating water bucket: $bucketId")
//
//        if (waterBucketService.updateWaterBucket(bucketId, 0.0) != null) {
//            log.error("Water bucket not updated")
//            return ResponseEntity.badRequest().build()
//        }
//
//        log.info("Water bucket updated")
//        replConfig.regenerateSingleModel().invoke("waterbuckets")
//
//        val updatedWaterBucket = waterBucketService.getWaterBucketById(bucketId) ?: return ResponseEntity.notFound().build()
//
//        return ResponseEntity.ok(updatedWaterBucket)
    }

    @Operation(summary = "Delete a water bucket")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Successfully deleted the water bucket"),
        ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
        ApiResponse(responseCode = "403", description = "Accessing the resource you were trying to reach is forbidden"),
        ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
    ])
    @DeleteMapping("/{bucketId}")
    fun deleteWaterBucket(@PathVariable bucketId: String) : ResponseEntity<Boolean> {
        log.info("Deleting water bucket: $bucketId")

        if (!waterBucketService.deleteWaterBucket(bucketId)) {
            log.error("Water bucket not deleted")
            return ResponseEntity.badRequest().build()
        }

        log.info("Water bucket deleted")
        replConfig.regenerateSingleModel().invoke("waterbuckets")

        return ResponseEntity.ok(true)
    }
}
