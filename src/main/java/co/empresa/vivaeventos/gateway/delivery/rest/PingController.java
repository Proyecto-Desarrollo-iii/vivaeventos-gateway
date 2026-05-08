package co.empresa.vivaeventos.gateway.delivery.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class PingController {

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("status", "ok");
        respuesta.put("message", "Gateway is running");
        respuesta.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(respuesta);
    }
}
