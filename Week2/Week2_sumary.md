Dưới đây là bản **tổng hợp toàn bộ kiến thức cốt lõi của Week 2** (Kiến trúc Hệ thống phân tán & Cơ sở dữ liệu quy mô lớn), được biên soạn súc tích, trực quan kèm theo các ví dụ thực tế giúp bạn nắm bắt nhanh nhất.

---

# 🗺️ TỔNG QUAN CHƯƠNG TRÌNH TUẦN 2
Tuần 2 giải quyết câu hỏi lớn nhất của kỹ sư phần mềm khi hệ thống mở rộng:  
👉 **"Làm sao để lưu trữ và xử lý hàng chục triệu người dùng mà dữ liệu không bị mất, không bị sai lệch và hệ thống không bị sập?"**

Chương trình gồm **7 chủ đề chính** đi từ lý thuyết nền tảng đến kỹ thuật thực chiến:
1. **CAP Theorem** (Quy luật đánh đổi trong hệ thống phân tán).
2. **PACELC Theorem** (Mở rộng của CAP trong trạng thái bình thường).
3. **Consistency Models** (Các mức độ nhất quán dữ liệu).
4. **Database Replication** (Nhân bản cơ sở dữ liệu - Primary/Replica).
5. **Replication Strategies** (Đồng bộ, Bán đồng bộ và Bất đồng bộ).
6. **Database Sharding** (Phân mảnh dữ liệu theo chiều ngang).
7. **Sharding Strategies & Challenges** (Kỹ thuật chia Shard và các cạm bẫy).

---

# 1. CAP THEOREM (ĐỊNH LÝ CAP)

### 💡 Định nghĩa ngắn gọn
Trong một hệ thống phân tán gồm nhiều node máy tính giao tiếp qua mạng, bạn **chỉ có thể chọn tối đa 2 trong 3 yếu tố**:

* **C - Consistency (Nhất quán):** Mọi node đều thấy cùng một dữ liệu mới nhất tại cùng một thời điểm. Sau khi ghi thành công, ai đọc cũng phải thấy dữ liệu mới đó, nếu không thì phải báo lỗi chứ không được trả dữ liệu cũ.
* **A - Availability (Sẵn sàng):** Mọi request gửi đến hệ thống (nếu node còn sống) đều nhận được câu trả lời thành công (không bị lỗi 500 hay timeout), dù dữ liệu trả về có thể là dữ liệu cũ (*stale data*).
* **P - Partition Tolerance (Chịu lỗi đứt mạng):** Hệ thống vẫn tiếp tục hoạt động ngay cả khi đường truyền mạng giữa các node bị đứt (*network partition*).

```
          [C] Consistency
             /   \
            /     \
           /  CAP  \
          /         \
[A] Availability ── [P] Partition Tolerance
```

### ⚠️ Bản chất cốt lõi: Không phải chọn tùy ý 2/3!
Trong thực tế, đường truyền mạng giữa các máy chủ **chắc chắn sẽ có lúc bị chập chờn hoặc đứt đoạn**. Vì vậy, **`P` là bắt buộc phải có**.  
👉 Khi xảy ra sự cố đứt mạng (Network Partition), sự lựa chọn thực tế chỉ là: **Đánh đổi giữa C hay A?**

| Lựa chọn | Cơ chế xử lý | Khi nào dùng? | Ví dụ thực tế |
| :---: | :--- | :--- | :--- |
| **CP System** | Ưu tiên **C** $\rightarrow$ **Thà từ chối phục vụ (hy sinh A) chứ không để dữ liệu bị sai lệch**. | Tiền bạc, tài chính, thanh toán, kho hàng. | **Chuyển tiền ngân hàng:** Nếu 2 server mất kết nối, hệ thống báo *"Dịch vụ tạm gián đoạn"* chứ tuyệt đối không cho chuyển tiền vì dễ bị trừ tiền 2 lần. |
| **AP System** | Ưu tiên **A** $\rightarrow$ **Vẫn trả lời người dùng (hy sinh C), chấp nhận dữ liệu có thể bị trễ/cũ**. | Mạng xã hội, tin tức, giỏ hàng, xem video. | **Facebook / TikTok / YouTube:** Mất mạng giữa các server thì bạn vẫn lướt xem được video và bài post (dù có thể là bài đăng từ 5 phút trước). |

---

# 2. PACELC THEOREM (ĐỊNH LÝ PACELC)

### 💡 Tại sao cần PACELC khi đã có CAP?
Định lý **CAP chỉ nói về lúc mạng bị đứt (Partition)**. Nhưng trong 99.9% thời gian bình thường mạng hoạt động tốt, hệ thống phân tán vẫn phải đối mặt với một sự đánh đổi khác: **Tốc độ (Latency) hay Độ chính xác (Consistency)?**

### 📐 Công thức PACELC
```text
If (P) Partition:
    Trade-off: (A) Availability  vs  (C) Consistency
Else (E):
    Trade-off: (L) Latency       vs  (C) Consistency
```

* **Nếu có Partition (`P`):** Chọn giữa **`A`** (Sẵn sàng) hay **`C`** (Nhất quán).
* **Ngược lại (`E` - Else, khi mạng bình thường):** Chọn giữa **`L`** (Độ trễ thấp / Phản hồi nhanh) hay **`C`** (Dữ liệu nhất quán tuyệt đối).

### 🎯 Ví dụ thực tế
* **Hệ thống PA/EL (ví dụ: DynamoDB, Cassandra):** Khi bình thường, ưu tiên phản hồi cực nhanh (`L`), ghi xong trả kết quả ngay, đồng bộ dữ liệu ngầm sau. Khi đứt mạng, ưu tiên sẵn sàng (`A`).
* **Hệ thống PC/EC (ví dụ: Bigtable, RDBMS Cluster):** Luôn đặt tính đúng đắn của dữ liệu lên hàng đầu (`C`), chấp nhận chạy chậm hơn một chút (`Latency` cao hơn) để chờ các node đồng bộ xong.

---

# 3. CONSISTENCY MODELS (CÁC MÔ HÌNH NHẤT QUÁN)

Không phải lúc nào ta cũng cần dữ liệu phải đồng bộ 100% ngay tức khắc. Dưới đây là các cấp độ nhất quán từ mạnh đến yếu:

```
[MẠNH NHẤT] ─── Strong Consistency
      │
      ├─── Causal Consistency
      │
      ├─── Read-Your-Writes Consistency
      │
      ├─── Monotonic Reads
      │
[YẾU NHẤT]  ─── Eventual Consistency
```

| Mô hình | Định nghĩa | Ví dụ thực tế |
| :--- | :--- | :--- |
| **1. Strong Consistency** | Ghi xong là **ngay lập tức 100% mọi nơi** đọc ra đều thấy dữ liệu mới nhất. | Rút tiền ATM: Tài khoản còn 1 triệu, rút 1 triệu thì 1 giây sau xem ở đâu cũng phải thấy số dư = 0. |
| **2. Eventual Consistency** | Dữ liệu cập nhật sẽ lan truyền từ từ; các node có thể lệch nhau vài giây, nhưng **cuối cùng sẽ bằng nhau**. | Lượt View/Like video Youtube: Bạn thấy 1.000 view, bạn của bạn thấy 995 view, vài phút sau cả hai đều thấy 1.050 view. |
| **3. Read-Your-Writes** | **Chính người vừa thực hiện thao tác** phải thấy được dữ liệu của mình vừa ghi, dù người khác có thể chưa thấy. | Bạn vừa đổi **Avatar Facebook** $\rightarrow$ Bấm F5 trang cá nhân bạn phải thấy ảnh mới ngay (dù bạn bè của bạn có thể 1 phút sau mới thấy). |
| **4. Monotonic Reads** | Người dùng đọc dữ liệu **không bao giờ bị "tua ngược về quá khứ"**. | Lần 1 xem đơn hàng thấy `CONFIRMED`. Bấm F5 lần 2 tuyệt đối không được giật lùi về trạng thái cũ `PENDING`. |
| **5. Causal Consistency** | Các sự kiện có quan hệ **nguyên nhân - kết quả** phải hiển thị đúng thứ tự logic. | Phải nhìn thấy **Bài đăng (Post)** trước rồi mới nhìn thấy **Bình luận (Comment)** của bài đăng đó. |

---

# 4. DATABASE REPLICATION (SAO CHÉP DỮ LIỆU)

### 💡 Định nghĩa
**Replication** là kỹ thuật sao chép **cùng một bộ dữ liệu giống hệt nhau** từ máy chủ gốc (**Primary / Leader**) sang nhiều máy chủ bản sao (**Replicas / Followers**).

```text
               ┌────────────────┐
               │  Client Write  │
               └───────┬────────┘
                       ▼
               ┌────────────────┐
               │ Primary/Leader │ ──(Ghi dữ liệu)
               └───────┬────────┘
                       │ Replication Log
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
  ┌───────────┐  ┌───────────┐  ┌───────────┐
  │ Replica 1 │  │ Replica 2 │  │ Replica 3 │ ──(Chỉ đọc - Read Replicas)
  └───────────┘  └───────────┘  └───────────┘
        ▲              ▲              ▲
        └──────────────┴──────────────┘
                 Client Reads
```

### 🎯 Điểm mấu chốt cần nhớ:
1. **Read Scaling:** Rất hiệu quả khi tỷ lệ Đọc nhiều hơn Ghi (ví dụ: 10 lượt đọc mới có 1 lượt mua). Ta có thể thêm 5 - 10 Replica để gánh hàng triệu lượt đọc.
2. **KHÔNG giải quyết được nghẽn Ghi (Write Bottleneck):** Vì chỉ có **1 Primary duy nhất** nhận lệnh Ghi. Nếu có 10.000 request Ghi/giây, Primary vẫn bị quá tải!
3. **Replica Lag:** Thời gian để dữ liệu truyền từ Primary sang Replica có thể mất từ vài chục mili-giây đến vài giây.
4. **Failover:** Khi Primary bị chết đột ngột, hệ thống sẽ tự động bình bầu một Replica có dữ liệu mới nhất lên làm Primary mới.

---

# 5. REPLICATION STRATEGIES (CHIẾN LƯỢC SAO CHÉP)

Có 3 cách để Primary đồng bộ dữ liệu sang các Replicas:

| Tiêu chí | **Synchronous (Đồng bộ)** | **Asynchronous (Bất đồng bộ)** | **Semi-synchronous (Bán đồng bộ)** |
| :--- | :--- | :--- | :--- |
| **Cơ chế** | Primary ghi xong $\rightarrow$ Chờ **TẤT CẢ** Replica xác nhận (ACK) $\rightarrow$ Mới báo thành công cho khách. | Primary ghi xong $\rightarrow$ **Báo thành công ngay lập tức** $\rightarrow$ Dữ liệu truyền ngầm sang Replica sau. | Primary chỉ cần chờ **TỐI THIỂU 1 Replica** xác nhận (ACK) là báo thành công ngay. |
| **Tốc độ ghi (Latency)** | **Chậm nhất** (phụ thuộc vào node mạng chậm nhất). | **Nhanh nhất** (không phải chờ ai). | **Cân bằng** (rất nhanh). |
| **Nguy cơ mất dữ liệu** | **Bằng 0 (Zero Data Loss):** Đảm bảo an toàn tuyệt đối. | **Có rủi ro:** Nếu Primary chết khi dữ liệu chưa kịp gửi sang Replica $\rightarrow$ Mất dữ liệu! | **Gần như bằng 0:** Vì luôn có ít nhất 1 Replica giữ bản copy. |
| **Use case phù hợp** | Giao dịch tài chính, thanh toán cốt lõi. | Ghi log hệ thống, lượt xem video, telemetry. | Hệ thống E-commerce hiện đại, MySQL/Postgres HA. |

---

# 6. DATABASE SHARDING (PHÂN MẢNH CƠ SỞ DỮ LIỆU)

### 💡 So sánh sự khác nhau giữa Replication và Sharding

* **Replication (Nhân bản):** Mọi máy chủ đều giữ **100% dữ liệu**. Mục tiêu là chịu lỗi và tăng tốc độ Đọc.
* **Sharding (Cắt nhỏ / Phân mảnh):** Dữ liệu bị **cắt thành nhiều phần nhỏ (Shards)** theo chiều ngang (*Horizontal Partitioning*). Mỗi máy chủ chỉ giữ một phần dữ liệu.

```text
[ REPLICATION ]                             [ SHARDING ]
Mỗi máy giữ TOÀN BỘ dữ liệu                 Mỗi máy giữ MỘT PHẦN dữ liệu

┌───────────────┐                           ┌───────────────┐
│ Primary       │ Orders 1..100M            │ Shard 1       │ Customer 1..1M
└───────────────┘                           └───────────────┘
┌───────────────┐                           ┌───────────────┐
│ Replica 1     │ Orders 1..100M            │ Shard 2       │ Customer 1M..2M
└───────────────┘                           └───────────────┘
┌───────────────┐                           ┌───────────────┐
│ Replica 2     │ Orders 1..100M            │ Shard 3       │ Customer 2M..3M
└───────────────┘                           └───────────────┘
```

### 🎯 Khi nào bắt buộc phải dùng Sharding?
Khi dữ liệu vượt quá giới hạn vật lý của 1 máy chủ:
* **Storage Bottleneck:** Database phình to hàng trăm Terabytes, không ổ cứng nào chứa nổi.
* **Write Bottleneck:** Hàng trăm nghìn lượt Ghi mỗi giây, 1 CPU/RAM của Primary không tài nào xử lý kịp. Sharding giúp chia đều tải ghi ra 10 - 20 máy chủ.

