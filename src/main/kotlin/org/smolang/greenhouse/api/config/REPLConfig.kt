package org.smolang.greenhouse.api.config

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.main.ReasonerMode
import no.uio.microobject.main.Settings
import no.uio.microobject.runtime.REPL
import no.uio.microobject.type.STRINGTYPE
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.MessageDigest
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Configuration
open class REPLConfig {

    private lateinit var repl: REPL
    private val objectMapper = ObjectMapper()
    private val md = MessageDigest.getInstance("MD5")
    private val modelOperationLock = ReentrantLock()
    private val logger = LoggerFactory.getLogger(REPLConfig::class.java)

    @PostConstruct
    fun initRepl() {
        val verbose = System.getenv("VERBOSE_OUTPUT")?.toBoolean() ?: true
        val materialize = true
        val liftedStateOutputPath = System.getenv("LIFTED_STATE_OUTPUT_PATH") ?: "./"
        val progPrefix = "https://github.com/Edkamb/SemanticObjects/Program#"
        // Use the md5 of "GreenhouseDT" as run ID to ensure it remains the same across runs
        val runId = md.digest("GreenhouseDT".toByteArray()).joinToString("") { String.format("%02x", it) }
        val runPrefix = "https://github.com/Edkamb/SemanticObjects/Run$runId#"
        val langPrefix = "https://github.com/Edkamb/SemanticObjects#"
        val extraPrefixes = HashMap<String, String>()
        val useQueryType = false
        val tripleStoreHost = System.getenv("TRIPLESTORE_URL") ?: "localhost"
        val tripleStoreDataset = System.getenv("TRIPLESTORE_DATASET") ?: "ds"
        val odrlTripleStoreDataset = System.getenv("ODRL_TRIPLESTORE_DATASET") ?: "odrl"
        val tripleStores = (odrlTripleStoreDataset.split(";") + tripleStoreDataset).distinct()
            .map { "http://$tripleStoreHost:3030/$it/query" }
        val domainPrefixUri = System.getenv("DOMAIN_PREFIX_URI") ?: ""
        val reasoner = ReasonerMode.off
//        val features = mutableMapOf(
//            "odrl" to true
//        )
        val features = mutableMapOf<String, Boolean>()

        if (System.getenv("EXTRA_PREFIXES") != null) {
            val prefixes = System.getenv("EXTRA_PREFIXES")!!.split(";")
            for (prefix in prefixes) {
                val parts = prefix.split(",")
                extraPrefixes.putAll(mapOf(parts[0] to parts[1]))
            }
        }

        logger.info("Initializing REPL with the following triplestores: $tripleStores")

        val settings = Settings(
            verbose = false,
            materialize,
            liftedStateOutputPath,
            tripleStores,
            "",
            domainPrefixUri,
            progPrefix,
            runPrefix,
            langPrefix,
            extraPrefixes,
            useQueryType,
            reasoner,
            features = features
        )

        val smolPath = System.getenv("SMOL_PATH") ?: "GreenHouse.smol"
        // get all file in SMOL_PATH
        println("SMOL_PATH: $smolPath")

        repl = REPL(settings)
        repl.command("multiread", smolPath)
        repl.command("auto", "")

        if (!validatePolicies()) {
            throw RuntimeException("Policy validation failed")
        }
    }

    fun evaluatePolicies(
        odrlEndpoint: String,
        odrlPort: String,
        odrlToken: String,
        policyString: String,
        requestString: String,
        sotwString: String,
        static: Boolean = false
    ): Boolean {
        val evaluateUrl =
            if (static) "http://$odrlEndpoint:$odrlPort/evaluate-static" else "http://$odrlEndpoint:$odrlPort/evaluate"
        logger.info("Evaluating ODRL policies at $evaluateUrl")
        logger.info("Authorization token: $odrlToken")

        val evaluateBody = objectMapper.writeValueAsString(
            mapOf(
                "policy" to policyString,
                "request" to requestString,
                "sotw" to sotwString
            )
        )

        logger.info("Request body length: ${evaluateBody.length}")

        val connection = java.net.URI(evaluateUrl).toURL().openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer $odrlToken")
        connection.doOutput = true
        connection.outputStream.use { os ->
            val input = evaluateBody.toByteArray(Charsets.UTF_8)
            os.write(input, 0, input.size)
        }
        val responseCode = connection.responseCode
        if (responseCode != 200) {
            val errorStream = connection.errorStream
            val errorMessage = errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
            logger.error("Error evaluating ODRL: $errorMessage")
            return false
        }

        return true
    }

    private fun validatePolicies(): Boolean {
        val tripleStoreHost = System.getenv("TRIPLESTORE_URL") ?: "localhost"
        val odrlTripleStoreDataset = System.getenv("ODRL_TRIPLESTORE_DATASET") ?: "odrl"
        val odrlEndpoint = System.getenv("ODRL_URL") ?: "localhost"
        val odrlPort = System.getenv("ODRL_PORT") ?: "3000"
        val odrlToken = System.getenv("ODRL_TOKEN") ?: ""

        val policyUrl = "http://$tripleStoreHost:3030/policies/data"
        val requestUrl = "http://$tripleStoreHost:3030/requests/data"
        val sotwUrl = "http://$tripleStoreHost:3030/sotw/data"

        // Make basic get requests to the above urls and get the content as string. Don't use khttp
        val policyString = java.net.URI(policyUrl).toURL().readText()
        val requestString = java.net.URI(requestUrl).toURL().readText()
        val sotwString = java.net.URI(sotwUrl).toURL().readText()

        return evaluatePolicies(odrlEndpoint, odrlPort, odrlToken, policyString, requestString, sotwString)
    }

    @Bean
    open fun validatePoliciesBean(): () -> Unit = {
        validatePolicies()
    }

    @Bean
    open fun repl(): REPL {
        return repl
    }

    @Bean
    open fun regenerateSingleModel(): (String) -> Unit = { modelName: String ->
        modelOperationLock.withLock {
            val escapedModelName = "\"$modelName\""
            val tripleStoreHost = System.getenv("TRIPLESTORE_URL") ?: "localhost"
            val tripleStoreDataset = System.getenv("TRIPLESTORE_DATASET") ?: "ds"
            val endpoint = "http://$tripleStoreHost:3030/$tripleStoreDataset"
            repl.interpreter!!.tripleManager.regenerateTripleStoreModel(endpoint)
            repl.interpreter!!.evalCall(
                repl.interpreter!!.getObjectNames("AssetModel")[0],
                "AssetModel",
                "reconfigureSingleModel",
                mapOf("mod" to LiteralExpr(escapedModelName, STRINGTYPE))
            )
            repl.interpreter!!.evalCall(
                repl.interpreter!!.getObjectNames("AssetModel")[0],
                "AssetModel",
                "reclassifySingleModel",
                mapOf("mod" to LiteralExpr(escapedModelName, STRINGTYPE))
            )
        }
    }

    @Bean
    open fun reclassifySingleModel(): (String) -> Unit = { modelName: String ->
        modelOperationLock.withLock {
            val escapedModelName = "\"$modelName\""
            repl.interpreter!!.evalCall(
                repl.interpreter!!.getObjectNames("AssetModel")[0],
                "AssetModel",
                "reclassifySingleModel",
                mapOf("mod" to LiteralExpr(escapedModelName, STRINGTYPE))
            )
        }
    }
}