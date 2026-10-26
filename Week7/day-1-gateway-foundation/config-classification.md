# Externalized Configuration Classification & Management

## 1. Phân Loại Cấu Hình Hệ Thống

| Phân loại | Định nghĩa kỹ thuật | Ví dụ cụ thể | Nơi lưu trữ (Storage) | Commit Git? |
| :--- | :--- | :--- | :--- | :---: |
| Application Config | Tham số phi nhạy cảm, định hình hành vi runtime nội bộ của ứng dụng | `server.port`, `feign.client.config.default.readTimeout`, `spring.jpa.open-in-view` | `application.yml` trong repository | CÓ |
| Environment Config | Tham số định tuyến hạ tầng thay đổi theo từng môi trường triển khai (Dev / Staging / Prod) | `spring.datasource.url`, `spring.kafka.bootstrap-servers`, `eureka.client.serviceUrl` | Kubernetes ConfigMap / Spring Cloud Config Server | CÓ (Tách theo branch / profile) |
| Secrets / Credentials | Dữ liệu nhạy cảm xác thực danh tính, khóa mật mã, quyền truy cập | Database password, JWT Private Key, API Secret Key, SSL Keystore password | HashiCorp Vault / Kubernetes Secret / AWS Secrets Manager | TUYỆT ĐỐI KHÔNG |
| Feature Flags | Cờ bật/tắt chức năng động theo thời gian thực không cần redeploy | `features.payment.v2.enabled`, `features.fraud-check.strict-mode` | Unleash / LaunchDarkly / Redis / Config Server Spring Cloud Bus | CÓ (Chỉ commit default values) |

---

## 2. Mô Hình Nạp Cấu Hình Ngoài (Spring Cloud Config Client)

Sử dụng cơ chế `spring.config.import` chuẩn từ Spring Boot 2.4+:

```yaml
# application.yml của downstream service
spring:
  application:
    name: order-service
  config:
    import: "optional:configserver:${CONFIG_SERVER_URL:http://config-server:8888}"
  cloud:
    config:
      fail-fast: true
      retry:
        max-attempts: 5
        initial-interval: 1500
```
