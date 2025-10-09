package com.thirdeye3.gateway.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayFilterConfig implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(GatewayFilterConfig.class);

    @Value("${thirdeye.api.key}")
    private String apiKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> safeProcess(exchange, chain, ctx))
                .onErrorResume(ex -> {
                    return chain.filter(exchange);
                })
                .switchIfEmpty(addApiKeyHeader(exchange, chain));
    }
    private Mono<Void> safeProcess(ServerWebExchange exchange, GatewayFilterChain chain,
                                   org.springframework.security.core.context.SecurityContext ctx) {
        try {
            String userId = null;

            if (ctx != null && ctx.getAuthentication() != null) {
                if (ctx.getAuthentication().getCredentials() != null) {
                    userId = ctx.getAuthentication().getCredentials().toString();
                }
            }
            var requestBuilder = exchange.getRequest().mutate();
            if (userId != null) {
                requestBuilder.header("TOKEN-USER-ID", userId);
            }
            requestBuilder.header("THIRDEYE-API-KEY", apiKey);
            var mutatedExchange = exchange.mutate().request(requestBuilder.build()).build();
            return chain.filter(mutatedExchange);
        } catch (Exception ex) {
            logger.error("Error adding headers : {}", ex.getMessage());
            return chain.filter(exchange);
        }
    }
    private Mono<Void> addApiKeyHeader(ServerWebExchange exchange, GatewayFilterChain chain) {
        var mutatedExchange = exchange.mutate()
                .request(exchange.getRequest()
                        .mutate()
                        .header("THIRDEYE-API-KEY", apiKey)
                        .build())
                .build();
        return chain.filter(mutatedExchange);
    }
    @Override
    public int getOrder() {
        return -1;
    }
}
