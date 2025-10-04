package com.thirdeye3.gateway.exceptions.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thirdeye3.gateway.dtos.Response;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.*;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@RestControllerAdvice
@Order(-2)
@Component
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Mono<Void> writeError(ServerWebExchange exchange, Response<?> body, HttpStatus status) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("❌ Failed to write error response", e);
            byte[] fallback = ("{\"status\":" + status.value() + ",\"message\":\"" + body.getResponse() + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(fallback);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
    }

    @ExceptionHandler({MalformedJwtException.class, SignatureException.class, UnsupportedJwtException.class})
    public Mono<Void> handleInvalidJwt(ServerWebExchange exchange, RuntimeException ex) {
        log.warn("🚫 Invalid JWT: {}", ex.getMessage());
        return writeError(exchange,
                new Response<>(false, HttpStatus.UNAUTHORIZED.value(), "Invalid or malformed token", null),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public Mono<Void> handleExpiredJwt1(ServerWebExchange exchange, ExpiredJwtException ex) {
        log.warn("⌛ Token expired at {}", ex.getClaims().getExpiration());
        return writeError(exchange,
                new Response<>(false, HttpStatus.UNAUTHORIZED.value(), "Token expired", null),
                HttpStatus.UNAUTHORIZED);
    }


    @ExceptionHandler(ExpiredJwtException.class)
    public Mono<Void> handleExpiredJwt2(ServerWebExchange exchange, ExpiredJwtException ex) {
        log.warn("⌛ Token expired at {}", ex.getClaims().getExpiration());
        return writeError(exchange,
                new Response<>(false, HttpStatus.UNAUTHORIZED.value(), "Token expired", null),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public Mono<Void> handleBadCredentials(ServerWebExchange exchange, BadCredentialsException ex) {
        log.warn("🚫 Bad credentials: {}", ex.getMessage());
        return writeError(exchange,
                new Response<>(false, HttpStatus.UNAUTHORIZED.value(), "Invalid credentials", null),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public Mono<Void> handleAuthException(ServerWebExchange exchange, AuthenticationException ex) {
        log.warn("🚫 Authentication failed: {}", ex.getMessage());
        return writeError(exchange,
                new Response<>(false, HttpStatus.UNAUTHORIZED.value(), "Unauthorized", null),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Mono<Void> handleAccessDenied(ServerWebExchange exchange, AccessDeniedException ex) {
        log.warn("⛔ Access denied: {}", ex.getMessage());
        return writeError(exchange,
                new Response<>(false, HttpStatus.FORBIDDEN.value(), "Access denied", null),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<Void> handleResponseStatus(ServerWebExchange exchange, ResponseStatusException ex) {
        log.warn("⚠️ ResponseStatusException: {}", ex.getReason());
        return writeError(exchange,
                new Response<>(false, ex.getStatusCode().value(), ex.getReason(), null),
                HttpStatus.valueOf(ex.getStatusCode().value()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<Void> handleWebExchangeBind(ServerWebExchange exchange, WebExchangeBindException ex) {
        String errorMsg = ex.getAllErrors().isEmpty() ? "Validation failed" :
                ex.getAllErrors().get(0).getDefaultMessage();
        log.warn("📌 Validation error: {}", errorMsg);
        return writeError(exchange,
                new Response<>(false, HttpStatus.BAD_REQUEST.value(), errorMsg, null),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BindException.class)
    public Mono<Void> handleBindException(ServerWebExchange exchange, BindException ex) {
        String errorMsg = ex.getAllErrors().isEmpty() ? "Invalid input" :
                ex.getAllErrors().get(0).getDefaultMessage();
        log.warn("📌 Bind error: {}", errorMsg);
        return writeError(exchange,
                new Response<>(false, HttpStatus.BAD_REQUEST.value(), errorMsg, null),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ServerWebInputException.class, HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public Mono<Void> handleInputException(ServerWebExchange exchange, Exception ex) {
        log.warn("📝 Invalid request input: {}", ex.getMessage());
        return writeError(exchange,
                new Response<>(false, HttpStatus.BAD_REQUEST.value(), "Invalid request body or parameters", null),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Throwable.class)
    public Mono<Void> handleGenericException(ServerWebExchange exchange, Throwable ex) {
        log.error("💥 Unexpected error", ex);
        return writeError(exchange,
                new Response<>(false, HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", null),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
