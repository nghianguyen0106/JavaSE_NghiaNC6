# 8. Circuit Breaker
## **Cần học**

Mentee cần tìm hiểu:

- Cascading failure.
- Timeout.
- Retry.
- Circuit Breaker.
- Trạng thái Closed.
- Trạng thái Open.
- Trạng thái Half-open.
- Fallback.

## **Cần hiểu**

Text

`Closed
  ↓ lỗi nhiều
Open
  ↓ hết thời gian chờ
Half-open
  ↓ request thành công
Closed`

Circuit Breaker giúp ngăn việc tiếp tục gọi một service đang lỗi, tránh làm sự cố lan rộng.

## **Yêu cầu thực hành**

Mentee cần mô phỏng payment service:

- Payment service phản hồi thành công.
- Payment service timeout.
- Payment service trả lỗi liên tục.
- Circuit chuyển sang Open.
- Sau một khoảng thời gian, thử lại ở Half-open.
- Nếu thành công, đóng circuit.

## **Sản phẩm cần nộp**

- State diagram.
- Code mô phỏng hoặc demo dùng thư viện phù hợp.
- Cấu hình:
    - Failure threshold.
    - Timeout.
    - Retry count.
    - Fallback behavior.
- Phân tích sự khác nhau giữa retry và circuit breaker.

## **Tiêu chí hoàn thành**

Mentee giải thích được:

- Retry dùng để thử lại request.
- Circuit Breaker dùng để ngừng gọi dependency đang lỗi.
- Retry sai cách có thể làm hệ thống quá tải hơn.
- Fallback cần trả về hành vi có ý nghĩa cho người dùng hoặc hệ thống.