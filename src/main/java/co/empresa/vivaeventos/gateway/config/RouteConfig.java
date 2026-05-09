package co.empresa.vivaeventos.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Value("${auth.uri:http://localhost:8083}")
    private String authUri;

    @Value("${events.uri:http://localhost:8081}")
    private String eventsUri;

    @Value("${tickets.uri:http://localhost:8082}")
    private String ticketsUri;

    @Value("${orders.uri:http://localhost:8084}")
    private String ordersUri;

    @Value("${payments.uri:http://localhost:8085}")
    private String paymentsUri;

    @Value("${checkin.uri:http://localhost:8086}")
    private String checkinUri;

    @Value("${notifications.uri:http://localhost:8087}")
    private String notificationsUri;

    @Value("${analytics.uri:http://localhost:8088}")
    private String analyticsUri;

    @Value("${audit.uri:http://localhost:8089}")
    private String auditUri;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, JwtAuthGatewayFilterFactory jwtFilter) {
        return builder.routes()
                .route("auth", r -> r.path("/api/v1/auth/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthGatewayFilterFactory.Config())))
                        .uri(authUri))
                .route("events", r -> r.path("/api/v1/events/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthGatewayFilterFactory.Config())))
                        .uri(eventsUri))
                .route("tickets", r -> r.path("/api/v1/tickets/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthGatewayFilterFactory.Config())))
                        .uri(ticketsUri))
                .route("orders", r -> r.path("/api/v1/orders/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthGatewayFilterFactory.Config())))
                        .uri(ordersUri))
                .route("payments", r -> r.path("/api/v1/payments/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthGatewayFilterFactory.Config())))
                        .uri(paymentsUri))
                .route("checkin", r -> r.path("/api/v1/checkin/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthGatewayFilterFactory.Config())))
                        .uri(checkinUri))
                .route("notifications", r -> r.path("/api/v1/notifications/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthGatewayFilterFactory.Config())))
                        .uri(notificationsUri))
                .route("analytics", r -> r.path("/api/v1/analytics/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthGatewayFilterFactory.Config())))
                        .uri(analyticsUri))
                .route("audit", r -> r.path("/api/v1/audit/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthGatewayFilterFactory.Config())))
                        .uri(auditUri))
                .build();
    }
}
