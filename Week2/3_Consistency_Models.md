# 3. Consistency Models
### **Cần học gì**

- **Strong consistency**.
- **Eventual consistency**.
- **Read-your-writes consistency**.
- **Monotonic reads**.
- **Causal consistency**.
- Replica lag và stale read.

### **Cần hiểu đến mức nào**

#### **Strong Consistency**

Sau write thành công, mọi read tiếp theo phải thấy write mới nhất.

`Write status = PAID
↓
Read status
↓
Phải nhận PAID`

#### **Eventual Consistency**

Các replicas có thể khác nhau trong một khoảng thời gian, nhưng cuối cùng sẽ hội tụ về cùng dữ liệu.

`10:00:00  Write status = PAID vào primary
10:00:01  Read từ replica vẫn thấy PENDING
10:00:03  Replica sync xong
10:00:04  Read thấy PAID`

#### **Read-your-writes**

User vừa write dữ liệu thì chính user đó cần đọc lại được dữ liệu mình vừa ghi.

`User A đổi avatar.
User A refresh profile.
→ Phải thấy avatar mới, dù các user khác có thể thấy avatar cũ tạm thời.`

#### **Monotonic Reads**

Một user không được đọc dữ liệu “lùi về quá khứ”.

`Lần 1: User thấy order = CONFIRMED
Lần 2: User không nên thấy order = PENDING`

#### **Causal Consistency**

Nếu event B xảy ra sau và phụ thuộc vào event A, mọi nơi phải thấy A trước B.

`1. User tạo post.
2. User comment vào post.

Không được thấy comment trước khi thấy post.`

### **Assignment**

Tạo file `consistency-models.md` với bảng:

| **Model** | **Định nghĩa** | **Ví dụ** | **Ưu điểm** | **Rủi ro/giới hạn** |
| --- | --- | --- | --- | --- |
| Strong consistency |  |  |  |  |
| Eventual consistency |  |  |  |  |
| Read-your-writes |  |  |  |  |
| Monotonic reads |  |  |  |  |
| Causal consistency |  |  |  |  |

Sau đó giải quyết tình huống:

`Order vừa được payment thành công.
Frontend gọi GET /orders/{id} nhưng request bị route sang read replica chưa sync.
Frontend thấy PENDING thay vì PAID.`

Mentee cần đề xuất ít nhất 2 cách xử lý, ví dụ:

- Read từ primary trong một khoảng thời gian sau write.
- Sticky session / session consistency.
- Return dữ liệu mới ngay trong response của command.
- Retry/polling có giới hạn.
- Hiển thị trạng thái “Payment is being confirmed”.

### **Sản phẩm cần nộp**

- `consistency-models.md`
- Sequence diagram cho tình huống stale read.
- Đề xuất giải pháp và trade-off.

### **Hoàn thành khi**

- [ ]  Phân biệt được strong và eventual consistency.
- [ ]  Giải thích được replica lag.
- [ ]  Hiểu read-your-writes và monotonic reads.
- [ ]  Đề xuất được giải pháp cho stale read sau write.