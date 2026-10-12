# 3. Spring AOP: Custom Aspect, @Around, Annotation-driven AOP
 ### **Cần học gì**

- Aspect.
- Join point.
- Pointcut.
- Advice.
- `@Before`, `@AfterReturning`, `@AfterThrowing`, `@Around`.
- Proxy-based AOP.
- Annotation-driven AOP.
- Cross-cutting concern là gì.
- Khi nào **không nên** dùng AOP.

Trong Spring AOP, join point của Spring là method execution; `@Around` là advice mạnh nhất vì có thể xử lý trước/sau, thay đổi kết quả hoặc throw exception, nên chỉ nên dùng khi thật cần thiết. Spring khuyến nghị dùng loại advice ít mạnh hơn nếu đủ đáp ứng yêu cầu. (docs.spring.io)

### **Cần hiểu đến mức nào**

cần phân biệt:

| **Nên dùng AOP** | **Không nên dùng AOP** |
| --- | --- |
| Logging, tracing, metrics | Core business rule |
| Audit metadata | Business workflow phức tạp |
| Authorization kiểm tra chung | Logic quyết định lương/thuế |
| Distributed lock wrapper | Logic cần đọc dễ dàng trong use case |
| Transaction boundary | Sequence nghiệp vụ quan trọng cần explicit |

### **Assignment**

Thiết kế 2 custom annotations:

`@AuditAction(action = "UPDATE_SALARY")
@DistributedLock(key = "'payroll:' + #request.month")`

cần mô tả:

- Pointcut áp dụng vào đâu.
- `@Around` advice cần làm gì.
- Log/audit cần chứa field nào.
- Lock key được tạo thế nào.
- Nếu lock không lấy được thì API trả gì.
- Nếu method throw exception thì audit ghi gì.

### **Sản phẩm cần nộp**

- `03-spring-aop-design.md`
- Sequence diagram cho `@AuditAction`.
- Sequence diagram cho `@DistributedLock`.
- Danh sách rủi ro khi lạm dụng AOP.

### **Hoàn thành khi**

- [ ]  Phân biệt aspect, pointcut, advice.
- [ ]  Giải thích được `@Around` phải gọi `proceed()` khi muốn method gốc chạy.
- [ ]  Không nhét business rule quan trọng vào aspect.
- [ ]  Có strategy cho exception, timeout và observability.