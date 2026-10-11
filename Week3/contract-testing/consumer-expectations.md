# Phân tích Kỳ vọng Người dùng: Breaking vs Non-breaking Changes

Bảng đối chiếu dưới đây phân loại chi tiết các tình huống thay đổi từ phía **Product Service (Provider)** và tác động trực tiếp tới **Order Service (Consumer)**.

---

## 1. Bảng phân loại Thay đổi API (API Evolution Compatibility Table)

| Thay đổi phía Provider | Loại thay đổi | Có làm gãy Consumer không? | Lý do kỹ thuật chuyên sâu |
| :--- | :---: | :---: | :--- |
| **Thêm field mới `description: "RGB bàn phím"`** | **Non-breaking** | 🟢 **AN TOÀN** | Jackson ObjectMapper trong Spring Boot theo mặc định (hoặc khi cấu hình `FAIL_ON_UNKNOWN_PROPERTIES = false`) sẽ tự động bỏ qua các trường lạ mà Consumer không cần. |
| **Đổi tên field `name` $\rightarrow$ `productName`** | **Breaking** | 🔴 **GÃY HỆ THỐNG** | Consumer trông đợi trường `name`. Khi đổi tên, trường `name` trong DTO của Consumer sẽ nhận giá trị `null`, làm rỗng tên hàng trong đơn. |
| **Đổi kiểu `price` từ số `120.5` sang chuỗi `"120.50"`** | **Breaking** | 🔴 **GÃY HỆ THỐNG** | Nếu Consumer cấu hình parser chặt chẽ (hoặc trong các ngôn ngữ strongly typed), việc ép chuỗi vào trường `BigDecimal` hoặc `Double` có thể gây lỗi `MismatchedInputException` hoặc `JsonParseException`. |
| **Xóa field `available`** | **Breaking** | 🔴 **GÃY HỆ THỐNG** | Consumer có logic nghiệp vụ cốt lõi: `if (!product.isAvailable()) throw new OutOfStockException()`. Mất field này sẽ khiến Jackson gán mặc định `false`, làm từ chối toàn bộ đơn hàng! |
| **Thêm optional query param `?includeDetails=true`** | **Non-breaking** | 🟢 **AN TOÀN** | Request cũ của Consumer không truyền param này vẫn được Provider xử lý với giá trị mặc định `false`, không gây ảnh hưởng luồng gọi cũ. |
| **Bọc response vào object bọc ngoài `{"data": {...}}`** | **Breaking** | 🔴 **GÃY HỆ THỐNG** | Cấu trúc root JSON thay đổi hoàn toàn, làm fail toàn bộ tầng Deserialize DTO của Consumer. |
| **Đổi HTTP Status từ `200 OK` sang `202 Accepted`** | **Breaking** | 🔴 **GÃY HỆ THỐNG** | `RestTemplate` hoặc `WebClient` của Consumer kiểm tra `response.getStatusCode() == HttpStatus.OK`. Nhận về `202` sẽ bị coi là lỗi hoặc không parse body đồng bộ được. |
| **Bắt buộc thêm Request Header mới `X-Api-Key`** | **Breaking** | 🔴 **GÃY HỆ THỐNG** | Consumer cũ chưa hề cấu hình gửi Header này $\rightarrow$ Sẽ bị Provider chặn cửa trả về ngay mã lỗi `401 Unauthorized` hoặc `400 Bad Request`. |

---

## 2. Quy trình Tiến hóa API An toàn (Safe API Evolution: Expand and Contract)

Để Product Service có thể đổi tên trường từ `name` thành `productName` và `price` thành `unitPrice` mà **hoàn toàn không làm chết Order Service**, team Product cần tuân thủ mô hình 3 giai đoạn:

```
[Phase 1: Gốc]              [Phase 2: Mở rộng (Expand)]          [Phase 3: Thu hẹp (Contract)]
Chỉ có field cũ             Hỗ trợ CẢ HAI field song song        Chỉ còn field mới
{                          {                                    {
  "name": "Keyboard",        "name": "Keyboard",                  "productName": "Keyboard",
  "price": 120.5             "productName": "Keyboard",           "unitPrice": 120.5
}                            "price": 120.5,                    }
                             "unitPrice": 120.5                 (Chỉ làm khi Order Service
                           }                                     đã cập nhật code xong!)
```

### Các bước thực hiện cụ thể:
1. **Bước 1 (Expand):** Provider deploy code mới trả về **cả 2 trường** (`name` lẫn `productName`).
2. **Bước 2 (Deprecate):** Đánh dấu `@Deprecated` trường cũ trong OpenAPI/Swagger và thêm Header `Warning: 299 - "Field 'name' is deprecated"`.
3. **Bước 3 (Migrate):** Team Order Service cập nhật code đọc trường `productName`, verify với Contract mới và deploy lên Production.
4. **Bước 4 (Contract):** Sau khi xác nhận trên Pact Broker rằng không còn Consumer nào phụ thuộc vào `name`, Product Service mới chính thức xóa bỏ trường cũ ở version tiếp theo.
