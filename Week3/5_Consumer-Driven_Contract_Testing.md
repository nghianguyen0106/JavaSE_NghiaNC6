# 5. Consumer-Driven Contract Testing
### **Cần học gì**

- Consumer-Driven Contract (CDC) là gì.
- Pact là gì ở mức khái niệm.
- Consumer tạo contract.
- Provider verify contract.
- Pact broker hoặc nơi lưu contract.
- Provider state.
- Versioning và backward compatibility.
- Breaking vs non-breaking API change.

### **Cần hiểu đến mức nào**

Mentee phải mô tả được flow:

`1. Order Service viết consumer contract:
   "Khi gọi GET /products/P-1,
    tôi cần status 200 và field id, name, price, available."

2. Contract được publish.

3. Product Service chạy provider verification:
   "Implementation hiện tại có đáp ứng contract của Order Service không?"

4. Nếu provider đổi API làm contract fail:
   CI/CD phải block deployment hoặc cảnh báo team.`

### **Assignment**

Thiết kế consumer contract cho:

`Order Service → Product Service

GET /api/products/{productId}`

Consumer cần:

`{
  "id": "P-100",
  "name": "Mechanical Keyboard",
  "price": 120.50,
  "available": true
}`

cần nêu:

- Request.
- Status code.
- Required fields.
- Field type.
- Error response khi product không tồn tại.
- Những thay đổi nào là breaking change.
- Những thay đổi nào không breaking.

Ví dụ:

| **Provider change** | **Breaking?** | **Lý do** |
| --- | --- | --- |
| Thêm field `description` |  |  |
| Đổi `name` thành `productName` |  |  |
| Đổi `price` từ number sang string |  |  |
| Xóa `available` |  |  |
| Thêm optional field |  |  |

### **Sản phẩm cần nộp**

`contract-testing/
├── README.md
├── product-contract.md
├── consumer-expectations.md
└── provider-verification-plan.md`

Nếu có thời gian, có thể làm PoC Pact:

`- Consumer test tạo pact file.
- Provider verification đọc pact file.`

### **Hoàn thành khi**

- [ ]  Viết được consumer expectation rõ ràng.
- [ ]  Phân biệt breaking và non-breaking change.
- [ ]  Hiểu provider verification phải chạy trong CI.
- [ ]  Không biến contract thành bản sao toàn bộ database model.
- [ ]  Contract chỉ mô tả những gì consumer thực sự cần.