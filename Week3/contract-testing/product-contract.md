# Đặc tả Hợp đồng API: Product Service Contract

* **Consumer:** `OrderService`
* **Provider:** `ProductService`
* **Pact Specification Version:** `4.0`

---

## 1. Interaction 1: Lấy thông tin sản phẩm thành công (Happy Path)

### Provider State:
> `"product with ID P-100 exists and is in stock"`

### Request:
```http
GET /api/products/P-100 HTTP/1.1
Host: product-service.internal
Accept: application/json
```

### Expected Response:
* **HTTP Status Code:** `200 OK`
* **Headers:** `Content-Type: application/json; charset=utf-8`
* **Response Body:**
```json
{
  "id": "P-100",
  "name": "Mechanical Keyboard",
  "price": 120.50,
  "available": true
}
```

### Quy tắc so khớp (Pact Matching Rules):
* `$.id`: Bắt buộc kiểu **String** (Ví dụ mẫu: `"P-100"`, Regex: `^P-[0-9]+$`).
* `$.name`: Bắt buộc kiểu **String**, không được rỗng.
* `$.price`: Bắt buộc kiểu **Number / Decimal** (Không chấp nhận chuỗi `"120.50"`). Giá trị phải $> 0$.
* `$.available`: Bắt buộc kiểu **Boolean** (`true` hoặc `false`).

---

## 2. Interaction 2: Sản phẩm không tồn tại trong hệ thống (Not Found Path)

### Provider State:
> `"product with ID P-999 does not exist"`

### Request:
```http
GET /api/products/P-999 HTTP/1.1
Host: product-service.internal
Accept: application/json
```

### Expected Response:
* **HTTP Status Code:** `404 Not Found`
* **Headers:** `Content-Type: application/json; charset=utf-8`
* **Response Body:**
```json
{
  "errorCode": "PRODUCT_NOT_FOUND",
  "message": "Product with ID P-999 does not exist"
}
```

### Quy tắc so khớp:
* `$.errorCode`: Khớp chính xác chuỗi `"PRODUCT_NOT_FOUND"`.
* `$.message`: Bắt buộc kiểu **String**.

---

## 3. Nội dung file Pact JSON chuẩn sinh ra bởi Consumer Test

```json
{
  "consumer": {
    "name": "OrderService"
  },
  "provider": {
    "name": "ProductService"
  },
  "interactions": [
    {
      "description": "a request for an existing product P-100",
      "providerStates": [
        {
          "name": "product with ID P-100 exists and is in stock"
        }
      ],
      "request": {
        "method": "GET",
        "path": "/api/products/P-100",
        "headers": {
          "Accept": "application/json"
        }
      },
      "response": {
        "status": 200,
        "headers": {
          "Content-Type": "application/json"
        },
        "body": {
          "id": "P-100",
          "name": "Mechanical Keyboard",
          "price": 120.50,
          "available": true
        },
        "matchingRules": {
          "body": {
            "$.id": {
              "matchers": [{ "match": "type" }]
            },
            "$.name": {
              "matchers": [{ "match": "type" }]
            },
            "$.price": {
              "matchers": [{ "match": "number" }]
            },
            "$.available": {
              "matchers": [{ "match": "type" }]
            }
          }
        }
      }
    },
    {
      "description": "a request for a non-existing product P-999",
      "providerStates": [
        {
          "name": "product with ID P-999 does not exist"
        }
      ],
      "request": {
        "method": "GET",
        "path": "/api/products/P-999",
        "headers": {
          "Accept": "application/json"
        }
      },
      "response": {
        "status": 404,
        "headers": {
          "Content-Type": "application/json"
        },
        "body": {
          "errorCode": "PRODUCT_NOT_FOUND",
          "message": "Product with ID P-999 does not exist"
        }
      }
    }
  ],
  "metadata": {
    "pactSpecification": {
      "version": "4.0"
    }
  }
}
```
