# 5. Hibernate Envers / Data Auditing
### **Cần học gì**

- Audit trail.
- Revision.
- `@Audited`.
- Audit table.
- Revision metadata: ai thay đổi, khi nào, request/correlation ID.
- Audit strategy.
- Data retention và privacy.
- Audit query performance.

Envers có thể audit entity bằng dependency `hibernate-envers` và annotation `@Audited`; nó tạo audit table cho entity và lưu historical data theo revision khi transaction commit. Revision metadata có thể mở rộng để lưu thêm thông tin như người thực hiện thay đổi. (docs.hibernate.org)

### **Assignment**

Thiết kế audit cho:

`EmployeeSalary
- employeeId
- baseSalary
- effectiveDate
- status`

Yêu cầu:

- Biết ai sửa lương.
- Biết giá trị trước/sau.
- Biết request nào gây thay đổi.
- Có thể xem salary tại thời điểm bất kỳ.
- Không audit password hoặc dữ liệu nhạy cảm không cần thiết.

### **Sản phẩm cần nộp**

- `05-audit-design.md`
- Bảng audit schema tối giản.
- Quy định dữ liệu nào audit/dữ liệu nào không audit.
- Cách hiển thị lịch sử thay đổi cho HR/Admin.

### **Hoàn thành khi**

- [ ]  Phân biệt audit trail và application log.
- [ ]  Biết audit table tăng storage/write cost.
- [ ]  Có metadata actor/correlation ID.
- [ ]  Có chính sách dữ liệu nhạy cảm và retention.