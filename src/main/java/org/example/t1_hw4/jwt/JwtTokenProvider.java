package org.example.t1_hw4.jwt;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret-path}")
    private String jwtPath;

    @Value("${app.jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private byte[] secretBytes;
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() throws IOException {
        String jwtSecret = Files.readString(Path.of(jwtPath)).trim();
        if (jwtSecret.length() < 64) {
            throw new IllegalArgumentException("Secret must be at least 64 characters for A256CBC-HS512");
        }
        this.secretBytes = jwtSecret.getBytes();
    }


    public String generateAccessToken(String username) {
        return generateToken(username, accessExpirationMs);
    }

    public String generateRefreshToken(String username) {
        return generateToken(username, refreshExpirationMs);
    }

    private String generateToken(String username, long durationMs) {
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + durationMs);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(username)
                    .issueTime(now)
                    .expirationTime(expiry)
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet
            );
            signedJWT.sign(new MACSigner(secretBytes));

            JWEObject jweObject = new JWEObject(
                    new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256CBC_HS512),
                    new Payload(signedJWT)
            );
            jweObject.encrypt(new DirectEncrypter(secretBytes));

            return jweObject.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate token", e);
        }
    }

    public boolean validateToken(String token) {
        if (isTokenBlacklisted(token)) return false;
        try {
            SignedJWT jwt = decryptAndVerify(token);
            Date now = new Date();
            return jwt.getJWTClaimsSet().getExpirationTime().after(now);
        } catch (Exception e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            SignedJWT jwt = decryptAndVerify(token);
            return jwt.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            throw new RuntimeException("Invalid token", e);
        }
    }

    public String getRoleFromToken(String token) {
        try {
            SignedJWT jwt = decryptAndVerify(token);
            return jwt.getJWTClaimsSet().getStringClaim("role");
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public void blacklistToken(String token) {
        blacklistedTokens.add(token);
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }


    private SignedJWT decryptAndVerify(String token) throws Exception {
        JWEObject jweObject = JWEObject.parse(token);
        jweObject.decrypt(new DirectDecrypter(secretBytes));

        SignedJWT signedJWT = jweObject.getPayload().toSignedJWT();
        if (!signedJWT.verify(new MACVerifier(secretBytes))) {
            throw new SecurityException("Invalid token signature");
        }
        return signedJWT;
    }
}
