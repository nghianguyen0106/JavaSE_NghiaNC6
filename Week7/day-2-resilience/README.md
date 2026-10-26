# Day 2: Service-to-Service Communication Resilience

## 1. Thành Phần Đã Hoàn Thành
- **[resilience-design.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week7/day-2-resilience/resilience-design.md):**
  1. Cấu hình OpenFeign kết hợp Resilience4j (TimeLimiter 1s, Retry 2x với Exponential Backoff & Jitter, Circuit Breaker Sliding Window, Bulkhead Semaphore 50).
  2. Bảng phân loại chi tiết: Transient Errors (được phép retry) vs Non-Retryable Errors (cấm retry).
  3. Chiến lược Idempotency Key cho tác vụ trừ tiền (Payment Retry Safety).
  4. Cơ chế Bulkhead Isolation ngăn chặn Cascading Failure (đảm bảo sự cố của Inventory Service không làm tê liệt các API độc lập như `GET /orders/{id}`).

---

## 2. Tiêu Chí Nghiệm Thu (DoD Checklist)
- [x] Phân biệt rõ ràng cơ chế hoạt động của Timeout, Retry, Circuit Breaker và Bulkhead.
- [x] Cấm tuyệt đối hành vi retry mù quáng cho mọi exception (chỉ retry lỗi transient).
- [x] Tác vụ Payment Retry bắt buộc có Idempotency Strategy.
- [x] Đảm bảo Inventory failure không gây ảnh hưởng tới Get Order latency.
