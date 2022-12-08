package de.balonek.checkservicedbupdate.kafka.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaConfig {

    @Bean
    fun createTopic(): NewTopic {
        return TopicBuilder.name("check-results").build()

    }
}