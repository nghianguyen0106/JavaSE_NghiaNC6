# 1. JVM Performance: G1, ZGC, GC Metrics
### **Cần học gì**

- Heap, stack, metaspace, native memory ở mức tổng quan.
- Object allocation và garbage collection.
- Stop-the-world pause.
- Heap usage, allocation rate, GC pause time.
- G1 GC và ZGC ở mức trade-off.
- Khi nào nghi ngờ:
    - Memory leak.
    - Allocation pressure.
    - GC pause cao.
    - Thread pool exhaustion.
    - CPU saturation.

### **Cần hiểu đến mức nào**

cần giải thích được:

| **Khái niệm** | **Cần hiểu** |
| --- | --- |
| Heap | Nơi phần lớn object Java được cấp phát |
| GC pause | Thời gian JVM tạm dừng một phần/toàn bộ application work để thu hồi bộ nhớ |
| Allocation rate | Tốc độ application tạo object mới |
| Memory leak | Object không còn cần thiết nhưng vẫn bị reference nên GC không thu hồi được |
| G1 | Collector hướng tới cân bằng throughput và pause-time goals cho heap lớn |
| ZGC | Collector hướng tới pause rất thấp, phù hợp khi latency-sensitive; cần đánh giá theo workload và môi trường thực tế |

### **Assignment**

Tạo `01-jvm-performance-notes.md`:

1. Giải thích heap, stack, GC pause, allocation rate.
2. So sánh G1 và ZGC:
    - Mục tiêu.
    - Trade-off.
    - Khi nào cần benchmark thay vì đoán.
3. Phân tích tình huống:

`Payroll API có p99 tăng từ 400ms lên 8 giây.
Application CPU chỉ 45%.
Heap usage saw-tooth liên tục.
Full GC xuất hiện nhiều lần trong giờ cao điểm.`

Mentee phải nêu:

- Hypothesis ban đầu.
- Metric/log cần xem thêm.
- Không được kết luận chỉ từ một biểu đồ.

### **Sản phẩm cần nộp**

- `01-jvm-performance-notes.md`
- Một sơ đồ heap/GC flow.
- Một bảng “symptom → possible cause → evidence cần kiểm tra”.

### **Hoàn thành khi**

- [ ]  Phân biệt được heap issue và thread issue.
- [ ]  Không kết luận “GC chậm” chỉ vì heap cao.
- [ ]  Biết cần đo pause time, allocation rate, heap after GC.
- [ ]  Nêu được vì sao benchmark thực tế quan trọng khi chọn GC.