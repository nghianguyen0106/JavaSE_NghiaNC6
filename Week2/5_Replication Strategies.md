# 5. Replication Strategies
### **Cần học gì**

- Synchronous replication.
- Asynchronous replication.
- Semi-synchronous replication.
- Ack/acknowledgement.
- Durability.
- Replication lag.
- Data-loss window.

### **Cần hiểu đến mức nào**

#### **Synchronous Replication**

Primary chỉ báo thành công khi replica cần thiết đã xác nhận ghi thành công.

`Client → Primary write
           ↓
        Replica ACK
           ↓
Client nhận success`

Đặc điểm:

- Consistency/durability tốt hơn.
- Latency write cao hơn.
- Nếu replica không phản hồi, write có thể chậm hoặc fail tùy policy.

#### **Asynchronous Replication**

Primary báo write thành công ngay sau khi ghi local; replica nhận dữ liệu sau.

`Client → Primary write → Client nhận success
           ↓
    Replicate background
           ↓
        Replica cập nhật sau`

Đặc điểm:

- Write nhanh hơn.
- Replica có thể lag.
- Nếu primary chết trước khi replica nhận write, có thể mất dữ liệu đã “ack” cho client.

#### **Semi-synchronous Replication**

Primary chờ tối thiểu một hoặc một số replica xác nhận, không nhất thiết chờ toàn bộ.

`Client → Primary write
           ↓
  Chờ tối thiểu 1 replica ACK
           ↓
Client nhận success`

### **Assignment**

Tạo `replication-strategies.md` so sánh:

| **Tiêu chí** | **Synchronous** | **Asynchronous** | **Semi-synchronous** |
| --- | --- | --- | --- |
| Write latency |  |  |  |
| Data durability |  |  |  |
| Replica lag |  |  |  |
| Rủi ro data loss |  |  |  |
| Use case phù hợp |  |  |  |

Phân tích các use case:

1. Payment transaction.
2. Product catalog.
3. Application logs.
4. Notification delivery history.
5. Analytics events.

Mentee cần chọn strategy và giải thích lý do.

### **Sản phẩm cần nộp**

- `replication-strategies.md`
- Timeline minh họa:
    - Synchronous write.
    - Asynchronous write.
- Một case data loss của asynchronous replication.

### **Hoàn thành khi**

- [ ]  Giải thích được replication acknowledgement.
- [ ]  Phân biệt được latency và durability.
- [ ]  Hiểu data-loss window của async replication.
- [ ]  Chọn được strategy phù hợp cho use case cụ thể.