# 4_Database_Replication
### **Cần học gì**

- Database replication là gì.
- Primary/leader và replica/follower là gì.
- Write routing và read routing.
- Read scaling.
- High availability và failover.
- Replica lag.
- Promotion của replica khi leader down.

### **Cần hiểu đến mức nào**

Mentee cần mô tả được flow:

`Client write
    ↓
Primary / Leader
    ↓
Replication log
    ↓
Replica 1 / Replica 2 / Replica 3`

Và flow đọc:

`Write request → Primary
Read request  → Primary hoặc Replica`

Mentee phải hiểu:

- Không phải replica nào cũng luôn có dữ liệu mới nhất.
- Read scaling tăng khả năng phục vụ request đọc.
- Replication **không tự động giải quyết write bottleneck** vì leader vẫn là nơi xử lý write chính.
- Khi leader down, cần có failover:
    - Chọn replica phù hợp.
    - Promote thành leader mới.
    - Route traffic sang leader mới.

### **Assignment**

Tạo file `replication-architecture.md` cho hệ thống e-commerce:

1. Vẽ kiến trúc gồm:
    - Một primary.
    - Hai replicas.
    - Application.
    - Read/write router.
2. Mô tả routing rules:

| **Loại request** | **Đi đến đâu** | **Lý do** |
| --- | --- | --- |
| Create order |  |  |
| Update payment status |  |  |
| Get product catalog |  |  |
| Get order vừa tạo |  |  |
| View reporting dashboard |  |  |
1. Mô tả tình huống primary down:
    - Hệ thống phát hiện thế nào?
    - Replica nào được promote?
    - Application cần cập nhật gì?

### **Sản phẩm cần nộp**

- `replication-architecture.md`
- Architecture diagram.
- Bảng routing read/write.
- Failover flow gồm ít nhất 5 bước.

### **Hoàn thành khi**

- [ ]  Biết primary và replica có vai trò gì.
- [ ]  Phân biệt read scaling và write scaling.
- [ ]  Hiểu replica lag.
- [ ]  Mô tả được failover cơ bản.
- [ ]  Biết vì sao “read-after-write” có thể không nên route sang replica ngay.