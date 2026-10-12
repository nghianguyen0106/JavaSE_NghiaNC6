# 6. Introduction to Chaos Engineering
### **Cần học gì**

- Chaos Engineering là gì.
- Chaos Engineering không phải random testing.
- Steady state hypothesis.
- Blast radius.
- Experiment.
- Guardrail.
- Abort condition.
- Observability.
- Game day.
- Resilience vs reliability.

### **Cần hiểu đến mức nào**

cần hiểu flow của chaos experiment:

`1. Define steady state:
   "Checkout success rate >= 99.5%, p95 < 500ms."

2. Define hypothesis:
   "Nếu một Product Service instance down,
    Order Service vẫn xử lý checkout bằng retry/fallback
    và error rate không vượt 1%."

3. Inject fault:
   "Kill one Product Service pod/instance."

4. Observe:
   - Success rate.
   - Latency.
   - Retry count.
   - Circuit breaker state.
   - CPU/thread pool.
   - Alert behavior.

5. Stop / rollback:
   "Abort ngay nếu checkout error rate > 2%."`

### **Assignment**

Tạo `06-chaos-engineering-basics.md`.

cần thiết kế một experiment cho scenario:

`Order Service gọi Inventory Service để reserve stock.`

Bao gồm:

- Steady state.
- Hypothesis.
- Fault injection.
- Blast radius.
- Metrics theo dõi.
- Abort condition.
- Rollback procedure.
- Expected result.

### **Sản phẩm cần nộp**

- `06-chaos-engineering-basics.md`
- Một experiment canvas hoặc bảng kế hoạch.

### **Hoàn thành khi**

- [ ]  Có hypothesis rõ ràng.
- [ ]  Không chạy experiment khi không có observability.
- [ ]  Có blast radius nhỏ.
- [ ]  Có abort condition.
- [ ]  Có rollback plan.
- [ ]  Hiểu chaos experiment cần bắt đầu ở staging/test environment.