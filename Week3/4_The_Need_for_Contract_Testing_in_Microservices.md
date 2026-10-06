# 4. The Need for Contract Testing in Microservices
### **Cần học gì**

- Integration test trong microservices khó ở điểm nào.
- Consumer và provider là gì.
- API contract là gì:
    - Endpoint.
    - HTTP method.
    - Request headers.
    - Request body.
    - Response status.
    - Response body.
    - Field type.
    - Mandatory/optional field.
    - Error response.
- Vì sao unit test và end-to-end test không đủ.
- Contract drift là gì.

### **Cần hiểu đến mức nào**

cần hiểu scenario:

`Order Service gọi Product Service:

GET /products/{id}

Order Service mong Product Service trả:
{
  "id": "P-1",
  "name": "Keyboard",
  "price": 100.0,
  "available": true
}`

Product team deploy version mới:

`{
  "id": "P-1",
  "productName": "Keyboard",
  "unitPrice": 100.0
}`

Product Service vẫn chạy bình thường.

Order Service unit test vẫn pass vì mock cũ.

Nhưng production fail vì:

`- Field name bị đổi.
- `available` bị xóa.
- Consumer parse response lỗi hoặc logic sai.`

Đây là contract drift.

### **Assignment**

Tạo file `04-contract-testing-need.md`.

cần phân tích:

`Order Service phụ thuộc vào Product Service và Payment Service.`

Viết bảng:

| **Dependency** | **Consumer expectation** | **Nếu provider đổi sai** | **Unit test có phát hiện không?** | **E2E test có hạn chế gì?** |
| --- | --- | --- | --- | --- |
| Product Service |  |  |  |  |
| Payment Service |  |  |  |  |

Sau đó trả lời:

1. Vì sao mock-based unit test không đủ?
2. Vì sao chỉ dùng end-to-end test không đủ?
3. Contract test đứng ở đâu trong testing pyramid/microservice test strategy?

### **Sản phẩm cần nộp**

- `04-contract-testing-need.md`
- Sequence diagram: Order Service gọi Product Service.
- Một ví dụ contract breaking change.

### **Hoàn thành khi**

- [ ]  Phân biệt được consumer/provider.
- [ ]  Nêu được contract drift.
- [ ]  Hiểu unit test mock không đảm bảo provider thật tương thích.
- [ ]  Hiểu E2E test chậm, flaky và khó cover mọi combination.