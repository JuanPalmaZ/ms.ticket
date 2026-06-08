package cl.paris.marketplace.ms.ticket.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        
        // Si no hay token o no empieza con Bearer, sigue de largo
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        try {
            final String userEmail = jwtService.extraerUsername(jwt);

            // Si hay correo y aún no ha sido autenticado en este hilo
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                if (jwtService.isTokenValid(jwt)) {
                    // 1. Extraemos los roles y aseguramos que tengan el prefijo ROLE_
                    List<String> roles = jwtService.extraerRoles(jwt);
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    // =========================================================
                    // ¡NUEVO PASO 2! Extraemos el UUID directamente del aire
                    // =========================================================
                    String usuarioId = jwtService.extraerUsuarioId(jwt);

                    // 3. Creamos la autenticación. 
                    // OJO AQUÍ: Ponemos userEmail como "Identidad" y usuarioId como "Credenciales"
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userEmail, usuarioId, authorities
                    );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 4. Registramos al usuario en la memoria temporal del servidor
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token inválido o expirado, lo bloquea en silencio
        }
        
        filterChain.doFilter(request, response);
    }
}