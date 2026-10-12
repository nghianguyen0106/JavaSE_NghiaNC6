# 2. Heap Dump & Thread Dump
### **Cần học gì**

- Heap dump dùng để trả lời câu hỏi gì.
- Thread dump dùng để trả lời câu hỏi gì.
- Dominator tree, retained size, class histogram.
- Thread states:
    - `RUNNABLE`
    - `BLOCKED`
    - `WAITING`
    - `TIMED_WAITING`
- Deadlock, connection-pool exhaustion, blocked external call.
- `jcmd`, Java Flight Recorder (JFR) ở mức sử dụng cơ bản.

Oracle khuyến nghị `jcmd` cho chẩn đoán JVM; nó hỗ trợ `GC.heap_dump`, `GC.class_histogram`, `Thread.print` và JFR. Heap dump có thể tác động cao vì phụ thuộc heap size/nội dung, nên không nên lấy tùy tiện trên production. (docs.oracle.com)

### **Cần hiểu đến mức nào**

| **Artifact** | **Câu hỏi trả lời** |
| --- | --- |
| Heap dump | Object nào giữ nhiều memory? Reference nào giữ object không được GC? |
| Class histogram | Class nào có số instance/size lớn bất thường? |
| Thread dump | Thread đang chạy, chờ I/O, bị lock, hay deadlock ở đâu? |
| JFR | Trong khoảng thời gian cụ thể, JVM/GC/thread/CPU có pattern gì? |

### **Assignment**

Tạo `02-jvm-diagnostics.md` cho 2 scenario:

**Scenario A — Memory leak**

`Payroll export tăng memory từ 1GB lên 6GB.
Sau GC, heap vẫn gần đầy.
Cuối cùng service OOM.`

**Scenario B — Thread starvation**

`API chậm nhưng CPU chỉ 30%.
Thread pool 200 threads đều đang chờ HTTP call đến Tax Service.`

cần nêu:

- Lấy artifact nào trước.
- Quan sát gì trong artifact.
- Root cause hypothesis.
- Mitigation ngắn hạn và fix dài hạn.

### **Sản phẩm cần nộp**

- `02-jvm-diagnostics.md`
- Checklist “khi nào lấy heap dump, khi nào lấy thread dump”.
- Một runbook ngắn gồm lệnh chẩn đoán an toàn.

### **Hoàn thành khi**

- [ ]  Biết heap dump không phải công cụ đầu tiên cho mọi sự cố.
- [ ]  Biết thread dump hữu ích khi request treo/chậm nhưng CPU thấp.
- [ ]  Biết heap dump có thể chứa dữ liệu nhạy cảm.
- [ ]  Có hypothesis dựa trên evidence.