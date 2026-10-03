# 2. PACELC Theorem
### **Cần học gì**

- PACELC là phần mở rộng của CAP.
- Trade-off khi có partition: **P → A hoặc C**.
- Trade-off khi không có partition: **E → L hoặc C**.
    - **L**: Latency thấp.
    - **C**: Consistency mạnh hơn.

### **Cần hiểu đến mức nào**

Mentee cần giải thích được công thức:

`If Partition happens:
    choose Availability or Consistency

Else, when there is no Partition:
    choose Latency or Consistency`

Mentee cần hiểu:

- CAP tập trung vào lúc network partition.
- PACELC bổ sung câu hỏi quan trọng hơn cho trạng thái bình thường:
    - Muốn read nhanh, trả dữ liệu sớm?
    - Hay chờ replica sync để chắc chắn dữ liệu mới nhất?

### **Ví dụ**

`Client write order status = PAID vào primary.

Option 1:
- Primary chờ replica xác nhận.
- Read ở replica thấy dữ liệu mới.
- Latency cao hơn.

Option 2:
- Primary trả response ngay.
- Replica cập nhật sau.
- Latency thấp hơn, nhưng read từ replica có thể thấy status cũ.`

### **Assignment**

Viết file `pacelc-analysis.md` cho hệ thống Order Service:

| **Scenario** | **Ưu tiên** | **Lý do** |
| --- | --- | --- |
| User vừa thanh toán xong và xem trạng thái đơn | Low latency hay strong consistency? |  |
| Trang danh sách sản phẩm | Low latency hay strong consistency? |  |
| Admin xem báo cáo doanh thu cuối ngày | Low latency hay strong consistency? |  |
| Kiểm tra số dư ví điện tử | Low latency hay strong consistency? |  |

### **Sản phẩm cần nộp**

- `pacelc-analysis.md`
- Một sơ đồ so sánh:
    - Read từ primary.
    - Read từ replica.
    - Read-after-write inconsistency.

### **Hoàn thành khi**

- [ ]  Nêu được PACELC mở rộng CAP ở điểm nào.
- [ ]  Phân biệt được latency và consistency.
- [ ]  Biết read nhanh hơn không đồng nghĩa dữ liệu luôn mới nhất.
- [ ]  Đưa ra được trade-off phù hợp cho từng scenario.