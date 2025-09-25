package com.thirdeye3.gateway.security.jwt;

import com.thirdeye3.gateway.utils.JwtUtil;
import io.jsonwebtoken.Claims;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {
	
	private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationManager.class);

    private final JwtUtil jwtUtil;

    public JwtAuthenticationManager(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();
        if (!jwtUtil.validateToken(authToken)) {
            logger.info("Invalid JWT token");
            return Mono.error(new BadCredentialsException("Invalid JWT token"));
        }
        Claims claims = jwtUtil.getClaims(authToken);
        String username = claims.getSubject();
        Object rolesObj = claims.get("roles");
        List<String> roles = rolesObj instanceof List<?> ? ((List<?>) rolesObj).stream().map(Object::toString).toList() : List.of();
        var authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority(role))
                .collect(Collectors.toList());
        Authentication auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
        return Mono.just(auth);
    }
}
