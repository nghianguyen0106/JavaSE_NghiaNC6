# BÀI TẬP: CONSISTENCY MODELS

## 1. Bảng so sánh các Consistency Models

| Model | Định nghĩa | Ví dụ | Ưu điểm | Rủi ro / Giới hạn |
| :--- | :--- | :--- | :--- | :--- |
| Strong consistency | Sau khi write thành công, mọi read tiếp theo ở bất kỳ đâu đều phải thấy giá trị mới nhất. | Ghi `status = PAID` vào database $\rightarrow$ Mọi request đọc ngay sau đó bắt buộc phải nhận được `PAID`. | Dữ liệu luôn chính xác 100%, không lo conflict hay đọc phải dữ liệu cũ; tư duy lập trình đơn giản. | Latency cao vì phải chờ sync qua các node; Throughput thấp; giảm tính Availability khi mạng có độ trễ. |
| Eventual consistency | Các replica có thể tạm thời khác nhau trong một khoảng thời gian, nhưng cuối cùng sẽ hội tụ về cùng một dữ liệu khi không còn write mới. | 10:00:00 ghi `PAID` vào Primary.<br>10:00:01 đọc từ Replica thấy `PENDING`.<br>10:00:03 Replica sync xong.<br>10:00:04 đọc thấy `PAID`. | Latency thấp, Throughput cao, hệ thống chịu tải đọc cực tốt và dễ mở rộng (scale). | Xuất hiện hiện tượng đọc dữ liệu cũ (*stale read*); logic xử lý ở application phức tạp hơn. |
| Read-your-writes | Người dùng vừa ghi dữ liệu thì chính người dùng đó phải lập tức đọc lại được dữ liệu mình vừa ghi. | User A cập nhật avatar mới $\rightarrow$ User A tải lại trang cá nhân phải thấy ngay avatar mới (dù người dùng khác có thể tạm thời thấy avatar cũ). | Đảm bảo trải nghiệm người dùng không bị hoang mang (tránh cảm giác thao tác vừa làm bị biến mất). | Cần cơ chế theo dõi session hoặc định tuyến đặc biệt (chuyển hướng đọc về Primary trong thời gian ngắn). |
| Monotonic reads | Một người dùng sau khi đã đọc được dữ liệu mới thì những lần đọc tiếp theo không bao giờ bị "tua ngược về quá khứ" để thấy dữ liệu cũ hơn. | Lần 1: User đọc thấy đơn hàng là `CONFIRMED`.<br>Lần 2: User đọc lại, tuyệt đối không được thấy đơn hàng tụt về `PENDING`. | Dữ liệu nhất quán theo trục thời gian với từng client, tránh gây hiểu lầm hệ thống bị rollback dữ liệu. | Client phải gắn chặt với replica có tiến độ sync phù hợp hoặc phải chuyển tiếp về Primary khi replica bị trễ. |
| Causal consistency | Nếu sự kiện B xảy ra sau và phụ thuộc nhân quả vào sự kiện A, mọi nơi trong hệ thống đều phải nhìn thấy A trước khi thấy B. | 1. User đăng bài viết (A).<br>2. User bình luận vào bài viết đó (B).<br>$\rightarrow$ Không ai được nhìn thấy bình luận trước khi thấy bài viết. | Đảm bảo đúng logic nhân quả của nghiệp vụ mà không cần toàn bộ hệ thống phải khóa đồng bộ cứng như Strong consistency. | Cần theo dõi vết nhân quả (như version vector, causal token), phức tạp trong việc cài đặt và kiểm thử. |

## 2. Đề xuất giải pháp và Phân tích Trade-off

Để giải quyết vấn đề trên, có các hướng xử lý cụ thể như sau (đáp ứng tiêu chí đề xuất ít nhất 2 giải pháp):

### Giải pháp 1: Đọc từ Primary trong một khoảng thời gian sau Write (Time-window routing)
* Cơ chế hoạt động:
  - Khi một user thực hiện cập nhật trạng thái đơn hàng thành công, hệ thống lưu một cờ trong Session hoặc Cookie: `last_order_update_time = now()`.
  - Trong vòng 3 - 5 giây kế tiếp (khoảng thời gian đủ lớn hơn mức replica lag thông thường), mọi request đọc đơn hàng của chính user này sẽ được định tuyến trực tiếp về Primary DB.
  - Sau khi hết 5 giây, các request đọc tiếp theo lại được gửi sang Read Replica như bình thường.
* Ưu điểm:
  - Đảm bảo 100% đạt được mô hình Read-your-writes consistency.
  - Người dùng luôn thấy đơn hàng là `PAID` ngay sau khi thanh toán.
* Trade-off (Đánh đổi):
  - Làm tăng thêm một lượng tải đọc nhất định lên Primary DB trong cửa sổ thời gian ngắn đó.
  - Tầng API Gateway / Router cần có logic kiểm tra cờ để phân luồng request.

---

### Giải pháp 2: Trả dữ liệu mới ngay trong response của command (Return on Write)
* Cơ chế hoạt động:
  - Thay vì API thanh toán chỉ trả về `{ success: true }` rồi bắt Frontend phải gọi thêm `GET /orders/{id}`, API `POST /orders/{id}/pay` sẽ trả về luôn toàn bộ thông tin đơn hàng mới nhất:
    ```json
    {
      "orderId": "1",
      "status": "PAID",
      "updatedAt": "2026-10-04T21:00:00Z"
    }
    ```
  - Frontend sử dụng dữ liệu này để cập nhật trực tiếp giao diện (Client-side state / Redux / Context), không cần thực hiện thêm request `GET` ngay lúc đó.
* Ưu điểm:
  - Triển khai rất đơn giản, không cần can thiệp tầng hạ tầng Database hay Load Balancer.
  - Giảm thiểu số lượng request mạng, phản hồi tức thì cho người dùng.
* Trade-off (Đánh đổi):
  - Chỉ giải quyết được cho chính màn hình hiện tại. Nếu người dùng ngay lập tức chuyển sang thiết bị khác hoặc mở tab mới và F5 lại trang, request vẫn có thể rơi vào Replica đang bị trễ.

---

### Giải pháp 3: Hiển thị trạng thái trung gian "Đang xác nhận" (UI Polling có giới hạn)
* Cơ chế hoạt động:
  - Nếu hệ thống chấp nhận Eventual Consistency hoàn toàn, trên giao diện sau khi thanh toán hiển thị trạng thái: *"Thanh toán đang được xác nhận..."* kèm hiệu ứng chờ (spinner).
  - Frontend tự động gửi request kiểm tra (polling) sau mỗi 1 giây (tối đa 3 lần). Khi Replica đã đồng bộ xong và trả về `PAID`, giao diện mới chuyển sang hoàn tất.
* Ưu điểm:
  - Không làm tăng tải cho Primary DB, giữ nguyên tính phân tải của hệ thống Read Replica.
  - Quản lý kỳ vọng tâm lý của người dùng tốt.
* Trade-off (Đánh đổi):
  - Tăng nhẹ số lượng request đọc thăm dò từ client.
