# Module 2 — Reactive Programming & Spring WebFlux

## 1. Reactive Manifesto, Mono, Flux, Non-blocking và Backpressure

### **Cần học gì**

- Responsive, resilient, elastic, message-driven.
- Synchronous/blocking vs asynchronous/non-blocking.
- Reactive Streams.
- Publisher, Subscriber, Subscription, demand.
- Backpressure.
- `Mono<T>` và `Flux<T>`.
- Operators: `map`, `flatMap`, `filter`, `zip`, `timeout`, `retry`.

Reactor cung cấp `Mono` cho luồng 0..1 item và `Flux` cho luồng 0..N item; hai kiểu này là reactive types triển khai `Publisher`. (projectreactor.io)

### **Cần hiểu đến mức nào**

`Mono<Employee>
= có thể trả 0 hoặc 1 employee

Flux<Employee>
= có thể trả 0 đến N employee`

Backpressure:

`Producer tạo 100,000 events/second
Consumer chỉ xử lý được 10,000 events/second`

Nếu không kiểm soát:

`- Queue phình to.
- Memory tăng.
- Latency tăng.
- Cuối cùng OOM hoặc crash.`

Backpressure là cơ chế downstream báo upstream lượng dữ liệu nó có thể nhận/xử lý. Reactor cung cấp các operator để kiểm soát demand và giới hạn lượng item upstream phát ra. (projectreactor.io)

### **Assignment**

Tạo `04-reactive-fundamentals.md`:

- Giải thích `Mono` và `Flux`.
- Vẽ flow producer → consumer → backpressure.
- Phân tích scenario:

`Payroll Event Service publish 50,000 employee-change events/second.
Audit consumer chỉ xử lý ổn định 5,000 events/second.`

Cần trả lời:

- Nếu không có backpressure/buffering policy thì rủi ro gì?
- Khi nào drop, buffer, retry hoặc queue?
- Data nào không được drop?

### **Hoàn thành khi**

- [ ]  Phân biệt được non-blocking với “chạy nhanh hơn”.
- [ ]  Biết `Flux` không có nghĩa là luôn load tất cả data vào memory.
- [ ]  Giải thích được backpressure bằng business scenario.
- [ ]  Có chiến lược khi consumer chậm hơn producer.


## 2. Spring MVC vs WebFlux & R2DBC

### **Cần học gì**

- Spring MVC blocking model.
- WebFlux event-loop/non-blocking model.
- Khi nào WebFlux phù hợp.
- Khi nào MVC vẫn phù hợp hơn.
- Blocking JDBC/JPA trong WebFlux là anti-pattern.
- R2DBC là reactive database connectivity.
- `WebClient` và parallel remote calls.

Spring MVC giả định request handler có thể block nên servlet container thường dùng thread pool lớn; WebFlux giả định không block và dùng số thread event-loop nhỏ, cố định. WebFlux không tự làm application “nhanh hơn”; lợi ích chính là scale predictable hơn khi workload có nhiều I/O chậm/không ổn định. Mixing blocking I/O trong WebFlux context có thể gây vấn đề runtime. (docs.spring.io)

### **Cần hiểu đến mức nào**

| **Scenario** | **MVC** | **WebFlux** |
| --- | --- | --- |
| CRUD nội bộ dùng JPA/JDBC | Thường phù hợp | Không nên ép dùng nếu vẫn blocking |
| API gọi nhiều remote service chậm | Có thể, nhưng tốn threads | Có thể phù hợp |
| Streaming data/SSE | Có thể làm nhưng không tối ưu bằng | Phù hợp |
| Team chưa hiểu reactive debugging | Dễ hơn | Learning curve cao |
| DB driver R2DBC + WebClient | Không tối ưu bằng | Có thể phù hợp |

### **Assignment**

Tạo `05-webflux-design.md`:

Phân tích hai API:

`A. GET /employees/{id}
- Query bằng JPA/Hibernate.
- Có cache.
- Traffic vừa phải.

B. GET /payroll-dashboard
- Cần gọi Payroll Service, Tax Service, Benefits Service.
- Các API downstream thường mất 200–800ms.
- Có thể gọi song song.`

Developer cần chọn MVC/WebFlux cho mỗi API và giải thích:

- Blocking source nằm ở đâu.
- Nếu dùng WebFlux thì dependency nào phải non-blocking.
- Timeout/fallback/concurrency limit cần gì.
- Không được trả lời “WebFlux nhanh hơn”.

### **Hoàn thành khi**

- [ ]  Chọn stack theo workload, không theo trend.
- [ ]  Nhận diện blocking call trong reactive flow.
- [ ]  Hiểu WebFlux + JPA blocking có rủi ro.
- [ ]  Có timeout và failure strategy cho downstream call.