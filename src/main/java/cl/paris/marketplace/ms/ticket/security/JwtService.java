package cl.paris.marketplace.ms.ticket.security;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    // 1. Extraer email
    public String extraerUsername(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    // 2. Extraer roles
    @SuppressWarnings("unchecked")
    public List<String> extraerRoles(String token) {
        Claims claims = extraerAllClaims(token);
        List<Map<String, String>> rolesList = claims.get("roles", List.class);
        
        if (rolesList == null) return List.of();
        
        return rolesList.stream()
                .map(roleMap -> roleMap.get("authority"))
                .toList();
    }

    // 3. Extraer el UUID 
    public String extraerUsuarioId(String token) {
        Claims claims = extraerAllClaims(token);
        
        // Lo sacamos como un objeto genérico primero para evitar errores de versión de JJWT
        Object usuarioIdClaim = claims.get("usuarioId");
        
        // Si el token es viejo y no trae la llave, avisamos en vez de explotar feo
        if (usuarioIdClaim == null) {
            throw new IllegalArgumentException("¡Alto ahí! El token no contiene un usuarioId. Necesitas hacer Login de nuevo para generar un token actualizado.");
        }
        
        return usuarioIdClaim.toString();
    }

    // 4. Validar token (Solo revisa que no esté vencido)
    public boolean isTokenValid(String token) {
        return !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extraerExpiration(token).before(new Date());
    }

    private Date extraerExpiration(String token) {
        return extraerClaim(token, Claims::getExpiration);
    }

    private <T> T extraerClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extraerAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extraerAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}