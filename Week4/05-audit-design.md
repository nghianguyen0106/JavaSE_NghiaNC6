# 05. Audit Trail Design: Hibernate Envers

## 1. Bản chất Audit Trail vs Application Log

* **Application Log (Logback/SLF4J):** Ghi nhận diễn biến kỹ thuật để lập trình viên điều tra lỗi hệ thống (Debug, Exception, Warning). Dữ liệu phi cấu trúc, thường được xoay vòng (log rotation) và xóa sau 7–30 ngày.
* **Audit Trail (Hibernate Envers):** Ghi nhận lịch sử biến động trạng thái dữ liệu nghiệp vụ theo thời gian (Ai sửa, sửa lúc nào, giá trị cũ/mới là gì) phục vụ mục đích pháp lý, kiểm toán tài chính và giải quyết khiếu nại. Dữ liệu có cấu trúc trong DB và phải được bảo lưu lâu dài.

---

## 2. Thiết kế Audit Schema với Hibernate Envers

### A. Mở rộng thông tin Revision (Custom Revision Entity)
Mặc định Envers chỉ lưu `id` và `timestamp`. Để biết **ai sửa** và **từ request nào**, ta mở rộng `DefaultRevisionEntity`:

```java
@Entity
@RevisionEntity(CustomRevisionListener.class)
@Table(name = "custom_revinfo")
public class CustomRevisionEntity extends DefaultRevisionEntity {
    private String actor;         // Username / UserID của HR Manager thực hiện
    private String correlationId;  // TraceId từ HTTP Request Header để liên kết log
    private String clientIp;       // Địa chỉ IP của client
}
```

### B. Schema Database tối giản

#### Bảng thông tin phiên bản (`custom_revinfo`):
```sql
CREATE TABLE custom_revinfo (
    rev BIGINT PRIMARY KEY,
    revtstmp BIGINT NOT NULL,
    actor VARCHAR(100),
    correlation_id VARCHAR(64),
    client_ip VARCHAR(45)
);
```

#### Bảng lịch sử thay đổi lương (`employee_salary_aud`):
```sql
CREATE TABLE employee_salary_aud (
    id BIGINT,
    rev BIGINT,
    revtype SMALLINT, -- 0: ADD (tạo mới), 1: MOD (chỉnh sửa), 2: DEL (xóa)
    employee_id BIGINT,
    base_salary DECIMAL(15, 2),
    effective_date DATE,
    status VARCHAR(30),
    PRIMARY KEY (id, rev),
    FOREIGN KEY (rev) REFERENCES custom_revinfo(rev)
);
```

---

## 3. Quy định dữ liệu được phép Audit & Cấm Audit

| Loại dữ liệu | Ví dụ cụ thể | Chính sách Audit |
| :--- | :--- | :--- |
| **BẮT BUỘC Audit** | `baseSalary`, `effectiveDate`, `status`, `allowances` | Gắn `@Audited` để giám sát biến động tài chính và phúc lợi. |
| **KHÔNG ĐƯỢC Audit** | `password_hash`, `bank_pin`, `refresh_token`, mã thẻ bảo mật | Đánh dấu `@NotAudited` để tránh vi phạm quy chuẩn an toàn và rò rỉ dữ liệu nhạy cảm vào bảng phụ. |
| **Dữ liệu tạm/kỹ thuật** | `updated_at`, `temp_cache_data` | Đánh dấu `@NotAudited` để tránh lãng phí dung lượng lưu trữ không cần thiết. |

---

## 4. Cơ chế hiển thị Lịch sử thay đổi cho HR/Admin

Sử dụng `AuditReader` của Hibernate Envers để truy vấn:

1. **Xem lịch sử tất cả các lần thay đổi của một nhân viên:**
   ```java
   AuditReader reader = AuditReaderFactory.get(entityManager);
   List<Number> revisions = reader.getRevisions(EmployeeSalary.class, salaryId);
   ```
2. **Xem giá trị lương tại một thời điểm cụ thể trong quá khứ (Time Travel Query):**
   ```java
   Date targetDate = ...;
   Number revNumber = reader.getRevisionNumberForDate(targetDate);
   EmployeeSalary historicalSalary = reader.find(EmployeeSalary.class, salaryId, revNumber);
   ```

* **Hiển thị trên giao diện (UI Admin Timeline):**
  * Danh sách dạng Timeline:
    * *Thời gian:* `2026-10-15 14:30:00` | *Người sửa:* `hr_admin_lan` | *Request:* `req-abc-123`
    * *Thay đổi:* `baseSalary`: `20,000,000` $\rightarrow$ `25,000,000` VNĐ.
    * *Trạng thái:* `ACTIVE`.

---

## 5. Lưu ý về Performance & Data Retention
* **Chi phí ghi (Write amplification):** Mỗi lần cập nhật bảng chính sẽ kéo theo 2 lần insert (1 vào `revinfo` và 1 vào `_aud`). Cần đánh index hợp lý trên `(id, rev)` và `revtstmp`.
* **Chiến lược dọn dẹp dữ liệu (Retention Policy):** Dữ liệu bảng audit phình to rất nhanh. Cần có chính sách lưu trữ theo phân vùng (Partitioning theo năm) và archive các bản ghi cũ hơn 3 năm sang Data Lake / Cold Storage (S3/Parquet).
