package co.uniquindio.crackscolombia.cracks_colombia_api.shared.security;

import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMillis;
    private final Clock clock;

    public JwtService(@Value("${app.jwt.secret-base64}") String secretBase64,
                      @Value("${app.jwt.expiration-ms:3600000}") long expirationMillis,
                      Clock clock) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        if (expirationMillis <= 0) {
            throw new IllegalArgumentException("JWT expiration must be greater than zero.");
        }
        this.expirationMillis = expirationMillis;
        this.clock = clock;
    }

    public String createToken(UserAccount user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusMillis(expirationMillis);
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
