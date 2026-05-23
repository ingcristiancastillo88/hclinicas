package ec.salud.citas.hclinicas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    // ── Generación ─────────────────────────────────────────────────────────────

    public String generarToken(UserDetails userDetails) {
        return generarToken(new HashMap<>(), userDetails);
    }

    public String generarToken(Map<String, Object> claimsExtra, UserDetails userDetails) {
        claimsExtra.put("roles", userDetails.getAuthorities()
                .stream().map(a -> a.getAuthority()).toList());
        return construirToken(claimsExtra, userDetails, jwtExpiration);
    }

    private String construirToken(Map<String, Object> claimsExtra,
                                  UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .claims(claimsExtra)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Validación ─────────────────────────────────────────────────────────────

    public boolean esTokenValido(String token, UserDetails userDetails) {
        final String username = extraerUsername(token);
        return (username.equals(userDetails.getUsername())) && !esTokenExpirado(token);
    }

    public boolean esTokenExpirado(String token) {
        return extraerExpiracion(token).before(new Date());
    }

    // ── Extracción de Claims ───────────────────────────────────────────────────

    public String extraerUsername(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public Date extraerExpiracion(String token) {
        return extraerClaim(token, Claims::getExpiration);
    }

    public <T> T extraerClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extraerTodosClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extraerTodosClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getJwtExpiration() {
        return jwtExpiration;
    }
}
