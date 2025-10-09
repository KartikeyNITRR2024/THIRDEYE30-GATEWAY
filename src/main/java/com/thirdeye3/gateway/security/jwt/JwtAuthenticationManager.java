package com.thirdeye3.gateway.security.jwt;

import com.thirdeye3.gateway.configs.SecurityConfig;
import com.thirdeye3.gateway.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

	private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationManager.class);
	
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();
        try {
            Claims claims = jwtUtil.validateToken(authToken);
            String username = claims.getSubject();
            Long userId = claims.get("userId", Long.class);
            
            List<String> roles = Optional.ofNullable(claims.get("roles"))
                                         .filter(List.class::isInstance)
                                         .map(r -> ((List<?>) r).stream().map(Object::toString).toList())
                                         .orElse(List.of());
            var authorities = roles.stream()
                                   .map(SimpleGrantedAuthority::new)
                                   .collect(Collectors.toList());
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, userId, authorities);
            return Mono.just(auth);

        } catch (BadCredentialsException ex) {
            return Mono.error(new BadCredentialsException(ex.getMessage()));
        }
    }

}
