# Module 3 — Performance Profiling & Tuning

## 1. Profiling bằng VisualVM, JFR, CPU/Memory/Thread Analysis

### **Cần học gì**

- Profiling vs monitoring.
- CPU profiling.
- Allocation profiling.
- Heap trend.
- Thread analysis.
- Java Flight Recorder (JFR).
- VisualVM overview.
- Flame graph/call tree ở mức khái niệm.
- Safe profiling trong production.

JFR có các configuration như `default.jfc` và `profile.jfc`; Java hỗ trợ khởi động recording bằng `-XX:StartFlightRecording`, cho phép thu thập dữ liệu chẩn đoán với mức overhead cần được đánh giá theo môi trường. (docs.oracle.com)

### **Cần hiểu đến mức nào**

| **Symptom** | **Evidence cần xem** |
| --- | --- |
| CPU cao | CPU sample, hot methods, thread states |
| Memory tăng | Heap after GC, allocation rate, retained objects |
| API chậm nhưng CPU thấp | Thread dump, blocked I/O, DB pool, lock wait |
| Latency tăng theo traffic | DB query, connection pool, GC, downstream latency |
| Throughput thấp | Thread saturation, locks, blocking calls, queue depth |

### **Assignment**

Tạo `06-profiling-runbook.md`:

Cho scenario:

`Payroll Dashboard p99 = 8s.
CPU: 35%.
Heap: ổn định.
Database CPU: 40%.
Thread count: 450.
Có 300 thread đang WAITING ở HTTP client call đến Tax Service.`

Yêu cầu:

- Root-cause hypothesis.
- Artifact cần lấy: JFR, thread dump, metrics nào.
- Vì sao heap dump không phải ưu tiên đầu tiên.
- Immediate mitigation.
- Long-term fix.

### **Hoàn thành khi**

- [ ]  Biết profiler không thay thế metrics/log/tracing.
- [ ]  Biết chọn artifact theo symptom.
- [ ]  Phân biệt CPU-bound, memory-bound, I/O-bound.
- [ ]  Kết luận dựa trên evidence.

## 2. Performance Anti-patterns trong Spring/Hibernate

### **Cần học gì**

- N+1 query.
- Missing index.
- Fetch all records.
- Unbounded pagination.
- Entity graph/fetch join.
- Calling external service trong loop.
- Blocking call trên event loop.
- Oversized payload.
- Cache misuse.
- Connection pool starvation.
- Transaction quá dài.

### **Assignment**

Tạo `07-performance-anti-patterns.md`.

Phân tích đoạn pseudo-code:

`public List<EmployeeDashboardResponse> getDashboard(String tenantId) {
    List<Employee> employees = employeeRepository.findByTenantId(tenantId);

    return employees.stream()
        .map(employee -> {
            Salary salary = salaryRepository.findLatestByEmployeeId(employee.getId());
            TaxProfile tax = taxClient.getTaxProfile(employee.getId());
            return new EmployeeDashboardResponse(employee, salary, tax);
        })
        .toList();
}`

Cần chỉ ra:

- N+1 query ở đâu.
- N+1 remote calls ở đâu.
- Rủi ro khi tenant có 10,000 employees.
- Nếu dùng WebFlux, đoạn nào vẫn blocking.
- Refactor/optimization strategy.
- Metric cần theo dõi sau fix.

### **Hoàn thành khi**

- [ ]  Nhận diện N+1 DB và N+1 remote call.
- [ ]  Không tối ưu mù quáng trước khi đo.
- [ ]  Có strategy pagination/batch fetch/cache/concurrency limit.
- [ ]  Biết test lại performance sau khi fix.