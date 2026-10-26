# Day 6: Monitoring, Performance & Observability Best Practices

## 1. Tổng Kết Các Thành Phần Đã Hoàn Thành
1. [observability-design.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week7/day-6-monitoring-performance/observability-design.md): Thiết kế toàn diện 6 trụ cột: Logs, Metrics, Traces, Dashboard, Alerts, Performance Risks.
2. [custom-metrics.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week7/day-6-monitoring-performance/custom-metrics.md): Mã nguồn Micrometer `CheckoutMetrics` kèm nguyên tắc triệt tiêu High Cardinality.
3. [dashboard-design.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week7/day-6-monitoring-performance/dashboard-design.md): Đặc tả các panels trên Grafana theo 3 tầng: Business, API, Infrastructure/Dependencies.
4. [alert-runbook.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week7/day-6-monitoring-performance/alert-runbook.md): PromQL Rules và quy trình ứng phó sự cố (First-Response Runbooks) có tính hành động thực tiễn.
5. [incident-analysis.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week7/day-6-monitoring-performance/incident-analysis.md): Lời giải chi tiết cho Incident Drill (Inventory Service chậm 3 giây).
6. [application-observability.yml](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week7/day-6-monitoring-performance/application-observability.yml): File cấu hình Spring Boot Actuator, Prometheus và OpenTelemetry Tracing.

---

## 2. Tiêu Chí Nghiệm Thu (DoD Checklist)
- [x] Phân biệt rõ ràng Logs (diễn biến sự kiện), Metrics (chuỗi thời gian tổng hợp), Traces (đường đi request phân tán).
- [x] Tích hợp endpoint Prometheus và Actuator Probes chuẩn container.
- [x] Phân định rạch ròi Technical Metrics và Business Metrics.
- [x] Loại bỏ hoàn toàn High Cardinality Tags khỏi hệ thống đo lường Micrometer.
- [x] Duy trì Correlation ID xuyên suốt qua HTTP Headers và Kafka Headers.
- [x] Đưa ra giải pháp phân tích sự cố (Incident Drill) dựa trên bằng chứng kỹ thuật (Evidence-driven), không phỏng đoán cảm tính.
- [x] Nhận diện 3 Performance Anti-patterns và đề xuất phương án khắc phục triệt để.
