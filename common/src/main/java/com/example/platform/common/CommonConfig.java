package com.example.platform.common;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class CommonConfig {

    @Bean
    NewTopic ordersTopic() {
        return TopicBuilder.name(Events.Topics.ORDERS).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic inventoryTopic() {
        return TopicBuilder.name(Events.Topics.INVENTORY).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic paymentsTopic() {
        return TopicBuilder.name(Events.Topics.PAYMENTS).partitions(3).replicas(1).build();
    }
}
