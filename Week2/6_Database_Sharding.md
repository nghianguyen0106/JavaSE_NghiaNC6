# 6. Database Sharding
### **Cần học gì**

- Partitioning là gì.
- Vertical partitioning và horizontal partitioning.
- Sharding là gì.
- Shard key là gì.
- Shard router là gì.
- Vì sao sharding giúp scale write.
- Khác nhau giữa replication và sharding.

### **Cần hiểu đến mức nào**

#### **Replication**

`Cùng một dữ liệu được copy ở nhiều node.

Primary: Orders 1..100M
Replica 1: Orders 1..100M
Replica 2: Orders 1..100M`

Mục tiêu chính:

- High availability.
- Read scaling.

#### **Sharding**

`Dữ liệu được chia thành các phần khác nhau.

Shard 1: Orders của customer 1..1,000,000
Shard 2: Orders của customer 1,000,001..2,000,000
Shard 3: Orders của customer 2,000,001..3,000,000`

Mục tiêu chính:

- Chia write load.
- Chia storage.
- Tăng khả năng scale dữ liệu lớn.

#### **Shard Key**

Shard key là field dùng để quyết định dữ liệu thuộc shard nào.

Ví dụ:

`userId
tenantId
customerId
countryCode
orderId`

Mentee phải hiểu shard key tốt cần:

- Phân phối dữ liệu tương đối đều.
- Phân phối traffic tương đối đều.
- Phù hợp với query phổ biến.
- Hạn chế query xuyên nhiều shard.

### **Assignment**

Tạo `sharding-basics.md`:

1. So sánh replication và sharding.
2. Với bảng `orders`, chọn shard key giữa:
    - `orderId`.
    - `customerId`.
    - `createdAt`.
    - `countryCode`.
3. Phân tích theo các query:

`- Lấy tất cả orders của một customer.
- Tạo order cho customer.
- Tìm order theo orderId.
- Báo cáo doanh thu toàn hệ thống theo tháng.`
4. Chọn shard key và giải thích.

### **Sản phẩm cần nộp**

- `sharding-basics.md`
- Sơ đồ tối thiểu 3 shards.
- Bảng so sánh replication vs sharding.
- Lý do chọn shard key.

### **Hoàn thành khi**

- [ ]  Phân biệt được partitioning, replication và sharding.
- [ ]  Hiểu shard key quyết định data placement.
- [ ]  Biết sharding hỗ trợ write scaling.
- [ ]  Giải thích được vì sao shard key ảnh hưởng query performance.