package com.thirdeye3.gateway.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thirdeye3.gateway.dtos.Response;
import com.thirdeye3.gateway.security.jwt.JwtAuthenticationManager;
import com.thirdeye3.gateway.security.jwt.JwtSecurityContextRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.core.io.buffer.DataBuffer;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtAuthenticationManager authManager;
    private final JwtSecurityContextRepository contextRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SecurityConfig(JwtAuthenticationManager authManager,
                          JwtSecurityContextRepository contextRepository) {
        this.authManager = authManager;
        this.contextRepository = contextRepository;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::unauthorizedHandler)
                        .accessDeniedHandler(this::accessDeniedHandler)
                )
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/um/auth/register", "/um/auth/login").permitAll()
                        .pathMatchers("/mb/message/telegrambot/**").permitAll()
                        .pathMatchers("/pm/properties/telegrambot/**").permitAll()
                        .pathMatchers("/pm/properties/webscrapper/**").permitAll()
                        .pathMatchers("/sm/stocks/webscrapper/**").permitAll()
                        .pathMatchers("/sv/webscrapper/**").permitAll()
                        .pathMatchers("/api/statuschecker/**").permitAll()
                        .pathMatchers("/api/updateinitiatier").permitAll()
                        .pathMatchers("/pm/**", "/mb/**", "/sm/**", "/sv/**", "/me/**").hasRole("ADMIN")
                        .pathMatchers("/um/admin/**").hasRole("ADMIN")
                        .pathMatchers("/um/**").hasAnyRole("USER", "ADMIN")
                        .anyExchange().authenticated()
                )
                .authenticationManager(authManager)
                .securityContextRepository(contextRepository)
                .build();
    }

    private Mono<Void> unauthorizedHandler(ServerWebExchange exchange, org.springframework.security.core.AuthenticationException ex) {
        return writeResponse(exchange, new Response<>(false, HttpStatus.UNAUTHORIZED.value(), "Unauthorized", null), HttpStatus.UNAUTHORIZED);
    }

    private Mono<Void> accessDeniedHandler(ServerWebExchange exchange, org.springframework.security.access.AccessDeniedException ex) {
        return writeResponse(exchange, new Response<>(false, HttpStatus.FORBIDDEN.value(), "Access Denied", null), HttpStatus.FORBIDDEN);
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, Response<?> resp, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(resp);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
