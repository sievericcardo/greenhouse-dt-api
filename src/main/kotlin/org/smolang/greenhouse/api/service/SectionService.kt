package org.smolang.greenhouse.api.service

import org.apache.jena.query.ResultSet
import org.apache.jena.update.UpdateExecutionFactory
import org.apache.jena.update.UpdateFactory
import org.smolang.greenhouse.api.config.ComponentsConfig
import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.slf4j.LoggerFactory
import org.smolang.greenhouse.api.model.Section
import org.springframework.stereotype.Service

@Service
class SectionService(
    private val replConfig: REPLConfig,
    private val triplestoreProperties: TriplestoreProperties,
    private val potService: PotService,
    private val componentsConfig: ComponentsConfig
) {

    private val logger = LoggerFactory.getLogger(SectionService::class.java)
    private val tripleStore = triplestoreProperties.tripleStore
    private val prefix = triplestoreProperties.prefix
    private val ttlPrefix = triplestoreProperties.ttlPrefix
    private val repl = replConfig.repl()

    fun createSection(sectionId: String): Section? {
        val query = """
            PREFIX ast: <$prefix>
            
            INSERT DATA {
                ast:section$sectionId a ast:Section ;
                    ast:sectionId "$sectionId" .
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            val pots = potService.getPots() ?: emptyList()
            val section = Section(sectionId, pots)
            componentsConfig.addSectionToCache(section)
            return section
        } catch (e: Exception) {
            return null
        }
    }

    fun getSectionsByGreenHouseId(greenhouseId: String): List<Section>? {
        // Simplified implementation for now to avoid circular dependencies
        return getAllSections()
    }

    fun getAllSections(): List<Section>? {
        // Return cached sections if available
        val cached = componentsConfig.getSectionCache()
        if (cached.isNotEmpty()) return cached.values.toList()
        val sectionsQuery = """
            SELECT DISTINCT ?sectionId WHERE {
                ?sectionObj a prog:Section ;
                    prog:Section_sectionId ?sectionId .
            }
        """

        val result: ResultSet? = repl.interpreter!!.query(sectionsQuery)
        if (result == null || !result.hasNext()) {
            return null
        }

        val sectionsList = mutableListOf<Section>()

        while (result.hasNext()) {
            val solution = result.next()
            val sectionId = solution.get("?sectionId").asLiteral().toString()

            // For now, return sections with empty pot lists to avoid circular dependency
            // This should be properly implemented with pot resolution later
            sectionsList.add(Section(sectionId, emptyList()))
        }

        return sectionsList
    }

    fun getSectionById(sectionId: String): Section? {
        // Check cache first
        componentsConfig.getSectionById(sectionId)?.let { return it }
        return getAllSections()?.find { it.sectionId == sectionId }?.also { componentsConfig.addSectionToCache(it) }
    }

    fun deleteSection(sectionId: String): Boolean {
        val query = """
            PREFIX ast: <$prefix>
            
            DELETE {
                ast:section$sectionId ?p ?o .
                ?s ?p2 ast:section$sectionId .
            }
            WHERE {
                { ast:section$sectionId ?p ?o . }
                UNION
                { ?s ?p2 ast:section$sectionId . }
            }
        """.trimIndent()

        val updateRequest = UpdateFactory.create(query)
        val fusekiEndpoint = "$tripleStore/update"
        val updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiEndpoint)

        try {
            updateProcessor.execute()
            componentsConfig.removeSectionFromCache(sectionId)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
