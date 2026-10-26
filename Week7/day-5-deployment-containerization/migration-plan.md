# Database Migration Plan: Expand / Contract Pattern

## 1. Bài Toán Nghiệp Vụ
Bổ sung trạng thái chi tiết cho đơn hàng:
- Trạng thái cũ: `PENDING`, `CONFIRMED`, `CANCELLED`.
- Trạng thái mới: `PENDING_PAYMENT`, `PAYMENT_PROCESSING`, `CONFIRMED`, `PAYMENT_FAILED`, `CANCELLED`.

---

## 2. Các Bước Thực Thi (Step-by-Step Phases)

### Phase 1: Expand (Release N)
- **Thao tác DDL:** Thêm cột mới `payment_detailed_status` nullable:
  ```sql
  ALTER TABLE orders ADD COLUMN payment_detailed_status VARCHAR(32) NULL;
  ```
- **Hành vi ứng dụng:**
  - Phiên bản ứng dụng v1 cũ: Tiếp tục đọc và ghi cột `status`.
  - Phiên bản ứng dụng v2 mới: Đọc `status` (nếu `payment_detailed_status` là null), ghi đồng thời vào cả `status` và `payment_detailed_status`.

### Phase 2: Backfill (Background Data Migration)
- Chạy batch script cập nhật các bản ghi lịch sử theo lô (Batch size = 1,000 bản ghi):
  ```sql
  UPDATE orders 
  SET payment_detailed_status = CASE 
      WHEN status = 'PENDING' THEN 'PENDING_PAYMENT'
      WHEN status = 'CONFIRMED' THEN 'CONFIRMED'
      WHEN status = 'CANCELLED' THEN 'CANCELLED'
  END
  WHERE payment_detailed_status IS NULL;
  ```

### Phase 3: Cutover (Release N+1)
- Ứng dụng chuyển đổi hoàn toàn: Chỉ đọc và ghi vào cột mới `payment_detailed_status`.
- Thêm ràng buộc NOT NULL:
  ```sql
  ALTER TABLE orders ALTER COLUMN payment_detailed_status SET NOT NULL;
  ```

### Phase 4: Contract (Release N+2)
- Sau khi toàn bộ các pod chạy phiên bản mới ổn định tối thiểu 1 tháng, thực thi DDL drop cột cũ:
  ```sql
  ALTER TABLE orders DROP COLUMN status;
  ```
