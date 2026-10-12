# 3. Analyzing Performance Test Results
### **Cần học gì**

- Cách đọc báo cáo performance test.
- Correlation giữa application metrics và infrastructure metrics.
- Các bottleneck phổ biến:
    - CPU saturation.
    - Memory pressure / GC pause.
    - Thread pool exhaustion.
    - Connection pool exhaustion.
    - Database slow query.
    - Missing index.
    - N+1 query.
    - Database lock contention.
    - Network latency.
    - Cache miss.
    - Rate limit / downstream dependency failure.

### **Cần hiểu đến mức nào**

cần biết không được kết luận kiểu:

> “p95 cao nên chắc database chậm.”
> 

Cần tìm evidence bằng correlation:

`Load tăng
    ↓
p95 latency tăng
    ↓
Cần xem đồng thời:
- Application CPU?
- GC pause?
- Active request threads?
- DB connection pool?
- Slow query log?
- DB CPU/I/O?
- Cache hit rate?
- Downstream API latency?`

### **Assignment**

Cho dữ liệu giả định sau:

| **Load** | **p95 API latency** | **Error rate** | **App CPU** | **DB CPU** | **DB connections** | **Cache hit rate** |
| --- | --- | --- | --- | --- | --- | --- |
| 100 RPS | 120ms | 0% | 25% | 20% | 20/100 | 95% |
| 500 RPS | 280ms | 0.1% | 55% | 60% | 70/100 | 91% |
| 800 RPS | 1.8s | 3.5% | 65% | 95% | 100/100 | 88% |
| 1,000 RPS | 5.2s | 15% | 68% | 98% | 100/100 | 85% |

cần viết `03-performance-analysis.md` trả lời:

1. Bottleneck có khả năng cao nhất nằm ở đâu?
2. Evidence nào hỗ trợ kết luận?
3. Vì sao application CPU không phải root cause chính?
4. Các bước điều tra tiếp theo là gì?
5. Đề xuất short-term fix và long-term fix.

### **Sản phẩm cần nộp**

- `03-performance-analysis.md`
- Một biểu đồ hoặc bảng correlation.
- Root cause hypothesis.
- Investigation plan.
- Fix plan theo mức:
    - Immediate mitigation.
    - Short-term fix.
    - Long-term architecture improvement.

### **Hoàn thành khi**

- [ ]  Có hypothesis dựa trên metric, không đoán cảm tính.
- [ ]  Phân biệt được symptom và root cause.
- [ ]  Nêu được metric cần kiểm chứng thêm.
- [ ]  Đề xuất fix có ưu tiên rõ ràng.