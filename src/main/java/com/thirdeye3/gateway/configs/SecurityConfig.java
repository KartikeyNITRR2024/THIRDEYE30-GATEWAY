package com.thirdeye3.gateway.configs;

import com.thirdeye3.gateway.security.jwt.JwtAuthenticationManager;
import com.thirdeye3.gateway.security.jwt.JwtSecurityContextRepository;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationManager authManager;

    @Autowired
    private JwtSecurityContextRepository contextRepository;

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
                        .authenticationEntryPoint((exchange, ex) -> Mono.error(ex))
                        .accessDeniedHandler((exchange, ex) -> Mono.error(ex))
                )
                .build();
    }
}
