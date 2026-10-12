Listed directory Week4
Viewed 1_JVM_Performance_Metrics.md:1-62
Viewed 2_Heap_Dump_Thread_Dump.md:1-59
Viewed 3_Spring_AOP_Custom_Aspect_Around_AnnotationdrivenAOP.md:1-56
Viewed 4_Hibernate_Locking_Optimistic_vs_Pessimistic.md:1-47
Viewed 5_Hibernate_Envers_DataAuditing.md:1-45
Viewed 6_Spring_Batch_Architecture.md:1-52

Chào bro! Bước sang **Week 4**, chúng ta tiến thẳng vào những bài toán "nặng đô" nhất của một Senior Java Backend Engineer: **Từ tầng sâu Runtime máy ảo JVM, kiểm soát tranh chấp dữ liệu (Concurrency/Locking), xử lý khía cạnh dùng chung (AOP) cho tới kiến trúc xử lý dữ liệu hàng triệu dòng (Spring Batch)**.

Dưới đây là bức tranh tổng hợp, phân tích 4 trụ cột kiến thức kèm **phương pháp học nhanh nhất (Top-Down)** và các **ví dụ đời thực cực kỳ dễ hiểu**!

---

### 🗺️ BỨC TRANH TỔNG THỂ WEEK 4: QUY VỀ 4 TRỤ CỘT

Thay vì học rời rạc 6 bài lý thuyết, tuần 4 được ánh xạ trực tiếp thành **4 Trụ cột tương ứng với 4 Case Study thực tế**:

| Trụ cột | Bài lý thuyết | Case Study tương ứng | Bài toán cốt lõi |
| :--- | :--- | :--- | :--- |
| **1. JVM Deep Dive & Diagnostics** | [Bài 1](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week4/1_JVM_Performance_Metrics.md), [Bài 2](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week4/2_Heap_Dump_Thread_Dump.md) | **Case 1:** Payroll OOM cuối tháng | *App bị chậm hoặc chết OOM do đâu? Khi nào bắt Heap Dump, khi nào bắt Thread Dump?* |
| **2. Spring AOP & Concurrency Lock** | [Bài 3](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week4/3_Spring_AOP_Custom_Aspect_Around_AnnotationdrivenAOP.md) | **Case 2:** Chạy trùng Payroll 2 lần | *Tách biệt Cross-cutting concern thế nào? Dùng `@Around` để Distributed Lock & Audit ra sao?* |
| **3. Data Locking & Auditing** | [Bài 4](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week4/4_Hibernate_Locking_Optimistic_vs_Pessimistic.md), [Bài 5](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week4/5_Hibernate_Envers_DataAuditing.md) | **Case 3:** 2 HR Manager sửa lương ghi đè | *Chống Lost Update bằng Optimistic hay Pessimistic Lock? Lưu vết lịch sử bằng Envers thế nào?* |
| **4. Large-Scale Batch Processing** | [Bài 6](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week4/6_Spring_Batch_Architecture.md) | **Case 4:** Import CSV 3 triệu dòng timeout | *Tại sao REST API sập khi import file lớn? Kiến trúc Chunk (`Reader-Processor-Writer`) & Restartability.* |

---

### ⚡ PHÂN TÍCH 4 TRỤ CỘT KÈM VÍ DỤ "DỄ HIỂU 1 NỐT NHẠC"

#### 🧩 Trụ cột 1: JVM Performance & Cặp đôi chẩn đoán (Heap Dump vs Thread Dump)
* **Bản chất vấn đề:**
  * **Heap** là "bãi đất" cấp phát object. **GC Pause (Stop-The-World)** là lúc JVM tạm dừng code của bạn để đi dọn rác.
  * Khi App CPU thấp mà API vẫn chậm $\rightarrow$ Khả năng cao do GC Pause liên tục (Full GC) hoặc Threads đang bị tắc nghẽn chờ I/O bên ngoài.
* **Bộ đôi "Ống nghe bệnh viện":**
  * **Heap Dump (`jcmd GC.heap_dump`):** Là bức ảnh chụp X-quang bộ nhớ $\rightarrow$ Dùng khi nghi ngờ **Memory Leak / OOM** (Tìm xem class/object nào đang phình to mà không chịu nhả reference).
  * **Thread Dump (`jcmd Thread.print`):** Là danh sách điểm danh trạng thái của tất cả nhân viên $\rightarrow$ Dùng khi **App bị đơ / treo / chậm mà CPU rất thấp** (Xem thread nào đang bị `BLOCKED`, `WAITING` chờ lock hoặc deadlock).
