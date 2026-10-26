# 6. Spring Batch Architecture
### **Cần học gì**

- Batch processing phù hợp với bài toán nào.
- Job.
- Step.
- ItemReader.
- ItemProcessor.
- ItemWriter.
- Chunk-oriented processing.
- JobRepository.
- Transaction boundary.
- Restartability.
- Skip/retry.
- Idempotency.
- Parallelism/partitioning ở mức khái niệm.

Spring Batch mô hình hóa batch bằng Job và Step; một Step có thể dùng `ItemReader`, `ItemProcessor` và `ItemWriter`, trong đó processor là optional. Với chunk processing, item được đọc/xử lý theo nhóm và commit theo chunk size. (docs.spring.io)

### **Assignment**

Thiết kế batch job:

`Mỗi tháng HR upload CSV chứa 3 triệu salary adjustments.

Input:
employee_id, effective_date, adjustment_type, amount

Yêu cầu:
- Validate input.
- Bỏ qua record lỗi có kiểm soát.
- Không apply adjustment trùng.
- Có thể restart sau khi job fail.
- Ghi audit.
- Xuất file lỗi cho HR.`

### **Sản phẩm cần nộp**

- `06-spring-batch-design.md`
- Sơ đồ Job → Step → Reader → Processor → Writer.
- Chunk size đề xuất và lý do.
- Skip/retry policy.
- Idempotency strategy.
- Restart/recovery flow.

### **Hoàn thành khi**

- [ ]  Phân biệt online API processing và batch processing.
- [ ]  Hiểu chunk size tác động transaction/memory/throughput.
- [ ]  Có idempotency key hoặc unique constraint.
- [ ]  Có error output cho record lỗi.
- [ ]  Có plan restart sau failure.