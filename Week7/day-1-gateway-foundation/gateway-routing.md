# Gateway Routing & Global Filter Architecture

## 1. Cấu Hình Routing (Spring Cloud Gateway Route Configuration)

File cấu hình định tuyến `application.yml` cho API Gateway điều hướng traffic tới 3 downstream microservices:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service-route
          uri: lb://order-service
          predicates:
            - Path=/api/orders/
          filters:
            - StripPrefix=1
            - name: CircuitBreaker
              args:
                name: orderCircuitBreaker
                fallbackUri: forward:/fallback/orders

        - id: inventory-service-route
          uri: lb://inventory-service
          predicates:
            - Path=/api/inventory/
          filters:
            - StripPrefix=1

        - id: payment-service-route
          uri: lb://payment-service
          predicates:
            - Path=/api/payments/
          filters:
            - StripPrefix=1
```

---

## 2. Global Filter: Correlation ID & Request Logging

Global Filter đảm bảo tính toàn vẹn của Distributed Tracing Context và quan sát luồng request:
- Kiểm tra HTTP header `X-Correlation-Id`. Nếu chưa tồn tại, tự động sinh UUID mới.
- Forward header này xuống toàn bộ downstream services.
- Gắn `X-Correlation-Id` vào HTTP Response Header gửi ngược về cho client.
- Ghi structured log ghi nhận HTTP Method, Path, Status Code và Processing Latency.

```java
@Component
public class GlobalCorrelationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GlobalCorrelationFilter.class);
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();

        String correlationId = headers.getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Đính kèm correlationId vào mutated request chuyển tiếp
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        long startTime = System.currentTimeMillis();
        final String finalCorrelationId = correlationId;

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    HttpStatus status = (HttpStatus) exchange.getResponse().getStatusCode();
                    
                    // Gắn correlationId vào response header trả về client
                    exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);
                    
                    log.info("method={} path={} status={} correlationId={} durationMs={}",
                            mutatedRequest.getMethod(),
                            mutatedRequest.getURI().getPath(),
                            status != null ? status.value() : 500,
                            finalCorrelationId,
                            duration);
                }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
```
