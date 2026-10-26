# 07. Security by Design & STRIDE Threat Modeling

## 1. Kiến Trúc Bảo Mật & Mô Hình Ủy Quyền (Security Architecture)

Hệ thống Payroll SaaS áp dụng mô hình Zero-Trust kết hợp OAuth 2.0 / OpenID Connect:
- Authentication: Tập trung tại Identity Provider (Keycloak / Okta). Cấp phát Asymmetric Signed JWT (RSA-256).
- Multi-Tenant Token Verification: Token chứa claims bắt buộc: `sub` (userId), `tid` (tenantId), `roles`, `scope`, `iss`, `aud`, `exp`.
- Tenant Context Enforcement: API Gateway và Service Filters trích xuất `tid` từ JWT claims và lưu vào `TenantContextHolder` (ThreadLocal). Tuyệt đối không tin cậy `tenantId` truyền qua Request Body hoặc Path Variable từ phía client.

---

## 2. Bảng Mô Hình Đe Dọa Chi Tiết (Threat Model Matrix - STRIDE & OWASP Top 10)

| Tài nguyên (Asset) & Endpoint | Phân loại mối đe dọa (STRIDE) | Đường dẫn tấn công (Attack Path) | Biện pháp giảm thiểu (Mitigation) | Cơ chế phát hiện (Detection) |
| :--- | :--- | :--- | :--- | :--- |
| `GET /payslips/{id}`<br>(Payslip Asset) | Broken Access Control / IDOR (BOLA) | Kẻ tấn công thay đổi `{id}` trên URL thành ID của nhân viên khác để xem trộm thông tin thu nhập cá nhân. | 1. Tenant & Owner Filter: Query database bắt buộc bổ sung điều kiện: `WHERE id = :id AND tenant_id = :currentTenantId AND (employee_id = :currentUserId OR :hasAdminRole = true)`.<br>2. Sử dụng UUIDv4 hoặc Obfuscated ID thay cho Auto-increment Long ID. | Cảnh báo khi có nhiều request 403 Forbidden liên tiếp từ cùng một IP/User trong vòng 1 phút. |
| `GET /employees/{id}/salary`<br>(Salary Asset) | Sensitive Data Exposure / Token Theft | Token JWT bị đánh cắp (qua XSS hoặc Man-in-the-Middle) và kẻ tấn công gọi endpoint đọc toàn bộ bảng lương. | 1. Cấu hình thời hạn Access Token ngắn (15 phút) kết hợp Refresh Token Rotation.<br>2. Toàn bộ API bắt buộc chạy qua TLS 1.3.<br>3. Masking dữ liệu nhạy cảm (ví dụ số tài khoản ngân hàng chỉ hiển thị 4 số cuối). | Phát hiện IP bất thường thông qua User-Agent mismatch hoặc Geo-location jump trong thời gian ngắn. |
| `PUT /employees/{id}/salary`<br>(Salary Modification) | Tampering / Thiếu Audit Logging | HR Admin nâng khống mức lương của người quen nhưng xóa vết hoặc không có bản ghi lịch sử để đối soát. | 1. Bắt buộc kích hoạt Hibernate Envers lưu vết toàn bộ revision (Actor, Old Value, New Value, Correlation ID).<br>2. Áp dụng Maker-Checker Principle (Dual-control workflow: Thay đổi lương cần 1 HR đề xuất và 1 Giám đốc phê duyệt). | Audit Service định kỳ đối soát số lượng bản ghi update với số lượng sự kiện audit được ghi nhận. |
| `POST /payroll-runs`<br>(Payroll Execution) | Denial of Service / Rate-Limit Abuse | Kẻ tấn công gọi liên tục endpoint tính lương làm cạn kiệt tài nguyên CPU/RAM của Worker pool. | 1. Rate Limiting tại API Gateway bằng Token Bucket Algorithm: Giới hạn tối đa 5 requests/phút per tenant cho endpoint này.<br>2. Distributed Lock: Chỉ cho phép 1 tiến trình xử lý tại một thời điểm per tenant. | Alert kích hoạt khi rate limit threshold bị vượt qua 5 lần liên tiếp. |
| Toàn bộ hệ sinh thái API | SQL Injection / Expression Injection | Kẻ tấn công truyền chuỗi SQL độc qua các tham số lọc hoặc SpEL injection qua trường metadata. | 1. Sử dụng Hibernate Parameterized Queries / Criteria API tuyệt đối, cấm String Concatenation.<br>2. Validate chặt chẽ dữ liệu đầu vào bằng Hibernate Validator (`@Pattern`, `@Size`). | WAF (Web Application Firewall) phát hiện các mẫu payload chứa ký tự đặc biệt (`'`, `--`, `UNION`). |
| Kubernetes / Pod Configuration | Security Misconfiguration | Image chạy dưới quyền `root`, port debug mở trên production, hoặc secret DB bị commit nhầm vào repository. | 1. Enforce Non-root user trong Dockerfile (`USER 10001`).<br>2. Tích hợp HashiCorp Vault / Kubernetes Secrets, cấm lưu secret trong Git.<br>3. Scan image tự động trong CI bằng Trivy / Snyk. | CI Pipeline tự động ngắt (Fail build) nếu phát hiện CVE mức độ High/Critical. |

---

## 3. Quy Tắc Xác Thực Quyền Ở Mức Dữ Liệu (Data-Level Authorization)

Ví dụ mã nguồn triển khai kiểm tra quyền kết hợp Tenant Isolation:

```java
@PreAuthorize("hasAuthority('PAYROLL_READ')")
@GetMapping("/payslips/{id}")
public PayslipResponse getPayslipById(@PathVariable UUID id) {
    String currentTenantId = TenantContext.getTenantId();
    String currentUserId = SecurityContext.getUserId();
    boolean isManager = SecurityContext.hasRole("HR_MANAGER");

    return payslipRepository.findByIdAndTenantId(id, currentTenantId)
        .filter(payslip -> isManager || payslip.getEmployeeId().equals(currentUserId))
        .map(payslipMapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Payslip not found or access denied"));
}
```
