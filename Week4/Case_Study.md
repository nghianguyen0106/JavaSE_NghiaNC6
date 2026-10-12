# Case 1 — Payroll API chậm dần rồi OOM lúc cuối tháng

**Topics:** JVM performance, G1/ZGC, heap dump, thread dump.

## **Bài toán công ty**

HR SaaS có API:

`GET /payroll/monthly-summary?tenantId=...`

Cuối tháng, HR manager mở dashboard liên tục để xem tổng lương, tax, bonus và employee breakdown.

Requirement:

`- p95 dưới 500ms.
- Không OOM.
- Dashboard không làm ảnh hưởng API update payroll.`

## **Kỹ thuật hiện tại**

`Payroll Service
  ↓
Cache: static ConcurrentHashMap<tenantId, PayrollSummary>
  ↓
Hibernate/JPA
  ↓
Payroll DB`

Team dùng cache in-memory để giảm query database. Cache không có eviction rõ ràng; report object chứa nhiều employee detail.

## **Incident**

`09:00  Heap: 2GB / 8GB
11:00  Heap: 5GB / 8GB
13:00  Heap: 7.5GB / 8GB
13:05  Full GC liên tục
13:10  p99 API: 12 seconds
13:12  OutOfMemoryError`

CPU chỉ khoảng 40–50%.

## **Câu hỏi**

`1. Heap cao có đủ để kết luận memory leak không?
2. Vì sao CPU không cao nhưng API vẫn chậm?
3. Cần lấy thread dump hay heap dump trước? Vì sao?
4. Trong heap dump, cần tìm evidence gì?
5. G1 hay ZGC có “fix” memory leak không?
6. Immediate mitigation và long-term fix là gì?`

## Giải pháp

`- Heap tăng liên tục và không giảm đáng kể sau GC là strong signal cần điều tra retained objects.
- Full GC/pause có thể làm latency cao dù CPU trung bình không cao.
- Heap dump phù hợp để xem object nào chiếm/retain memory.
- Thread dump bổ sung nếu nghi request threads bị block.
- Đổi GC không giải quyết object bị giữ reference sai.
- Root cause có thể là cache không eviction + cache nhầm dữ liệu detail quá lớn.`

Giải pháp:

`Immediate:
- Limit cache size/TTL.
- Disable cache detail lớn.
- Restart có kiểm soát nếu cần.

Long-term:
- Cache summary thay vì full employee list.
- Bounded cache + eviction.
- Tách report data/read model.
- Dashboard pagination.
- Monitor heap after GC, GC pause, allocation rate, cache size.`


# **Case 2 — Payroll tháng bị chạy hai lần vì scheduler retry**

**Topics:** Spring AOP, custom annotation, `@Around`, distributed locking, audit.

## **Bài toán công ty**

Mỗi tháng hệ thống chạy payroll cho một tenant:

`POST /payroll/run
tenantId = ABC
month = 2026-10`

Business requirement:

`- Một tenant chỉ được chạy payroll một lần cho một tháng.
- Có thể retry nếu job fail.
- Mọi thao tác phải audit được.`

## **Kỹ thuật hiện tại**

Có hai trigger:

`1. Scheduler chạy lúc 00:00.
2. HR admin có nút "Run Payroll" thủ công.`

Cả hai cùng gọi `PayrollService.runPayroll()`.

Team chỉ check:

`if (payrollRunRepository.existsByTenantAndMonth(...)) {
    return;
}`

## **Incident**

`00:00:00 Scheduler bắt đầu run payroll.
00:00:01 HR admin không thấy trạng thái ngay, bấm Run Payroll.
00:00:02 Hai request cùng check exists = false.
00:00:03 Cả hai cùng tạo payroll records.
00:00:05 Một số employee có 2 payslip.`

## **Câu hỏi**

`1. `exists()` rồi `insert()` có atomic không?
2. Đây là business rule hay cross-cutting concern?
3. AOP có phù hợp không? Aspect cần làm gì?
4. Lock key là gì: payroll hay tenant + month?
5. Nếu lock hết hạn khi job chưa chạy xong thì sao?
6. Nếu aspect nuốt exception, audit có đáng tin không?
7. Ngoài lock, database cần constraint gì?`

## Giải pháp

`- Check-then-act không atomic; hai request có thể cùng thấy false.
- Cần defense in depth:
  1. Unique constraint: (tenant_id, payroll_month).
  2. Distributed lock theo key: payroll:{tenantId}:{month}.
  3. Idempotent job design.
  4. Audit action/result.
- AOP phù hợp để wrap lock/audit/metrics, nhưng không thay thế business validation.
- @Around aspect cần:
  acquire lock → audit STARTED → proceed() → audit SUCCESS/FAILURE → release lock.`

