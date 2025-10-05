package com.thirdeye3.gateway.security.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class JwtSecurityContextRepository implements ServerSecurityContextRepository {

    @Value("${thirdeye.jwt.token.starter}")
    private String tokenStarter;

    @Autowired
    private JwtAuthenticationManager authenticationManager;

    private static final Logger logger = LoggerFactory.getLogger(JwtSecurityContextRepository.class);

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("token");

        if (authHeader == null) {
            logger.debug("No 'token' header found in the request. Skipping authentication.");
            return Mono.error(new BadCredentialsException("Invalid token"));
        }

        String authToken = authHeader.startsWith(tokenStarter)
                ? authHeader.substring(tokenStarter.length())
                : authHeader;

        Authentication auth = new UsernamePasswordAuthenticationToken(authToken, authToken);

        return authenticationManager.authenticate(auth)
                .doOnSubscribe(sub -> {})
                .doOnNext(a -> {})
                .map(authObj -> {
                    return (SecurityContext) new SecurityContextImpl(authObj);
                })
                .doOnSuccess(context -> {})
                .doOnError(ex -> {})
                .onErrorResume(ex -> {
                    return Mono.error(new BadCredentialsException(ex.getMessage()));
                });
    }
}
