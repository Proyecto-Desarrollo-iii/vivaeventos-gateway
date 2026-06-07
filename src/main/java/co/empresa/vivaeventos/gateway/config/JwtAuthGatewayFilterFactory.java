package co.empresa.vivaeventos.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthGatewayFilterFactory.Config> {

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/registro",
            "/api/v1/auth/validar-email",
            "/api/v1/auth/ping",
            "/api/v1/auth/2fa/authenticate",
            "/api/v1/auth/2fa/send-code",
            "/api/v1/events",
            "/ping"
    );

    @Value("${jwt.secret}")
    private String secretKey;

    public JwtAuthGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                SecretKey key = getSignInKey();
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userRole = claims.get("role", String.class);

                // Solución al IF anidado y reducción drástica de Complejidad Cognitiva
                if (isRoleForbidden(userRole, config.getAllowedRoles())) {
                    return onError(exchange, HttpStatus.FORBIDDEN);
                }

                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(r -> r.header("X-User-Email", claims.getSubject())
                                .header("X-User-Role", userRole))
                        .build();

                return chain.filter(mutatedExchange);
            } catch (Exception e) {
                // Se unificaron los catch idénticos reduciendo líneas y bifurcaciones operacionales
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(p -> {
            if (path.endsWith("/")) {
                return (path + "/").contains(p + "/");
            }
            return path.contains(p);
        });
    }

    /**
     * Evalúa si el rol del usuario está restringido según la configuración de la ruta.
     */
    private boolean isRoleForbidden(String userRole, List<String> allowedRoles) {
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            return false;
        }
        return userRole == null || !allowedRoles.contains(userRole);
    }

    /**
     * Centraliza el corte de la petición devolviendo el estado HTTP correspondiente.
     */
    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static class Config {
        private List<String> allowedRoles;

        public List<String> getAllowedRoles() {
            return allowedRoles;
        }

        public void setAllowedRoles(List<String> allowedRoles) {
            this.allowedRoles = allowedRoles;
        }

        public static Config withRoles(String... roles) {
            Config config = new Config();
            config.setAllowedRoles(Arrays.asList(roles));
            return config;
        }
    }
}