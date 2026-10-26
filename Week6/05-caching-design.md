# 05. Distributed Caching Strategies & Cache Invalidation

## 1. Caching Decision Matrix

| Dữ liệu (Data) | Caching? | Chiến lược (Strategy) | TTL đề xuất | Cơ chế Invalidation | Chấp nhận Stale? | Lý do kỹ thuật |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| A. Employee Profile | CÓ | Cache-Aside (Lazy Loading) | 60 phút | Event-Driven Invalidation (`EmployeeUpdatedEvent`) | CÓ (Tối đa vài giây đến khi event invalidate chạy xong) | Tần suất đọc cao (Read-heavy), tần suất cập nhật thấp. Dữ liệu profile hiển thị trên giao diện không yêu cầu strictly immediate consistency. |
| B. Leave Balance | KHÔNG | Direct DB Query (hoặc Write-Through với Transactional Lock) | 0 (Không cache) | N/A | TUYỆT ĐỐI KHÔNG | Thuộc tính số dư là đối tượng tranh chấp giao dịch khi nộp đơn nghỉ phép đồng thời (Race Condition). Cache số dư dễ dẫn tới duyệt đơn vượt quá hạn mức (Over-allocation). |
| C. Payroll Payslip | CÓ | Read-Through / Cache-Aside | 24 giờ (hoặc vĩnh viễn đến khi kỳ lương bị chỉnh sửa) | Explicit Eviction khi `PayrollRun` bị recalculate | KHÔNG (Dữ liệu tài chính, nhưng bản ghi Payslip là Immutable sau khi hoàn tất) | Payslip sau khi chốt kỳ lương là dữ liệu bất biến (Immutable Data). Cache giúp giải quyết đỉnh tải (read spike) khi hàng triệu nhân viên vào xem cùng lúc. |
| D. Tax Rule | CÓ | In-Memory Local Cache (Caffeine) + Distributed Sync | 24 giờ | Manual Admin Push / Pub-Sub reload | KHÔNG (Tuy nhiên luật thuế thay đổi theo năm/chu kỳ pháp lý đã định trước) | Dữ liệu dung lượng nhỏ (<100KB), tần suất đọc trong mẻ tính lương cực lớn (hàng triệu lần). Dùng Local Cache loại bỏ hoàn toàn Network Hop tới Redis. |
| E. Permission Policy | CÓ | Cache-Aside kết hợp In-Memory Short TTL | 5 phút | Xóa cache ngay khi role/permission bị chỉnh sửa qua Admin API | CÓ (Chấp nhận độ trễ quyền tối đa 5 phút nếu event bus lỗi) | Kiểm tra quyền (Authorization Check) diễn ra trên mọi request. Cache giúp giảm tải cho DB/Identity Service. |

---

## 2. Cache Key Schema

Quy chuẩn định dạng: `{system}:{tenantId}:{entity}:{identifier}`

1. Employee Profile:
   - Pattern: `hr:tenant:{tenantId}:emp:{employeeId}:profile`
   - Data structure: Redis `HASH` hoặc serialized JSON `STRING`.
2. Payroll Payslip:
   - Pattern: `payroll:tenant:{tenantId}:period:{payPeriod}:payslip:{employeeId}`
   - Data structure: Serialized JSON `STRING` (nén zstd nếu kích thước >10KB).
3. Tax Rule:
   - Pattern: `tax:country:{countryCode}:year:{taxYear}`
   - Data structure: In-Memory Java Object (Caffeine Cache trên từng JVM instance).
4. User Permission Policy:
   - Pattern: `auth:tenant:{tenantId}:user:{userId}:permissions`
   - Data structure: Redis `SET` chứa danh sách permission strings (ví dụ `["payroll:run", "employee:read"]`).

---

## 3. Cơ Chế Xử Lý Các Sự Cố Cache Phổ Biến

### 3.1. Cache Stampede (Thundering Herd)
- Hiện tượng: Khi một key quan trọng (ví dụ Tax Rule hoặc Payslip mẫu) hết hạn đúng thời điểm hàng ngàn request đồng thời đổ về, tất cả đều cache miss và cùng truy vấn DB, gây sập DB connection pool.
- Giải pháp:
  - Áp dụng Distributed Mutex Lock (Redisson): Request đầu tiên bị miss sẽ chiếm lock để query DB và nạp cache; các request khác đợi từ 50–100ms hoặc đọc giá trị cũ tạm thời.
  - Áp dụng XFetch / Probabilistic Early Expiration: Tính toán xác suất để một background thread chủ động refresh cache trước khi TTL thực tế hết hạn.

### 3.2. Cache Penetration (Truy vấn khóa không tồn tại)
- Hiện tượng: Hacker gửi liên tục các request truy vấn `employeeId` ngẫu nhiên không tồn tại, khiến request luôn bypass cache và đánh thẳng vào DB.
- Giải pháp:
  - Bloom Filter: Đặt Bloom Filter trước cache layer để kiểm tra nhanh sự tồn tại của `employeeId` theo tenant. Nếu Bloom Filter trả về `false`, trả về 404 ngay lập tức mà không truy vấn DB.
  - Cache Null Objects: Lưu cache kết quả rỗng `"{}"` với TTL rất ngắn (ví dụ 30–60 giây).

### 3.3. Cache Breakdown
- Hiện tượng: Một key "hot" (Hot Key) bất ngờ hết hạn.
- Giải pháp: Cấu hình Jitter cho TTL (ngẫu nhiên hóa TTL thêm `random(1..300)s`) để phân tán thời điểm hết hạn của các key cùng tạo trong một mẻ batch.

---

## 4. Cache Observability Metrics

1. Cache Hit Ratio: Đo lường tỷ lệ hit: $\frac{\text{hits}}{\text{hits} + \text{misses}}$. Mục tiêu cho Employee Profile và Payslip $> 90\%$.
2. Cache Latency: Thời gian đọc/ghi Redis: $P95 < 2\text{ms}$, $P99 < 5\text{ms}$.
3. Eviction Rate: Tần suất Redis đẩy key ra ngoài do chạm ngưỡng bộ nhớ (`maxmemory-policy: allkeys-lru`). Nếu chỉ số này tăng đột biến $\rightarrow$ Cần scale RAM.
4. Database Fallback Rate: Tần suất request rơi xuống Database sau khi cache miss. Cần thiết lập cảnh báo khi chỉ số này vượt quá ngưỡng bình thường 3 lần.
