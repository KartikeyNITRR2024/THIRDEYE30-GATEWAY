package com.thirdeye3.gateway.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayFilterConfig {

    @Value("${thirdeye.api.key}")
    private String apiKey;

    @Bean
    public GlobalFilter addApiKeyHeaderFilter() {
        return (exchange, chain) -> {
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(exchange.getRequest()
                            .mutate()
                            .header("THIRDEYE-API-KEY", apiKey)
                            .build())
                    .build();
            return chain.filter(mutatedExchange);
        };
    }
}
