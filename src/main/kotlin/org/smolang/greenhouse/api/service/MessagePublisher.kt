package org.smolang.greenhouse.api.service

import jakarta.jms.Session
import jakarta.jms.TextMessage
import org.smolang.greenhouse.api.config.QueueConfig
import org.springframework.stereotype.Service

@Service
class MessagePublisher(
    private val queueConfig: QueueConfig
) {
    private val queueType = System.getenv().getOrDefault("QUEUE_TYPE", "activemq")
    private val connectionFactory = if (queueType == "activemq") {
        queueConfig.getActiveMQConnectionFactory()
    } else {
        queueConfig.getArtemisConnectionFactory()
    }

    @Throws(Exception::class)
    fun publish(queueName: String, message: String) {
        if (queueType == "activemq") {
            queueConfig.getActiveMQConnectionFactory().createConnection().use { connection ->
                connection.start()
                connection.createSession(false, Session.AUTO_ACKNOWLEDGE).use { session ->
                    val destination = session.createQueue(queueName)
                    val producer = session.createProducer(destination)
                    val textMessage: TextMessage = session.createTextMessage(message)
                    producer.send(textMessage)
                }
            }
        } else {
            queueConfig.getArtemisConnectionFactory().createConnection().use { connection ->
                connection.start()
                connection.createSession(false, Session.AUTO_ACKNOWLEDGE).use { session ->
                    val destination = session.createQueue(queueName)
                    val producer = session.createProducer(destination)
                    val textMessage: TextMessage = session.createTextMessage(message)
                    producer.send(textMessage)
                }
            }
        }
    }
}