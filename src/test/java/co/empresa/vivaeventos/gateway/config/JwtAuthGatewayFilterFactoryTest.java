package co.empresa.vivaeventos.gateway.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthGatewayFilterFactoryTest {

    private static final String SECRET = "dGhpc0lzQVZlcnlTZWNyZXRLZXlGb3JWYWlhRXZlbnRvc1RoYXROZWVkczUw";

    private JwtAuthGatewayFilterFactory filterFactory;
    private ServerWebExchange exchange;
    private ServerHttpRequest request;
    private ServerHttpResponse response;
    private GatewayFilterChain chain;
    private ServerWebExchange.Builder builder;

    @BeforeEach
    void setUp() {
        filterFactory = new JwtAuthGatewayFilterFactory();
        ReflectionTestUtils.setField(filterFactory, "secretKey", SECRET);

        exchange = mock(ServerWebExchange.class);
        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);
        chain = mock(GatewayFilterChain.class);
        builder = mock(ServerWebExchange.Builder.class);

        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(exchange.mutate()).thenReturn(builder);
        when(builder.request(any(Consumer.class))).thenReturn(builder);
        when(builder.build()).thenReturn(exchange);
        when(chain.filter(any())).thenReturn(Mono.empty());
        when(response.setComplete()).thenReturn(Mono.empty());
    }

    private String generateToken(String role) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        return Jwts.builder()
                .subject("test@email.com")
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    private void mockNonPublicPath() {
        when(request.getURI()).thenReturn(URI.create("/api/v1/orders/test"));
        when(request.getHeaders()).thenReturn(new HttpHeaders());
    }

    private void mockAuthenticatedRequest(String role) {
        when(request.getURI()).thenReturn(URI.create("/api/v1/orders/test"));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateToken(role));
        when(request.getHeaders()).thenReturn(headers);
    }

    @Test
    void shouldReturn403WhenRoleNotAllowed() {
        mockAuthenticatedRequest("CLIENT");
        GatewayFilter filter = filterFactory.apply(
                JwtAuthGatewayFilterFactory.Config.withRoles("ORGANIZER", "ADMIN"));

        filter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
        verify(response).setComplete();
        verify(chain, never()).filter(any());
    }

    @Test
    void shouldReturn403WhenUserHasNoRoleClaim() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        String token = Jwts.builder()
                .subject("test@email.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        when(request.getURI()).thenReturn(URI.create("/api/v1/orders/test"));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        when(request.getHeaders()).thenReturn(headers);

        GatewayFilter filter = filterFactory.apply(
                JwtAuthGatewayFilterFactory.Config.withRoles("ORGANIZER", "ADMIN"));

        filter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
        verify(response).setComplete();
    }

    @Test
    void shouldAllowWhenRoleMatches() {
        mockAuthenticatedRequest("ADMIN");
        GatewayFilter filter = filterFactory.apply(
                JwtAuthGatewayFilterFactory.Config.withRoles("ORGANIZER", "ADMIN"));

        filter.filter(exchange, chain).block();

        verify(response, never()).setStatusCode(any());
        verify(chain).filter(any());
    }

    @Test
    void shouldAllowWhenNoRolesConfigured() {
        mockAuthenticatedRequest("CLIENT");
        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        filter.filter(exchange, chain).block();

        verify(response, never()).setStatusCode(any());
        verify(chain).filter(any());
    }

    @Test
    void shouldAllowWhenAllowedRolesIsEmpty() {
        mockAuthenticatedRequest("CLIENT");
        JwtAuthGatewayFilterFactory.Config config = new JwtAuthGatewayFilterFactory.Config();
        config.setAllowedRoles(Collections.emptyList());
        GatewayFilter filter = filterFactory.apply(config);

        filter.filter(exchange, chain).block();

        verify(response, never()).setStatusCode(any());
        verify(chain).filter(any());
    }

    @Test
    void shouldSetUserHeadersOnValidToken() {
        mockAuthenticatedRequest("ORGANIZER");
        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        filter.filter(exchange, chain).block();

        verify(builder).request(any(Consumer.class));
        verify(builder).build();
        verify(chain).filter(any());
    }

    @Test
    void shouldReturn401WhenNoAuthHeader() {
        mockNonPublicPath();
        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        filter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response).setComplete();
    }

    @Test
    void shouldReturn401WhenTokenExpired() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        String expiredToken = Jwts.builder()
                .subject("test@email.com")
                .claim("role", "ORGANIZER")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(key)
                .compact();

        when(request.getURI()).thenReturn(URI.create("/api/v1/orders/test"));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(expiredToken);
        when(request.getHeaders()).thenReturn(headers);

        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        filter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response).setComplete();
    }

    @Test
    void shouldAllowPublicPathWithoutToken() {
        when(request.getURI()).thenReturn(URI.create("/api/v1/auth/ping"));

        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(response, never()).setStatusCode(any());
    }

    @Test
    void withRolesShouldCreateConfigWithGivenRoles() {
        JwtAuthGatewayFilterFactory.Config config =
                JwtAuthGatewayFilterFactory.Config.withRoles("ORGANIZER", "ADMIN");

        assertNotNull(config.getAllowedRoles());
        assertEquals(2, config.getAllowedRoles().size());
        assertTrue(config.getAllowedRoles().contains("ORGANIZER"));
        assertTrue(config.getAllowedRoles().contains("ADMIN"));
    }

    @Test
    void configShouldAllowSettingRoles() {
        JwtAuthGatewayFilterFactory.Config config = new JwtAuthGatewayFilterFactory.Config();
        List<String> roles = Arrays.asList("LOGISTICA", "ORGANIZER");
        config.setAllowedRoles(roles);

        assertSame(roles, config.getAllowedRoles());
    }
}
