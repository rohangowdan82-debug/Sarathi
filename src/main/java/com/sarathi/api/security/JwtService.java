package com.sarathi.api.security;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
@Service
public class JwtService {
 private final SecretKey key; private final long ttlMinutes;
 public JwtService(@Value("${sarathi.security.jwt-secret}") String secret,@Value("${sarathi.security.access-ttl-minutes:15}") long ttlMinutes){if(secret.getBytes(StandardCharsets.UTF_8).length<32)throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));this.ttlMinutes=ttlMinutes;}
 public String create(String userId,String role){Instant now=Instant.now();return Jwts.builder().subject(userId).claim("role",role).issuer("sarathi-api").audience().add("sarathi-web").and().issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(ttlMinutes*60))).signWith(key).compact();}
 public UserPrincipal verify(String token){var claims=Jwts.parser().verifyWith(key).requireIssuer("sarathi-api").build().parseSignedClaims(token).getPayload();if(claims.getAudience()==null||!claims.getAudience().contains("sarathi-web"))throw new IllegalArgumentException("Wrong audience");String role=claims.get("role",String.class);if(role==null)throw new IllegalArgumentException("Missing role");return new UserPrincipal(claims.getSubject(),role);}
}
