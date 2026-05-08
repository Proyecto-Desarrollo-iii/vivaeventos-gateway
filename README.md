# vivaeventos-gateway

API Gateway central para el sistema de gestión de eventos y boletería VivaEventos.

## Descripción

Punto de entrada único para todas las peticiones de los microservicios. Encargado de:
- Enrutar peticiones a los microservicios correspondientes (auth, events, tickets, etc.)
- Validación de tokens JWT para rutas protegidas
- Cabeceras CORS
- Rate limiting (próximamente)
- Logueo de peticiones

## Puerto

```
8080
```

## Rutas

| Ruta | Microservicio | Puerto |
|------|--------------|--------|
| `/api/v1/auth/**` | vivaeventos-auth | 8083 |
| `/api/v1/events/**` | vivaeventos-events | 8081 |
| `/api/v1/tickets/**` | vivaeventos-tickets | 8082 |
| `/api/v1/orders/**` | vivaeventos-orders | 8084 |
| `/api/v1/payments/**` | vivaeventos-payments | 8085 |
| `/api/v1/checkin/**` | vivaeventos-checkin | 8086 |
| `/api/v1/notifications/**` | vivaeventos-notifications | 8087 |
| `/api/v1/analytics/**` | vivaeventos-analytics | 8088 |
| `/api/v1/audit/**` | vivaeventos-audit | 8089 |
| `/ping` | Gateway propio | 8080 |

## Rutas públicas (sin JWT)

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/registro`
- `GET /api/v1/auth/validar-email`
- `GET /api/v1/auth/ping`
- `GET /ping`

## Documentación

Ver [MULTIREPO.md](../MULTIREPO.md)
