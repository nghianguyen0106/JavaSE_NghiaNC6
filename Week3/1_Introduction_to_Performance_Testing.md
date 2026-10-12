# 1. Introduction to Performance Testing

### **Cần học gì**

- Performance testing là gì.
- Mục tiêu của performance testing.
- Các loại performance test:
    - Load test.
    - Stress test.
    - Spike test.
    - Soak/endurance test.
    - Capacity test.
- Các metric chính:
    - Latency.
    - Response time.
    - Throughput.
    - Requests per second (RPS).
    - Error rate.
    - Concurrent users / virtual users.
    - Percentile: p50, p95, p99.
    - Resource utilization: CPU, memory, DB connections, I/O.

### **Cần hiểu đến mức nào**

cần giải thích được:

#### **Latency / Response Time**

Thời gian từ lúc client gửi request đến lúc nhận response.

`Client gửi GET /products
        ↓
Application xử lý
        ↓
Database query
        ↓
Client nhận response

Tổng thời gian = response time / latency quan sát được từ client`

Ví dụ:

`p50 = 120ms
p95 = 800ms
p99 = 2.5s`

Ý nghĩa:

- 50% request nhanh hơn hoặc bằng 120ms.
- 95% request nhanh hơn hoặc bằng 800ms.
- 99% request nhanh hơn hoặc bằng 2.5 giây.

> Không được chỉ nhìn average response time. Average có thể đẹp nhưng một nhóm user vẫn đang có trải nghiệm rất tệ ở p95/p99.
> 

#### **Throughput**

Số request hoặc transaction hệ thống xử lý được trong một đơn vị thời gian.

`500 RPS = 500 requests/second`

#### **Error Rate**

Tỷ lệ request lỗi:

`Error Rate = số request lỗi / tổng request`

Ví dụ:

`100,000 requests
500 failed requests

Error rate = 0.5%`

#### **Saturation**

Mức độ tài nguyên gần chạm giới hạn:

`- CPU: 95%
- DB connection pool: 100/100 connections
- Thread pool: 200/200 active threads
- Disk I/O: saturated`

### **Assignment**

Tạo file `01-performance-fundamentals.md`.

cần:

1. Định nghĩa các loại test.
2. Giải thích các metric bằng ví dụ.
3. Phân tích requirement sau:

`Order API:
- Peak traffic: 1,000 requests/second.
- p95 response time phải dưới 500ms.
- Error rate phải dưới 0.5%.
- Hệ thống cần chịu được traffic liên tục trong 4 giờ.
- Trong flash sale, traffic có thể tăng gấp 5 lần trong 2 phút.`

1. Chọn loại performance test tương ứng với từng requirement.

| **Requirement** | **Test type** | **Metric cần theo dõi** | **Lý do** |
| --- | --- | --- | --- |
| 1,000 RPS bình thường |  |  |  |
| Traffic liên tục 4 giờ |  |  |  |
| Flash sale tăng 5 lần |  |  |  |
| Tìm giới hạn tối đa |  |  |  |

### **Sản phẩm cần nộp**

- `01-performance-fundamentals.md`
- Một bảng định nghĩa metric.
- Một bảng mapping requirement → test type → success criteria.

### **Hoàn thành khi**

- [ ]  Phân biệt được load, stress, spike và soak test.
- [ ]  Giải thích được p50, p95, p99.
- [ ]  Không dùng average response time làm metric duy nhất.
- [ ]  Viết được success criteria có số liệu rõ ràng.