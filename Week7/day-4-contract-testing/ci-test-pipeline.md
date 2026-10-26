# CI/CD Quality Gate & Contract Verification Pipeline

## 1. Thiết Kế Pipeline CI/CD Tích Hợp Contract Testing

```
[Git Commit / Pull Request]
            |
            v
+---------------------------------------------------------------------------------+
| Stage 1: Build & Unit Test                                                      |
| - Compile mã nguồn Java                                                         |
| - Chạy Local Unit Tests                                                         |
+---------------------------------------------------------------------------------+
            |
            v
+---------------------------------------------------------------------------------+
| Stage 2: Contract Verification (Quality Gate Cốt Lõi)                           |
|                                                                                 |
| * Phía Producer (Product Service):                                              |
|   - Tự động sinh test từ file contract                                         |
|   - Chạy test xác thực controller:                                              |
|     + Nếu Producer xóa trường `available` -> Auto-generated test FAIL!         |
|     + Build bị ngắt (FAIL BUILD) -> Chặn merge code lên branch chính!           |
|                                                                                 |
| * Phía Consumer (Order Service):                                                |
|   - Tải Stub Artifact của Producer từ Nexus / Artifactory                       |
|   - Chạy WireMock Stub Runner test tích hợp client API                          |
+---------------------------------------------------------------------------------+
            |
            v (Pass Quality Gate)
+---------------------------------------------------------------------------------+
| Stage 3: Publish Contract Stubs                                                 |
| - Đóng gói file JAR chứa Contract Stubs                                         |
| - Publish lên kho lưu trữ Artifact (Maven Repository) kèm versioning            |
+---------------------------------------------------------------------------------+
            |
            v
+---------------------------------------------------------------------------------+
| Stage 4: Build Docker Image & Deploy Staging                                    |
+---------------------------------------------------------------------------------+
```

---

## 2. Ưu Điểm So Với End-to-End (E2E) Testing
- **Tốc độ thực thi:** Contract Test chạy hoàn toàn in-memory trong thời gian vài giây (thay vì hàng giờ như E2E test phải bật đầy đủ các container).
- **Phát hiện lỗi sớm (Shift-Left Testing):** Phát hiện xung đột giao tiếp API ngay tại thời điểm mở Pull Request, không để lọt lỗi tới môi trường Integration/Staging.
- **Tính ổn định (No Flakiness):** Loại bỏ hoàn toàn sự cố test thất bại do nghẽn mạng, timeout hạ tầng hoặc database bị bẩn dữ liệu seed.
