package com.hims.billing.infrastructure.kafka;

import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class BillingKafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(BillingKafkaConfig.class);

    @Bean
    public DefaultErrorHandler billingKafkaErrorHandler() {
        FixedBackOff backOff = new FixedBackOff(2000L, 4L);
        ConsumerRecordRecoverer recoverer = (record, ex) -> log.error(
                "Kafka consume failed: topic={}, partition={}, offset={}, exception={}",
                record != null ? record.topic() : null,
                record != null ? record.partition() : null,
                record != null ? record.offset() : null,
                ex);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(InvalidBillingRequestException.class);
        return handler;
    }
}
