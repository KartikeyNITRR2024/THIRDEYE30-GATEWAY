package com.thirdeye3.gateway.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thirdeye3.gateway.dtos.Response;
import com.thirdeye3.gateway.security.jwt.JwtAuthenticationManager;
import com.thirdeye3.gateway.security.jwt.JwtSecurityContextRepository;
import com.thirdeye3.gateway.utils.SecurityPaths;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private JwtAuthenticationManager authManager;

    @Autowired
    private JwtSecurityContextRepository contextRepository;

    @Autowired
    private SecurityPaths securityPaths;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors().and()
                .authorizeExchange(exchanges -> exchanges
                        // Allow all OPTIONS requests (preflight)
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        // Open APIs
                        .pathMatchers(securityPaths.getOpenApiPaths().toArray(new String[0])).permitAll()
                        // User APIs
                        .pathMatchers(securityPaths.getUserApiPaths().toArray(new String[0])).hasRole("USER")
                        // Admin APIs
                        .anyExchange().hasRole("ADMIN")
                )
                .authenticationManager(authManager)
                .securityContextRepository(contextRepository)
                .exceptionHandling(spec -> spec
                        .authenticationEntryPoint((exchange, ex) ->
                                writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, ex.getMessage()))
                        .accessDeniedHandler((exchange, ex) ->
                                writeErrorResponse(exchange, HttpStatus.FORBIDDEN, "Access denied"))
                )
                .addFilterAt((exchange, chain) ->
                                chain.filter(exchange)
                                        .onErrorResume(ex ->
                                                writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, ex.getMessage())),
                        SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Response<String> body = new Response<>(false, status.value(), message, null);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            byte[] fallback = ("{\"success\":false,\"status\":" + status.value() +
                    ",\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(fallback)));
        }
    }
}
