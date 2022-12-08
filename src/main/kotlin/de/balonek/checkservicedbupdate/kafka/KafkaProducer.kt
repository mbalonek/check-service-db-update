package de.balonek.checkservicedbupdate.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.balonek.model.Checkpoint
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
final class KafkaProducer (
    var logger : Logger = LoggerFactory.getLogger("KafkaProducer"),
    var kafkaTemplate : KafkaTemplate<String, String>,
    var env: Environment,

    var checkpoint: Checkpoint = Checkpoint.builder()
        .protocol(env.getProperty("microservice.protocol"))
        .hostName(env.getProperty("microservice.host"))
        .hostPort(env.getProperty("server.port"))
        .endpointName(env.getProperty("microservice.endpoint")).build()
){
    @EventListener(ApplicationReadyEvent::class)
    final fun register() {
        val mapper = jacksonObjectMapper()
        val checkpointJSON = mapper.writeValueAsString(this.checkpoint)
        kafkaTemplate.send("registration-microservices", checkpointJSON)
        logger.info("Object ${checkpoint.endpointName} was send")
    }


}