package com.thirdeye3.gateway.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayFilterConfig {

    private static final Logger logger = LoggerFactory.getLogger(GatewayFilterConfig.class);

    @Value("${thirdeye.api.key}")
    private String apiKey;

    @Bean
    public GlobalFilter addApiKeyHeaderFilter() {
        logger.info("Initializing GlobalFilter for API key and X-User-Id header");
        return new ApiKeyAndUserIdFilter(apiKey);
    }

    public static class ApiKeyAndUserIdFilter implements GlobalFilter, Ordered {

        private static final Logger logger = LoggerFactory.getLogger(ApiKeyAndUserIdFilter.class);

        private final String apiKey;

        public ApiKeyAndUserIdFilter(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

            logger.debug("GatewayFilter triggered for request: {}", exchange.getRequest().getURI());

            return exchange.getPrincipal()
                    .cast(Authentication.class)
                    .defaultIfEmpty(null)
                    .flatMap(auth -> {

                        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                                .header("THIRDEYE-API-KEY", apiKey);

                        if (auth != null && auth.getDetails() != null) {
                            Object userId = auth.getDetails();
                            requestBuilder.header("X-User-Id", userId.toString());
                            logger.debug("Added X-User-Id header: {}", userId);
                        } else {
                            logger.debug("No Authentication details found, X-User-Id not added");
                        }

                        ServerWebExchange mutatedExchange = exchange.mutate()
                                .request(requestBuilder.build())
                                .build();

                        return chain.filter(mutatedExchange);
                    });
        }

        @Override
        public int getOrder() {
            return 0;
        }
    }
}
