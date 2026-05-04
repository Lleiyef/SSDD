package es.um.sisdist.backend.dao.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

public class JwtUtil
{
    private static final long EXPIRATION_MS = 86_400_000L; // 24 horas

    private static SecretKey getKey()
    {
        String secret = Optional.ofNullable(System.getenv("JWT_SECRET"))
            .orElse("ssdd-super-secret-jwt-key-hmac-sha256-minimum-32-chars-ok");
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateToken(String userId, String email)
    {
        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
            .signWith(getKey())
            .compact();
    }

    public static Optional<Claims> getClaims(String token)
    {
        try
        {
            Claims claims = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return Optional.of(claims);
        }
        catch (Exception e)
        {
            return Optional.empty();
        }
    }

    public static boolean validateToken(String token)
    {
        return getClaims(token).isPresent();
    }
}
