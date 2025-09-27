package com.thirdeye3.gateway.exceptions.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thirdeye3.gateway.dtos.Response;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestControllerAdvice
@Order(-2)
@Component
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Mono<Void> writeError(ServerWebExchange exchange, Response<?> body, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            logger.error("❌ Failed to write error response", e);
            return Mono.error(e);
        }
    }

    @ExceptionHandler({MalformedJwtException.class, SignatureException.class})
    public Mono<Void> handleInvalidToken(ServerWebExchange exchange, RuntimeException ex) {
        logger.warn("🚫 Invalid or malformed JWT token: {}", ex.getMessage());
        return writeError(exchange,
                new Response<>(false, HttpStatus.UNAUTHORIZED.value(), "Invalid token", null),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public Mono<Void> handleExpiredToken(ServerWebExchange exchange, ExpiredJwtException ex) {
        logger.warn("⌛ Token expired at: {}", ex.getClaims().getExpiration());
        return writeError(exchange,
                new Response<>(false, HttpStatus.UNAUTHORIZED.value(), "Token expired", null),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public Mono<Void> handleBadCredentials(ServerWebExchange exchange, BadCredentialsException ex) {
        logger.warn("🚫 Bad credentials: {}", ex.getMessage());
        return writeError(exchange,
                new Response<>(false, HttpStatus.UNAUTHORIZED.value(), "Invalid credentials", null),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public Mono<Void> handleAuthentication(ServerWebExchange exchange, AuthenticationException ex) {
        logger.warn("🚫 Authentication failed: {}", ex.getMessage());
        return writeError(exchange,
                new Response<>(false, HttpStatus.UNAUTHORIZED.value(), "Unauthorized", null),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Mono<Void> handleAccessDenied(ServerWebExchange exchange, AccessDeniedException ex) {
        logger.warn("⛔ Access denied: {}", ex.getMessage());
        return writeError(exchange,
                new Response<>(false, HttpStatus.FORBIDDEN.value(), "Access Denied", null),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<Void> handleResponseStatus(ServerWebExchange exchange, ResponseStatusException ex) {
        logger.warn("⚠️ ResponseStatusException: {}", ex.getReason());
        return writeError(exchange,
                new Response<>(false, ex.getStatusCode().value(), ex.getReason(), null),
                HttpStatus.valueOf(ex.getStatusCode().value()));
    }

    @ExceptionHandler(Exception.class)
    public Mono<Void> handleGeneric(ServerWebExchange exchange, Exception ex) {
        logger.error("💥 Unexpected error", ex);
        return writeError(exchange,
                new Response<>(false, HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", null),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