* 💡 **Ví dụ đời thực:**
  * Giả sử quán ăn bị nghẽn:
    * Nếu **rác chất đống ngập kín quán** không còn chỗ ngồi $\rightarrow$ Vấn đề **Heap Memory** (cần Heap Dump tìm rác).
    * Nếu bàn ghế còn trống nhưng **tất cả nhân viên đứng ngơ ngác chờ người giao đá** $\rightarrow$ Vấn đề **Thread Starvation** (cần Thread Dump xem nhân viên đang kẹt ở đâu).

---

#### 🧩 Trụ cột 2: Spring AOP — Khi nào nên dùng và Khi nào cấm dùng?
* **Bản chất vấn đề:**
  * **Cross-cutting Concern:** Là những việc "phụ nhưng ai cũng cần" (Log, đo thời gian chạy, kiểm tra quyền, Distributed Lock, ghi Audit).
  * `@Around`: Lớp bọc quyền năng nhất, có thể chặn trước, can thiệp lúc chạy (`proceed()`), và xử lý sau khi method xong.
* **Quy tắc sống còn:**
  * ✅ **NÊN dùng AOP cho:** Infrastructure concerns (Distributed lock `@DistributedLock`, Ghi vết `@AuditAction`, Metric timing).
  * ⛔ **CẤM nhét vào AOP:** Core Business Rules (ví dụ: công thức tính lương, điều kiện giảm thuế, workflow đơn hàng) vì nó biến code thành "ma thuật đen" (ẩn giấu logic, cực kỳ khó debug).
* 💡 **Ví dụ đời thực:**
  * Hãy coi Business Method là **"Bác sĩ đang phẫu thuật"**.
  * Việc **rửa tay sát trùng trước khi mổ** và **ghi hồ sơ bệnh án sau khi mổ** chính là **AOP Aspect**.
  * Nhưng **kỹ thuật rạch dao hay khâu vết mổ** bắt buộc phải là code của bác sĩ (Core Business), không thể giao cho bảo vệ đứng ngoài làm hộ qua AOP!

---

#### 🧩 Trụ cột 3: Concurrency Locking & Hibernate Envers
* **Bản chất vấn đề (Lost Update):** Hai người cùng đọc bản ghi lương 20M. Ông A sửa thành 25M lưu trước, ông B sửa thành 22M lưu sau $\rightarrow$ Dữ liệu ông A bị mất trắng!
* **So sánh hai loại Lock:**
  * **Optimistic Lock (`@Version`):**
    * *Tư tưởng:* "Cứ lạc quan, hiếm khi trùng nhau đâu".
    * *Cơ chế:* Khi update, kiểm tra `WHERE id = ? AND version = ?`. Nếu version đã bị người khác tăng lên $\rightarrow$ Ném lỗi `OptimisticLockException` $\rightarrow$ Báo UI cho user reload.
    * *Phù hợp:* Tác vụ giao diện web người dùng, đọc nhiều - sửa ít (như sửa form thông tin nhân viên).
  * **Pessimistic Lock (`SELECT ... FOR UPDATE`):**
    * *Tư tưởng:* "Bi quan, chắc chắn sẽ có đứa chen ngang, khóa luôn cho chắc".
    * *Cơ chế:* Khóa chặt dòng dữ liệu ngay từ lúc đọc, bắt transaction khác phải xếp hàng chờ.
    * *Phù hợp:* Tranh chấp cực cao trong tích tắc ngắn (như trừ vé máy bay, trừ tồn kho Flash Sale). **Tuyệt đối không dùng khi mở form chờ người dùng gõ phím suy nghĩ!**
* **Hibernate Envers (`@Audited`):** Tự động sinh ra bảng phụ (ví dụ `employee_salary_AUD`) để lưu lại từng Revision (ai sửa, sửa lúc nào, giá trị cũ/mới là gì) mà không cần dev viết tay trigger phức tạp.
* 💡 **Ví dụ đời thực:**
  * **Optimistic Lock:** Giống như chỉnh sửa tài liệu Google Docs: Nếu mạng chập chờn bạn lưu đè lên bản mới hơn của đồng nghiệp, phần mềm sẽ báo: *"Bản này đã có người sửa rồi, hãy xem lại diff"*.
  * **Pessimistic Lock:** Giống như đi vào buồng vệ sinh công cộng rồi **chốt cửa trong**. Người khác phải đứng ngoài chờ bạn bước ra mới được vào.

