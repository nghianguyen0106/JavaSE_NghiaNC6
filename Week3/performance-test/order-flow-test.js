import http from "k6/http";
import { check, sleep, group } from "k6";

// 1. Cấu hình Stages (Tải) và Thresholds (Tiêu chí Pass/Fail)
export const options = {
  stages: [
    { duration: "1m", target: 50 },  // Ramp-up: 0 lên 50 VUs trong 1 phút
    { duration: "3m", target: 50 },  // Steady State: Giữ ổn định 50 VUs trong 3 phút
    { duration: "30s", target: 0 },  // Ramp-down: Hạ dần từ 50 về 0 VU trong 30 giây
  ],
  thresholds: {
    // Tổng tỷ lệ request lỗi toàn bộ test phải < 1%
    http_req_failed: ["rate<0.01"],
    // p95 của API GET product list phải < 300ms
    "http_req_duration{endpoint:get-product-list}": ["p(95)<300"],
    // p95 của API POST create order phải < 800ms
    "http_req_duration{endpoint:create-order}": ["p(95)<800"],
    // p95 của API GET chi tiết đơn hàng < 400ms
    "http_req_duration{endpoint:get-order-detail}": ["p(95)<400"],
  },
};

const BASE_URL = __ENV.BASE_URL || "https://test.example.com";

// Danh sách dữ liệu test chuẩn bị sẵn để tránh cache giả lập & tránh xung đột ID
const TEST_PRODUCTS = ["P-101", "P-102", "P-103", "P-104", "P-105"];

export default function () {
  const commonHeaders = {
    "Content-Type": "application/json",
    Authorization: "Bearer mock-jwt-token-vu-" + __VU,
  };

  let selectedProductId = "P-101";
  let createdOrderId = null;

  // Step 1: User xem danh sách sản phẩm (GET /api/products?page=1&pageSize=20)
  group("Step 1 - View Product List", function () {
    const resList = http.get(`${BASE_URL}/api/products?page=1&pageSize=20`, {
      headers: commonHeaders,
      tags: { endpoint: "get-product-list" },
    });

    const isSuccess = check(resList, {
      "product list status is 200": (r) => r.status === 200,
      "product list has items": (r) => {
        try {
          const body = JSON.parse(r.body);
          return Array.isArray(body.items) && body.items.length > 0;
        } catch (e) {
          return false;
        }
      },
    });

    if (isSuccess) {
      try {
        const body = JSON.parse(resList.body);
        if (body.items && body.items.length > 0) {
          // Lấy ngẫu nhiên 1 productId từ danh sách thực trả về
          selectedProductId = body.items[Math.floor(Math.random() * body.items.length)].id;
        }
      } catch (e) {}
    }

    // Giả lập Think Time người dùng lướt xem danh sách: 1 - 2 giây
    sleep(1.5);
  });

  // Step 2: User xem chi tiết một sản phẩm (GET /api/products/{id})
  group("Step 2 - View Product Detail", function () {
    const resDetail = http.get(`${BASE_URL}/api/products/${selectedProductId}`, {
      headers: commonHeaders,
      tags: { endpoint: "get-product-detail" },
    });

    check(resDetail, {
      "product detail status is 200": (r) => r.status === 200,
      "product detail has valid price": (r) => {
        try {
          const b = JSON.parse(r.body);
          return b.price > 0 && b.available === true;
        } catch (e) {
          return false;
        }
      },
    });

    // Think Time người dùng đọc mô tả sản phẩm: 2 giây
    sleep(2);
  });

  // Step 3: User tạo đơn hàng (POST /api/orders)
  group("Step 3 - Create Order", function () {
    const orderPayload = JSON.stringify({
      customerId: `customer-vu-${__VU}`,
      idempotencyKey: `idemp-${__VU}-${__ITER}-${Date.now()}`,
      items: [
        {
          productId: selectedProductId,
          quantity: 1,
        },
      ],
      paymentMethod: "CREDIT_CARD",
    });

    const resOrder = http.post(`${BASE_URL}/api/orders`, orderPayload, {
      headers: commonHeaders,
      tags: { endpoint: "create-order" },
    });

    const isCreated = check(resOrder, {
      "create order status is 200 or 201": (r) => r.status === 200 || r.status === 201,
      "order returns orderId": (r) => {
        try {
          const b = JSON.parse(r.body);
          return b.id !== undefined && b.id !== null;
        } catch (e) {
          return false;
        }
      },
    });

    if (isCreated) {
      try {
        createdOrderId = JSON.parse(resOrder.body).id;
      } catch (e) {}
    }

    // Think Time sau khi đặt hàng: 1 giây
    sleep(1);
  });

  // Step 4: User kiểm tra trạng thái đơn hàng vừa tạo (GET /api/orders/{id})
  if (createdOrderId) {
    group("Step 4 - Check Created Order Status", function () {
      const resCheck = http.get(`${BASE_URL}/api/orders/${createdOrderId}`, {
        headers: commonHeaders,
        tags: { endpoint: "get-order-detail" },
      });

      check(resCheck, {
        "get order status is 200": (r) => r.status === 200,
        "order status is PENDING or CONFIRMED": (r) => {
          try {
            const b = JSON.parse(r.body);
            return b.status === "PENDING" || b.status === "CONFIRMED";
          } catch (e) {
            return false;
          }
        },
      });

      sleep(1);
    });
  }
}