### 🔑 Shard Key là gì?
Là **cột dữ liệu** được chọn để thuật toán quyết định dòng dữ liệu đó sẽ được lưu vào Shard nào (ví dụ: `customerId`, `orderId`, `countryCode`).
* *Tiêu chí chọn Shard Key chuẩn:* Phân phối dữ liệu đồng đều, phân phối tải đồng đều, và phù hợp với các câu query thường xuyên nhất.

---

# 7. SHARDING STRATEGIES & CHALLENGES (CHIẾN LƯỢC VÀ THÁCH THỨC)

### 1. Ba chiến lược chia Shard phổ biến
1. **Hash-Based Sharding:** Dùng hàm băm: `shardId = hash(customerId) % totalShards`.
   - *Ưu điểm:* Dữ liệu rải cực kỳ đều giữa các máy.
   - *Nhược điểm:* Không thể truy vấn theo khoảng (`WHERE age BETWEEN 20 AND 30` phải quét tất cả các Shard).
2. **Range-Based Sharding:** Chia theo dải giá trị (ví dụ ID từ 1 - 1.000.000 vào Shard 1; 1.000.001 - 2.000.000 vào Shard 2).
   - *Ưu điểm:* Dễ hiểu, query theo khoảng cực nhanh.
   - *Nhược điểm:* Dễ bị **Hot Spot** (người dùng mới đăng ký luôn có ID lớn $\rightarrow$ Toàn bộ lượt ghi mới dồn hết vào Shard cuối cùng).
3. **Directory-Based Sharding:** Dùng 1 bảng tra cứu riêng (*Lookup Service*) để tra xem ID này nằm ở Shard nào.
   - *Ưu điểm:* Rất linh hoạt, muốn chuyển khách hàng VIP sang server xịn lúc nào cũng được.
   - *Nhược điểm:* Bảng tra cứu trở thành điểm nghẽn cổ chai.

### 2. Bốn thách thức "ác mộng" khi làm Sharding
* **🔥 Hot Spot / Hot Shard:** Một Shard bị quá tải traffic trong khi các Shard khác ngồi chơi (ví dụ: Shard chứa tài khoản của Sơn Tùng M-TP hoặc một công ty lớn trong hệ thống SaaS).
* **🌐 Cross-Shard Query:** Cần dữ liệu từ nhiều Shard khác nhau (ví dụ: Tìm đơn hàng theo ngày). Hệ thống phải gửi request đến tất cả các Shard rồi gộp kết quả lại $\rightarrow$ Cực kỳ chậm!
* **💳 Cross-Shard Transaction:** Chuyển tiền từ User ở Shard 1 sang User ở Shard 2 $\rightarrow$ Không thể dùng `@Transactional` của database thông thường, bắt buộc phải dùng **Saga Pattern** hoặc **2PC**.
* **🚚 Re-sharding (Tăng thêm Shard):** Khi dữ liệu tăng, muốn nâng từ 4 Shard lên 8 Shard thì phải di chuyển hàng Terabyte dữ liệu giữa các máy chủ mà không được làm sập hệ thống (Zero-downtime migration).

---

# 📝 BẢNG LIÊN HỆ ĐẾN CÁC BÀI ASSIGNMENT TUẦN 2

| Bài tập | Nội dung áp dụng | Câu hỏi mấu chốt cần giải quyết |
| :--- | :--- | :--- |
| **[Assignment 1](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week2/Assignment1.md)** | **CAP & PACELC** | Sự cố khách bị trừ tiền 2 lần do hệ thống chọn **AP** cho dữ liệu Payment (cho phép ghi vào Replica Tokyo khi đứt mạng với Singapore). Cần chuyển sang **CP** và áp dụng `Idempotency-Key`. |
| **Assignment 2** | **Consistency Models** | Xử lý tình huống "Vừa thanh toán xong F5 thấy trạng thái cũ PENDING" (*Read-after-write inconsistency*). |
| **Assignment 3** | **Replication & Failover** | Thiết kế định tuyến Read/Write và quy trình 5 bước Failover khi Primary bị chết. |
| **Assignment 4** | **Sharding & Hot Spot** | Chọn Shard Key cho hệ thống SaaS 5 triệu Tenants và giải quyết vấn đề khách hàng lớn làm nghẽn Shard (*Hot Tenant*). |

Bạn có muốn bắt đầu bằng việc giải chi tiết và viết câu trả lời cho **[Assignment 1](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week2/Assignment1.md)** vào file `.md` không?Listed directory Week2
Viewed 1_CAP_Theorem.md:1-75
Viewed 2_PACELC_Theorem.md:1-65
Viewed 3_Consistency_Models.md:1-93
Viewed 4_Database_Replication.md:1-75
Viewed 5_Replication%20Strategies.md:1-91
Viewed 6_Database_Sharding.md:1-90
Viewed 7_Sharding_Strategies_Challenges.md:1-115

Dưới đây là bản **tổng hợp toàn bộ kiến thức cốt lõi của Week 2** (Kiến trúc Hệ thống phân tán & Cơ sở dữ liệu quy mô lớn), được biên soạn súc tích, trực quan kèm theo các ví dụ thực tế giúp bạn nắm bắt nhanh nhất.

---

# 🗺️ TỔNG QUAN CHƯƠNG TRÌNH TUẦN 2
Tuần 2 giải quyết câu hỏi lớn nhất của kỹ sư phần mềm khi hệ thống mở rộng:  
👉 **"Làm sao để lưu trữ và xử lý hàng chục triệu người dùng mà dữ liệu không bị mất, không bị sai lệch và hệ thống không bị sập?"**

Chương trình gồm **7 chủ đề chính** đi từ lý thuyết nền tảng đến kỹ thuật thực chiến:
1. **CAP Theorem** (Quy luật đánh đổi trong hệ thống phân tán).
2. **PACELC Theorem** (Mở rộng của CAP trong trạng thái bình thường).
3. **Consistency Models** (Các mức độ nhất quán dữ liệu).
4. **Database Replication** (Nhân bản cơ sở dữ liệu - Primary/Replica).
5. **Replication Strategies** (Đồng bộ, Bán đồng bộ và Bất đồng bộ).
6. **Database Sharding** (Phân mảnh dữ liệu theo chiều ngang).
7. **Sharding Strategies & Challenges** (Kỹ thuật chia Shard và các cạm bẫy).

---

# 1. CAP THEOREM (ĐỊNH LÝ CAP)

### 💡 Định nghĩa ngắn gọn
Trong một hệ thống phân tán gồm nhiều node máy tính giao tiếp qua mạng, bạn **chỉ có thể chọn tối đa 2 trong 3 yếu tố**:

* **C - Consistency (Nhất quán):** Mọi node đều thấy cùng một dữ liệu mới nhất tại cùng một thời điểm. Sau khi ghi thành công, ai đọc cũng phải thấy dữ liệu mới đó, nếu không thì phải báo lỗi chứ không được trả dữ liệu cũ.
* **A - Availability (Sẵn sàng):** Mọi request gửi đến hệ thống (nếu node còn sống) đều nhận được câu trả lời thành công (không bị lỗi 500 hay timeout), dù dữ liệu trả về có thể là dữ liệu cũ (*stale data*).
* **P - Partition Tolerance (Chịu lỗi đứt mạng):** Hệ thống vẫn tiếp tục hoạt động ngay cả khi đường truyền mạng giữa các node bị đứt (*network partition*).

```
          [C] Consistency
             /   \
            /     \
           /  CAP  \
          /         \
[A] Availability ── [P] Partition Tolerance
```

### ⚠️ Bản chất cốt lõi: Không phải chọn tùy ý 2/3!
Trong thực tế, đường truyền mạng giữa các máy chủ **chắc chắn sẽ có lúc bị chập chờn hoặc đứt đoạn**. Vì vậy, **`P` là bắt buộc phải có**.  
👉 Khi xảy ra sự cố đứt mạng (Network Partition), sự lựa chọn thực tế chỉ là: **Đánh đổi giữa C hay A?**

| Lựa chọn | Cơ chế xử lý | Khi nào dùng? | Ví dụ thực tế |
| :---: | :--- | :--- | :--- |
| **CP System** | Ưu tiên **C** $\rightarrow$ **Thà từ chối phục vụ (hy sinh A) chứ không để dữ liệu bị sai lệch**. | Tiền bạc, tài chính, thanh toán, kho hàng. | **Chuyển tiền ngân hàng:** Nếu 2 server mất kết nối, hệ thống báo *"Dịch vụ tạm gián đoạn"* chứ tuyệt đối không cho chuyển tiền vì dễ bị trừ tiền 2 lần. |
| **AP System** | Ưu tiên **A** $\rightarrow$ **Vẫn trả lời người dùng (hy sinh C), chấp nhận dữ liệu có thể bị trễ/cũ**. | Mạng xã hội, tin tức, giỏ hàng, xem video. | **Facebook / TikTok / YouTube:** Mất mạng giữa các server thì bạn vẫn lướt xem được video và bài post (dù có thể là bài đăng từ 5 phút trước). |

---

# 2. PACELC THEOREM (ĐỊNH LÝ PACELC)

### 💡 Tại sao cần PACELC khi đã có CAP?
Định lý **CAP chỉ nói về lúc mạng bị đứt (Partition)**. Nhưng trong 99.9% thời gian bình thường mạng hoạt động tốt, hệ thống phân tán vẫn phải đối mặt với một sự đánh đổi khác: **Tốc độ (Latency) hay Độ chính xác (Consistency)?**

### 📐 Công thức PACELC
```text
If (P) Partition:
    Trade-off: (A) Availability  vs  (C) Consistency
Else (E):
    Trade-off: (L) Latency       vs  (C) Consistency
```

* **Nếu có Partition (`P`):** Chọn giữa **`A`** (Sẵn sàng) hay **`C`** (Nhất quán).
* **Ngược lại (`E` - Else, khi mạng bình thường):** Chọn giữa **`L`** (Độ trễ thấp / Phản hồi nhanh) hay **`C`** (Dữ liệu nhất quán tuyệt đối).

### 🎯 Ví dụ thực tế
* **Hệ thống PA/EL (ví dụ: DynamoDB, Cassandra):** Khi bình thường, ưu tiên phản hồi cực nhanh (`L`), ghi xong trả kết quả ngay, đồng bộ dữ liệu ngầm sau. Khi đứt mạng, ưu tiên sẵn sàng (`A`).
* **Hệ thống PC/EC (ví dụ: Bigtable, RDBMS Cluster):** Luôn đặt tính đúng đắn của dữ liệu lên hàng đầu (`C`), chấp nhận chạy chậm hơn một chút (`Latency` cao hơn) để chờ các node đồng bộ xong.

---

# 3. CONSISTENCY MODELS (CÁC MÔ HÌNH NHẤT QUÁN)

Không phải lúc nào ta cũng cần dữ liệu phải đồng bộ 100% ngay tức khắc. Dưới đây là các cấp độ nhất quán từ mạnh đến yếu:

```
[MẠNH NHẤT] ─── Strong Consistency
      │
      ├─── Causal Consistency
      │
      ├─── Read-Your-Writes Consistency
      │
      ├─── Monotonic Reads
      │
[YẾU NHẤT]  ─── Eventual Consistency
```

| Mô hình | Định nghĩa | Ví dụ thực tế |
| :--- | :--- | :--- |
| **1. Strong Consistency** | Ghi xong là **ngay lập tức 100% mọi nơi** đọc ra đều thấy dữ liệu mới nhất. | Rút tiền ATM: Tài khoản còn 1 triệu, rút 1 triệu thì 1 giây sau xem ở đâu cũng phải thấy số dư = 0. |
| **2. Eventual Consistency** | Dữ liệu cập nhật sẽ lan truyền từ từ; các node có thể lệch nhau vài giây, nhưng **cuối cùng sẽ bằng nhau**. | Lượt View/Like video Youtube: Bạn thấy 1.000 view, bạn của bạn thấy 995 view, vài phút sau cả hai đều thấy 1.050 view. |
| **3. Read-Your-Writes** | **Chính người vừa thực hiện thao tác** phải thấy được dữ liệu của mình vừa ghi, dù người khác có thể chưa thấy. | Bạn vừa đổi **Avatar Facebook** $\rightarrow$ Bấm F5 trang cá nhân bạn phải thấy ảnh mới ngay (dù bạn bè của bạn có thể 1 phút sau mới thấy). |
| **4. Monotonic Reads** | Người dùng đọc dữ liệu **không bao giờ bị "tua ngược về quá khứ"**. | Lần 1 xem đơn hàng thấy `CONFIRMED`. Bấm F5 lần 2 tuyệt đối không được giật lùi về trạng thái cũ `PENDING`. |
| **5. Causal Consistency** | Các sự kiện có quan hệ **nguyên nhân - kết quả** phải hiển thị đúng thứ tự logic. | Phải nhìn thấy **Bài đăng (Post)** trước rồi mới nhìn thấy **Bình luận (Comment)** của bài đăng đó. |

---

# 4. DATABASE REPLICATION (SAO CHÉP DỮ LIỆU)

