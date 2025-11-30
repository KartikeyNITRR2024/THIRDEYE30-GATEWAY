package com.thirdeye3.gateway.security.jwt;

import com.thirdeye3.gateway.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationManager.class);

    @Autowired
    private JwtUtil jwtUtil;

    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {

        return Mono.fromCallable(() -> {

            String token = authentication.getCredentials().toString();
            String redisKey = "jwt:" + token;

            Map<String, Object> claimsMap = null;

            if (redisTemplate != null) {
                try {
                    Object cached = redisTemplate.opsForValue().get(redisKey);
                    if (cached instanceof Map<?, ?> cachedMap) {
                        log.info("Using cached JWT claims from Redis for key={}", redisKey);
                        claimsMap = (Map<String, Object>) cachedMap;
                    }
                } catch (Exception ex) {
                    log.warn("Redis not available, falling back to JWT decode");
                }
            }

            if (claimsMap == null) {
                Claims claims;
                try {
                    claims = jwtUtil.validateToken(token);
                } catch (Exception ex) {
                    throw new BadCredentialsException("Invalid Token");
                }

                claimsMap = claims;

                if (redisTemplate != null) {
                    long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
                    if (ttl > 0) {
                        try {
                            redisTemplate.opsForValue().set(redisKey, claimsMap, ttl, TimeUnit.MILLISECONDS);
                            log.info("Stored JWT claims in Redis for key={} with TTL={}ms", redisKey, ttl);
                        } catch (Exception ex) {
                            log.warn("Failed to write JWT claims to Redis");
                        }
                    }
                }
            }

            return buildAuthFromMap(claimsMap);
        });
    }

    private Authentication buildAuthFromMap(Map<String, Object> claims) {

        String username = (String) claims.get("sub");
        Long userId = claims.get("userId") != null ?
                Long.valueOf(claims.get("userId").toString()) : null;

        List<String> roles =
                claims.get("roles") instanceof List<?> r
                        ? r.stream().map(Object::toString).toList()
                        : List.of();

        var authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(username, userId, authorities);
    }
}
