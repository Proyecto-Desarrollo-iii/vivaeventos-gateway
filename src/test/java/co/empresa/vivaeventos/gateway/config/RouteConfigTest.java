package co.empresa.vivaeventos.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RouteConfigTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void shouldDefineExpectedRoutes() {
        StepVerifier.create(routeLocator.getRoutes()
                        .map(r -> r.getId())
                        .collectList())
                .assertNext(ids -> {
                    assertTrue(ids.contains("auth"));
                    assertTrue(ids.contains("events"));
                    assertTrue(ids.contains("orders"));
                    assertTrue(ids.contains("promocodes"));
                    assertTrue(ids.contains("checkin"));
                    assertTrue(ids.contains("payments"));
                    assertTrue(ids.contains("payments-webhook"));
                    assertTrue(ids.contains("notifications"));
                })
                .verifyComplete();
    }

    @Test
    void shouldHavePromocodesRoute() {
        StepVerifier.create(routeLocator.getRoutes()
                        .filter(r -> r.getId().equals("promocodes"))
                        .count())
                .assertNext(count -> assertTrue(count > 0))
                .verifyComplete();
    }

    @Test
    void shouldHaveCheckinRoute() {
        StepVerifier.create(routeLocator.getRoutes()
                        .filter(r -> r.getId().equals("checkin"))
                        .count())
                .assertNext(count -> assertTrue(count > 0))
                .verifyComplete();
    }
}