### 💡 Định nghĩa
**Replication** là kỹ thuật sao chép **cùng một bộ dữ liệu giống hệt nhau** từ máy chủ gốc (**Primary / Leader**) sang nhiều máy chủ bản sao (**Replicas / Followers**).

```text
               ┌────────────────┐
               │  Client Write  │
               └───────┬────────┘
                       ▼
               ┌────────────────┐
               │ Primary/Leader │ ──(Ghi dữ liệu)
               └───────┬────────┘
                       │ Replication Log
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
  ┌───────────┐  ┌───────────┐  ┌───────────┐
  │ Replica 1 │  │ Replica 2 │  │ Replica 3 │ ──(Chỉ đọc - Read Replicas)
  └───────────┘  └───────────┘  └───────────┘
        ▲              ▲              ▲
        └──────────────┴──────────────┘
                 Client Reads
```

### 🎯 Điểm mấu chốt cần nhớ:
1. **Read Scaling:** Rất hiệu quả khi tỷ lệ Đọc nhiều hơn Ghi (ví dụ: 10 lượt đọc mới có 1 lượt mua). Ta có thể thêm 5 - 10 Replica để gánh hàng triệu lượt đọc.
2. **KHÔNG giải quyết được nghẽn Ghi (Write Bottleneck):** Vì chỉ có **1 Primary duy nhất** nhận lệnh Ghi. Nếu có 10.000 request Ghi/giây, Primary vẫn bị quá tải!
3. **Replica Lag:** Thời gian để dữ liệu truyền từ Primary sang Replica có thể mất từ vài chục mili-giây đến vài giây.
4. **Failover:** Khi Primary bị chết đột ngột, hệ thống sẽ tự động bình bầu một Replica có dữ liệu mới nhất lên làm Primary mới.

---

# 5. REPLICATION STRATEGIES (CHIẾN LƯỢC SAO CHÉP)

Có 3 cách để Primary đồng bộ dữ liệu sang các Replicas:

| Tiêu chí | **Synchronous (Đồng bộ)** | **Asynchronous (Bất đồng bộ)** | **Semi-synchronous (Bán đồng bộ)** |
| :--- | :--- | :--- | :--- |
| **Cơ chế** | Primary ghi xong $\rightarrow$ Chờ **TẤT CẢ** Replica xác nhận (ACK) $\rightarrow$ Mới báo thành công cho khách. | Primary ghi xong $\rightarrow$ **Báo thành công ngay lập tức** $\rightarrow$ Dữ liệu truyền ngầm sang Replica sau. | Primary chỉ cần chờ **TỐI THIỂU 1 Replica** xác nhận (ACK) là báo thành công ngay. |
| **Tốc độ ghi (Latency)** | **Chậm nhất** (phụ thuộc vào node mạng chậm nhất). | **Nhanh nhất** (không phải chờ ai). | **Cân bằng** (rất nhanh). |
| **Nguy cơ mất dữ liệu** | **Bằng 0 (Zero Data Loss):** Đảm bảo an toàn tuyệt đối. | **Có rủi ro:** Nếu Primary chết khi dữ liệu chưa kịp gửi sang Replica $\rightarrow$ Mất dữ liệu! | **Gần như bằng 0:** Vì luôn có ít nhất 1 Replica giữ bản copy. |
| **Use case phù hợp** | Giao dịch tài chính, thanh toán cốt lõi. | Ghi log hệ thống, lượt xem video, telemetry. | Hệ thống E-commerce hiện đại, MySQL/Postgres HA. |

---

# 6. DATABASE SHARDING (PHÂN MẢNH CƠ SỞ DỮ LIỆU)

### 💡 So sánh sự khác nhau giữa Replication và Sharding

* **Replication (Nhân bản):** Mọi máy chủ đều giữ **100% dữ liệu**. Mục tiêu là chịu lỗi và tăng tốc độ Đọc.
* **Sharding (Cắt nhỏ / Phân mảnh):** Dữ liệu bị **cắt thành nhiều phần nhỏ (Shards)** theo chiều ngang (*Horizontal Partitioning*). Mỗi máy chủ chỉ giữ một phần dữ liệu.

```text
[ REPLICATION ]                             [ SHARDING ]
Mỗi máy giữ TOÀN BỘ dữ liệu                 Mỗi máy giữ MỘT PHẦN dữ liệu

┌───────────────┐                           ┌───────────────┐
│ Primary       │ Orders 1..100M            │ Shard 1       │ Customer 1..1M
└───────────────┘                           └───────────────┘
┌───────────────┐                           ┌───────────────┐
│ Replica 1     │ Orders 1..100M            │ Shard 2       │ Customer 1M..2M
└───────────────┘                           └───────────────┘
┌───────────────┐                           ┌───────────────┐
│ Replica 2     │ Orders 1..100M            │ Shard 3       │ Customer 2M..3M
└───────────────┘                           └───────────────┘
```

### 🎯 Khi nào bắt buộc phải dùng Sharding?
Khi dữ liệu vượt quá giới hạn vật lý của 1 máy chủ:
* **Storage Bottleneck:** Database phình to hàng trăm Terabytes, không ổ cứng nào chứa nổi.
* **Write Bottleneck:** Hàng trăm nghìn lượt Ghi mỗi giây, 1 CPU/RAM của Primary không tài nào xử lý kịp. Sharding giúp chia đều tải ghi ra 10 - 20 máy chủ.

### 🔑 Shard Key là gì?
Là **cột dữ liệu** được chọn để thuật toán quyết định dòng dữ liệu đó sẽ được lưu vào Shard nào (ví dụ: `customerId`, `orderId`, `countryCode`).
* *Tiêu chí chọn Shard Key chuẩn:* Phân phối dữ liệu đồng đều, phân phối tải đồng đều, và phù hợp với các câu query thường xuyên nhất.

---

# 7. SHARDING STRATEGIES & CHALLENGES (CHIẾN LƯỢC VÀ THÁCH THỨC)

### 1. Ba chiến lược chia Shard phổ biến
1. **Hash-Based Sharding:** Dùng hàm băm: `shardId = hash(customerId) % totalShards`.
   - *Ưu điểm:* Dữ liệu rải cực kỳ đều giữa các máy.
   - *Nhược điểm:* Không thể truy vấn theo khoảng (`WHERE age BETWEEN 20 AND 30` phải quét tất cả các Shard).
2. **Range-Based Sharding:** Chia theo dải giá trị (ví dụ ID từ 1 - 1.000.000 vào Shard 1; 1.000.001 - 2.000.000 vào Shard 2).
   - *Ưu điểm:* Dễ hiểu, query theo khoảng cực nhanh.
   - *Nhược điểm:* Dễ bị **Hot Spot** (người dùng mới đăng ký luôn có ID lớn $\rightarrow$ Toàn bộ lượt ghi mới dồn hết vào Shard cuối cùng).
3. **Directory-Based Sharding:** Dùng 1 bảng tra cứu riêng (*Lookup Service*) để tra xem ID này nằm ở Shard nào.
   - *Ưu điểm:* Rất linh hoạt, muốn chuyển khách hàng VIP sang server xịn lúc nào cũng được.
   - *Nhược điểm:* Bảng tra cứu trở thành điểm nghẽn cổ chai.

### 2. Bốn thách thức "ác mộng" khi làm Sharding
* **🔥 Hot Spot / Hot Shard:** Một Shard bị quá tải traffic trong khi các Shard khác ngồi chơi (ví dụ: Shard chứa tài khoản của Sơn Tùng M-TP hoặc một công ty lớn trong hệ thống SaaS).
* **🌐 Cross-Shard Query:** Cần dữ liệu từ nhiều Shard khác nhau (ví dụ: Tìm đơn hàng theo ngày). Hệ thống phải gửi request đến tất cả các Shard rồi gộp kết quả lại $\rightarrow$ Cực kỳ chậm!
* **💳 Cross-Shard Transaction:** Chuyển tiền từ User ở Shard 1 sang User ở Shard 2 $\rightarrow$ Không thể dùng `@Transactional` của database thông thường, bắt buộc phải dùng **Saga Pattern** hoặc **2PC**.
* **🚚 Re-sharding (Tăng thêm Shard):** Khi dữ liệu tăng, muốn nâng từ 4 Shard lên 8 Shard thì phải di chuyển hàng Terabyte dữ liệu giữa các máy chủ mà không được làm sập hệ thống (Zero-downtime migration).

---

# 📝 BẢNG LIÊN HỆ ĐẾN CÁC BÀI ASSIGNMENT TUẦN 2

| Bài tập | Nội dung áp dụng | Câu hỏi mấu chốt cần giải quyết |
| :--- | :--- | :--- |
| **[Assignment 1](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week2/Assignment1.md)** | **CAP & PACELC** | Sự cố khách bị trừ tiền 2 lần do hệ thống chọn **AP** cho dữ liệu Payment (cho phép ghi vào Replica Tokyo khi đứt mạng với Singapore). Cần chuyển sang **CP** và áp dụng `Idempotency-Key`. |
| **Assignment 2** | **Consistency Models** | Xử lý tình huống "Vừa thanh toán xong F5 thấy trạng thái cũ PENDING" (*Read-after-write inconsistency*). |
| **Assignment 3** | **Replication & Failover** | Thiết kế định tuyến Read/Write và quy trình 5 bước Failover khi Primary bị chết. |
| **Assignment 4** | **Sharding & Hot Spot** | Chọn Shard Key cho hệ thống SaaS 5 triệu Tenants và giải quyết vấn đề khách hàng lớn làm nghẽn Shard (*Hot Tenant*). |

Bạn có muốn bắt đầu bằng việc giải chi tiết và viết câu trả lời cho **[Assignment 1](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week2/Assignment1.md)** vào file `.md` không?Listed directory Week2
Viewed 1_CAP_Theorem.md:1-75
Viewed 2_PACELC_Theorem.md:1-65
Viewed 3_Consistency_Models.md:1-93
Viewed 4_Database_Replication.md:1-75
Viewed 5_Replication%20Strategies.md:1-91
Viewed 6_Database_Sharding.md:1-90
Viewed 7_Sharding_Strategies_Challenges.md:1-115

Dưới đây là bản **tổng hợp toàn bộ kiến thức cốt lõi của Week 2** (Kiến trúc Hệ thống phân tán & Cơ sở dữ liệu quy mô lớn), được biên soạn súc tích, trực quan kèm theo các ví dụ thực tế giúp bạn nắm bắt nhanh nhất.

---

# 🗺️ TỔNG QUAN CHƯƠNG TRÌNH TUẦN 2
Tuần 2 giải quyết câu hỏi lớn nhất của kỹ sư phần mềm khi hệ thống mở rộng:  
👉 **"Làm sao để lưu trữ và xử lý hàng chục triệu người dùng mà dữ liệu không bị mất, không bị sai lệch và hệ thống không bị sập?"**

Chương trình gồm **7 chủ đề chính** đi từ lý thuyết nền tảng đến kỹ thuật thực chiến:
1. **CAP Theorem** (Quy luật đánh đổi trong hệ thống phân tán).
2. **PACELC Theorem** (Mở rộng của CAP trong trạng thái bình thường).
3. **Consistency Models** (Các mức độ nhất quán dữ liệu).
4. **Database Replication** (Nhân bản cơ sở dữ liệu - Primary/Replica).
5. **Replication Strategies** (Đồng bộ, Bán đồng bộ và Bất đồng bộ).
6. **Database Sharding** (Phân mảnh dữ liệu theo chiều ngang).
7. **Sharding Strategies & Challenges** (Kỹ thuật chia Shard và các cạm bẫy).

---

# 1. CAP THEOREM (ĐỊNH LÝ CAP)

### 💡 Định nghĩa ngắn gọn
Trong một hệ thống phân tán gồm nhiều node máy tính giao tiếp qua mạng, bạn **chỉ có thể chọn tối đa 2 trong 3 yếu tố**:

* **C - Consistency (Nhất quán):** Mọi node đều thấy cùng một dữ liệu mới nhất tại cùng một thời điểm. Sau khi ghi thành công, ai đọc cũng phải thấy dữ liệu mới đó, nếu không thì phải báo lỗi chứ không được trả dữ liệu cũ.
* **A - Availability (Sẵn sàng):** Mọi request gửi đến hệ thống (nếu node còn sống) đều nhận được câu trả lời thành công (không bị lỗi 500 hay timeout), dù dữ liệu trả về có thể là dữ liệu cũ (*stale data*).
* **P - Partition Tolerance (Chịu lỗi đứt mạng):** Hệ thống vẫn tiếp tục hoạt động ngay cả khi đường truyền mạng giữa các node bị đứt (*network partition*).

```
          [C] Consistency
             /   \
            /     \
           /  CAP  \
          /         \
[A] Availability ── [P] Partition Tolerance
```

### ⚠️ Bản chất cốt lõi: Không phải chọn tùy ý 2/3!
Trong thực tế, đường truyền mạng giữa các máy chủ **chắc chắn sẽ có lúc bị chập chờn hoặc đứt đoạn**. Vì vậy, **`P` là bắt buộc phải có**.  
👉 Khi xảy ra sự cố đứt mạng (Network Partition), sự lựa chọn thực tế chỉ là: **Đánh đổi giữa C hay A?**

