# 03. Method Security Design: SpEL & Permission Evaluator

## 1. Phân tầng kiểm soát: Request-Level vs Method-Level Security

* **Request-Level Security (`SecurityFilterChain`):** Đóng vai trò tường lửa thô ngoài cùng, kiểm tra trạng thái đăng nhập (Authentication) và phân luồng URL cơ bản (ví dụ `/api/public/**` vs `/api/admin/**`).
* **Method-Level Security (`@PreAuthorize`, `@PostAuthorize`):** Đóng vai trò bảo vệ cốt lõi bên trong, gắn trực tiếp vào interface/method tầng Service để kiểm tra quyền chi tiết (Ownership, Tenant isolation, Business state) mà tầng URL không thể nắm bắt được.

---

## 2. Thiết kế Authorization chi tiết cho 4 API cốt lõi

### API 1: `GET /employees/{id}`
* **Request-level:** Đã đăng nhập (`authenticated()`).
* **Method-level:**
  ```java
  @PreAuthorize("hasAuthority('employee:read:all') or @employeeSecurity.isSelfOrManager(authentication, #id)")
  public EmployeeResponse getEmployeeById(Long id) { ... }
  ```
* **Cơ chế:** Kiểm tra Object-level ownership: User có thể xem nếu có quyền đọc toàn bộ, HOẶC user chính là chủ sở hữu bản ghi (`user.id == employee.userId`), HOẶC user là quản lý trực tiếp của nhân viên đó.

### API 2: `PUT /employees/{id}/salary`
* **Request-level:** Bắt buộc có role thuộc nhóm quản lý (`hasAnyRole('HR_MANAGER', 'PAYROLL_ADMIN')`).
* **Method-level & Custom Security Bean:**
  ```java
  @PreAuthorize("@salarySecurity.canUpdateSalary(authentication, #id, #request)")
  public void updateSalary(Long id, UpdateSalaryRequest request) { ... }
  ```
* **Cơ chế Custom Evaluator:** Kiểm tra đồng thời:
  1. Cùng `tenantId`.
  2. Nhân viên có trạng thái `ACTIVE` (không sửa lương người đã nghỉ việc).
  3. Mức lương mới không vượt quá ngân sách trần của phòng ban (`departmentBudget`).

### API 3: `POST /payroll-runs`
* **Request-level:** Yêu cầu quyền vận hành lương (`hasAuthority('payroll:run')`).
* **Method-level:**
  ```java
  @PreAuthorize("hasRole('PAYROLL_ADMIN') and @payrollSecurity.isAllowedTimeWindow()")
  public PayrollRunResponse executePayrollRun(PayrollRunRequest request) { ... }
  ```
* **Cơ chế:** Kiểm tra Environment Attribute (chỉ cho phép chạy trong khung giờ chốt sổ từ ngày 25 đến cuối tháng, ngăn chặn chạy nhầm giữa tháng).

### API 4: `GET /audit/salary/{employeeId}`
* **Request-level:** Yêu cầu quyền kiểm toán (`hasAuthority('audit:read')`).
* **Method-level:**
  ```java
  @PreAuthorize("hasRole('AUDITOR') and @tenantSecurity.isSameTenant(#employeeId)")
  public List<SalaryAuditResponse> getSalaryAuditTrail(Long employeeId) { ... }
  ```
* **Cơ chế:** Ngăn chặn IDOR (Insecure Direct Object Reference) đa tổ chức: Auditor của Tenant A tuyệt đối không được xem lịch sử lương của nhân viên Tenant B.

---

## 3. Ma trận Test Cases: Allow vs Deny cho từng API

| API | Kịch bản Test | Actor & Ngữ cảnh | Kết quả kỳ vọng |
| :--- | :--- | :--- | :---: |
| **`GET /employees/{id}`** | TC1: Nhân viên xem hồ sơ của chính mình | `ROLE_EMPLOYEE`, `id = 100`, user sở hữu `id = 100` | **200 OK (ALLOW)** |
| | TC2: Nhân viên xem lén hồ sơ đồng nghiệp | `ROLE_EMPLOYEE`, `id = 200`, user sở hữu `id = 100` | **403 Forbidden (DENY)** |
| | TC3: HR Manager xem hồ sơ nhân viên trong công ty | `ROLE_HR_MANAGER`, cùng `tenantId` | **200 OK (ALLOW)** |
| **`PUT /employees/{id}/salary`** | TC4: HR Manager sửa lương cho nhân viên đang làm việc | `ROLE_HR_MANAGER`, cùng Tenant, status `ACTIVE` | **200 OK (ALLOW)** |
| | TC5: HR Specialist cố tình gọi API sửa lương | `ROLE_HR_SPECIALIST` (chỉ có quyền `salary:read`) | **403 Forbidden (DENY)** |
| | TC6: HR Manager sửa lương cho nhân viên đã thôi việc | `ROLE_HR_MANAGER`, nhân viên status `TERMINATED` | **403 Forbidden (DENY)** |
| **`POST /payroll-runs`** | TC7: Payroll Admin chạy lương vào ngày 28 | `ROLE_PAYROLL_ADMIN`, ngày chạy 28/10 | **200 OK (ALLOW)** |
| | TC8: Payroll Admin bấm chạy lương ngày 10 | `ROLE_PAYROLL_ADMIN`, ngày chạy 10/10 (ngoài cửa sổ) | **403 Forbidden (DENY)** |
| **`GET /audit/salary/{id}`** | TC9: Auditor xem log nhân viên cùng công ty | `ROLE_AUDITOR`, cùng `tenantId` | **200 OK (ALLOW)** |
| | TC10: Auditor Tenant X xem log nhân viên Tenant Y | `ROLE_AUDITOR` Tenant X, nhân viên Tenant Y | **403 Forbidden (DENY)** |
