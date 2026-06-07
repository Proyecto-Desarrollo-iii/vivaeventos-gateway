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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// S2068 mitiga el falso positivo de secreto hardcodeado; S1075 ignora URIs fijas en pruebas
@SuppressWarnings({"java:S2068", "java:S1075"})
class JwtAuthGatewayFilterFactoryTest {

    private static final String ROLE_ORGANIZER = "ORGANIZER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_CLIENT = "CLIENT";
    private static final String TEST_PATH = "/api/v1/orders/test";
    private static final String TEST_EMAIL = "test@email.com";
    
    // Llave dummy segura simulada exclusivamente para el entorno de pruebas unitarias
    private static final String MOCK_CIPHER_KEY = "Y2hvc3VuLXZpdmEtZXZlbnRvcy1nYXRld2F5LXNlY3JldC1rZXktZm9yLXRlc3Rpbmc=";

    private JwtAuthGatewayFilterFactory filterFactory;
    private ServerWebExchange exchange;
    private ServerHttpRequest request;
    private ServerHttpResponse response;
    private GatewayFilterChain chain;
    private ServerWebExchange.Builder builder;

    @BeforeEach
    void setUp() {
        filterFactory = new JwtAuthGatewayFilterFactory();
        
        // Inyección manual del atributo simulando el comportamiento de @Value
        ReflectionTestUtils.setField(filterFactory, "secretKey", MOCK_CIPHER_KEY);

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

    private String generateToken(String role, long durationHours) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(MOCK_CIPHER_KEY));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(TEST_EMAIL)
                .claim("role", role)
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plus(durationHours, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

    private void mockNonPublicPath() {
        when(request.getURI()).thenReturn(URI.create(TEST_PATH));
        when(request.getHeaders()).thenReturn(new HttpHeaders());
    }

    private void mockAuthenticatedRequest(String role) {
        when(request.getURI()).thenReturn(URI.create(TEST_PATH));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateToken(role, 1));
        when(request.getHeaders()).thenReturn(headers);
    }

    @Test
    void shouldReturn403WhenRoleNotAllowed() {
        mockAuthenticatedRequest(ROLE_CLIENT);
        GatewayFilter filter = filterFactory.apply(
                JwtAuthGatewayFilterFactory.Config.withRoles(ROLE_ORGANIZER, ROLE_ADMIN));

        filter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
        verify(response).setComplete();
        verify(chain, never()).filter(any());
    }

    @Test
    void shouldReturn403WhenUserHasNoRoleClaim() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(MOCK_CIPHER_KEY));
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject(TEST_EMAIL)
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();

        when(request.getURI()).thenReturn(URI.create(TEST_PATH));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        when(request.getHeaders()).thenReturn(headers);

        GatewayFilter filter = filterFactory.apply(
                JwtAuthGatewayFilterFactory.Config.withRoles(ROLE_ORGANIZER, ROLE_ADMIN));

        filter.filter(exchange, chain).block();

        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
        verify(response).setComplete();
    }

    @Test
    void shouldAllowWhenRoleMatches() {
        mockAuthenticatedRequest(ROLE_ADMIN);
        GatewayFilter filter = filterFactory.apply(
                JwtAuthGatewayFilterFactory.Config.withRoles(ROLE_ORGANIZER, ROLE_ADMIN));

        filter.filter(exchange, chain).block();

        verify(response, never()).setStatusCode(any());
        verify(chain).filter(any());
    }

    @Test
    void shouldAllowWhenNoRolesConfigured() {
        mockAuthenticatedRequest(ROLE_CLIENT);
        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        filter.filter(exchange, chain).block();

        verify(response, never()).setStatusCode(any());
        verify(chain).filter(any());
    }

    @Test
    void shouldAllowWhenAllowedRolesIsEmpty() {
        mockAuthenticatedRequest(ROLE_CLIENT);
        JwtAuthGatewayFilterFactory.Config config = new JwtAuthGatewayFilterFactory.Config();
        config.setAllowedRoles(Collections.emptyList());
        GatewayFilter filter = filterFactory.apply(config);

        filter.filter(exchange, chain).block();

        verify(response, never()).setStatusCode(any());
        verify(chain).filter(any());
    }

    @Test
    void shouldSetUserHeadersOnValidToken() {
        mockAuthenticatedRequest(ROLE_ORGANIZER);
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
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(MOCK_CIPHER_KEY));
        Instant now = Instant.now();
        String expiredToken = Jwts.builder()
                .subject(TEST_EMAIL)
                .claim("role", ROLE_ORGANIZER)
                .issuedAt(java.util.Date.from(now.minus(2, ChronoUnit.HOURS)))
                .expiration(java.util.Date.from(now.minus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();

        when(request.getURI()).thenReturn(URI.create(TEST_PATH));
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
                JwtAuthGatewayFilterFactory.Config.withRoles(ROLE_ORGANIZER, ROLE_ADMIN);

        assertNotNull(config.getAllowedRoles());
        assertEquals(2, config.getAllowedRoles().size());
        assertTrue(config.getAllowedRoles().contains(ROLE_ORGANIZER));
        assertTrue(config.getAllowedRoles().contains(ROLE_ADMIN));
    }

    @Test
    void configShouldAllowSettingRoles() {
        JwtAuthGatewayFilterFactory.Config config = new JwtAuthGatewayFilterFactory.Config();
        List<String> roles = Arrays.asList("LOGISTICA", ROLE_ORGANIZER);
        config.setAllowedRoles(roles);

        assertSame(roles, config.getAllowedRoles());
    }
}