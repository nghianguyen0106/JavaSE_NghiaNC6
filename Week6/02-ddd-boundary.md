# 02. Domain-Driven Design (DDD) Boundary Definition

## 1. Xác định 4 Bounded Contexts Cốt Lõi

```
+-----------------------------------------------------------------------------------+
|                                  HR/Payroll SaaS                                  |
+-----------------------------------------------------------------------------------+
       |                                                    |
       v                                                    v
+-------------------------------+                  +-------------------------------+
|       Identity Context        |                  |       Employee Context        |
| - Authentication              |                  | - Personnel Lifecycle         |
| - Tenant Isolation            |                  | - Department / Position       |
| - RBAC Policy                 |                  | - Employment Status           |
+-------------------------------+                  +-------------------------------+
       |                                                    |
       v                                                    v
+-------------------------------+                  +-------------------------------+
|         Leave Context         |                  |        Payroll Context        |
| - Leave Entitlement           |                  | - Payroll Run                 |
| - Request / Approval Workflow |                  | - Compensation Calculation    |
| - Accrual Engine              |                  | - Payslip & Tax Profile       |
+-------------------------------+                  +-------------------------------+
```

---

## 2. Bounded Context, Aggregate Root & Invariants

| Bounded Context | Aggregate Root | Invariants (Ràng buộc bất biến nghiệp vụ) | Data Ownership |
| :--- | :--- | :--- | :--- |
| Identity Context | `UserAccount` | 1. `email` là duy nhất trên toàn hệ thống trong cùng một `tenantId`.<br>2. Tài khoản ở trạng thái `SUSPENDED` hoặc `DEACTIVATED` không được phép cấp phát access token mới.<br>3. Mọi role gán cho user bắt buộc phải thuộc cùng `tenantId`. | `identity_users`<br>`identity_roles`<br>`identity_permissions` |
| Employee Context | `EmployeeProfile` | 1. Mã nhân viên (`employeeCode`) là duy nhất trong phạm vi một tenant.<br>2. Trạng thái nhân viên chuyển dịch theo Finite State Machine: `PROBATION` $\rightarrow$ `ACTIVE` $\rightarrow$ `TERMINATED`. Không thể chuyển trực tiếp từ `TERMINATED` sang `PROBATION`.<br>3. Một nhân viên chỉ thuộc về một phòng ban (`Department`) chính tại một thời điểm. | `emp_profiles`<br>`emp_departments`<br>`emp_job_histories` |
| Leave Context | `LeaveRequest` | 1. Tổng số ngày nghỉ (`requestedDays`) không được vượt quá số dư hiện có (`leaveBalance`) của loại nghỉ phép tương ứng.<br>2. Một đơn nghỉ phép đã ở trạng thái cuối (`APPROVED`, `REJECTED`, `CANCELLED`) không được phép thay đổi trạng thái.<br>3. Thời gian nghỉ không được trùng lặp với các đơn nghỉ phép đã được `APPROVED` trước đó của cùng một nhân viên. | `leave_requests`<br>`leave_balances`<br>`leave_policies` |
| Payroll Context | `PayrollRun` | 1. Một tenant chỉ có tối đa một `PayrollRun` ở trạng thái `PROCESSING` hoặc `OPEN` cho cùng một kỳ tính lương (`payPeriod`: Tháng/Năm).<br>2. Khi `PayrollRun` đã chuyển sang trạng thái `LOCKED` hoặc `COMPLETED`, không thể bổ sung hoặc cập nhật bất kỳ bản ghi lương (`Payslip`) nào thuộc kỳ đó.<br>3. Tổng thực lĩnh (`netPay`) của mỗi Payslip phải thỏa mãn: $\text{netPay} = \text{grossPay} - (\text{statutoryDeductions} + \text{taxes} + \text{voluntaryDeductions}) \ge 0$. | `payroll_runs`<br>`payroll_payslips`<br>`payroll_salary_items`<br>`payroll_tax_profiles` |

