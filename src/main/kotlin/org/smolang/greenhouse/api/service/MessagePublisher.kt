package org.smolang.greenhouse.api.service

import jakarta.jms.Session
import jakarta.jms.TextMessage
import org.smolang.greenhouse.api.config.QueueConfig
import org.springframework.stereotype.Service

@Service
class MessagePublisher (
    private val queueConfig: QueueConfig
) {
    private val connectionFactory = queueConfig.getActiveMQConnectionFactory()

    @Throws(Exception::class)
    fun publish(queueName: String, message: String) {
        val connection = connectionFactory.createConnection()
        connection.start()
        val session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        val destination = session.createQueue(queueName)

        val producer = session.createProducer(destination)
        val textMessage: TextMessage = session.createTextMessage(message)

        producer.send(textMessage)
        session.close()
        connection.close()
    }
}