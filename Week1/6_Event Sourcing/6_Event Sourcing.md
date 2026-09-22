# 6. Event Sourcing
## **Cần học**

Mentee cần tìm hiểu:

- Event là gì.
- Event Sourcing là gì.
- State được rebuild từ event như thế nào.
- Event khác command như thế nào.
- Event versioning.
- Replay event.
- Ưu điểm và hạn chế.

## **Cần hiểu**

Ví dụ các event:

Text

`OrderCreated
ItemAddedToOrder
OrderConfirmed
OrderCancelled`

Thay vì chỉ lưu:

Text

`Order status = CONFIRMED`

Hệ thống lưu chuỗi sự kiện:

Text

`OrderCreated
ItemAddedToOrder
OrderConfirmed`

Sau đó rebuild lại trạng thái order từ các event này.

## **Yêu cầu thực hành**

Không bắt buộc dùng Kafka hoặc event store thật.

Mentee có thể dùng in-memory event list:

Java

`List<DomainEvent> events;`

Cần triển khai flow:

Text

`ConfirmOrderCommand
        ↓
Order.confirm()
        ↓
OrderConfirmedEvent
        ↓
EventHandler`

## **Sản phẩm cần nộp**

- Một hoặc hai event class.
- Demo lưu event in-memory.
- Flow diagram.
- File phân tích:
    - Event Sourcing có lợi ích gì?
    - Khó khăn gì khi thay đổi event schema?
    - Bài toán Order có thực sự cần Event Sourcing không?

## **Tiêu chí hoàn thành**

- Phân biệt được command và event.
- Hiểu event là điều đã xảy ra.
- Hiểu event sourcing không đơn giản chỉ là publish message.
- Biết nêu ít nhất hai ưu điểm và hai hạn chế.