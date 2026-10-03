# Assignment 1: Tại sao khách hàng thấy đơn hàng bị mất tiền 2 lần?

**Topics áp dụng:** CAP Theorem, PACELC

## **1. Đề bài thực tế**

`Bối cảnh:
Hệ thống Order Service có 1 Database Primary ở Region A (Singapore)
và 1 Replica ở Region B (Tokyo) để phục vụ đọc cho user Nhật.

Sự cố xảy ra lúc 2h sáng:
- Network giữa Singapore và Tokyo bị đứt 45 giây (network partition).
- Trong 45 giây đó:
  + User A (đang connect vào Tokyo) bấm "Thanh toán" cho đơn hàng ORD123.
  + Request được xử lý bởi Replica Tokyo (vì lúc đó hệ thống cho phép
    replica tạm thời nhận write khi mất kết nối với Primary, để tránh
    downtime - đây là quyết định kiến trúc có sẵn).
  + Payment được ghi nhận: Order ORD123 = PAID, charge thẻ 500,000đ.

- Sau 45s, network được khôi phục.
- Primary ở Singapore VẪN CÒN dữ liệu cũ: ORD123 = PENDING
  (vì Primary không biết Tokyo đã tự xử lý request trong lúc partition).
- Khi 2 node đồng bộ lại (reconciliation), do conflict giữa 2 bản ghi,
  hệ thống merge sai và tạo ra 2 lần charge cho cùng 1 đơn hàng.

Hậu quả: Khách hàng bị trừ tiền 2 lần, phải hoàn tiền, viết báo cáo
incident cho leadership.`

## **2. Câu hỏi**

`Q1: "Theo các bạn, khi network partition xảy ra, hệ thống này đã ưu 
    tiên C hay A? Vì sao các bạn biết?"

Q2: "Nếu là các bạn thiết kế hệ thống Order/Payment này, các bạn sẽ 
    chọn CP hay AP? Tại sao?"

Q3: "Giả sử ta chọn CP (từ chối request khi partition). Trải nghiệm 
    user sẽ như thế nào? Có chấp nhận được không?"

Q4: "Có cách nào vừa tránh mất tiền, vừa không làm user chờ đợi quá 
    lâu không?" (Gợi ý: idempotency key, không phải mọi thứ đều 
    all-or-nothing)

Q5: "Loại dữ liệu nào trong hệ thống E-commerce có thể chấp nhận AP? 
    Cho ví dụ cụ thể."`

## **3. Thông tin thêm**

**CAP Theorem:**

- Hệ thống đã chọn **Availability (A)** khi partition xảy ra: cho phép Tokyo tự xử lý write thay vì từ chối request.
- Đây chính là **AP system**: hy sinh Consistency để giữ Availability.
- Vấn đề: **Payment là loại dữ liệu cần Consistency (CP), không nên là AP.**

**PACELC:**

- Ngay cả khi không có partition, câu hỏi vẫn còn: giữa latency thấp (cho phép ghi ở gần user Nhật) và consistency mạnh (chỉ ghi ở primary), hệ thống đã chọn sai ưu tiên cho loại dữ liệu payment.

## **4. Yêu cầu:**

- trả lời và đưa ra cách hiểu và xử lý bài toán vào file .md