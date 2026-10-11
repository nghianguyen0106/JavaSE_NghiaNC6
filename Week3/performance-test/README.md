## 1. Test Environment
* Hạ tầng Test: Môi trường Staging/Perf độc lập (cấu hình tương đương 50% Production).
  * App Pods: 2 Pods (mỗi pod 2 vCPU, 4GB RAM, JVM max heap `-Xmx2g`).
  * Database: PostgreSQL/MySQL 4 vCPU, 16GB RAM, SSD NVMe, Connection pool `maximumPoolSize = 60`.
  * Network: Mạng nội bộ 10 Gbps, máy chạy test k6 nằm cùng dải mạng VPC với app để loại trừ yếu tố giật mạng Internet công cộng.
* Quy tắc môi trường: 
  * Không chạy test trực tiếp trên Production đang có khách thật.
  * Đảm bảo Database đã được migrate và seed sẵn dữ liệu mẫu thực tế (Warm-up DB).

---

## 2. User Journey & Endpoint Test

```
[1. GET /api/products] ──(1.5s think time)──> [2. GET /api/products/{id}]
                                                         │
                                                  (2s think time)
                                                         │
                                                         ▼
[4. GET /api/orders/{id}] <──(1s think time)── [3. POST /api/orders]
```

1. Step 1: `GET /api/products?page=1&pageSize=20` (Xem danh sách sản phẩm trang 1).
2. Step 2: `GET /api/products/{productId}` (Xem chi tiết sản phẩm được chọn).
3. Step 3: `POST /api/orders` (Tạo đơn hàng thanh toán - kèm `idempotencyKey`).
4. Step 4: `GET /api/orders/{orderId}` (Kiểm tra trạng thái đơn hàng vừa tạo).

---

## 3. Load & Thresholds

### Load Stages
* Ramp-up: 0 -> 50 Virtual Users (VUs) trong 1 phút.
* Sustain / Steady State: 50 VUs trong 3 phút.
* Ramp-down: 50 -> 0 VUs trong 30 giây.
* Total test time: 4 phút 30 giây.

### SLA / Thresholds:
* Error Rate: `http_req_failed < 1%`.
* Latency xem danh sách: `p95 của GET /api/products < 300ms`.
* Latency tạo đơn hàng: `p95 của POST /api/orders < 800ms`.
* Latency tra cứu đơn hàng: `p95 của GET /api/orders/{id} < 400ms`.

---

## 4. Cách cài đặt và thực thi

### Cài đặt k6:
* Windows (Chocolatey/Winget):
  ```powershell
  winget install k6 --source winget
  # hoặc
  choco install k6
  ```
* macOS (Homebrew): `brew install k6`
* Linux (APT): `sudo apt-get install k6`

### Thực thi k6:
Chạy với cấu hình URL mặc định:
```bash
k6 run order-flow-test.js
```

Chạy với URL môi trường cụ thể (truyền qua Environment Variable):
```bash
k6 run -e BASE_URL=https://staging-api.example.com order-flow-test.js
```

Xuất kết quả chi tiết ra file JSON hoặc CSV để vẽ đồ thị:
```bash
k6 run --out json=result-metrics.json order-flow-test.js
```

---

## 5. Known Limitations
1. Test Data còn hạn chế: 
   * Số lượng sản phẩm mẫu có giới hạn, có thể dẫn đến việc Database cache (Buffer Pool) lưu trọn vào RAM, làm kết quả đọc nhanh hơn thực tế khi Production có hàng triệu sản phẩm khác nhau.
2. Mock Third-party Latency: 
   * Cổng thanh toán (Payment Gateway) và bên vận chuyển bên ngoài đang được mock ở local/internal; chưa phản ánh đúng độ trễ ngẫu nhiên từ mạng Internet công cộng của đối tác thứ ba (thường từ 300ms - 1.5s).
3. Chưa giả lập biến động băng thông Client: 
   * Kịch bản chạy trên mạng băng thông cao; chưa mô phỏng độ trễ của mạng 4G/5G người dùng thực trên điện thoại di động (Packet loss, Handshake latency).
