package org.smolang.greenhouse.api.listener

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.jms.Message
import jakarta.jms.MessageListener
import jakarta.jms.Session
import jakarta.jms.TextMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.smolang.greenhouse.api.config.QueueConfig
import org.smolang.greenhouse.api.service.MessagePublisher
import org.smolang.greenhouse.api.service.NutrientSensorService
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy

@Component
class NutrientSensorListener(
    private val queueConfig: QueueConfig,
    private val nutrientSensorService: NutrientSensorService,
    private val messagePublisher: MessagePublisher,
    private val objectMapper: ObjectMapper
) : MessageListener {

    private val log: Logger = LoggerFactory.getLogger(NutrientSensorListener::class.java)
    private val queueName = "orchestrator.1.nutrient-sensors"
    private val queueType = System.getenv().getOrDefault("QUEUE_TYPE", "activemq")
    
    private var connection: jakarta.jms.Connection? = null
    private var session: Session? = null

    @PostConstruct
    fun startListening() {
        log.info("Starting NutrientSensorListener for queue: $queueName")
        try {
            connection = if (queueType == "activemq") {
                queueConfig.getActiveMQConnectionFactory().createConnection()
            } else {
                queueConfig.getArtemisConnectionFactory().createConnection()
            }
            
            connection?.start()
            session = connection?.createSession(false, Session.AUTO_ACKNOWLEDGE)
            val destination = session?.createQueue(queueName)
            val consumer = session?.createConsumer(destination)
            consumer?.messageListener = this
            
            log.info("NutrientSensorListener successfully started for queue: $queueName")
        } catch (e: Exception) {
            log.error("Failed to start NutrientSensorListener: ${e.message}", e)
        }
    }

    @PreDestroy
    fun stopListening() {
        log.info("Stopping NutrientSensorListener")
        try {
            session?.close()
            connection?.close()
            log.info("NutrientSensorListener successfully stopped")
        } catch (e: Exception) {
            log.error("Error stopping NutrientSensorListener: ${e.message}", e)
        }
    }

    override fun onMessage(message: Message) {
        try {
            if (message !is TextMessage) {
                log.warn("Received non-text message, ignoring")
                return
            }

            val messageBody = message.text?.trim()
            log.info("Received message from $queueName: $messageBody")

            if (messageBody.isNullOrBlank()) {
                log.warn("Received empty message, ignoring")
                return
            }

            // Extract the collector ID from the message
            val collectorId = try {
                messageBody.toInt()
            } catch (e: NumberFormatException) {
                log.error("Invalid message format, expected a number but got: $messageBody", e)
                return
            }

            log.info("Processing request for collector ID: $collectorId")

            // Retrieve all nutrient sensors
            val sensors = nutrientSensorService.getAllSensors()
            
            // Prepare the response message
            val responseMessage = if (sensors.isEmpty()) {
                log.info("No nutrient sensors found, sending empty message")
                ""
            } else {
                log.info("Found ${sensors.size} nutrient sensor(s), serializing to JSON")
                objectMapper.writeValueAsString(sensors)
            }

            // Publish to the collector queue
            val targetQueue = "collector.$collectorId.nutrient-sensors"
            log.info("Publishing response to queue: $targetQueue")
            messagePublisher.publish(targetQueue, responseMessage)
            log.info("Successfully published response to $targetQueue")

        } catch (e: Exception) {
            log.error("Error processing message: ${e.message}", e)
        }
    }
}
