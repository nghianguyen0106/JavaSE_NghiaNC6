# HTTP Contract Specification: Order Service ↔ Product Service

## 1. Hợp Đồng Giao Tiếp API (Spring Cloud Contract Groovy DSL)

Hợp đồng định nghĩa tương tác giữa Consumer (`order-service`) và Producer (`product-service`):
- Endpoint: `GET /api/products/{id}`
- Trạng thái trả về: HTTP 200 OK
- Headers: `Content-Type: application/json`

```groovy
package contracts.products

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Khi Order Service gửi request lấy thông tin sản phẩm hợp lệ, Product Service trả về HTTP 200 kèm chi tiết tồn kho"
    
    request {
        method GET()
        urlPath(value('/api/products/prod-1001')) {
            headers {
                contentType(applicationJson())
                header('X-Correlation-Id', value(consumer(regex('[a-zA-Z0-9-]+')), producer('c1a8-9921-ef4b')))
            }
        }
    }
    
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body([
            id: $(consumer('prod-1001'), producer(regex('[a-zA-Z0-9-]+'))),
            name: $(consumer('MacBook Pro M3'), producer(regex('.+'))),
            price: $(consumer(25000000.00), producer(regex('^[0-9]+\\.[0-9]{2}$'))),
            available: $(consumer(true), producer(anyBoolean())),
            stockQuantity: $(consumer(50), producer(regex('^[0-9]+$')))
        ])
    }
}
```

---

## 2. Ý Nghĩa Kỹ Thuật
- **Phía Producer (Product Service):** Plugin Spring Cloud Contract tự động sinh ra một Unit/Integration Test dựa trên file contract này. Test này mock request gửi tới controller của Product Service và assert response thực tế khớp hoàn toàn với định nghĩa.
- **Phía Consumer (Order Service):** Khi chạy unit test của Order Service, contract này sinh ra một WireMock Stub Server ảo (trả về đúng body mẫu), giúp Order Service test độc lập mà không cần bật service Product thật.
