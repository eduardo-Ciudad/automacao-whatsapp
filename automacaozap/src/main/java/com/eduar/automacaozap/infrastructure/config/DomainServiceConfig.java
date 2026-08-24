package com.eduar.automacaozap.infrastructure.config;

import com.eduar.automacaozap.domain.service.FlowEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public FlowEngine flowEngine() {
        return new FlowEngine();
    }
}
