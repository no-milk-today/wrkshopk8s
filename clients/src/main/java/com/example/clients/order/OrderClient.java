package com.example.clients.order;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
        name = "order",
        url = "${clients.order.url}"
)
public interface OrderClient {
    @GetMapping("/orders/customer/{customerId}")
    List<OrderResponse> findByCustomerId(@PathVariable("customerId") Long customerId);
}
