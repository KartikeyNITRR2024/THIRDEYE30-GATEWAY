package com.thirdeye3.gateway.configs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thirdeye3.gateway.dtos.Response;
import com.thirdeye3.gateway.security.jwt.JwtAuthenticationManager;
import com.thirdeye3.gateway.security.jwt.JwtSecurityContextRepository;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationManager authManager;

    @Autowired
    private JwtSecurityContextRepository contextRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
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
                .exceptionHandling(exceptionHandlingSpec -> exceptionHandlingSpec
                        .authenticationEntryPoint((exchange, ex) -> {
                            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                            if (cause instanceof MalformedJwtException || cause instanceof SignatureException) {
                                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Invalid or malformed JWT token");
                            } else if (cause instanceof ExpiredJwtException) {
                                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "JWT token expired");
                            } else {
                                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized or missing/invalid token");
                            }
                        })
                        .accessDeniedHandler((exchange, ex) ->
                                writeErrorResponse(exchange, HttpStatus.FORBIDDEN, "Access denied"))
                )
                .build();
    }


    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Response<Object> body = new Response<>(false, status.value(), message, null);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
