package org.smolang.greenhouse.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.model.Section
import org.smolang.greenhouse.api.service.SectionService
import org.smolang.greenhouse.api.types.CreateSectionRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@RequestMapping("/api/sections")
class SectionController(
    private val replConfig: REPLConfig,
    private val sectionService: SectionService
) {

    private val log: Logger = LoggerFactory.getLogger(SectionController::class.java.name)

    @Operation(summary = "Create a new section")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Section created"),
            ApiResponse(responseCode = "400", description = "Invalid section"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            ),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    @PostMapping(produces = ["application/json"])
    fun createSection(@SwaggerRequestBody(description = "Request to add a new section") @RequestBody request: CreateSectionRequest): ResponseEntity<String> {
        log.info("Creating section $request")

        if (sectionService.createSection(request.sectionId) == null) {
            return ResponseEntity.badRequest().body("Failed to create section")
        }
        replConfig.regenerateSingleModel().invoke("sections")

        return ResponseEntity.ok("Section ${request.sectionId} created successfully")
    }

    @Operation(summary = "Retrieve all sections")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved the sections"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            ),
            ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
        ]
    )
    @GetMapping(produces = ["application/json"])
    fun getSections(): ResponseEntity<List<Section>> {
        log.info("Getting all sections")

        val sections = sectionService.getAllSections() ?: return ResponseEntity.noContent().build()

        log.info("Sections: $sections")

        return ResponseEntity.ok(sections)
    }

    @Operation(summary = "Retrieve a section")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved the section"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            ),
            ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
        ]
    )
    @GetMapping("/{sectionId}", produces = ["application/json"])
    fun getSectionById(@PathVariable sectionId: String): ResponseEntity<Section> {
        log.info("Getting section by ID: $sectionId")

        val section = sectionService.getSectionById(sectionId) ?: return ResponseEntity.notFound().build()

        log.info("Section: $section")

        return ResponseEntity.ok(section)
    }

    @Operation(summary = "Delete a section")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully deleted the section"),
            ApiResponse(responseCode = "401", description = "You are not authorized to view the resource"),
            ApiResponse(
                responseCode = "403",
                description = "Accessing the resource you were trying to reach is forbidden"
            ),
            ApiResponse(responseCode = "404", description = "The resource you were trying to reach is not found")
        ]
    )
    @DeleteMapping("/{sectionId}")
    fun deleteSection(@PathVariable sectionId: String): ResponseEntity<Boolean> {
        log.info("Deleting section: $sectionId")

        if (!sectionService.deleteSection(sectionId)) {
            log.error("Section not deleted")
            return ResponseEntity.badRequest().build()
        }

        log.info("Section deleted")
        replConfig.regenerateSingleModel().invoke("sections")

        return ResponseEntity.ok(true)
    }
}
