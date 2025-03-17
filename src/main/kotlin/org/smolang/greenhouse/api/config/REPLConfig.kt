package org.smolang.greenhouse.api.config

import jakarta.annotation.PostConstruct
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.main.Settings
import no.uio.microobject.main.ReasonerMode
import no.uio.microobject.runtime.REPL
import no.uio.microobject.type.STRINGTYPE
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class REPLConfig {

    private lateinit var repl: REPL

    @PostConstruct
    fun initRepl() {
        val verbose = true
        val materialize = false
        val liftedStateOutputPath = System.getenv("LIFTED_STATE_OUTPUT_PATH") ?: "./"
        val progPrefix = "https://github.com/Edkamb/SemanticObjects/Program#"
        val runPrefix = "https://github.com/Edkamb/SemanticObjects/Run" + System.currentTimeMillis() + "#"
        val langPrefix = "https://github.com/Edkamb/SemanticObjects#"
        val extraPrefixes = HashMap<String, String>()
        val useQueryType = false
        val tripleStoreHost = System.getenv("TRIPLESTORE_URL") ?: "localhost"
        val tripleStoreDataset = System.getenv("TRIPLESTORE_DATASET") ?: "ds"
        val triplestoreUrl = "http://$tripleStoreHost:3030/$tripleStoreDataset"
        val domainPrefixUri = System.getenv("DOMAIN_PREFIX_URI") ?: ""
        val reasoner = ReasonerMode.off

        if (System.getenv("EXTRA_PREFIXES") != null) {
            val prefixes = System.getenv("EXTRA_PREFIXES")!!.split(";")
            for (prefix in prefixes) {
                val parts = prefix.split(",")
                extraPrefixes.putAll(mapOf(parts[0] to parts[1]))
            }
        }

        val settings = Settings(
            verbose,
            materialize,
            liftedStateOutputPath,
            triplestoreUrl,
            "",
            domainPrefixUri,
            progPrefix,
            runPrefix,
            langPrefix,
            extraPrefixes,
            useQueryType,
            reasoner
        )

        val smolPath = System.getenv("SMOL_PATH") ?: "GreenHouse.smol"
        // get all file in SMOL_PATH

        repl = REPL(settings)
        repl.command("multiread", smolPath)
        repl.command("auto", "")
    }

    @Bean
    open fun repl(): REPL {
        return repl
    }

    @Bean
    open fun regenerateSingleModel() : (String) -> Unit = { modelName: String ->
        val escapedModelName = "\"$modelName\""
        repl.interpreter!!.tripleManager.regenerateTripleStoreModel()
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