---

## 3. Hiện Tượng Đa Nghĩa (Polysemy): Phân Tích Thực Thể "Employee"

Việc sử dụng một Canonical Data Model duy nhất chứa class `Employee` dùng chung cho toàn bộ hệ thống gây vi phạm tính đóng gói (encapsulation), tạo coupling cao và xung đột transaction boundary. Thực thể `Employee` được ánh xạ độc lập theo từng Context:

```
                  +----------------------------------------------+
                  |         Khái niệm thực tế: Nhân viên         |
                  +----------------------------------------------+
                                         |
     +-------------------+---------------+-------------------+--------------------+
     |                   |                                   |                    |
     v                   v                                   v                    v
+----------------+ +--------------------+         +--------------------+ +--------------------+
| Identity       | | Employee (HR)      |         | Leave              | | Payroll            |
| Context        | | Context            |         | Context            | | Context            |
+----------------+ +--------------------+         +--------------------+ +--------------------+
| - userId       | | - employeeId       |         | - employeeId       | | - employeeId       |
| - tenantId     | | - tenantId         |         | - tenantId         | | - tenantId         |
| - credentials  | | - personalInfo     |         | - leaveTier        | | - salaryGrade      |
| - roles        | | - hireDate         |         | - accruedDays      | | - taxProfile       |
| - mfaStatus    | | - departmentId     |         | - approverId       | | - bankAccount      |
| - authStatus   | | - employmentStatus |         | - policyId         | | - statutoryInfo    |
+----------------+ +--------------------+         +--------------------+ +--------------------+
```

### Chi tiết phân rã mô hình dữ liệu:
1. Identity Context: 
   - Đại diện bằng `UserAccount`.
   - Mục đích: Định danh, xác thực và phân quyền truy cập.
   - Thuộc tính trọng tâm: `userId`, `username`, `passwordHash`, `assignedRoles`, `mfaEnabled`, `accountStatus`.
2. Employee Context:
   - Đại diện bằng `EmployeeProfile`.
   - Mục đích: Quản lý vòng đời nhân sự, hồ sơ lý lịch, cơ cấu tổ chức.
   - Thuộc tính trọng tâm: `employeeId`, `legalName`, `dateOfBirth`, `departmentId`, `jobTitle`, `employmentStatus`.
3. Leave Context:
   - Đại diện bằng `LeaveApplicant` (Value Object hoặc Thin Entity).
   - Mục đích: Theo dõi quyền lợi nghỉ phép và hạn mức theo chính sách.
   - Thuộc tính trọng tâm: `employeeId`, `entitlementTier`, `accrualBalance`, `approverLine`.
4. Payroll Context:
   - Đại diện bằng `Payee` (Value Object gắn với Payslip).
   - Mục đích: Tính toán công nợ, khấu trừ thuế, chi trả tài chính.
   - Thuộc tính trọng tâm: `employeeId`, `baseSalary`, `taxIdentificationNumber`, `bankAccountNumber`, `bankRoutingCode`, `dependentCount`.

---

## 4. Quy Tắc Giao Tiếp Giữa Các Bounded Contexts

1. Context Boundary Isolation: Cấm chia sẻ cơ sở dữ liệu vật lý (No Shared Database Tables) giữa các Bounded Context. Mọi tương tác xuyên Context chỉ được thực hiện qua:
   - Asynchronous Integration Events (ví dụ: `EmployeeTerminatedEvent` từ Employee Context kích hoạt thu hồi quyền ở Identity Context và chốt số dư phép ở Leave Context).
   - Synchronous Open Host Service / REST API Contract thông qua Anti-Corruption Layer (ACL) để biên dịch dữ liệu ngoại lai sang domain language nội bộ.
2. Idempotency & Isolation: Aggregate Root chịu trách nhiệm bảo toàn Invariant nội bộ trong một Transaction duy nhất; tính nhất quán giữa các Context tuân theo mô hình Eventual Consistency.