---

#### 🧩 Trụ cột 4: Spring Batch — Xử lý dữ liệu quy mô lớn (3 triệu dòng)
* **Bản chất vấn đề:** Đem 1 file CSV 3 triệu dòng ném vào REST API đồng bộ (`POST /import`) sẽ gây chết sập hệ thống (Timeout HTTP sau vài phút, tràn bộ nhớ RAM, insert nửa chừng rồi lỗi không biết chạy tiếp từ đâu).
* **Kiến trúc Spring Batch:**
  * **Job $\rightarrow$ Step $\rightarrow$ Chunk-oriented Processing:**
    $$\text{ItemReader (Đọc từng dòng)} \longrightarrow \text{ItemProcessor (Validate)} \longrightarrow \text{ItemWriter (Ghi DB theo mẻ)}$$
  * **Chunk Size (ví dụ 1,000 dòng):** Đọc đủ 1,000 dòng mới mở Transaction và `INSERT` hàng loạt 1 lần $\rightarrow$ Tối ưu DB I/O và giải phóng bộ nhớ RAM liên tục.
  * **Khả năng tự phục hồi (Restartability & JobRepository):** Nếu dòng thứ 1,500,001 bị cúp điện, Spring Batch ghi nhớ vị trí lỗi trong DB metadata. Khi restart, nó tự động nhảy cóc qua 1.5 triệu dòng cũ và chạy tiếp dòng 1,500,002!
  * **Skip & Retry Policy:** Gặp dòng lỗi format (sai ngày tháng, âm tiền) $\rightarrow$ Skip dòng đó, ghi ra file `error_rows.csv` cho HR xem lại, không làm dừng cả mẻ 3 triệu dòng!
* 💡 **Ví dụ đời thực:**
  * REST API giống như **1 người bê vác cố gánh cùng lúc 3 triệu viên gạch** $\rightarrow$ Gãy lưng chết ngay lập tức!
  * Spring Batch giống như **xe cút kít chở từng mẻ 1,000 viên gạch (Chunk)**. Vừa đi vừa ghi sổ nhật trình. Nếu rơi vỡ 1 viên gạch xấu $\rightarrow$ nhặt vứt sang bên lề đường (Skip) và tiếp tục chở mẻ tiếp theo.

---

### 🚀 LỘ TRÌNH HỌC NHANH NHẤT ĐỀ XUẤT (FAST-TRACK PLAN)

Để đạt hiệu quả tối đa mà tốn ít thời gian nhất, bro nên đi theo 3 bước sau:

1. **Bước 1 (30 phút - Học ngược từ Case Study):**
   * Mở file **[Case_Study.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week4/Case_Study.md)**, đọc lướt qua bối cảnh và câu hỏi của 4 Case:
     * **Case 1:** OOM vì Cache `ConcurrentHashMap` không dọn rác.
     * **Case 2:** AOP `@Around` Distributed Lock chống chạy đúp Payroll.
     * **Case 3:** Optimistic Lock `@Version` + Envers chống Lost Update sửa lương.
     * **Case 4:** Spring Batch Chunk 1,000 dòng xử lý CSV 3 triệu bản ghi.
2. **Bước 2 (Mổ xẻ phản xạ từng Case):**
   * Chúng ta sẽ cùng nhau "mổ xẻ" và trả lời phản xạ từng Case một (tương tự như cách bro đã làm rất xuất sắc ở Tuần 3).
3. **Bước 3 (Thực hành viết Assignment):**
   * Dựa trên phần mổ xẻ để hoàn thiện các file Assignment tương ứng ([1_JVM_Performance_Metrics.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week4/1_JVM_Performance_Metrics.md), [4_Hibernate_Locking_Optimistic_vs_Pessimistic.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week4/4_Hibernate_Locking_Optimistic_vs_Pessimistic.md)...).

---

Bro thấy bức tranh tổng quan của Tuần 4 đã sáng rõ chưa? Mình cùng bắt đầu "bắn phá" ngay từ **Case 1: Payroll API chậm dần rồi OOM lúc cuối tháng (JVM, G1/ZGC, Heap Dump vs Thread Dump)** luôn nhé?