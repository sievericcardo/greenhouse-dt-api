package org.smolang.greenhouse.api.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Provide a custom primary ObjectMapper to avoid automatic registration of
 * the jackson-module-kotlin (which triggers kotlin.reflect access and fails
 * when an incompatible kotlin-reflect implementation is present on the
 * classpath). This is a pragmatic workaround — it disables Kotlin-module
 * specific behavior. The long-term fix is to ensure a single compatible
 * kotlin-reflect on the runtime classpath (or use a non-shaded SemanticObjects).
 */
@Configuration
open class JacksonConfig {

    @Bean
    @Primary
    open fun objectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        // Support java.time types
        mapper.registerModule(JavaTimeModule())
        // Prefer ISO date formats instead of timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        // Additional configuration can go here if needed
        return mapper
    }
}