| Lựa chọn | Cơ chế xử lý | Khi nào dùng? | Ví dụ thực tế |
| :---: | :--- | :--- | :--- |
| **CP System** | Ưu tiên **C** $\rightarrow$ **Thà từ chối phục vụ (hy sinh A) chứ không để dữ liệu bị sai lệch**. | Tiền bạc, tài chính, thanh toán, kho hàng. | **Chuyển tiền ngân hàng:** Nếu 2 server mất kết nối, hệ thống báo *"Dịch vụ tạm gián đoạn"* chứ tuyệt đối không cho chuyển tiền vì dễ bị trừ tiền 2 lần. |
| **AP System** | Ưu tiên **A** $\rightarrow$ **Vẫn trả lời người dùng (hy sinh C), chấp nhận dữ liệu có thể bị trễ/cũ**. | Mạng xã hội, tin tức, giỏ hàng, xem video. | **Facebook / TikTok / YouTube:** Mất mạng giữa các server thì bạn vẫn lướt xem được video và bài post (dù có thể là bài đăng từ 5 phút trước). |

---

# 2. PACELC THEOREM (ĐỊNH LÝ PACELC)

### 💡 Tại sao cần PACELC khi đã có CAP?
Định lý **CAP chỉ nói về lúc mạng bị đứt (Partition)**. Nhưng trong 99.9% thời gian bình thường mạng hoạt động tốt, hệ thống phân tán vẫn phải đối mặt với một sự đánh đổi khác: **Tốc độ (Latency) hay Độ chính xác (Consistency)?**

### 📐 Công thức PACELC
```text
If (P) Partition:
    Trade-off: (A) Availability  vs  (C) Consistency
Else (E):
    Trade-off: (L) Latency       vs  (C) Consistency
```

* **Nếu có Partition (`P`):** Chọn giữa **`A`** (Sẵn sàng) hay **`C`** (Nhất quán).
* **Ngược lại (`E` - Else, khi mạng bình thường):** Chọn giữa **`L`** (Độ trễ thấp / Phản hồi nhanh) hay **`C`** (Dữ liệu nhất quán tuyệt đối).

### 🎯 Ví dụ thực tế
* **Hệ thống PA/EL (ví dụ: DynamoDB, Cassandra):** Khi bình thường, ưu tiên phản hồi cực nhanh (`L`), ghi xong trả kết quả ngay, đồng bộ dữ liệu ngầm sau. Khi đứt mạng, ưu tiên sẵn sàng (`A`).
* **Hệ thống PC/EC (ví dụ: Bigtable, RDBMS Cluster):** Luôn đặt tính đúng đắn của dữ liệu lên hàng đầu (`C`), chấp nhận chạy chậm hơn một chút (`Latency` cao hơn) để chờ các node đồng bộ xong.

---

# 3. CONSISTENCY MODELS (CÁC MÔ HÌNH NHẤT QUÁN)

Không phải lúc nào ta cũng cần dữ liệu phải đồng bộ 100% ngay tức khắc. Dưới đây là các cấp độ nhất quán từ mạnh đến yếu:

```
[MẠNH NHẤT] ─── Strong Consistency
      │
      ├─── Causal Consistency
      │
      ├─── Read-Your-Writes Consistency
      │
      ├─── Monotonic Reads
      │
[YẾU NHẤT]  ─── Eventual Consistency
```

| Mô hình | Định nghĩa | Ví dụ thực tế |
| :--- | :--- | :--- |
| **1. Strong Consistency** | Ghi xong là **ngay lập tức 100% mọi nơi** đọc ra đều thấy dữ liệu mới nhất. | Rút tiền ATM: Tài khoản còn 1 triệu, rút 1 triệu thì 1 giây sau xem ở đâu cũng phải thấy số dư = 0. |
| **2. Eventual Consistency** | Dữ liệu cập nhật sẽ lan truyền từ từ; các node có thể lệch nhau vài giây, nhưng **cuối cùng sẽ bằng nhau**. | Lượt View/Like video Youtube: Bạn thấy 1.000 view, bạn của bạn thấy 995 view, vài phút sau cả hai đều thấy 1.050 view. |
| **3. Read-Your-Writes** | **Chính người vừa thực hiện thao tác** phải thấy được dữ liệu của mình vừa ghi, dù người khác có thể chưa thấy. | Bạn vừa đổi **Avatar Facebook** $\rightarrow$ Bấm F5 trang cá nhân bạn phải thấy ảnh mới ngay (dù bạn bè của bạn có thể 1 phút sau mới thấy). |
| **4. Monotonic Reads** | Người dùng đọc dữ liệu **không bao giờ bị "tua ngược về quá khứ"**. | Lần 1 xem đơn hàng thấy `CONFIRMED`. Bấm F5 lần 2 tuyệt đối không được giật lùi về trạng thái cũ `PENDING`. |
| **5. Causal Consistency** | Các sự kiện có quan hệ **nguyên nhân - kết quả** phải hiển thị đúng thứ tự logic. | Phải nhìn thấy **Bài đăng (Post)** trước rồi mới nhìn thấy **Bình luận (Comment)** của bài đăng đó. |

---

# 4. DATABASE REPLICATION (SAO CHÉP DỮ LIỆU)

### 💡 Định nghĩa
**Replication** là kỹ thuật sao chép **cùng một bộ dữ liệu giống hệt nhau** từ máy chủ gốc (**Primary / Leader**) sang nhiều máy chủ bản sao (**Replicas / Followers**).

```text
               ┌────────────────┐
               │  Client Write  │
               └───────┬────────┘
                       ▼
               ┌────────────────┐
               │ Primary/Leader │ ──(Ghi dữ liệu)
               └───────┬────────┘
                       │ Replication Log
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
  ┌───────────┐  ┌───────────┐  ┌───────────┐
  │ Replica 1 │  │ Replica 2 │  │ Replica 3 │ ──(Chỉ đọc - Read Replicas)
  └───────────┘  └───────────┘  └───────────┘
        ▲              ▲              ▲
        └──────────────┴──────────────┘
                 Client Reads
```

### 🎯 Điểm mấu chốt cần nhớ:
1. **Read Scaling:** Rất hiệu quả khi tỷ lệ Đọc nhiều hơn Ghi (ví dụ: 10 lượt đọc mới có 1 lượt mua). Ta có thể thêm 5 - 10 Replica để gánh hàng triệu lượt đọc.
2. **KHÔNG giải quyết được nghẽn Ghi (Write Bottleneck):** Vì chỉ có **1 Primary duy nhất** nhận lệnh Ghi. Nếu có 10.000 request Ghi/giây, Primary vẫn bị quá tải!
3. **Replica Lag:** Thời gian để dữ liệu truyền từ Primary sang Replica có thể mất từ vài chục mili-giây đến vài giây.
4. **Failover:** Khi Primary bị chết đột ngột, hệ thống sẽ tự động bình bầu một Replica có dữ liệu mới nhất lên làm Primary mới.

---

# 5. REPLICATION STRATEGIES (CHIẾN LƯỢC SAO CHÉP)

Có 3 cách để Primary đồng bộ dữ liệu sang các Replicas:

| Tiêu chí | **Synchronous (Đồng bộ)** | **Asynchronous (Bất đồng bộ)** | **Semi-synchronous (Bán đồng bộ)** |
| :--- | :--- | :--- | :--- |
| **Cơ chế** | Primary ghi xong $\rightarrow$ Chờ **TẤT CẢ** Replica xác nhận (ACK) $\rightarrow$ Mới báo thành công cho khách. | Primary ghi xong $\rightarrow$ **Báo thành công ngay lập tức** $\rightarrow$ Dữ liệu truyền ngầm sang Replica sau. | Primary chỉ cần chờ **TỐI THIỂU 1 Replica** xác nhận (ACK) là báo thành công ngay. |
| **Tốc độ ghi (Latency)** | **Chậm nhất** (phụ thuộc vào node mạng chậm nhất). | **Nhanh nhất** (không phải chờ ai). | **Cân bằng** (rất nhanh). |
| **Nguy cơ mất dữ liệu** | **Bằng 0 (Zero Data Loss):** Đảm bảo an toàn tuyệt đối. | **Có rủi ro:** Nếu Primary chết khi dữ liệu chưa kịp gửi sang Replica $\rightarrow$ Mất dữ liệu! | **Gần như bằng 0:** Vì luôn có ít nhất 1 Replica giữ bản copy. |
| **Use case phù hợp** | Giao dịch tài chính, thanh toán cốt lõi. | Ghi log hệ thống, lượt xem video, telemetry. | Hệ thống E-commerce hiện đại, MySQL/Postgres HA. |

---

# 6. DATABASE SHARDING (PHÂN MẢNH CƠ SỞ DỮ LIỆU)

### 💡 So sánh sự khác nhau giữa Replication và Sharding

* **Replication (Nhân bản):** Mọi máy chủ đều giữ **100% dữ liệu**. Mục tiêu là chịu lỗi và tăng tốc độ Đọc.
* **Sharding (Cắt nhỏ / Phân mảnh):** Dữ liệu bị **cắt thành nhiều phần nhỏ (Shards)** theo chiều ngang (*Horizontal Partitioning*). Mỗi máy chủ chỉ giữ một phần dữ liệu.

```text
[ REPLICATION ]                             [ SHARDING ]
Mỗi máy giữ TOÀN BỘ dữ liệu                 Mỗi máy giữ MỘT PHẦN dữ liệu

┌───────────────┐                           ┌───────────────┐
│ Primary       │ Orders 1..100M            │ Shard 1       │ Customer 1..1M
└───────────────┘                           └───────────────┘
┌───────────────┐                           ┌───────────────┐
│ Replica 1     │ Orders 1..100M            │ Shard 2       │ Customer 1M..2M
└───────────────┘                           └───────────────┘
┌───────────────┐                           ┌───────────────┐
│ Replica 2     │ Orders 1..100M            │ Shard 3       │ Customer 2M..3M
└───────────────┘                           └───────────────┘
```

### 🎯 Khi nào bắt buộc phải dùng Sharding?
Khi dữ liệu vượt quá giới hạn vật lý của 1 máy chủ:
* **Storage Bottleneck:** Database phình to hàng trăm Terabytes, không ổ cứng nào chứa nổi.
* **Write Bottleneck:** Hàng trăm nghìn lượt Ghi mỗi giây, 1 CPU/RAM của Primary không tài nào xử lý kịp. Sharding giúp chia đều tải ghi ra 10 - 20 máy chủ.

### 🔑 Shard Key là gì?
Là **cột dữ liệu** được chọn để thuật toán quyết định dòng dữ liệu đó sẽ được lưu vào Shard nào (ví dụ: `customerId`, `orderId`, `countryCode`).
* *Tiêu chí chọn Shard Key chuẩn:* Phân phối dữ liệu đồng đều, phân phối tải đồng đều, và phù hợp với các câu query thường xuyên nhất.

---

# 7. SHARDING STRATEGIES & CHALLENGES (CHIẾN LƯỢC VÀ THÁCH THỨC)

### 1. Ba chiến lược chia Shard phổ biến
1. **Hash-Based Sharding:** Dùng hàm băm: `shardId = hash(customerId) % totalShards`.
   - *Ưu điểm:* Dữ liệu rải cực kỳ đều giữa các máy.
   - *Nhược điểm:* Không thể truy vấn theo khoảng (`WHERE age BETWEEN 20 AND 30` phải quét tất cả các Shard).
2. **Range-Based Sharding:** Chia theo dải giá trị (ví dụ ID từ 1 - 1.000.000 vào Shard 1; 1.000.001 - 2.000.000 vào Shard 2).
   - *Ưu điểm:* Dễ hiểu, query theo khoảng cực nhanh.
   - *Nhược điểm:* Dễ bị **Hot Spot** (người dùng mới đăng ký luôn có ID lớn $\rightarrow$ Toàn bộ lượt ghi mới dồn hết vào Shard cuối cùng).
3. **Directory-Based Sharding:** Dùng 1 bảng tra cứu riêng (*Lookup Service*) để tra xem ID này nằm ởListed directory Week2
Viewed 1_CAP_Theorem.md:1-75
Viewed 2_PACELC_Theorem.md:1-65
Viewed 3_Consistency_Models.md:1-93
Viewed 4_Database_Replication.md:1-75
Viewed 5_Replication%20Strategies.md:1-91
Viewed 6_Database_Sharding.md:1-90
Viewed 7_Sharding_Strategies_Challenges.md:1-115

Dưới đây là bản **tổng hợp toàn bộ kiến thức cốt lõi của Week 2** (Kiến trúc Hệ thống phân tán & Cơ sở dữ liệu quy mô lớn), được biên soạn súc tích, trực quan kèm theo các ví dụ thực tế giúp bạn nắm bắt nhanh nhất.

---

# 🗺️ TỔNG QUAN CHƯƠNG TRÌNH TUẦN 2
Tuần 2 giải quyết câu hỏi lớn nhất của kỹ sư phần mềm khi hệ thống mở rộng:  
👉 **"Làm sao để lưu trữ và xử lý hàng chục triệu người dùng mà dữ liệu không bị mất, không bị sai lệch và hệ thống không bị sập?"**

Chương trình gồm **7 chủ đề chính** đi từ lý thuyết nền tảng đến kỹ thuật thực chiến:
1. **CAP Theorem** (Quy luật đánh đổi trong hệ thống phân tán).
2. **PACELC Theorem** (Mở rộng của CAP trong trạng thái bình thường).
3. **Consistency Models** (Các mức độ nhất quán dữ liệu).
4. **Database Replication** (Nhân bản cơ sở dữ liệu - Primary/Replica).
5. **Replication Strategies** (Đồng bộ, Bán đồng bộ và Bất đồng bộ).
6. **Database Sharding** (Phân mảnh dữ liệu theo chiều ngang).
7. **Sharding Strategies & Challenges** (Kỹ thuật chia Shard và các cạm bẫy).

