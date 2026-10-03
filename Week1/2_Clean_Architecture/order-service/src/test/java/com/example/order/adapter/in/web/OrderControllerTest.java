package com.example.order.adapter.in.web;

import com.example.order.application.dto.OrderResponse;
import com.example.order.application.port.in.CreateOrderUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@DisplayName("Adapter Layer - OrderController WebMvc Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateOrderUseCase createOrderUseCase;

    @Test
    @DisplayName("Gửi POST /api/orders hợp lệ trả về HTTP 201 Created và dữ liệu OrderResponse")
    void testCreateOrderEndpoint() throws Exception {
        OrderResponse response = new OrderResponse("ORD123", 200.0, "CONFIRMED");
        when(createOrderUseCase.execute(any())).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "orderId": "ORD123",
                                "items": [
                                    {"productId": "P1", "quantity": 2, "price": 100.0}
                                ]
                            }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("ORD123"))
                .andExpect(jsonPath("$.total").value(200.0))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
