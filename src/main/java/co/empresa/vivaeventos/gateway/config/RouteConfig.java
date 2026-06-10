package co.empresa.vivaeventos.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RouteConfig {

    @Value("${auth.uri:http://localhost:8083}")
    private String authUri;

    @Value("${events.uri:http://localhost:8081}")
    private String eventsUri;

    @Value("${tickets.uri:http://localhost:8085}")
    private String ticketsUri;

    @Value("${orders.uri:http://orders:8083}")
    private String ordersUri;

    @Value("${payments.uri:http://localhost:8084}")
    private String paymentsUri;

    @Value("${checkin.uri:http://localhost:8086}")
    private String checkinUri;

    @Value("${notifications.uri:http://localhost:8087}")
    private String notificationsUri;

    @Value("${audit.uri:http://audit:8089}")
    private String auditUri;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, JwtAuthGatewayFilterFactory jwtFilter) {
        JwtAuthGatewayFilterFactory.Config allAuth = new JwtAuthGatewayFilterFactory.Config();
        JwtAuthGatewayFilterFactory.Config organizerAdmin = JwtAuthGatewayFilterFactory.Config.withRoles("ORGANIZER", "ADMIN");
        JwtAuthGatewayFilterFactory.Config checkinRoles = JwtAuthGatewayFilterFactory.Config.withRoles("LOGISTICA", "ORGANIZER", "ADMIN");

        return builder.routes()
                .route("auth", r -> r.path("/api/v1/auth/**")
                        .filters(f -> f.filter(jwtFilter.apply(allAuth)))
                        .uri(authUri))
                .route("events", r -> r.path("/api/v1/events/**")
                        .filters(f -> f.filter(jwtFilter.apply(allAuth)))
                        .uri(eventsUri))
                .route("tickets", r -> r.path("/api/v1/tickets/**")
                        .filters(f -> f.filter(jwtFilter.apply(allAuth)))
                        .uri(ticketsUri))
                .route("issued-tickets", r -> r.path("/api/v1/issued-tickets/**")
                        .filters(f -> f.filter(jwtFilter.apply(allAuth)))
                        .uri(ticketsUri))
                .route("orders", r -> r.path("/api/v1/orders/**")
                        .filters(f -> f.filter(jwtFilter.apply(allAuth)))
                        .uri(ordersUri))
                .route("promocodes-read", r -> r.path("/api/v1/promocodes/code/**")
                        .and().method("GET")
                        .filters(f -> f.filter(jwtFilter.apply(allAuth)))
                        .uri(ordersUri))
                .route("promocodes-validate", r -> r.path("/api/v1/promocodes/validate/**")
                        .and().method("GET")
                        .filters(f -> f.filter(jwtFilter.apply(allAuth)))
                        .uri(ordersUri))
                .route("promocodes", r -> r.path("/api/v1/promocodes/**")
                        .filters(f -> f.filter(jwtFilter.apply(organizerAdmin)))
                        .uri(ordersUri))
                .route("payments-webhook", r -> r.path("/api/v1/payments/webhook")
                        .uri(paymentsUri))
                .route("payments", r -> r.path("/api/v1/payments/**")
                        .filters(f -> f.filter(jwtFilter.apply(allAuth)))
                        .uri(paymentsUri))
                .route("checkin", r -> r.path("/api/v1/checkin/**")
                        .filters(f -> f.filter(jwtFilter.apply(checkinRoles)))
                        .uri(checkinUri))
                .route("notifications", r -> r.path("/api/v1/notifications/**")
                        .filters(f -> f.filter(jwtFilter.apply(allAuth)))
                        .uri(notificationsUri))
                .route("audit", r -> r.path("/api/v1/audit/**")
                        .filters(f -> f.filter(jwtFilter.apply(allAuth)))
                        .uri(auditUri))
                .build();
    }
}