---

# 1. CAP THEOREM (ĐỊNH LÝ CAP)

### 💡 Định nghĩa ngắn gọn
Trong một hệ thống phân tán gồm nhiều node máy tính giao tiếp qua mạng, bạn **chỉ có thể chọn tối đa 2 trong 3 yếu tố**:

* **C - Consistency (Nhất quán):** Mọi node đều thấy cùng một dữ liệu mới nhất tại cùng một thời điểm. Sau khi ghi thành công, ai đọc cũng phải thấy dữ liệu mới đó, nếu không thì phải báo lỗi chứ không được trả dữ liệu cũ.
* **A - Availability (Sẵn sàng):** Mọi request gửi đến hệ thống (nếu node còn sống) đều nhận được câu trả lời thành công (không bị lỗi 500 hay timeout), dù dữ liệu trả về có thể là dữ liệu cũ (*stale data*).
* **P - Partition Tolerance (Chịu lỗi đứt mạng):** Hệ thống vẫn tiếp tục hoạt động ngay cả khi đường truyền mạng giữa các node bị đứt (*network partition*).

```
          [C] Consistency
             /   \
            /     \
           /  CAP  \
          /         \
[A] Availability ── [P] Partition Tolerance
```

### ⚠️ Bản chất cốt lõi: Không phải chọn tùy ý 2/3!
Trong thực tế, đường truyền mạng giữa các máy chủ **chắc chắn sẽ có lúc bị chập chờn hoặc đứt đoạn**. Vì vậy, **`P` là bắt buộc phải có**.  
👉 Khi xảy ra sự cố đứt mạng (Network Partition), sự lựa chọn thực tế chỉ là: **Đánh đổi giữa C hay A?**

| Lựa chọn | Cơ chế xử lý | Khi nào dùng? | Ví dụ thực tế |
| :---: | :--- | :--- | :--- |
| **CP System** | Ưu tiên **C** $\rightarrow$ **Thà từ chối phục vụ (hy sinh A) chứ không để dữ liệu bị sai lệch**. | Tiền bạc, tài chính, thanh toán, kho hàng. | **Chuyển tiền ngân hàng:** Nếu 2 server mất kết nối, hệ thống báo *"Dịch vụ tạm gián đoạn"* chứ tuyệt đối không cho chuyển tiền vì dễ bị trừ tiền 2 lần. |
| **AP System** | Ưu tiên **A** $\rightarrow$ **Vẫn trả lời người dùng (hy sinh C), chấp nhận dữ liệu có thể bị trễ/cũ**. | Mạng xã hội, tin tức, giỏ hàng, xem video. | **Facebook / TikTok / YouTube:** Mất mạng giữa các server thì bạn vẫn lướt xem được video và bài post (dù có thể là bài đăng từ 5 phút trước). |

---

# 2. PACELC THEOREM (ĐỊNH LÝ PACELC)

### 💡 Tại sao cần PACELC khi đã có CAP?
Định lý **CAP chỉ nói về lúc mạng bị đứt (Partition)**. Nhưng trong 99.9% thời gian bình thường mạng hoạt động tốt, hệ thống phân tán vẫn phải đối mặt với một sự đánh đổi khác: **Tốc độ (Latency) hay Độ chính xác (Consistency)?**

### 📐 Công thức PACELC
```text
If (P) Partition:
    Trade-off: (A) Availability  vs  (C) Consistency
Else (E):
    Trade-off: (L) Latency       vs  (C) Consistency
```

* **Nếu có Partition (`P`):** Chọn giữa **`A`** (Sẵn sàng) hay **`C`** (Nhất quán).
* **Ngược lại (`E` - Else, khi mạng bình thường):** Chọn giữa **`L`** (Độ trễ thấp / Phản hồi nhanh) hay **`C`** (Dữ liệu nhất quán tuyệt đối).

### 🎯 Ví dụ thực tế
* **Hệ thống PA/EL (ví dụ: DynamoDB, Cassandra):** Khi bình thường, ưu tiên phản hồi cực nhanh (`L`), ghi xong trả kết quả ngay, đồng bộ dữ liệu ngầm sau. Khi đứt mạng, ưu tiên sẵn sàng (`A`).
* **Hệ thống PC/EC (ví dụ: Bigtable, RDBMS Cluster):** Luôn đặt tính đúng đắn của dữ liệu lên hàng đầu (`C`), chấp nhận chạy chậm hơn một chút (`Latency` cao hơn) để chờ các node đồng bộ xong.

---

# 3. CONSISTENCY MODELS (CÁC MÔ HÌNH NHẤT QUÁN)

Không phải lúc nào ta cũng cần dữ liệu phải đồng bộ 100% ngay tức khắc. Dưới đây là các cấp độ nhất quán từ mạnh đến yếu:

```
[MẠNH NHẤT] ─── Strong Consistency
      │
      ├─── Causal Consistency
      │
      ├─── Read-Your-Writes Consistency
      │
      ├─── Monotonic Reads
      │
[YẾU NHẤT]  ─── Eventual Consistency
```

| Mô hình | Định nghĩa | Ví dụ thực tế |
| :--- | :--- | :--- |
| **1. Strong Consistency** | Ghi xong là **ngay lập tức 100% mọi nơi** đọc ra đều thấy dữ liệu mới nhất. | Rút tiền ATM: Tài khoản còn 1 triệu, rút 1 triệu thì 1 giây sau xem ở đâu cũng phải thấy số dư = 0. |
| **2. Eventual Consistency** | Dữ liệu cập nhật sẽ lan truyền từ từ; các node có thể lệch nhau vài giây, nhưng **cuối cùng sẽ bằng nhau**. | Lượt View/Like video Youtube: Bạn thấy 1.000 view, bạn của bạn thấy 995 view, vài phút sau cả hai đều thấy 1.050 view. |
| **3. Read-Your-Writes** | **Chính người vừa thực hiện thao tác** phải thấy được dữ liệu của mình vừa ghi, dù người khác có thể chưa thấy. | Bạn vừa đổi **Avatar Facebook** $\rightarrow$ Bấm F5 trang cá nhân bạn phải thấy ảnh mới ngay (dù bạn bè của bạn có thể 1 phút sau mới thấy). |
| **4. Monotonic Reads** | Người dùng đọc dữ liệu **không bao giờ bị "tua ngược về quá khứ"**. | Lần 1 xem đơn hàng thấy `CONFIRMED`. Bấm F5 lần 2 tuyệt đối không được giật lùi về trạng thái cũ `PENDING`. |
| **5. Causal Consistency** | Các sự kiện có quan hệ **nguyên nhân - kết quả** phải hiển thị đúng thứ tự logic. | Phải nhìn thấy **Bài đăng (Post)** trước rồi mới nhìn thấy **Bình luận (Comment)** của bài đăng đó. |

---

# 4. DATABASE REPLICATION (SAO CHÉP DỮ LIỆU)

### 💡 Định nghĩa
**Replication** là kỹ thuật sao chép **cùng một bộ dữ liệu giống hệt nhau** từ máy chủ gốc (**Primary / Leader**) sang nhiều máy chủ bản sao (**Replicas / Followers**).

```text
               ┌────────────────┐
               │  Client Write  │
               └───────┬────────┘
                       ▼
               ┌────────────────┐
               │ Primary/Leader │ ──(Ghi dữ liệu)
               └───────┬────────┘
                       │ Replication Log
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
  ┌───────────┐  ┌───────────┐  ┌───────────┐
  │ Replica 1 │  │ Replica 2 │  │ Replica 3 │ ──(Chỉ đọc - Read Replicas)
  └───────────┘  └───────────┘  └───────────┘
        ▲              ▲              ▲
        └──────────────┴──────────────┘
                 Client Reads
```

### 🎯 Điểm mấu chốt cần nhớ:
1. **Read Scaling:** Rất hiệu quả khi tỷ lệ Đọc nhiều hơn Ghi (ví dụ: 10 lượt đọc mới có 1 lượt mua). Ta có thể thêm 5 - 10 Replica để gánh hàng triệu lượt đọc.
2. **KHÔNG giải quyết được nghẽn Ghi (Write Bottleneck):** Vì chỉ có **1 Primary duy nhất** nhận lệnh Ghi. Nếu có 10.000 request Ghi/giây, Primary vẫn bị quá tải!
3. **Replica Lag:** Thời gian để dữ liệu truyền từ Primary sang Replica có thể mất từ vài chục mili-giây đến vài giây.
4. **Failover:** Khi Primary bị chết đột ngột, hệ thống sẽ tự động bình bầu một Replica có dữ liệu mới nhất lên làm Primary mới.

---

# 5. REPLICATION STRATEGIES (CHIẾN LƯỢC SAO CHÉP)

Có 3 cách để Primary đồng bộ dữ liệu sang các Replicas:

| Tiêu chí | **Synchronous (Đồng bộ)** | **Asynchronous (Bất đồng bộ)** | **Semi-synchronous (Bán đồng bộ)** |
| :--- | :--- | :--- | :--- |
| **Cơ chế** | Primary ghi xong $\rightarrow$ Chờ **TẤT CẢ** Replica xác nhận (ACK) $\rightarrow$ Mới báo thành công cho khách. | Primary ghi xong $\rightarrow$ **Báo thành công ngay lập tức** $\rightarrow$ Dữ liệu truyền ngầm sang Replica sau. | Primary chỉ cần chờ **TỐI THIỂU 1 Replica** xác nhận (ACK) là báo thành công ngay. |
| **Tốc độ ghi (Latency)** | **Chậm nhất** (phụ thuộc vào node mạng chậm nhất). | **Nhanh nhất** (không phải chờ ai). | **Cân bằng** (rất nhanh). |
| **Nguy cơ mất dữ liệu** | **Bằng 0 (Zero Data Loss):** Đảm bảo an toàn tuyệt đối. | **Có rủi ro:** Nếu Primary chết khi dữ liệu chưa kịp gửi sang Replica $\rightarrow$ Mất dữ liệu! | **Gần như bằng 0:** Vì luôn có ít nhất 1 Replica giữ bản copy. |
| **Use case phù hợp** | Giao dịch tài chính, thanh toán cốt lõi. | Ghi log hệ thống, lượt xem video, telemetry. | Hệ thống E-commerce hiện đại, MySQL/Postgres HA. |

---

# 6. DATABASE SHARDING (PHÂN MẢNH CƠ SỞ DỮ LIỆU)

### 💡 So sánh sự khác nhau giữa Replication và Sharding

* **Replication (Nhân bản):** Mọi máy chủ đều giữ **100% dữ liệu**. Mục tiêu là chịu lỗi và tăng tốc độ Đọc.
* **Sharding (Cắt nhỏ / Phân mảnh):** Dữ liệu bị **cắt thành nhiều phần nhỏ (Shards)** theo chiều ngang (*Horizontal Partitioning*). Mỗi máy chủ chỉ giữ một phần dữ liệu.

```text
[ REPLICATION ]                             [ SHARDING ]
Mỗi máy giữ TOÀN BỘ dữ liệu                 Mỗi máy giữ MỘT PHẦN dữ liệu

┌───────────────┐                           ┌───────────────┐
│ Primary       │ Orders 1..100M            │ Shard 1       │ Customer 1..1M
└───────────────┘                           └───────────────┘
┌───────────────┐                           ┌───────────────┐
│ Replica 1     │ Orders 1..100M            │ Shard 2       │ Customer 1M..2M
└───────────────┘                           └───────────────┘
┌───────────────┐                           ┌───────────────┐
│ Replica 2     │ Orders 1..100M            │ Shard 3       │ Customer 2M..3M
└───────────────┘                           └───────────────┘
```

### 🎯 Khi nào bắt buộc phải dùng Sharding?
Khi dữ liệu vượt quá giới hạn vật lý của 1 máy chủ:
* **Storage Bottleneck:** Database phình to hàng trăm Terabytes, không ổ cứng nào chứa nổi.
* **Write Bottleneck:** Hàng trăm nghìn lượt Ghi mỗi giây, 1 CPU/RAM của Primary không tài nào xử lý kịp. Sharding giúp chia đều tải ghi ra 10 - 20 máy chủ.

### 🔑 Shard Key là gì?
Là **cột dữ liệu** được chọn để thuật toán quyết định dòng dữ liệu đó sẽ được lưu vào Shard nào (ví dụ: `customerId`, `orderId`, `countryCode`).
* *Tiêu chí chọn Shard Key chuẩn:* Phân phối dữ liệu đồng đều, phân phối tải đồng đều, và phù hợp với các câu query thường xuyên nhất.

---

# 7. SHARDING STRATEGIES & CHALLENGES (CHIẾN LƯỢC VÀ THÁCH THỨC)

### 1. Ba chiến lược chia Shard phổ biến
1. **Hash-Based Sharding:** Dùng hàm băm: `shardId = hash(customerId) % totalShards`.
   - *Ưu điểm:* Dữ liệu rải cực kỳ đều giữa các máy.
   - *Nhược điểm:* Không thể truy vấn theo khoảng (`WHERE age BETWEEN 20 AND 30` phải quét tất cả các Shard).
