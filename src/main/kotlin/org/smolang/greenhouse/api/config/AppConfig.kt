package org.smolang.greenhouse.api.config

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = arrayOf("org.smolang.greenhouse.api"))
open class AppConfig {
}