Bài học:

> AOP giúp chuẩn hóa cross-cutting behavior. Nhưng tính đúng đắn cuối cùng vẫn phải được bảo vệ ở database và business domain.
>


# **Case 3 — Hai HR manager sửa lương cùng lúc, dữ liệu bị ghi đè**

**Topics:** Optimistic locking, pessimistic locking, Hibernate Envers.

## **Bài toán công ty**

Hai HR manager cùng mở Employee E-100:

`Current salary: 20,000,000 VND`

- Manager A tăng lương lên 25M.
- Manager B cập nhật lương lên 22M.

Business requirement:

`- Không được mất update.
- Phải biết ai thay đổi lương.
- Phải xem lại lịch sử salary.`

## **Kỹ thuật hiện tại**

`employee_salary
- employee_id
- base_salary
- updated_at`

Không có `version`.

Không có audit history.

API update dùng last-write-wins.

## **Incident**

`10:00 A đọc salary = 20M.
10:01 B đọc salary = 20M.
10:02 A save 25M.
10:03 B save 22M.
10:03 Database cuối cùng = 22M.`

A bị mất thay đổi mà không hề biết.

## **Câu hỏi**

`1. Đây là loại lỗi gì?
2. Optimistic lock xử lý case này như thế nào?
3. Khi Manager B bị conflict, UI/API nên phản hồi gì?
4. Pessimistic lock có phù hợp khi HR mở form và suy nghĩ 10 phút không?
5. Audit log cần lưu giá trị nào và actor nào?
6. Envers có thay thế application audit log hoàn toàn không?`

## Giải pháp

`- Đây là lost update.
- Optimistic locking:
  + thêm @Version;
  + update chỉ thành công nếu version còn khớp;
  + B nhận conflict nếu A đã cập nhật trước.
- UI nên báo data changed, yêu cầu reload/compare/reapply thay đổi.
- Không giữ pessimistic lock suốt user interaction vì gây wait/deadlock/poor scalability.
- Envers phù hợp lưu revision/history entity; cần bổ sung actor/request metadata nếu audit requirement cần biết ai thay đổi.`


# **Case 4 — CSV 3 triệu salary adjustment làm API timeout và dữ liệu bị apply nửa chừng**


**Topics:** Spring Batch, chunk processing, retry/skip, restartability, idempotency.

## **Bài toán công ty**

Mỗi cuối tháng HR upload file CSV:

`3,000,000 records salary adjustment`

Team hiện xử lý bằng REST endpoint:

`POST /salary-adjustments/import`

Endpoint đọc toàn bộ CSV, loop từng dòng, validate và insert DB trong một request.

## **Kỹ thuật hiện tại**

`Controller
  ↓
Service loop 3M records
  ↓
JPA save từng record
  ↓
Database`

Không có:

`- chunk commit;
- retry/skip;
- job tracking;
- restart;
- idempotency;
- error file.`

## **Incident**

`- Request timeout sau 5 phút.
- Một phần record đã insert.
- HR upload lại cùng file.
- Nhiều adjustment bị apply hai lần.
- Một số dòng lỗi format làm toàn job fail.
- Không ai biết chính xác dòng nào đã xử lý.`

## **Câu hỏi**

`1. Vì sao đây không còn là bài toán synchronous API?
2. Batch Job khác REST request ở điểm nào?
3. Chunk size ảnh hưởng điều gì?
4. Nếu một record sai format, có nên fail toàn bộ job không?
5. Restart job làm sao không apply lại record cũ?
6. Idempotency key nên là gì?
7. Khi tăng parallelism, risk nào xuất hiện?`

## Giải pháp

`- Cần Spring Batch:
  Job
   → ValidateInputStep
   → ImportAdjustmentStep
   → Reconcile/AuditStep

- ImportAdjustmentStep:
  Reader: CSV
  Processor: validate/transform
  Writer: upsert adjustment + audit
  Chunk: ví dụ 500 hoặc 1,000, phải benchmark theo DB/memory.

- Dùng unique key:
  tenant_id + employee_id + effective_date + adjustment_type + source_file_id

- Record lỗi:
  skip có giới hạn;
  ghi reject file;
  fail job nếu vượt ngưỡng lỗi.

- Restart:
  dùng JobRepository/execution context;
  writer phải idempotent;
  không dựa vào “chạy lại chắc không sao”.`

Spring Batch lưu trạng thái Job/Step qua JobRepository; tài liệu cũng cảnh báo khi trạng thái batch repository và processing database không cùng transaction, step có thể chạy lại sau failure và dẫn đến duplicate processing nếu không thiết kế idempotent. (docs.spring.io)