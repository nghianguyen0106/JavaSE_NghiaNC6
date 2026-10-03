# Assignment 3: Vì sao báo cáo doanh thu cuối tháng bị chậm 6 tiếng và làm sập cả hệ thống bán hàng?

**Topics áp dụng:** Replication Strategies (Sync/Async), Database Sharding

## **1. Câu chuyện / Đề bài thực tế**

`Bối cảnh:
Công ty có 1 database duy nhất chứa bảng "orders" với 500 triệu 
dòng, không hề sharding. Traffic bán hàng (write) và traffic báo 
cáo (read/aggregate query) đều chạy chung 1 database.

Sự cố:
- Cuối tháng, Kế toán chạy 1 query tổng hợp doanh thu:
  SELECT SUM(total), COUNT(*), region FROM orders 
  WHERE created_at BETWEEN ... GROUP BY region
- Query này quét gần như toàn bộ 500 triệu dòng, chạy mất 6 tiếng,
  chiếm hết CPU và I/O của database.
- Trong lúc đó, khách hàng đang mua hàng (giờ cao điểm 20h-22h) bị 
  timeout hàng loạt vì Database quá tải, không xử lý kịp write.
- Doanh thu ước tính thiệt hại: hàng trăm triệu đồng vì khách bỏ 
  giỏ hàng.

Thêm bối cảnh: Traffic viết (write) đã tăng 10 lần trong 1 năm qua,
1 database instance không còn đủ sức xử lý cả read lẫn write.`

## **2. Câu hỏi**

`Q1: "Vấn đề gốc rễ (root cause) ở đây là gì? Có phải chỉ là 'query 
    chậm' không, hay còn nguyên nhân sâu xa hơn?"

Q2: "Giải pháp 'tách Replica riêng cho reporting' có giải quyết 
    triệt để vấn đề không? Tại sao có/không?"

Q3: "Nếu evaluate thêm sharding, các bạn sẽ chọn shard key nào cho
    bảng orders? Cân nhắc giữa customerId, region, và created_at."

Q4: "Giả sử chọn sharding theo thời gian (mỗi tháng 1 shard), 
    query báo cáo cuối tháng có nhanh hơn không? Có đánh đổi gì?"

Q5: "Sync hay Async replication phù hợp cho Replica dùng cho 
    reporting? Vì sao?"`

## **3. Lý thuyết áp dụng**

**Replication Strategies:**

- Vấn đề đầu tiên: chưa tách được Read replica cho reporting query ra khỏi Primary phục vụ giao dịch bán hàng.
- Cần Replica riêng cho reporting (OLAP-style), chấp nhận **asynchronous replication** vì báo cáo không cần real-time tuyệt đối.

**Database Sharding:**

- Vấn đề thứ hai, sâu hơn: dù có Replica, một dòng query quét 500 triệu bản ghi vẫn chậm vì dữ liệu không được **partition/shard**.
- Cần sharding theo `region` hoặc theo thời gian (`created_at`, ví dụ shard theo tháng/quý) để giảm khối lượng data mỗi lần quét.

## **4. Yêu cầu:**

- trả lời và đưa ra cách hiểu và xử lý bài toán vào file .md