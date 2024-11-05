package org.smolang.greenhouse.api.config

import org.apache.activemq.ActiveMQConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class QueueConfig {
    @Bean
    open fun activeMQConnectionFactory(): ActiveMQConnectionFactory {
        val activeMQConnectionFactory = ActiveMQConnectionFactory()
        activeMQConnectionFactory.brokerURL = "tcp://" + System.getenv("ACTIVEMQ_HOST") + ":" + System.getenv("ACTIVEMQ_PORT")
        return activeMQConnectionFactory
    }
}