2. **Range-Based Sharding:** Chia theo dải giá trị (ví dụ ID từ 1 - 1.000.000 vào Shard 1; 1.000.001 - 2.000.000 vào Shard 2).
   - *Ưu điểm:* Dễ hiểu, query theo khoảng cực nhanh.
   - *Nhược điểm:* Dễ bị **Hot Spot** (người dùng mới đăng ký luôn có ID lớn $\rightarrow$ Toàn bộ lượt ghi mới dồn hết vào Shard cuối cùng).
3. **Directory-Based Sharding:** Dùng 1 bảng tra cứu riêng (*Lookup Service*) để tra xem ID này nằm ở Shard nào.
   - *Ưu điểm:* Rất linh hoạt, muốn chuyển khách hàng VIP sang server xịn lúc nào cũng được.
   - *Nhược điểm:* Bảng tra cứu trở thành điểm nghẽn cổ chai.

### 2. Bốn thách thức "ác mộng" khi làm Sharding
* **🔥 Hot Spot / Hot Shard:** Một Shard bị quá tải traffic trong khi các Shard khác ngồi chơi (ví dụ: Shard chứa tài khoản của Sơn Tùng M-TP hoặc một công ty lớn trong hệ thống SaaS).
* **🌐 Cross-Shard Query:** Cần dữ liệu từ nhiều Shard khác nhau (ví dụ: Tìm đơn hàng theo ngày). Hệ thống phải gửi request đến tất cả các Shard rồi gộp kết quả lại $\rightarrow$ Cực kỳ chậm!
* **💳 Cross-Shard Transaction:** Chuyển tiền từ User ở Shard 1 sang User ở Shard 2 $\rightarrow$ Không thể dùng `@Transactional` của database thông thường, bắt buộc phải dùng **Saga Pattern** hoặc **2PC**.
* **🚚 Re-sharding (Tăng thêm Shard):** Khi dữ liệu tăng, muốn nâng từ 4 Shard lên 8 Shard thì phải di chuyển hàng Terabyte dữ liệu giữa các máy chủ mà không được làm sập hệ thống (Zero-downtime migration).

---

# 📝 BẢNG LIÊN HỆ ĐẾN CÁC BÀI ASSIGNMENT TUẦN 2

| Bài tập | Nội dung áp dụng | Câu hỏi mấu chốt cần giải quyết |
| :--- | :--- | :--- |
| **[Assignment 1](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week2/Assignment1.md)** | **CAP & PACELC** | Sự cố khách bị trừ tiền 2 lần do hệ thống chọn **AP** cho dữ liệu Payment (cho phép ghi vào Replica Tokyo khi đứt mạng với Singapore). Cần chuyển sang **CP** và áp dụng `Idempotency-Key`. |
| **Assignment 2** | **Consistency Models** | Xử lý tình huống "Vừa thanh toán xong F5 thấy trạng thái cũ PENDING" (*Read-after-write inconsistency*). |
| **Assignment 3** | **Replication & Failover** | Thiết kế định tuyến Read/Write và quy trình 5 bước Failover khi Primary bị chết. |
| **Assignment 4** | **Sharding & Hot Spot** | Chọn Shard Key cho hệ thống SaaS 5 triệu Tenants và giải quyết vấn đề khách hàng lớn làm nghẽn Shard (*Hot Tenant*). |

Bạn có muốn bắt đầu bằng việc giải chi tiết và viết câu trả lời cho **[Assignment 1](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week2/Assignment1.md)** vào file `.md` không?Listed directory Week2
Viewed 1_CAP_Theorem.md:1-75
Viewed 2_PACELC_Theorem.md:1-65
Viewed 3_Consistency_Models.md:1-93
Viewed 4_Database_Replication.md:1-75
Viewed 5_Replication%20Strategies.md:1-91
Viewed 6_Database_Sharding.md:1-90
Viewed 7_Sharding_Strategies_Challenges.md:1-115

Dưới đây là bản **tổng hợp toàn bộ kiến thức cốt lõi của Week 2** (Kiến trúc Hệ thống phân tán & Cơ sở dữ liệu quy mô lớn), được biên soạn súc tích, trực quan kèm theo các ví dụ thực tế giúp bạn nắm bắt nhanh nhất.

---

# 🗺️ TỔNG QUAN CHƯƠNG TRÌNH TUẦN 2
Tuần 2 giải quyết câu hỏi lớn nhất của kỹ sư phần mềm khi hệ thống mở rộng:  
👉 **"Làm sao để lưu trữ và xử lý hàng chục triệu người dùng mà dữ liệu không bị mất, không bị sai lệch và hệ thống không bị sập?"**

Chương trình gồm **7 chủ đề chính** đi từ lý thuyết nền tảng đến kỹ thuật thực chiến:
1. **CAP Theorem** (Quy luật đánh đổi trong hệ thống phân tán).
2. **PACELC Theorem** (Mở rộng của CAP trong trạng thái bình thường).
3. **Consistency Models** (Các mức độ nhất quán dữ liệu).
4. **Database Replication** (Nhân bản cơ sở dữ liệu - Primary/Replica).
5. **Replication Strategies** (Đồng bộ, Bán đồng bộ và Bất đồng bộ).
6. **Database Sharding** (Phân mảnh dữ liệu theo chiều ngang).
7. **Sharding Strategies & Challenges** (Kỹ thuật chia Shard và các cạm bẫy).

---

# 1. CAP THEOREM (ĐỊNH LÝ CAP)

### 💡 Định nghĩa ngắn gọn
Trong một hệ thống phân tán gồm nhiều node máy tính giao tiếp qua mạng, bạn **chỉ có thể chọn tối đa 2 trong 3 yếu tố**:

* **C - Consistency (Nhất quán):** Mọi node đều thấy cùng một dữ liệu mới nhất tại cùng một thời điểm. Sau khi ghi thành công, ai đọc cũng phải thấy dữ liệu mới đó, nếu không thì phải báo lỗi chứ không được trả dữ liệu cũ.
* **A - Availability (Sẵn sàng):** Mọi request gửi đến hệ thống (nếu node còn sống) đều nhận được câu trả lời thành công (không bị lỗi 500 hay timeout), dù dữ liệu trả về có thể là dữ liệu cũ (*stale data*).
* **P - Partition Tolerance (Chịu lỗi đứt mạng):** Hệ thống vẫn tiếp tục hoạt động ngay cả khi đường truyền mạng giữa các node bị đứt (*network partition*).

```
          [C] Consistency
             /   \
            /     \
           /  CAP  \
          /         \
[A] Availability ── [P] Partition Tolerance
```

### ⚠️ Bản chất cốt lõi: Không phải chọn tùy ý 2/3!
Trong thực tế, đường truyền mạng giữa các máy chủ **chắc chắn sẽ có lúc bị chập chờn hoặc đứt đoạn**. Vì vậy, **`P` là bắt buộc phải có**.  
👉 Khi xảy ra sự cố đứt mạng (Network Partition), sự lựa chọn thực tế chỉ là: **Đánh đổi giữa C hay A?**

| Lựa chọn | Cơ chế xử lý | Khi nào dùng? | Ví dụ thực tế |
| :---: | :--- | :--- | :--- |
| **CP System** | Ưu tiên **C** $\rightarrow$ **Thà từ chối phục vụ (hy sinh A) chứ không để dữ liệu bị sai lệch**. | Tiền bạc, tài chính, thanh toán, kho hàng. | **Chuyển tiền ngân hàng:** Nếu 2 server mất kết nối, hệ thống báo *"Dịch vụ tạm gián đoạn"* chứ tuyệt đối không cho chuyển tiền vì dễ bị trừ tiền 2 lần. |
| **AP System** | Ưu tiên **A** $\rightarrow$ **Vẫn trả lời người dùng (hy sinh C), chấp nhận dữ liệu có thể bị trễ/cũ**. | Mạng xã hội, tin tức, giỏ hàng, xem video. | **Facebook / TikTok / YouTube:** Mất mạng giữa các server thì bạn vẫn lướt xem được video và bài post (dù có thể là bài đăng từ 5 phút trước). |

---

# 2. PACELC THEOREM (ĐỊNH LÝ PACELC)

### 💡 Tại sao cần PACELC khi đã có CAP?
Định lý **CAP chỉ nói về lúc mạng bị đứt (Partition)**. Nhưng trong 99.9% thời gian bình thường mạng hoạt động tốt, hệ thống phân tán vẫn phải đối mặt với một sự đánh đổi khác: **Tốc độ (Latency) hay Độ chính xác (Consistency)?**

### 📐 Công thức PACELC
```text
If (P) Partition:
    Trade-off: (A) Availability  vs  (C) Consistency
Else (E):
    Trade-off: (L) Latency       vs  (C) Consistency
```

* **Nếu có Partition (`P`):** Chọn giữa **`A`** (Sẵn sàng) hay **`C`** (Nhất quán).
* **Ngược lại (`E` - Else, khi mạng bình thường):** Chọn giữa **`L`** (Độ trễ thấp / Phản hồi nhanh) hay **`C`** (Dữ liệu nhất quán tuyệt đối).

### 🎯 Ví dụ thực tế
* **Hệ thống PA/EL (ví dụ: DynamoDB, Cassandra):** Khi bình thường, ưu tiên phản hồi cực nhanh (`L`), ghi xong trả kết quả ngay, đồng bộ dữ liệu ngầm sau. Khi đứt mạng, ưu tiên sẵn sàng (`A`).
* **Hệ thống PC/EC (ví dụ: Bigtable, RDBMS Cluster):** Luôn đặt tính đúng đắn của dữ liệu lên hàng đầu (`C`), chấp nhận chạy chậm hơn một chút (`Latency` cao hơn) để chờ các node đồng bộ xong.

---

# 3. CONSISTENCY MODELS (CÁC MÔ HÌNH NHẤT QUÁN)

Không phải lúc nào ta cũng cần dữ liệu phải đồng bộ 100% ngay tức khắc. Dưới đây là các cấp độ nhất quán từ mạnh đến yếu:

```
[MẠNH NHẤT] ─── Strong Consistency
      │
      ├─── Causal Consistency
      │
      ├─── Read-Your-Writes Consistency
      │
      ├─── Monotonic Reads
      │
[YẾU NHẤT]  ─── Eventual Consistency
```

| Mô hình | Định nghĩa | Ví dụ thực tế |
| :--- | :--- | :--- |
| **1. Strong Consistency** | Ghi xong là **ngay lập tức 100% mọi nơi** đọc ra đều thấy dữ liệu mới nhất. | Rút tiền ATM: Tài khoản còn 1 triệu, rút 1 triệu thì 1 giây sau xem ở đâu cũng phải thấy số dư = 0. |
| **2. Eventual Consistency** | Dữ liệu cập nhật sẽ lan truyền từ từ; các node có thể lệch nhau vài giây, nhưng **cuối cùng sẽ bằng nhau**. | Lượt View/Like video Youtube: Bạn thấy 1.000 view, bạn của bạn thấy 995 view, vài phút sau cả hai đều thấy 1.050 view. |
| **3. Read-Your-Writes** | **Chính người vừa thực hiện thao tác** phải thấy được dữ liệu của mình vừa ghi, dù người khác có thể chưa thấy. | Bạn vừa đổi **Avatar Facebook** $\rightarrow$ Bấm F5 trang cá nhân bạn phải thấy ảnh mới ngay (dù bạn bè của bạn có thể 1 phút sau mới thấy). |
| **4. Monotonic Reads** | Người dùng đọc dữ liệu **không bao giờ bị "tua ngược về quá khứ"**. | Lần 1 xem đơn hàng thấy `CONFIRMED`. Bấm F5 lần 2 tuyệt đối không được giật lùi về trạng thái cũ `PENDING`. |
| **5. Causal Consistency** | Các sự kiện có quan hệ **nguyên nhân - kết quả** phải hiển thị đúng thứ tự logic. | Phải nhìn thấy **Bài đăng (Post)** trước rồi mới nhìn thấy **Bình luận (Comment)** của bài đăng đó. |

---

# 4. DATABASE REPLICATION (SAO CHÉP DỮ LIỆU)

### 💡 Định nghĩa
**Replication** là kỹ thuật sao chép **cùng một bộ dữ liệu giống hệt nhau** từ máy chủ gốc (**Primary / Leader**) sang nhiều máy chủ bản sao (**Replicas / Followers**).

```text
               ┌────────────────┐
               │  Client Write  │
               └───────┬────────┘
                       ▼
               ┌────────────────┐
               │ Primary/Leader │ ──(Ghi dữ liệu)
               └───────┬────────┘
                       │ Replication Log
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
  ┌───────────┐  ┌───────────┐  ┌───────────┐
  │ Replica 1 │  │ Replica 2 │  │ Replica 3 │ ──(Chỉ đọc - Read Replicas)
  └───────────┘  └───────────┘  └───────────┘
        ▲              ▲              ▲
        └──────────────┴──────────────┘
                 Client Reads
```

### 🎯 Điểm mấu chốt cần nhớ:
1. **Read Scaling:** Rất hiệu quả khi tỷ lệ Đọc nhiều hơn Ghi (ví dụ: 10 lượt đọc mới có 1 lượt mua). Ta có thể thêm 5 - 10 Replica để gánh hàng triệu lượt đọc.
2. **KHÔNG giải quyết được nghẽn Ghi (Write Bottleneck):** Vì chỉ có **1 Primary duy nhất** nhận lệnh Ghi. Nếu có 10.000 request Ghi/giây, Primary vẫn bị quá tải!
3. **Replica Lag:** Thời gian để dữ liệu truyền từ Primary sang Replica có thể mất từ vài chục mili-giây đến vài giây.
4. **Failover:** Khi Primary bị chết đột ngột, hệ thống sẽ tự động bình bầu một Replica có dữ liệu mới nhất lên làm Primary mới.

