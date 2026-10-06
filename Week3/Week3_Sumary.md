Nội dung Week 3 nhìn có 7 bài lý thuyết tưởng nhiều, nhưng **bản chất chỉ xoay quanh đúng 3 bài toán sống còn của hệ thống Microservices/Distributed Systems**. 

Cách học nhanh nhất và thực chiến nhất là: **Học ngược (Top-Down) từ [CaseStudy.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week3/CaseStudy.md)** thay vì đọc xuôi từng bài lý thuyết.

---

### 🗺️ Bức tranh tổng thể: Quy về 3 Trụ cột

| Trụ cột | Bài lý thuyết tương ứng | Case Study tương ứng | Câu hỏi cốt lõi |
| :--- | :--- | :--- | :--- |
| **1. Performance & Bottlenecks** | [Bài 1](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week3/1_Introduction_to_Performance_Testing.md), [Bài 2](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week3/2_Load_Testing_Tools_Scripting.md), [Bài 3](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week3/3_Analyzing_Performance_Test_Results.md) | **Case 1** (Flash Sale sập ở 700 RPS) | *Hệ thống chịu được bao nhiêu tải và đang nghẽn ở đâu?* |
| **2. Contract Testing** | [Bài 4](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week3/4_The_Need_for_Contract_Testing_in_Microservices.md), [Bài 5](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week3/5_Consumer-Driven_Contract_Testing.md) | **Case 2** (Đổi API làm gãy Prod dù Unit Test xanh) | *Các service giao tiếp có bị "lệch pha" payload/schema không?* |
| **3. Chaos & Resilience** | [Bài 6](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week3/6_Introduction_to_Chaos_Engineering.md), [Bài 7](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week3/7_Chaos_Engineering_in_Practice.md) | **Case 3** (Inventory chậm 3s làm sập cả Order) | *Khi 1 service chậm/chết, làm sao không kéo sập cả hệ thống?* |

---

### ⚡ Cheat-sheet "3 phút thông suốt" từng trụ cột

#### 1. Trụ cột 1: Performance Testing & Bottleneck (Case 1)
* **Tử huyệt số liệu:** Đừng bao giờ chỉ nhìn **Average Latency**. Average là "kẻ nói dối" vì nó giấu đi nhóm user chịu tải cực đoan. Phải nhìn vào **p95, p99** (95% hoặc 99% request nằm trong khoảng thời gian nào) và **Error Rate**.
* **Đọc Correlation (Tương quan Metrics):** 
  * Khi App CPU mới 60% mà Latency tăng vọt, DB CPU chạm 98-99%, DB connections chạm 100/100 -> **Bottleneck nằm ở Database** (Slow query, thiếu index, lock table, connection pool cạn kiệt).
  * **Sai lầm phổ biến:** Thấy chậm vội vàng scale-out thêm App pod/instance. Kết quả: App pods mới đồng loạt tranh giành DB connection pool -> DB sập nhanh hơn!
* **Load Test Script:** Chỉ cần chọn 1 công cụ (ưu tiên **k6** nếu thích JS/TS nhẹ nhàng, hoặc **Gatling** nếu quen hệ Java). Cốt lõi của script là có **Ramp-up (tăng tải từ từ)**, **Test Data thực tế** và **Thresholds (ngưỡng pass/fail)**.

#### 2. Trụ cột 2: Contract Testing & CDC (Case 2)
* **Vì sao Unit Test & Mock lại "phản bội" bạn?** 
  * Service A (Consumer) gọi Service B (Provider). Trong Unit Test của A, ta mock response của B (`when(b.call()).thenReturn(...)`).
  * Một ngày đẹp trời, team B đổi tên trường (`name` -> `productName`) hoặc xoá field (`available`).
  * Unit test của A vẫn pass (vì dùng mock cũ), Unit test của B vẫn pass (theo code mới), nhưng lên Prod thì deserialize lỗi -> **Contract Drift**.
* **Giải pháp: Consumer-Driven Contract (CDC / Pact):**
  * Consumer định nghĩa ra "bản cam kết" (Contract file): *Tôi chỉ cần các field A, B, C*.
  * Bản cam kết này được đưa vào pipeline CI/CD của Provider để verify. Provider mà tự ý sửa/xoá field làm gãy contract -> **Build của Provider fail ngay, không cho deploy**.
* **Quy tắc vàng:** Thay đổi API an toàn luôn theo hướng: *Thêm field mới -> Giữ field cũ -> Deprecate -> Chỉ xoá khi tất cả Consumer đã migrate*.

#### 3. Trụ cột 3: Chaos Engineering & Resilience (Case 3)
* **Bản chất của Chaos:** Không phải "vào đập phá bừa bãi", mà là **chủ động tiêm lỗi có kiểm soát** (inject latency, kill pod, drop packet) để chứng minh một giả thuyết (Hypothesis) có kiểm soát phạm vi ảnh hưởng (Blast radius & Abort condition).
* **Bộ tứ phòng thủ (Resilience Patterns) chống sập dây chuyền (Cascading Failure):**
  1. **Timeout hợp lý:** Không để timeout quá dài (ví dụ: 30s là tử huyệt, làm request treo nghẽn thread pool).
  2. **Retry có não:** Downstream đang quá tải mà retry liên tục không delay sẽ biến client thành kẻ tự DDoS hệ thống. Phải dùng **Exponential Backoff + Jitter** và chỉ retry API có tính **Idempotent**.
  3. **Circuit Breaker:** Đóng cắt mạch khi tỷ lệ lỗi vượt ngưỡng để fail-fast, cho upstream cơ hội tự hồi phục, tránh nghẽn luồng.
  4. **Bulkhead (Vách ngăn khoang tàu):** Tách biệt thread pool giữa các API quan trọng và không liên quan. Không để việc gọi Inventory bị chậm làm kẹt luôn cả API xem đơn hàng (`GET /orders`).

---

### 🎯 Lộ trình học nhanh đề xuất (Fast-track Action Plan)

1. **Bước 1 (15 - 20 phút):** Mở [CaseStudy.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week3/CaseStudy.md) đọc lướt qua **3 tình huống thực tế**. Đọc phần câu hỏi Q1 - Q7 của mỗi case.
2. **Bước 2 (30 phút):** Tự nhẩm thử cách giải quyết dựa trên cheat-sheet ở trên. Sau đó so sánh với phần **"Hướng xử lý"** có sẵn ở cuối mỗi Case trong [CaseStudy.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week3/CaseStudy.md).
3. **Bước 3 (Tra cứu có chọn lọc):** Chỉ mở các file từ 1 đến 7 khi cần xem định dạng bảng biểu hoặc cú pháp ví dụ cụ thể để làm assignment.

Bro muốn mình cùng "mổ xẻ" và trả lời phản xạ nhanh từng case trong [CaseStudy.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week3/CaseStudy.md) (bắt đầu từ Case 1: Flash sale sập ở 700 RPS) luôn không?