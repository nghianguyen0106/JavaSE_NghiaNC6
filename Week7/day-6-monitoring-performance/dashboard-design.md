# Observability Dashboard Specification: Grafana

## 1. Business Health Dashboard (Executive / Product Tier)
- Panel 1 (Single Stat): Total Successful Checkouts (Today vs Yesterday).
- Panel 2 (Time Series): Checkout Conversion Rate ($\frac{\text{Successful Checkouts}}{\text{Total Checkout Initiations}} \times 100\%$).
- Panel 3 (Gauge): Orders in `PAYMENT_PROCESSING` state for $> 10\text{ minutes}$.
- Panel 4 (Bar Chart): Checkout Failure Breakdown (Out of stock, Insufficient balance, Gateway timeout).

---

## 2. API Performance Dashboard (Engineering Tier)
- Panel 1 (Time Series - Throughput): Requests per second (RPS) per HTTP Endpoint (`http_server_requests_seconds_count`).
- Panel 2 (Time Series - Latency Percentiles): Latency Percentiles P50, P90, P95, P99 của endpoint `POST /api/orders/checkout`.
- Panel 3 (Stacked Area - Errors): HTTP Status Code Distribution (2xx, 4xx, 5xx).

---

## 3. Dependency & Infrastructure Dashboard (Ops Tier)
- Panel 1 (Time Series): Outgoing HTTP Client Latency (OpenFeign calls to Inventory & Payment).
- Panel 2 (State Timeline): Circuit Breaker State per Service (Green = CLOSED, Yellow = HALF_OPEN, Red = OPEN).
- Panel 3 (Time Series): HikariCP Database Connections (Active, Idle, Pending Threads).
- Panel 4 (Time Series): Kafka Consumer Lag per Topic/Partition (`kafka_consumergroup_lag`).
- Panel 5 (Graph): JVM Heap Used vs Max, kèm các sự kiện GC Pause (`jvm_gc_pause_seconds_count`).
