# 2. Load Testing Tools & Scripting
### **Cần học gì**

- Vai trò của load testing tool.
- Tổng quan về:
    - k6.
    - Gatling.
    - JMeter.
- Các khái niệm trong script:
    - Virtual users (VUs).
    - Stages / ramp-up / ramp-down.
    - Request.
    - Headers.
    - Authentication token.
    - Test data.
    - Checks/assertions.
    - Thresholds.
- Vì sao test data và môi trường test rất quan trọng.

### **Cần hiểu đến mức nào**

không bắt buộc phải giỏi cả ba tool. Mỗi người chọn **một tool**:

| **Tool** | **Phù hợp khi** |
| --- | --- |
| k6 | Muốn script bằng JavaScript/TypeScript, CI-friendly, dễ version control |
| Gatling | Hệ Java/Scala, muốn simulation mạnh và report tốt |
| JMeter | Cần GUI, quick setup, team quen dùng JMeter |

một script test không chỉ là “gọi endpoint nhiều lần”.

Một test script tối thiểu phải có:
`1. Kịch bản user thật.
2. Tốc độ tăng tải.
3. Request hợp lệ.
4. Test data hợp lệ.
5. Assertion/check.
6. Threshold pass/fail.
7. Thời gian chạy.
8. Báo cáo kết quả.`

### **Assignment**

Tạo một performance test cho API giả định:

`POST /api/orders
GET  /api/orders/{id}
GET  /api/products?page=1&pageSize=20`

User journey:

`1. User xem product list.
2. User xem một product.
3. User tạo order.
4. User kiểm tra order vừa tạo.`

Yêu cầu tải:

`- Ramp từ 0 lên 50 VUs trong 1 phút.
- Giữ 50 VUs trong 3 phút.
- Ramp xuống 0 trong 30 giây.
- p95 của GET product list < 300ms.
- p95 của POST create order < 800ms.
- Error rate < 1%.`

### **Ví dụ expected structure với k6**

JavaScript

`import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "1m", target: 50 },
    { duration: "3m", target: 50 },
    { duration: "30s", target: 0 },
  ],
  thresholds: {
    http_req_failed: ["rate<0.01"],
    "http_req_duration{endpoint:product-list}": ["p(95)<300"],
    "http_req_duration{endpoint:create-order}": ["p(95)<800"],
  },
};

export default function () {
  const productResponse = http.get(
    "https://test.example.com/api/products?page=1&pageSize=20",
    { tags: { endpoint: "product-list" } }
  );

  check(productResponse, {
    "product list returns 200": (res) => res.status === 200,
  });

  const createOrderResponse = http.post(
    "https://test.example.com/api/orders",
    JSON.stringify({
      customerId: "test-customer",
      items: [{ productId: "product-1", quantity: 1 }],
    }),
    {
      headers: { "Content-Type": "application/json" },
      tags: { endpoint: "create-order" },
    }
  );

  check(createOrderResponse, {
    "create order returns success": (res) => res.status === 200 || res.status === 201,
  });

  sleep(1);
}`

### **Sản phẩm cần nộp**

`performance-test/
├── README.md
├── order-flow-test.js             hoặc Gatling/JMeter test file
├── test-data.md
└── result-summary.md`

README cần nêu:

- Test environment.
- Endpoint test.
- VUs/RPS mục tiêu.
- Threshold.
- Cách chạy.
- Hạn chế của test.

### **Hoàn thành khi**

- [ ]  Script có ramp-up/ramp-down.
- [ ]  Có check/assertion response.
- [ ]  Có threshold p95/p99/error rate.
- [ ]  Test data không làm request fail do dữ liệu giả sai.
- [ ]  Script nằm trong source control, chạy lặp lại được.