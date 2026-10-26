# Module 1 — Advanced Authorization & RBAC/BAC

## 1. Advanced Authorization & RBAC/ABAC

## **1. RBAC — Role-Based Access Control**

### **Cần học gì**

- Role, permission, authority.
- User → Role → Permission.
- Role hierarchy.
- Request-level authorization vs method-level authorization.
- Khi RBAC đủ và khi RBAC bắt đầu cồng kềnh.

### **Cần hiểu đến mức nào**

Ví dụ hệ thống HR/Payroll:

`Roles:
- EMPLOYEE
- HR_SPECIALIST
- HR_MANAGER
- PAYROLL_ADMIN
- AUDITOR
- SYSTEM_ADMIN

Permissions:
- employee:read:self
- employee:update:self
- salary:read
- salary:update
- payroll:run
- audit:read`

RBAC:

`User A
  ↓
ROLE_HR_MANAGER
  ↓
salary:read
salary:update
employee:read`

Role hierarchy:

`ROLE_SYSTEM_ADMIN
    ↓
ROLE_PAYROLL_ADMIN
    ↓
ROLE_HR_MANAGER
    ↓
ROLE_HR_SPECIALIST`

Spring Security có `GrantedAuthority` cho authority và hỗ trợ role hierarchy để role cấp cao có thể kế thừa permission/authority cấp thấp. (docs.spring.io)

### **Assignment**

Tạo `01-rbac-design.md`:

- Thiết kế roles và permissions cho HR/Payroll SaaS.
- Chỉ ra role nào kế thừa role nào.
- Phân tích vì sao không nên chỉ dùng `ROLE_ADMIN` cho mọi action.
- Nêu 3 rủi ro của “role explosion”.

### **Hoàn thành khi**

- [ ]  Phân biệt role và permission.
- [ ]  Có role hierarchy hợp lý.
- [ ]  Không dùng role cho mọi điều kiện business.
- [ ]  Nêu được trường hợp RBAC không đủ chi tiết.

## 2. ABAC — Attribute-Based Access Control

### **Cần học gì**

- Subject attributes: user, role, department, tenant, location.
- Resource attributes: owner, tenantId, status, classification.
- Environment attributes: thời gian, IP range, location, risk score.
- Policy-based authorization.
- RBAC + ABAC kết hợp.

### **Cần hiểu đến mức nào**

ABAC không hỏi đơn thuần:

`“User có role HR_MANAGER không?”`

Mà hỏi:

`“User có role HR_MANAGER,
thuộc cùng tenant,
quản lý department của employee,
và thao tác đang diễn ra trong giờ được phép không?”`

Ví dụ policy:

`Cho phép HR_MANAGER cập nhật salary nếu:
- user.tenantId == employee.tenantId
- user.departmentId == employee.departmentId
- employee.status != TERMINATED
- user có permission salary:update`

### **Assignment**

Tạo `02-abac-policy.md` cho các rule:

| **Action** | **Điều kiện RBAC** | **Điều kiện ABAC** |
| --- | --- | --- |
| Xem salary |  |  |
| Sửa salary |  |  |
| Chạy payroll |  |  |
| Xem audit log |  |  |
| Employee xem profile |  |  |

### **Hoàn thành khi**

- [ ]  Phân biệt RBAC và ABAC.
- [ ]  Biết ABAC cần attribute của subject/resource/environment.
- [ ]  Không hard-code toàn bộ policy trong controller.
- [ ]  Biết RBAC và ABAC có thể kết hợp.


## 3. Method Security, SpEL, Permission Evaluator và ACL

### **Cần học gì**

- `@PreAuthorize`.
- `@PostAuthorize`.
- SpEL security expression.
- Custom `PermissionEvaluator`.
- Domain Object Security / ACL.
- Inherited permissions.
- Multi-tenant authorization.

Ví dụ:

`@PreAuthorize("hasAuthority('salary:read')")
public SalaryResponse getSalary(UUID employeeId) {
    // ...
}`

Ví dụ object-level rule:

`@PreAuthorize("@salaryPermission.canEdit(authentication, #employeeId)")
public void updateSalary(UUID employeeId, UpdateSalaryRequest request) {
    // ...
}`

`@PreAuthorize` được xử lý trước method invocation; Spring Security cũng hỗ trợ `@PostAuthorize`, `hasPermission()` và ACL-based authorization cho domain object security. (docs.spring.io)

### **Assignment**

Tạo `03-method-security-design.md`:

Thiết kế authorization cho API:

`GET  /employees/{id}
PUT  /employees/{id}/salary
POST /payroll-runs
GET  /audit/salary/{employeeId}`

Yêu cầu:

- Rule nào nên dùng request-level security.
- Rule nào nên dùng method-level security.
- Rule nào cần custom permission evaluator.
- Rule nào cần object-level ACL/ownership check.
- Test cases allow/deny cho mỗi API.

### **Hoàn thành khi**

- [ ]  Biết `@PreAuthorize` không tự bảo vệ method không có annotation.
- [ ]  Không chỉ dựa vào controller security cho rule quan trọng.
- [ ]  Có test allow/deny cho authorization.
- [ ]  Phân biệt role check và object ownership check.