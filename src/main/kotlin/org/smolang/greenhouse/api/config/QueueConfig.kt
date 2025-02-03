package org.smolang.greenhouse.api.config

import jakarta.annotation.PostConstruct
import org.apache.activemq.ActiveMQConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class QueueConfig {

    private lateinit var activeMQConnectionFactory: ActiveMQConnectionFactory

    @PostConstruct
    open fun initActiveMQ() {
        activeMQConnectionFactory.brokerURL = "tcp://" + System.getenv("ACTIVEMQ_HOST") + ":" + System.getenv("ACTIVEMQ_PORT")
    }

    @Bean
    open fun getActiveMQConnectionFactory(): ActiveMQConnectionFactory {
        return activeMQConnectionFactory
    }
}