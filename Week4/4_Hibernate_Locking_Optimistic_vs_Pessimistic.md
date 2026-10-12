# 4. Hibernate Locking: Optimistic vs Pessimistic
### **Cần học gì**

- Lost update.
- Optimistic locking.
- `@Version`.
- Optimistic lock exception.
- Retry/reload UX.
- Pessimistic locking.
- Lock timeout.
- Deadlock.
- Transaction scope.

Hibernate mô tả optimistic locking là giả định conflict ít, transaction kiểm tra conflict trước commit; nó phù hợp read-often/write-sometimes. Pessimistic locking giả định conflict nhiều và giữ resource lock trong thời gian transaction sử dụng resource đó. (docs.hibernate.org)

### **Cần hiểu đến mức nào**

| **Tiêu chí** | **Optimistic Lock** | **Pessimistic Lock** |
| --- | --- | --- |
| Conflict kỳ vọng | Thấp/trung bình | Cao |
| Cách xử lý | Phát hiện conflict khi update | Khóa sớm để chặn transaction khác |
| Scale | Thường tốt hơn | Dễ gây wait/deadlock nếu dùng sai |
| UX | User có thể phải reload/retry | User có thể chờ/timeout |
| Ví dụ | Sửa hồ sơ nhân viên | Reserve một slot/tài nguyên khan hiếm trong transaction ngắn |

### **Assignment**

Tạo `04-locking-strategy.md` phân tích:

`Hai HR manager cùng sửa salary của Employee E-100.
Manager A tăng salary từ 20M lên 25M.
Manager B đổi salary từ 20M lên 22M.`

cần:

1. Mô tả lost update nếu không lock.
2. Thiết kế với `@Version`.
3. Mô tả exception/UX khi conflict.
4. Nêu khi nào cân nhắc pessimistic lock.
5. Nêu vì sao không được giữ pessimistic lock trong lúc gọi external Tax Service.

### **Hoàn thành khi**

- [ ]  Hiểu `@Version` chống lost update.
- [ ]  Có conflict resolution strategy.
- [ ]  Hiểu pessimistic lock không phải “an toàn hơn nên dùng mọi nơi”.
- [ ]  Nêu được lock timeout/deadlock risk.