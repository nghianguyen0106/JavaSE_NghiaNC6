# API & Event Breaking Change Analysis

## 1. Phân Loại Thay Đổi (Change Categorization)

| Loại thay đổi | Bản chất kỹ thuật | Tác động Consumer | Có phải Breaking Change? |
| :--- | :--- | :--- | :---: |
| **Thêm trường mới (Add Field)** | Bổ sung trường `discountAmount: 0.0` vào response JSON hoặc event payload | Không ảnh hưởng (Consumer tuân thủ Tolerant Reader Pattern sẽ tự bỏ qua trường chưa quan tâm) | **KHÔNG** |
| **Xóa trường (Remove Field)** | Xóa trường `available: true` khỏi `ProductResponse` | **Gãy Consumer:** Mọi consumer đang đọc trường `available` để kiểm tra điều kiện checkout sẽ bị NullPointerException hoặc lỗi parse JSON | **CÓ (CRITICAL)** |
| **Đổi tên trường (Rename Field)** | Đổi `stockQuantity` thành `inStockCount` | Tương đương với việc Xóa trường cũ và Thêm trường mới | **CÓ (CRITICAL)** |
| **Thay đổi kiểu dữ liệu (Type Change)** | Đổi `price` từ số thực `2500.00` sang chuỗi `"$2,500.00"` | Consumer ném ngoại lệ Deserialization Exception | **CÓ (CRITICAL)** |
| **Siết chặt Validation (Stricter Request)** | Thêm ràng buộc bắt buộc (Mandatory Header hoặc Parameter mới) | Toàn bộ request cũ từ Consumer không gửi trường này bị trả về HTTP 400 Bad Request | **CÓ (CRITICAL)** |

---

## 2. Quy Tắc Duy Trì Tính Tương Thích (Backward Compatibility Rules)
1. **Quy tắc đọc khoan dung (Tolerant Reader):** Consumer cấu hình ObjectMapper: `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES = false`.
2. **Quy tắc phát hành trường hợp thay đổi Breaking:** Bắt buộc duy trì trường cũ song song với trường mới trong ít nhất 2 chu kỳ release (Deprecation Period). Sử dụng annotation `@Deprecated` và ghi log cảnh báo trước khi chính thức xóa trường cũ.
