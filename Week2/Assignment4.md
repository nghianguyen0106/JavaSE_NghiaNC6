# Assignment 4: Vì sao 1 khách hàng VIP làm chậm cả hệ thống, còn 99% khách khác thì bình thường?
**Topics áp dụng:** Sharding Strategies (Hash vs Range), Sharding Challenges (Hot Spot, Re-sharding)

## **1. Đề bài thực tế**

`Bối cảnh:
Hệ thống SaaS multi-tenant (mỗi công ty khách hàng = 1 tenant) đã 
sharding database theo Range-based trên tenantId:
- Shard 1: tenantId 1 - 100,000
- Shard 2: tenantId 100,001 - 200,000
- Shard 3: tenantId 200,001 - 300,000

Vấn đề phát sinh:
- Công ty "ABC Corp" là khách hàng Enterprise lớn nhất, có tenantId 
  = 50,000 (rơi vào Shard 1).
- ABC Corp có 50,000 nhân viên sử dụng hệ thống hàng ngày, tạo ra 
  lượng traffic gấp 200 lần tenant trung bình.
- Toàn bộ traffic của ABC Corp dồn vào Shard 1.
- Shard 1 bị quá tải (CPU 95%, query latency tăng 10 lần), trong 
  khi Shard 2, Shard 3 gần như "rảnh rỗi" (CPU 15%).
- Tất cả các tenant KHÁC cũng nằm trong Shard 1 (do range 
  1-100,000 gồm rất nhiều tenant nhỏ) đều bị ảnh hưởng lây - đây 
  gọi là "noisy neighbor problem".

Câu hỏi từ sếp: "Tại sao ta có 3 shard, mà chỉ 1 shard chết, 2 
shard kia rảnh rang? Sao không tự cân bằng lại?"`

## **2. Câu hỏi**

`Q1: "Range-based sharding có công bằng về SỐ LƯỢNG tenant mỗi shard 
    không? Vậy tại sao vẫn bị quá tải?"

Q2: "Nếu đổi sang Hash-based sharding (hash(tenantId) % 3), vấn đề 
    này có được giải quyết không? Tại sao có/không?"

Q3: "Đây có phải là vấn đề mà 'đổi thuật toán sharding' là đủ để 
    giải quyết, hay cần thêm kiến trúc khác?"

Q4: "Nếu bạn là Architect, đề xuất giải pháp cho tenant lớn như 
    ABC Corp. Có bao nhiêu hướng giải quyết?"

Q5: "Giả sử chọn phương án 'tenant lớn có shard riêng', làm sao 
    migrate ABC Corp từ Shard 1 sang shard riêng MÀ KHÔNG DOWNTIME?"`

## **3. Lý thuyết áp dụng**

**Sharding Strategies (Hash vs Range):**

- Range-based sharding đang thất bại vì phân phối theo range tenantId, không quan tâm đến traffic thực tế của từng tenant.
- Đây là ví dụ điển hình của **Hot Spot** — 1 shard nhận tải vượt xa các shard khác dù số lượng tenant per shard tương đương.

**Sharding Challenges:**

- Cần phân biệt: **data distribution** (số tenant/shard) khác với **traffic distribution** (request/shard). Range sharding chỉ cân bằng cái đầu, không cân bằng cái sau.
- Giải pháp liên quan đến **re-sharding** hoặc **dedicated shard cho tenant lớn** (isolation strategy).

## **4. Yêu cầu:**

- trả lời và đưa ra cách hiểu và xử lý bài toán vào file .md