---

# 5. REPLICATION STRATEGIES (CHIẾN LƯỢC SAO CHÉP)

Có 3 cách để Primary đồng bộ dữ liệu sang các Replicas:

| Tiêu chí | **Synchronous (Đồng bộ)** | **Asynchronous (Bất đồng bộ)** | **Semi-synchronous (Bán đồng bộ)** |
| :--- | :--- | :--- | :--- |
| **Cơ chế** | Primary ghi xong $\rightarrow$ Chờ **TẤT CẢ** Replica xác nhận (ACK) $\rightarrow$ Mới báo thành công cho khách. | Primary ghi xong $\rightarrow$ **Báo thành công ngay lập tức** $\rightarrow$ Dữ liệu truyền ngầm sang Replica sau. | Primary chỉ cần chờ **TỐI THIỂU 1 Replica** xác nhận (ACK) là báo thành công ngay. |
| **Tốc độ ghi (Latency)** | **Chậm nhất** (phụ thuộc vào node mạng chậm nhất). | **Nhanh nhất** (không phải chờ ai). | **Cân bằng** (rất nhanh). |
| **Nguy cơ mất dữ liệu** | **Bằng 0 (Zero Data Loss):** Đảm bảo an toàn tuyệt đối. | **Có rủi ro:** Nếu Primary chết khi dữ liệu chưa kịp gửi sang Replica $\rightarrow$ Mất dữ liệu! | **Gần như bằng 0:** Vì luôn có ít nhất 1 Replica giữ bản copy. |
| **Use case phù hợp** | Giao dịch tài chính, thanh toán cốt lõi. | Ghi log hệ thống, lượt xem video, telemetry. | Hệ thống E-commerce hiện đại, MySQL/Postgres HA. |

---

# 6. DATABASE SHARDING (PHÂN MẢNH CƠ SỞ DỮ LIỆU)

### 💡 So sánh sự khác nhau giữa Replication và Sharding

* **Replication (Nhân bản):** Mọi máy chủ đều giữ **100% dữ liệu**. Mục tiêu là chịu lỗi và tăng tốc độ Đọc.
* **Sharding (Cắt nhỏ / Phân mảnh):** Dữ liệu bị **cắt thành nhiều phần nhỏ (Shards)** theo chiều ngang (*Horizontal Partitioning*). Mỗi máy chủ chỉ giữ một phần dữ liệu.

```text
[ REPLICATION ]                             [ SHARDING ]
Mỗi máy giữ TOÀN BỘ dữ liệu                 Mỗi máy giữ MỘT PHẦN dữ liệu

┌───────────────┐                           ┌───────────────┐
│ Primary       │ Orders 1..100M            │ Shard 1       │ Customer 1..1M
└───────────────┘                           └───────────────┘
┌───────────────┐                           ┌───────────────┐
│ Replica 1     │ Orders 1..100M            │ Shard 2       │ Customer 1M..2M
└───────────────┘                           └───────────────┘
┌───────────────┐                           ┌───────────────┐
│ Replica 2     │ Orders 1..100M            │ Shard 3       │ Customer 2M..3M
└───────────────┘                           └───────────────┘
```

### 🎯 Khi nào bắt buộc phải dùng Sharding?
Khi dữ liệu vượt quá giới hạn vật lý của 1 máy chủ:
* **Storage Bottleneck:** Database phình to hàng trăm Terabytes, không ổ cứng nào chứa nổi.
* **Write Bottleneck:** Hàng trăm nghìn lượt Ghi mỗi giây, 1 CPU/RAM của Primary không tài nào xử lý kịp. Sharding giúp chia đều tải ghi ra 10 - 20 máy chủ.

### 🔑 Shard Key là gì?
Là **cột dữ liệu** được chọn để thuật toán quyết định dòng dữ liệu đó sẽ được lưu vào Shard nào (ví dụ: `customerId`, `orderId`, `countryCode`).
* *Tiêu chí chọn Shard Key chuẩn:* Phân phối dữ liệu đồng đều, phân phối tải đồng đều, và phù hợp với các câu query thường xuyên nhất.

---

# 7. SHARDING STRATEGIES & CHALLENGES (CHIẾN LƯỢC VÀ THÁCH THỨC)

### 1. Ba chiến lược chia Shard phổ biến
1. **Hash-Based Sharding:** Dùng hàm băm: `shardId = hash(customerId) % totalShards`.
   - *Ưu điểm:* Dữ liệu rải cực kỳ đều giữa các máy.
   - *Nhược điểm:* Không thể truy vấn theo khoảng (`WHERE age BETWEEN 20 AND 30` phải quét tất cả các Shard).
2. **Range-Based Sharding:** Chia theo dải giá trị (ví dụ ID từ 1 - 1.000.000 vào Shard 1; 1.000.001 - 2.000.000 vào Shard 2).
   - *Ưu điểm:* Dễ hiểu, query theo khoảng cực nhanh.
   - *Nhược điểm:* Dễ bị **Hot Spot** (người dùng mới đăng ký luôn có ID lớn $\rightarrow$ Toàn bộ lượt ghi mới dồn hết vào Shard cuối cùng).
3. **Directory-Based Sharding:** Dùng 1 bảng tra cứu riêng (*Lookup Service*) để tra xem ID này nằm ở Shard nào.
   - *Ưu điểm:* Rất linh hoạt, muốn chuyển khách hàng VIP sang server xịn lúc nào cũng được.
   - *Nhược điểm:* Bảng tra cứu trở thành điểm nghẽn cổ chai.

### 2. Bốn thách thức "ác mộng" khi làm Sharding
* **🔥 Hot Spot / Hot Shard:** Một Shard bị quá tải traffic trong khi các Shard khác ngồi chơi (ví dụ: Shard chứa tài khoản của Sơn Tùng M-TP hoặc một công ty lớn trong hệ thống SaaS).
* **🌐 Cross-Shard Query:** Cần dữ liệu từ nhiều Shard khác nhau (ví dụ: Tìm đơn hàng theo ngày). Hệ thống phải gửi request đến tất cả các Shard rồi gộp kết quả lại $\rightarrow$ Cực kỳ chậm!
* **💳 Cross-Shard Transaction:** Chuyển tiền từ User ở Shard 1 sang User ở Shard 2 $\rightarrow$ Không thể dùng `@Transactional` của database thông thường, bắt buộc phải dùng **Saga Pattern** hoặc **2PC**.
* **🚚 Re-sharding (Tăng thêm Shard):** Khi dữ liệu tăng, muốn nâng từ 4 Shard lên 8 Shard thì phải di chuyển hàng Terabyte dữ liệu giữa các máy chủ mà không được làm sập hệ thống (Zero-downtime migration).

---

# 📝 BẢNG LIÊN HỆ ĐẾN CÁC BÀI ASSIGNMENT TUẦN 2

| Bài tập | Nội dung áp dụng | Câu hỏi mấu chốt cần giải quyết |
| :--- | :--- | :--- |
| **[Assignment 1](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week2/Assignment1.md)** | **CAP & PACELC** | Sự cố khách bị trừ tiền 2 lần do hệ thống chọn **AP** cho dữ liệu Payment (cho phép ghi vào Replica Tokyo khi đứt mạng với Singapore). Cần chuyển sang **CP** và áp dụng `Idempotency-Key`. |
| **Assignment 2** | **Consistency Models** | Xử lý tình huống "Vừa thanh toán xong F5 thấy trạng thái cũ PENDING" (*Read-after-write inconsistency*). |
| **Assignment 3** | **Replication & Failover** | Thiết kế định tuyến Read/Write và quy trình 5 bước Failover khi Primary bị chết. |
| **Assignment 4** | **Sharding & Hot Spot** | Chọn Shard Key cho hệ thống SaaS 5 triệu Tenants và giải quyết vấn đề khách hàng lớn làm nghẽn Shard (*Hot Tenant*). |

Bạn có muốn bắt đầu bằng việc giải chi tiết và viết câu trả lời cho **[Assignment 1](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week2/Assignment1.md)** vào file `.md` không?Listed directory Week2
Viewed 1_CAP_Theorem.md:1-75
Viewed 2_PACELC_Theorem.md:1-65
Viewed 3_Consistency_Models.md:1-93
Viewed 4_Database_Replication.md:1-75
Viewed 5_Replication%20Strategies.md:1-91
Viewed 6_Database_Sharding.md:1-90
Viewed 7_Sharding_Strategies_Challenges.md:1-115

Dưới đây là bản **tổng hợp toàn bộ kiến thức cốt lõi của Week 2** (Kiến trúc Hệ thống phân tán & Cơ sở dữ liệu quy mô lớn), được biên soạn súc tích, trực quan kèm theo các ví dụ thực tế giúp bạn nắm bắt nhanh nhất.

---

# 🗺️ TỔNG QUAN CHƯƠNG TRÌNH TUẦN 2
Tuần 2 giải quyết câu hỏi lớn nhất của kỹ sư phần mềm khi hệ thống mở rộng:  
👉 **"Làm sao để lưu trữ và xử lý hàng chục triệu người dùng mà dữ liệu không bị mất, không bị sai lệch và hệ thống không bị sập?"**

Chương trình gồm **7 chủ đề chính** đi từ lý thuyết nền tảng đến kỹ thuật thực chiến:
1. **CAP Theorem** (Quy luật đánh đổi trong hệ thống phân tán).
2. **PACELC Theorem** (Mở rộng của CAP trong trạng thái bình thường).
3. **Consistency Models** (Các mức độ nhất quán dữ liệu).
4. **Database Replication** (Nhân bản cơ sở dữ liệu - Primary/Replica).
5. **Replication Strategies** (Đồng bộ, Bán đồng bộ và Bất đồng bộ).
6. **Database Sharding** (Phân mảnh dữ liệu theo chiều ngang).
7. **Sharding Strategies & Challenges** (Kỹ thuật chia Shard và các cạm bẫy).

---

# 1. CAP THEOREM (ĐỊNH LÝ CAP)

### 💡 Định nghĩa ngắn gọn
Trong một hệ thống phân tán gồm nhiều node máy tính giao tiếp qua mạng, bạn **chỉ có thể chọn tối đa 2 trong 3 yếu tố**:

* **C - Consistency (Nhất quán):** Mọi node đều thấy cùng một dữ liệu mới nhất tại cùng một thời điểm. Sau khi ghi thành công, ai đọc cũng phải thấy dữ liệu mới đó, nếu không thì phải báo lỗi chứ không được trả dữ liệu cũ.
* **A - Availability (Sẵn sàng):** Mọi request gửi đến hệ thống (nếu node còn sống) đều nhận được câu trả lời thành công (không bị lỗi 500 hay timeout), dù dữ liệu trả về có thể là dữ liệu cũ (*stale data*).
* **P - Partition Tolerance (Chịu lỗi đứt mạng):** Hệ thống vẫn tiếp tục hoạt động ngay cả khi đường truyền mạng giữa các node bị đứt (*network partition*).

```
          [C] Consistency
             /   \
            /     \
           /  CAP  \
          /         \
[A] Availability ── [P] Partition Tolerance
```

### ⚠️ Bản chất cốt lõi: Không phải chọn tùy ý 2/3!
Trong thực tế, đường truyền mạng giữa các máy chủ **chắc chắn sẽ có lúc bị chập chờn hoặc đứt đoạn**. Vì vậy, **`P` là bắt buộc phải có**.  
👉 Khi xảy ra sự cố đứt mạng (Network Partition), sự lựa chọn thực tế chỉ là: **Đánh đổi giữa C hay A?**

| Lựa chọn | Cơ chế xử lý | Khi nào dùng? | Ví dụ thực tế |
| :---: | :--- | :--- | :--- |
| **CP System** | Ưu tiên **C** $\rightarrow$ **Thà từ chối phục vụ (hy sinh A) chứ không để dữ liệu bị sai lệch**. | Tiền bạc, tài chính, thanh toán, kho hàng. | **Chuyển tiền ngân hàng:** Nếu 2 server mất kết nối, hệ thống báo *"Dịch vụ tạm gián đoạn"* chứ tuyệt đối không cho chuyển tiền vì dễ bị trừ tiền 2 lần. |
| **AP System** | Ưu tiên **A** $\rightarrow$ **Vẫn trả lời người dùng (hy sinh C), chấp nhận dữ liệu có thể bị trễ/cũ**. | Mạng xã hội, tin tức, giỏ hàng, xem video. | **Facebook / TikTok / YouTube:** Mất mạng giữa các server thì bạn vẫn lướt xem được video và bài post (dù có thể là bài đăng từ 5 phút trước). |

---

# 2. PACELC THEOREM (ĐỊNH LÝ PACELC)

### 💡 Tại sao cần PACELC khi đã có CAP?
Định lý **CAP chỉ nói về lúc mạng bị đứt (Partition)**. Nhưng trong 99.9% thời gian bình thường mạng hoạt động tốt, hệ thống phân tán vẫn phải đối mặt với một sự đánh đổi khác: **Tốc độ (Latency) hay Độ chính xác (Consistency)?**

