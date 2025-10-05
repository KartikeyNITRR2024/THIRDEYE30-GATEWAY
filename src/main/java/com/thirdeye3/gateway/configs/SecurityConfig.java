package com.thirdeye3.gateway.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thirdeye3.gateway.dtos.Response;
import com.thirdeye3.gateway.security.jwt.JwtAuthenticationManager;
import com.thirdeye3.gateway.security.jwt.JwtSecurityContextRepository;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;

import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private JwtAuthenticationManager authManager;

    @Autowired
    private JwtSecurityContextRepository contextRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        logger.info("🔐 Initializing SecurityWebFilterChain...");

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(
                                "/um/auth/register",
                                "/um/auth/login",
                                "/mb/message/telegrambot/**",
                                "/pm/properties/telegrambot/**",
                                "/pm/properties/webscrapper/**",
                                "/sm/stocks/webscrapper/**",
                                "/sv/webscrapper/**",
                                "/api/statuschecker/**",
                                "/api/updateinitiatier"
                        ).permitAll()
                        .pathMatchers("/pm/**", "/mb/**", "/sm/**", "/sv/**", "/me/**").hasRole("ADMIN")
                        .pathMatchers("/um/admin/**").hasRole("ADMIN")
                        .pathMatchers("/um/**").hasAnyRole("USER", "ADMIN")
                        .anyExchange().authenticated()
                )
                .authenticationManager(authManager)
                .securityContextRepository(contextRepository)
                .exceptionHandling(spec -> spec
                        .authenticationEntryPoint((exchange, ex) -> {
                            logger.error("🚨 Authentication entry point triggered due to: {}",  ex.getMessage());
                            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, ex.getMessage());
                        })
                        .accessDeniedHandler((exchange, ex) -> {
                            logger.warn("⛔ Access denied: {}", ex.getMessage());
                            return writeErrorResponse(exchange, HttpStatus.FORBIDDEN, "Access denied");
                        })
                )
                .addFilterAt((exchange, chain) -> chain.filter(exchange)
                        .onErrorResume(ex -> {
                            logger.error("🔥 Caught unhandled Mono.error in security filter chain: {}", ex.toString());
                            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, ex.getMessage());
                        }),
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
            logger.error("❌ Failed to write error response", e);
            byte[] fallback = ("{\"success\":false,\"status\":" + status.value() +
                    ",\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(fallback)));
        }
    }
}
