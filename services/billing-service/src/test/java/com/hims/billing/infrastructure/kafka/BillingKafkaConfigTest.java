package com.hims.billing.infrastructure.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.listener.CommonErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;

class BillingKafkaConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(BillingKafkaConfig.class);

    @Test
    void exactlyOneCommonErrorHandlerBean() {
        runner.run(context -> assertThat(context.getBeansOfType(CommonErrorHandler.class)).hasSize(1));
    }
}
