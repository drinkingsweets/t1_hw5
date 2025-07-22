package org.example.t1_hw4.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret-path}")
    private String jwtPath;

    @Value("${app.jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private String jwtSecret;
    private Key key;

    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() throws IOException {
        this.jwtSecret = Files.readString(Path.of(jwtPath)).trim();
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateAccessToken(String username) {
        return generateToken(username, accessExpirationMs);
    }

    public String generateRefreshToken(String username) {
        return generateToken(username, refreshExpirationMs);
    }

    private String generateToken(String username, long durationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + durationMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .setId(UUID.randomUUID().toString())
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean validateToken(String token) {
        if (isTokenBlacklisted(token)) return false;

        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String getRoleFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    public void blacklistToken(String token) {
        blacklistedTokens.add(token);
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }
}
