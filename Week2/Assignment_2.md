# Assignment 2: Vì sao user vừa đổi mật khẩu xong lại đăng nhập không được?

**Topics áp dụng:** Consistency Models (Read-your-writes, Eventual Consistency), Database Replication

## **1. Đề bài thực tế**

`Bối cảnh:
Hệ thống Authentication Service dùng kiến trúc:
- 1 Primary DB (nhận toàn bộ write).
- 3 Read Replicas (phục vụ read, mỗi replica handle ~30% traffic
  qua load balancer round-robin).
- Replication là ASYNCHRONOUS (để giảm latency cho các API đọc).

Complaint từ user:
"Tôi vừa đổi mật khẩu xong, bấm đăng nhập lại ngay bằng mật khẩu 
mới thì hệ thống báo 'Sai mật khẩu'. Tôi phải thử lại sau 5 giây 
mới đăng nhập được."

Debug team phát hiện:
- Request "Đổi mật khẩu" → ghi vào Primary → trả response "Thành công".
- Request "Đăng nhập" (ngay sau đó, 0.5 giây) → được load balancer 
  route sang Replica 2.
- Replica 2 CHƯA kịp nhận bản update mật khẩu mới (replication lag
  trung bình 1-3 giây).
- Replica 2 check password bằng bản ghi CŨ → FAIL.`

## **2. Câu hỏi**

`Q1: "Ai có thể vẽ lại flow của bug này trên bảng/giấy, chỉ ra chính 
    xác bước nào gây ra vấn đề?"

Q2: "Đây là lỗi ở tầng nào: Application code, Database, hay 
    Architecture design? Giải thích."

Q3: "Consistency model nào đang bị vi phạm ở đây? Có phải hệ thống 
    'sai' không, hay đây là trade-off đã biết trước của async 
    replication?"

Q4: "Nếu bạn là Tech Lead, bạn đề xuất bao nhiêu giải pháp để fix? 
    Liệt kê ít nhất 3, so sánh ưu nhược điểm."

Q5: "Giải pháp nào các bạn chọn cho production, và đánh đổi (trade-off) 
    của nó là gì?"`

## **3. Lý thuyết**

**Consistency Models:**

- Đây là vi phạm **Read-your-writes consistency**: user vừa ghi (đổi password) phải đọc lại được chính write đó ngay lập tức, nhưng hệ thống không đảm bảo điều này.
- Đây là hệ quả tự nhiên của **Eventual Consistency** trong replication bất đồng bộ.

**Database Replication:**

- Read routing đang áp dụng round-robin mù quáng, không phân biệt loại request nào cần đọc từ Primary (data mới nhất) và loại nào có thể đọc từ Replica (chấp nhận stale).
- Đây chính là vấn đề "read scaling nhưng thiếu routing strategy hợp lý".

## **4. Yêu cầu:**

- trả lời và đưa ra cách hiểu và xử lý bài toán vào file .md