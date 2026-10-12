# Hướng dẫn Kiểm thử Hợp đồng Hướng Người dùng (Consumer-Driven Contract Testing - CDC)

Thư mục này chứa toàn bộ tài liệu đặc tả hợp đồng, bảng đánh giá tương thích API và kế hoạch xác minh tự động (Provider Verification) giữa **Order Service (Consumer)** và **Product Service (Provider)** bằng công nghệ **Pact**.

---

## 1. Cấu trúc thư mục

```
contract-testing/
├── README.md                      # Tài liệu tổng quan & Hướng dẫn workflow
├── product-contract.md            # Đặc tả Contract chuẩn Pact JSON giữa Order và Product Service
├── consumer-expectations.md       # Bảng phân tích chi tiết Breaking vs Non-breaking changes
└── provider-verification-plan.md  # Kế hoạch tích hợp CI/CD & Provider State Verification
```

---

## 2. Bản chất của Consumer-Driven Contract (CDC)

Trong mô hình Microservices truyền thống, Provider (Product Service) thường tự thiết kế API và tài liệu (Swagger/OpenAPI) rồi công bố cho Consumer (Order Service). Cách làm này có nhược điểm lớn: **Provider không hề biết Consumer thực sự đang dùng những trường dữ liệu nào**, dẫn đến việc Provider vô tình sửa hoặc xóa các trường mà Consumer đang phụ thuộc sống còn.

**Mô hình CDC đảo ngược luồng này:**
1. **Consumer là người viết Contract:** Order Service chỉ định rõ: *"Tôi gọi `GET /api/products/P-100`, tôi chỉ cần các trường `id`, `name`, `price`, `available`. Các trường khác tôi không quan tâm."*
2. **Contract được xuất thành file Pact (JSON):** Chứa mẫu Request, Response và quy tắc Matching (kiểu dữ liệu, regex).
3. **Publish lên Pact Broker:** Nơi lưu trữ tập trung các phiên bản hợp đồng.
4. **Provider CI chạy Verification:** Trong pipeline CI của Product Service, Pact sẽ dựng một mock server gọi trực tiếp vào Controller của Product Service thật để kiểm tra: *"Phiên bản code mới của mày có trả đủ các trường mà Order Service yêu cầu hay không?"*.
5. **Chặn triển khai nếu gãy (Can-I-Deploy):** Nếu Product Service tự ý xóa field `available`, pipeline CI của Product Service sẽ **bị FAIL ngay lập tức**, ngăn không cho deploy lên Production!

---

## 3. Luồng phối hợp giữa 2 Team

```
[Order Service Repo (Consumer)]
        │
        ▼ (Chạy Consumer Test)
 Sinh ra: order_service-product_service.json
        │
        ▼ (Upload)
 [PACT BROKER (Trung tâm hợp đồng)] ◄──────┐
        │                                  │
        ▼ (Webhook thông báo có hợp đồng)   │ (Báo cáo kết quả verify)
[Product Service Repo (Provider)]          │
        │                                  │
        ▼ (Chạy Provider Verification) ────┘
  Kiểm tra code thật vs Contract
        │
        ├──> PASS  ──> Cho phép Deploy lên Production
        └──> FAIL  ──> CHẶN DEPLOY & Báo lỗi cho Dev
```
