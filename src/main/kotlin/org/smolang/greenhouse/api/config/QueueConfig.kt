package org.smolang.greenhouse.api.config

import jakarta.annotation.PostConstruct
import org.apache.activemq.ActiveMQConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class QueueConfig {

    private lateinit var activeMQConnectionFactory: ActiveMQConnectionFactory
    private val host = System.getenv().getOrDefault("ACTIVEMQ_HOST", "localhost")
    private val port = System.getenv().getOrDefault("ACTIVEMQ_PORT", "5672")

    @PostConstruct
    open fun initActiveMQ() {
        activeMQConnectionFactory = ActiveMQConnectionFactory()
        activeMQConnectionFactory.brokerURL = "amqp://$host:$port"
    }

    @Bean
    open fun getActiveMQConnectionFactory(): ActiveMQConnectionFactory {
        return activeMQConnectionFactory
    }
}