### 📐 Công thức PACELC
```text
If (P) Partition:
    Trade-off: (A) Availability  vs  (C) Consistency
Else (E):
    Trade-off: (L) Latency       vs  (C) Consistency
```

* **Nếu có Partition (`P`):** Chọn giữa **`A`** (Sẵn sàng) hay **`C`** (Nhất quán).
* **Ngược lại (`E` - Else, khi mạng bình thường):** Chọn giữa **`L`** (Độ trễ thấp / Phản hồi nhanh) hay **`C`** (Dữ liệu nhất quán tuyệt đối).

### 🎯 Ví dụ thực tế
* **Hệ thống PA/EL (ví dụ: DynamoDB, Cassandra):** Khi bình thường, ưu tiên phản hồi cực nhanh (`L`), ghi xong trả kết quả ngay, đồng bộ dữ liệu ngầm sau. Khi đứt mạng, ưu tiên sẵn sàng (`A`).
* **Hệ thống PC/EC (ví dụ: Bigtable, RDBMS Cluster):** Luôn đặt tính đúng đắn của dữ liệu lên hàng đầu (`C`), chấp nhận chạy chậm hơn một chút (`Latency` cao hơn) để chờ các node đồng bộ xong.

---

# 3. CONSISTENCY MODELS (CÁC MÔ HÌNH NHẤT QUÁN)

Không phải lúc nào ta cũng cần dữ liệu phải đồng bộ 100% ngay tức khắc. Dưới đây là các cấp độ nhất quán từ mạnh đến yếu:

```
[MẠNH NHẤT] ─── Strong Consistency
      │
      ├─── Causal Consistency
      │
      ├─── Read-Your-Writes Consistency
      │
      ├─── Monotonic Reads
      │
[YẾU NHẤT]  ─── Eventual Consistency
```

| Mô hình | Định nghĩa | Ví dụ thực tế |
| :--- | :--- | :--- |
| **1. Strong Consistency** | Ghi xong là **ngay lập tức 100% mọi nơi** đọc ra đều thấy dữ liệu mới nhất. | Rút tiền ATM: Tài khoản còn 1 triệu, rút 1 triệu thì 1 giây sau xem ở đâu cũng phải thấy số dư = 0. |
| **2. Eventual Consistency** | Dữ liệu cập nhật sẽ lan truyền từ từ; các node có thể lệch nhau vài giây, nhưng **cuối cùng sẽ bằng nhau**. | Lượt View/Like video Youtube: Bạn thấy 1.000 view, bạn của bạn thấy 995 view, vài phút sau cả hai đều thấy 1.050 view. |
| **3. Read-Your-Writes** | **Chính người vừa thực hiện thao tác** phải thấy được dữ liệu của mình vừa ghi, dù người khác có thể chưa thấy. | Bạn vừa đổi **Avatar Facebook** $\rightarrow$ Bấm F5 trang cá nhân bạn phải thấy ảnh mới ngay (dù bạn bè của bạn có thể 1 phút sau mới thấy). |
| **4. Monotonic Reads** | Người dùng đọc dữ liệu **không bao giờ bị "tua ngược về quá khứ"**. | Lần 1 xem đơn hàng thấy `CONFIRMED`. Bấm F5 lần 2 tuyệt đối không được giật lùi về trạng thái cũ `PENDING`. |
| **5. Causal Consistency** | Các sự kiện có quan hệ **nguyên nhân - kết quả** phải hiển thị đúng thứ tự logic. | Phải nhìn thấy **Bài đăng (Post)** trước rồi mới nhìn thấy **Bình luận (Comment)** của bài đăng đó. |

---

# 4. DATABASE REPLICATION (SAO CHÉP DỮ LIỆU)

### 💡 Định nghĩa
**Replication** là kỹ thuật sao chép **cùng một bộ dữ liệu giống hệt nhau** từ máy chủ gốc (**Primary / Leader**) sang nhiều máy chủ bản sao (**Replicas / Followers**).

```text
               ┌────────────────┐
               │  Client Write  │
               └───────┬────────┘
                       ▼
               ┌────────────────┐
               │ Primary/Leader │ ──(Ghi dữ liệu)
               └───────┬────────┘
                       │ Replication Log
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
  ┌───────────┐  ┌───────────┐  ┌───────────┐
  │ Replica 1 │  │ Replica 2 │  │ Replica 3 │ ──(Chỉ đọc - Read Replicas)
  └───────────┘  └───────────┘  └───────────┘
        ▲              ▲              ▲
        └──────────────┴──────────────┘
                 Client Reads
```

### 🎯 Điểm mấu chốt cần nhớ:
1. **Read Scaling:** Rất hiệu quả khi tỷ lệ Đọc nhiều hơn Ghi (ví dụ: 10 lượt đọc mới có 1 lượt mua). Ta có thể thêm 5 - 10 Replica để gánh hàng triệu lượt đọc.
2. **KHÔNG giải quyết được nghẽn Ghi (Write Bottleneck):** Vì chỉ có **1 Primary duy nhất** nhận lệnh Ghi. Nếu có 10.000 request Ghi/giây, Primary vẫn bị quá tải!
3. **Replica Lag:** Thời gian để dữ liệu truyền từ Primary sang Replica có thể mất từ vài chục mili-giây đến vài giây.
4. **Failover:** Khi Primary bị chết đột ngột, hệ thống sẽ tự động bình bầu một Replica có dữ liệu mới nhất lên làm Primary mới.

---

# 5. REPLICATION STRATEGIES (CHIẾN LƯỢC SAO CHÉP)

Có 3 cách để Primary đồng bộ dữ liệu sang các Replicas:

| Tiêu chí | **Synchronous (Đồng bộ)** | **Asynchronous (Bất đồng bộ)** | **Semi-synchronous (Bán đồng bộ)** |
| :--- | :--- | :--- | :--- |
| **Cơ chế** | Primary ghi xong $\rightarrow$ Chờ **TẤT CẢ** Replica xác nhận (ACK) $\rightarrow$ Mới báo thành công cho khách. | Primary ghi xong $\rightarrow$ **Báo thành công ngay lập tức** $\rightarrow$ Dữ liệu truyền ngầm sang Replica sau. | Primary chỉ cần chờ **TỐI THIỂU 1 Replica** xác nhận (ACK) là báo thành công ngay. |
| **Tốc độ ghi (Latency)** | **Chậm nhất** (phụ thuộc vào node mạng chậm nhất). | **Nhanh nhất** (không phải chờ ai). | **Cân bằng** (rất nhanh). |
| **Nguy cơ mất dữ liệu** | **Bằng 0 (Zero Data Loss):** Đảm bảo an toàn tuyệt đối. | **Có rủi ro:** Nếu Primary chết khi dữ liệu chưa kịp gửi sang Replica $\rightarrow$ Mất dữ liệu! | **Gần như bằng 0:** Vì luôn có ít nhất 1 Replica giữ bản copy. |
| **Use case phù hợp** | Giao dịch tài chính, thanh toán cốt lõi. | Ghi log hệ thống, lượt xem video, telemetry. | Hệ thống E-commerce hiện đại, MySQL/Postgres HA. |

---

# 6. DATABASE SHARDING (PHÂN MẢNH CƠ SỞ DỮ LIỆU)

### 💡 So sánh sự khác nhau giữa Replication và Sharding

* **Replication (Nhân bản):** Mọi máy chủ đều giữ **100% dữ liệu**. Mục tiêu là chịu lỗi và tăng tốc độ Đọc.
* **Sharding (Cắt nhỏ / Phân mảnh):** Dữ liệu bị **cắt thành nhiều phần nhỏ (Shards)** theo chiều ngang (*Horizontal Partitioning*). Mỗi máy chủ chỉ giữ một phần dữ liệu.

```text
[ REPLICATION ]                             [ SHARDING ]
Mỗi máy giữ TOÀN BỘ dữ liệu                 Mỗi máy giữ MỘT PHẦN dữ liệu

┌───────────────┐                           ┌───────────────┐
│ Primary       │ Orders 1..100M            │ Shard 1       │ Customer 1..1M
└───────────────┘                           └───────────────┘
┌───────────────┐                           ┌───────────────┐
│ Replica 1     │ Orders 1..100M            │ Shard 2       │ Customer 1M..2M
└───────────────┘                           └───────────────┘
┌───────────────┐                           ┌───────────────┐
│ Replica 2     │ Orders 1..100M            │ Shard 3       │ Customer 2M..3M
└───────────────┘                           └───────────────┘
```

### 🎯 Khi nào bắt buộc phải dùng Sharding?
Khi dữ liệu vượt quá giới hạn vật lý của 1 máy chủ:
* **Storage Bottleneck:** Database phình to hàng trăm Terabytes, không ổ cứng nào chứa nổi.
* **Write Bottleneck:** Hàng trăm nghìn lượt Ghi mỗi giây, 1 CPU/RAM của Primary không tài nào xử lý kịp. Sharding giúp chia đều tải ghi ra 10 - 20 máy chủ.

### 🔑 Shard Key là gì?
Là **cột dữ liệu** được chọn để thuật toán quyết định dòng dữ liệu đó sẽ được lưu vào Shard nào (ví dụ: `customerId`, `orderId`, `countryCode`).
* *Tiêu chí chọn Shard Key chuẩn:* Phân phối dữ liệu đồng đều, phân phối tải đồng đều, và phù hợp với các câu query thường xuyên nhất.

---

# 7. SHARDING STRATEGIES & CHALLENGES (CHIẾN LƯỢC VÀ THÁCH THỨC)

### 1. Ba chiến lược chia Shard phổ biến
1. **Hash-Based Sharding:** Dùng hàm băm: `shardId = hash(customerId) % totalShards`.
   - *Ưu điểm:* Dữ liệu rải cực kỳ đều giữa các máy.
   - *Nhược điểm:* Không thể truy vấn theo khoảng (`WHERE age BETWEEN 20 AND 30` phải quét tất cả các Shard).
2. **Range-Based Sharding:** Chia theo dải giá trị (ví dụ ID từ 1 - 1.000.000 vào Shard 1; 1.000.001 - 2.000.000 vào Shard 2).
   - *Ưu điểm:* Dễ hiểu, query theo khoảng cực nhanh.
   - *Nhược điểm:* Dễ bị **Hot Spot** (người dùng mới đăng ký luôn có ID lớn $\rightarrow$ Toàn bộ lượt ghi mới dồn hết vào Shard cuối cùng).
3. **Directory-Based Sharding:** Dùng 1 bảng tra cứu riêng (*Lookup Service*) để tra xem ID này nằm ở Shard nào.
   - *Ưu điểm:* Rất linh hoạt, muốn chuyển khách hàng VIP sang server xịn lúc nào cũng được.
   - *Nhược điểm:* Bảng tra cứu trở thành điểm nghẽn cổ chai.

### 2. Bốn thách thức "ác mộng" khi làm Sharding
* **🔥 Hot Spot / Hot Shard:** Một Shard bị quá tải traffic trong khi các Shard khác ngồi chơi (ví dụ: Shard chứa tài khoản của Sơn Tùng M-TP hoặc một công ty lớn trong hệ thống SaaS).
* **🌐 Cross-Shard Query:** Cần dữ liệu từ nhiều Shard khác nhau (ví dụ: Tìm đơn hàng theo ngày). Hệ thống phải gửi request đến tất cả các Shard rồi gộp kết quả lại $\rightarrow$ Cực kỳ chậm!
* **💳 Cross-Shard Transaction:** Chuyển tiền từ User ở Shard 1 sang User ở Shard 2 $\rightarrow$ Không thể dùng `@Transactional` của database thông thường, bắt buộc phải dùng **Saga Pattern** hoặc **2PC**.
* **🚚 Re-sharding (Tăng thêm Shard):** Khi dữ liệu tăng, muốn nâng từ 4 Shard lên 8 Shard thì phải di chuyển hàng Terabyte dữ liệu giữa các máy chủ mà không được làm sập hệ thống (Zero-downtime migration).

---

# 📝 BẢNG LIÊN HỆ ĐẾN CÁC BÀI ASSIGNMENT TUẦN 2

| Bài tập | Nội dung áp dụng | Câu hỏi mấu chốt cần giải quyết |
| :--- | :--- | :--- |
| **[Assignment 1](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week2/Assignment1.md)** | **CAP & PACELC** | Sự cố khách bị trừ tiền 2 lần do hệ thống chọn **AP** cho dữ liệu Payment (cho phép ghi vào Replica Tokyo khi đứt mạng với Singapore). Cần chuyển sang **CP** và áp dụng `Idempotency-Key`. |
| **Assignment 2** | **Consistency Models** | Xử lý tình huống "Vừa thanh toán xong F5 thấy trạng thái cũ PENDING" (*Read-after-write inconsistency*). |
| **Assignment 3** | **Replication & Failover** | Thiết kế định tuyến Read/Write và quy trình 5 bước Failover khi Primary bị chết. |
| **Assignment 4** | **Sharding & Hot Spot** | Chọn Shard Key cho hệ thống SaaS 5 triệu Tenants và giải quyết vấn đề khách hàng lớn làm nghẽn Shard (*Hot Tenant*). |

Bạn có muốn bắt đầu bằng việc giải chi tiết và viết câu trả lời cho **[Assignment 1](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week2/Assignment1.md)** vào file `.md` không?