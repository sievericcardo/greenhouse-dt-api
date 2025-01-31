package org.smolang.greenhouse.api.service

import org.smolang.greenhouse.api.config.REPLConfig
import org.smolang.greenhouse.api.config.TriplestoreProperties
import org.springframework.stereotype.Service

@Service
class PotService (
    private val replConfig: REPLConfig,
    private val triplestoreProperties: TriplestoreProperties
) {

    private val tripleStore = triplestoreProperties.tripleStore
    private val prefix = triplestoreProperties.prefix
    private val ttlPrefix = triplestoreProperties.ttlPrefix
    private val repl = replConfig.repl()

    fun createPot (shelfFloor: String, potPosition: String, pumpId: String, plantId: String): Boolean {
        return false
    }
}