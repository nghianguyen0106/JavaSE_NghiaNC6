# **5. CQRS — Command Query Responsibility Segregation**
## **Cần học**

Mentee cần tìm hiểu:

- Command là gì.
- Query là gì.
- Sự khác nhau giữa model ghi và model đọc.
- Khi nào CQRS hữu ích.
- Khi nào CQRS tạo thêm complexity không cần thiết.

## **Cần hiểu**

- Command dùng để thay đổi state.
- Query dùng để đọc dữ liệu.
- CQRS không bắt buộc phải dùng microservices.
- CQRS không bắt buộc phải dùng Kafka.
- Không phải hệ thống nào cũng cần CQRS.

## **Yêu cầu thực hành**

Thiết kế các thành phần:

Text

`CreateOrderCommand
ConfirmOrderCommand
CancelOrderCommand
GetOrderQuery`

Có thể tạo các handler:

Text

`CreateOrderCommandHandler
ConfirmOrderCommandHandler
GetOrderQueryHandler`

## **Sản phẩm cần nộp**

- Sơ đồ command/query flow.
- Một command handler.
- Một query handler.
- File phân tích:
    - Vì sao bài toán này có hoặc không cần CQRS?
    - Nếu dùng CQRS thì lợi ích gì?
    - Complexity phát sinh là gì?

## **Tiêu chí hoàn thành**

Mentee phải giải thích được:

- CQRS giải quyết vấn đề gì.
- CQRS khác CRUD như thế nào.
- Khi nào không nên sử dụng CQRS.

Không bắt buộc triển khai hệ thống CQRS phức tạp hoặc tách database read/write trong tuần đầu.