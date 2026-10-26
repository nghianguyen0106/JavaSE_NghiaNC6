# Multi-Stage Build Dockerfile for Spring Boot Application

# Stage 1: Build & Package
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace/app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Trích xuất các layer của Spring Boot để tối ưu Docker Layer Caching
RUN java -Djarmode=layertools -jar target/*.jar extract

# Stage 2: Minimal Runtime Container
FROM eclipse-temurin:17-jre-jammy AS runner
WORKDIR /application

# Tạo non-root user và group để tuân thủ nguyên tắc Least Privilege
RUN groupadd -g 10001 appgroup && \
    useradd -u 10001 -g appgroup -s /bin/bash -m appuser

# Copy các layer theo thứ tự từ ít thay đổi tới thường xuyên thay đổi nhất
COPY --from=builder /workspace/app/dependencies/ ./
COPY --from=builder /workspace/app/spring-boot-loader/ ./
COPY --from=builder /workspace/app/snapshot-dependencies/ ./
COPY --from=builder /workspace/app/application/ ./

RUN chown -R appuser:appgroup /application

USER 10001:10001

EXPOSE 8080

# Cấu hình JVM Flags tối ưu cho Container Memory & GC
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]
