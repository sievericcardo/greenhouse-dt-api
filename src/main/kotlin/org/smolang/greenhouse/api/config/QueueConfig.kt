package org.smolang.greenhouse.api.config

import org.apache.activemq.ActiveMQConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory as ArtemisConnectionFactory

@Configuration
open class QueueConfig {

    private lateinit var activeMQConnectionFactory: ActiveMQConnectionFactory
    private val host = System.getenv().getOrDefault("ACTIVEMQ_HOST", "localhost")
    private val port = System.getenv().getOrDefault("ACTIVEMQ_PORT", "61616")
    private val username = System.getenv("ACTIVEMQ_USERNAME")
    private val pass = System.getenv("ACTIVEMQ_PASSWORD")

    @Bean
    open fun getActiveMQConnectionFactory(): ActiveMQConnectionFactory {
        return ActiveMQConnectionFactory(username, pass, "tcp://$host:$port")
    }

    @Bean
    open fun getArtemisConnectionFactory(): ArtemisConnectionFactory {
        return ArtemisConnectionFactory("tcp://$host:$port", username, pass)
    }
}