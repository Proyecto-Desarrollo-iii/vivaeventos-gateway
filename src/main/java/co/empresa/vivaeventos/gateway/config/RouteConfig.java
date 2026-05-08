package co.empresa.vivaeventos.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    private static final String AUTH = "http://localhost:8083";
    private static final String EVENTS = "http://localhost:8081";
    private static final String TICKETS = "http://localhost:8082";
    private static final String ORDERS = "http://localhost:8084";
    private static final String PAYMENTS = "http://localhost:8085";
    private static final String CHECKIN = "http://localhost:8086";
    private static final String NOTIFICATIONS = "http://localhost:8087";
    private static final String ANALYTICS = "http://localhost:8088";
    private static final String AUDIT = "http://localhost:8089";

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, JwtAuthGatewayFilterFactory jwtFilter) {
        return builder.routes()
                .route("auth", r -> r.path("/api/v1/auth/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthGatewayFilterFactory.Config())))
                        .uri(AUTH))
                .route("events", r -> r.path("/api/v1/events/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthGatewayFilterFactory.Config())))
                        .uri(EVENTS))
                .route("tickets", r -> r.path("/api/v1/tickets/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthGatewayFilterFactory.Config())))
                        .uri(TICKETS))
                .route("orders", r -> r.path("/api/v1/orders/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthGatewayFilterFactory.Config())))
                        .uri(ORDERS))
                .route("payments", r -> r.path("/api/v1/payments/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthGatewayFilterFactory.Config())))
                        .uri(PAYMENTS))
                .route("checkin", r -> r.path("/api/v1/checkin/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthGatewayFilterFactory.Config())))
                        .uri(CHECKIN))
                .route("notifications", r -> r.path("/api/v1/notifications/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthGatewayFilterFactory.Config())))
                        .uri(NOTIFICATIONS))
                .route("analytics", r -> r.path("/api/v1/analytics/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthGatewayFilterFactory.Config())))
                        .uri(ANALYTICS))
                .route("audit", r -> r.path("/api/v1/audit/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthGatewayFilterFactory.Config())))
                        .uri(AUDIT))
                .build();
    }
}
