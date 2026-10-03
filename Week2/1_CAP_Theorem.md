# 1. CAP Theorem
### **Cần học gì**

- Distributed system là gì.
- Network partition là gì.
- Ba thuộc tính trong CAP:
    - **Consistency (C)**.
    - **Availability (A)**.
    - **Partition Tolerance (P)**.
- Sự khác nhau giữa hệ thống thiên về:
    - **CP**.
    - **AP**.
    - **CA** trong điều kiện lý tưởng không có partition.
- Trade-off khi network partition xảy ra.

### **Cần hiểu đến mức nào**

Mentee cần giải thích được:

1. **Consistency trong CAP** nghĩa là gì:
    - Sau khi write thành công, read phù hợp phải thấy dữ liệu mới nhất hoặc nhận lỗi; không được trả dữ liệu cũ như thể nó là dữ liệu hiện tại.
2. **Availability** nghĩa là gì:
    - Mỗi request gửi đến một node còn hoạt động phải nhận được response, dù response đó có thể chưa chứa dữ liệu mới nhất.
3. **Partition Tolerance** nghĩa là gì:
    - Hệ thống vẫn phải ra quyết định khi một nhóm node không thể giao tiếp với nhóm node khác.
4. Khi partition xảy ra:
    - Chọn **C**: từ chối/tạm dừng một số request để tránh dữ liệu sai.
    - Chọn **A**: vẫn phục vụ request nhưng có thể trả dữ liệu cũ.
5. CAP **không đơn giản là “chọn bất kỳ 2 trong 3 mọi lúc”**:
    - Trong hệ thống phân tán thực tế, partition có thể xảy ra nên thường phải chấp nhận P.
    - Khi có partition, trade-off thực tế là **C hoặc A**.

### **Case study cần phân tích**

**Bài toán chuyển tiền:**

`Account A: 1,000,000 VND
Account B: 500,000 VND

User chuyển 300,000 VND từ A sang B.`

Mentee cần trả lời:

- Nếu network partition xảy ra giữa hai replica, hệ thống banking nên ưu tiên C hay A?
- Nếu ưu tiên availability và cho phép đọc dữ liệu cũ, rủi ro là gì?
- Có chấp nhận tạm thời báo “service unavailable” không? Vì sao?

### **Assignment**

Viết file `cap-analysis.md` phân tích 3 hệ thống:

| **System** | **CP hay AP?** | **Lý do** |
| --- | --- | --- |
| Banking/Payment |  |  |
| Social-media news feed |  |  |
| E-commerce product catalog |  |  |

Mỗi hệ thống cần nêu:

- Dữ liệu nào quan trọng.
- Khi partition xảy ra, hệ thống nên từ chối request nào hoặc vẫn cho phép request nào.
- Hậu quả nếu chọn sai trade-off.

### **Sản phẩm cần nộp**

- `cap-analysis.md`
- Một sơ đồ mô tả network partition giữa Node A và Node B.
- Một ví dụ read/write cho thấy dữ liệu có thể khác nhau giữa các node.

### **Hoàn thành khi**

- [ ]  Giải thích được C, A, P bằng ví dụ.
- [ ]  Phân biệt được CP và AP.
- [ ]  Không diễn giải CAP thành “chọn tùy ý 2/3”.
- [ ]  Phân tích được ít nhất 3 use case có reasoning rõ ràng.