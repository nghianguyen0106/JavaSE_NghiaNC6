# 4. Domain-Driven Design## **Cần học**

Mentee cần tìm hiểu:

- Entity.
- Value Object.
- Aggregate.
- Aggregate Root.
- Domain Service.
- Domain Event.
- Invariant.
- Ubiquitous Language.

## **Cần hiểu**

Trong bài toán Order:

- `Order` là Entity và Aggregate Root.
- `OrderItem` thuộc về `Order`.
- `Money` hoặc `Price` có thể là Value Object.
- `OrderStatus` biểu diễn trạng thái đơn.
- Business rules nằm trong domain.
- Bên ngoài không được tự ý thay đổi trạng thái hoặc danh sách item.

## **Business rules cần triển khai**

- Order phải có ít nhất một item.
- Số lượng sản phẩm phải lớn hơn 0.
- Giá sản phẩm không được âm.
- Không thể confirm order rỗng.
- Chỉ order ở trạng thái `PENDING` mới được confirm.
- Không thể cancel order đã giao.
- Không thể chỉnh sửa item sau khi order đã được confirm.

## **Yêu cầu thực hành**

Mentee cần tạo:

Text

`Order
OrderItem
OrderStatus
Money hoặc Price
Domain exceptions`

Các hành vi nên được đặt trong domain:

Java

`order.addItem(item);
order.confirm();
order.cancel();
order.calculateTotal();`

Không nên đặt toàn bộ logic ở controller hoặc service bên ngoài.

## **Sản phẩm cần nộp**

- Domain model.
- Domain unit tests.
- Sơ đồ aggregate.
- Danh sách business rules.
- Giải thích vì sao `Order` là Aggregate Root.

## **Tiêu chí hoàn thành**

- Business rules được bảo vệ trong domain.
- Không cho phép object bên ngoài thay đổi state tùy ý.
- Có test cho case thành công và case lỗi.
- Mentee giải thích được Entity khác Value Object như thế nào.