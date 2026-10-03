# 7. Sharding Strategies & Challenges
### **Cần học gì**

- Hash/key-based sharding.
- Range-based sharding.
- Directory/lookup-based sharding.
- Consistent hashing ở mức khái niệm.
- Hot shard/hot spot.
- Re-sharding/rebalancing.
- Cross-shard query.
- Cross-shard transaction.

### **Cần hiểu đến mức nào**

#### **Hash-Based Sharding**

`shardId = hash(customerId) % numberOfShards`

Ví dụ:

`hash(customer-101) % 3 = 1
→ Customer 101 thuộc Shard 1`

Ưu điểm:

- Phân phối dữ liệu khá đều.
- Tránh hot spot tốt hơn range sharding trong nhiều trường hợp.

Nhược điểm:

- Range query khó.
- Tăng số shards có thể buộc di chuyển rất nhiều data nếu dùng modulo đơn giản.

#### **Range-Based Sharding**

`Shard 1: customerId 1 → 1,000,000
Shard 2: customerId 1,000,001 → 2,000,000
Shard 3: customerId 2,000,001 → 3,000,000`

Ưu điểm:

- Dễ hiểu.
- Query theo range hiệu quả.

Nhược điểm:

- Có thể tạo hot spot:
    - Nếu user mới luôn có ID lớn hơn.
    - Tất cả write mới dồn về shard cuối.

#### **Hot Spot**

Một shard nhận traffic hoặc dữ liệu vượt xa shard khác.

`Shard 1: 5,000 requests/s
Shard 2: 100 requests/s
Shard 3: 120 requests/s`

#### **Re-sharding**

Thêm hoặc chia shard khi dữ liệu/traffic tăng.

Khó khăn:

- Di chuyển dữ liệu mà không làm downtime.
- Giữ routing đúng trong lúc migration.
- Tránh mất hoặc ghi trùng dữ liệu.
- Đồng bộ dữ liệu cũ và dữ liệu mới.

### **Assignment**

Tạo `sharding-strategies.md`:

#### **Phần A — So sánh strategy**

| **Tiêu chí** | **Hash-based** | **Range-based** | **Directory-based** |
| --- | --- | --- | --- |
| Data distribution |  |  |  |
| Range query |  |  |  |
| Hot spot risk |  |  |  |
| Re-sharding complexity |  |  |  |
| Routing complexity |  |  |  |

#### **Phần B — Design case**

Thiết kế sharding cho hệ thống multi-tenant SaaS:

`- Có 5 triệu tenants.
- Một số enterprise tenants có traffic rất lớn.
- Phần lớn request query theo tenantId.
- Có báo cáo tổng hợp cross-tenant mỗi ngày.
- Cần thêm shard trong tương lai mà downtime thấp.`

Mentee cần đề xuất:

1. Chọn shard key.
2. Chọn strategy.
3. Cách xử lý hot tenant.
4. Cách làm báo cáo toàn hệ thống.
5. Hướng re-sharding khi số tenant tăng.

### **Sản phẩm cần nộp**

- `sharding-strategies.md`
- Diagram mô tả routing request theo shard key.
- Diagram tình huống hot shard.
- Kế hoạch re-sharding ở mức high-level.

### **Hoàn thành khi**

- [ ]  Phân biệt được hash và range sharding.
- [ ]  Hiểu hot spot/hot shard.
- [ ]  Hiểu re-sharding phức tạp vì data movement và routing.
- [ ]  Nhận biết cross-shard query/transaction là costly.
- [ ]  Đưa ra được design có reasoning, không chỉ chọn theo cảm tính.