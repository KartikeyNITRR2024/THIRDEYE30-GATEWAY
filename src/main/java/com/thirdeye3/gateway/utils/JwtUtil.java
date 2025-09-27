package com.thirdeye3.gateway.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${thirdeye.jwt.secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    public List<String> extractRoles(String token) {
        Object rolesObj = getClaims(token).get("roles");
        if (rolesObj instanceof List<?>) {
            return ((List<?>) rolesObj).stream().map(Object::toString).toList();
        }
        return List.of();
    }

    public Claims validateToken(String token) {
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return jws.getBody();
        } catch (ExpiredJwtException ex) {
            logger.warn("⚠️ Token has expired at: {}", ex.getClaims().getExpiration());
            throw ex;
        } catch (UnsupportedJwtException ex) {
            logger.error("❌ Unsupported JWT Token", ex);
            throw ex;
        } catch (MalformedJwtException ex) {
            logger.error("❌ Invalid JWT structure (Malformed)", ex);
            throw ex;
        } catch (SignatureException ex) {
            logger.error("❌ Invalid JWT Signature", ex);
            throw ex;
        } catch (IllegalArgumentException ex) {
            logger.error("❌ Token claims string is empty", ex);
            throw ex;
        } catch (Exception ex) {
            logger.error("❌ Unexpected error while validating token", ex);
            throw ex;
        }
    }

}
