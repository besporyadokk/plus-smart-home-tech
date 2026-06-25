package ru.yandex.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.CreateNewOrderRequest;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.ProductReturnRequest;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "order", path = "/api/v1/order")
public interface OrderClient {

    @GetMapping
    ResponseEntity<List<OrderDto>> getClientOrders(@RequestParam String username);

    @PutMapping
    ResponseEntity<OrderDto> createNewOrder(@RequestParam String username, @RequestBody CreateNewOrderRequest request);

    @PostMapping("/return")
    ResponseEntity<OrderDto> productReturn(@RequestBody ProductReturnRequest request);

    @PostMapping("/payment")
    ResponseEntity<OrderDto> payment(@RequestBody UUID orderId);

    @PostMapping("/payment/failed")
    ResponseEntity<OrderDto> paymentFailed(@RequestBody UUID orderId);

    @PostMapping("/delivery")
    ResponseEntity<OrderDto> delivery(@RequestBody UUID orderId);

    @PostMapping("/delivery/failed")
    ResponseEntity<OrderDto> deliveryFailed(@RequestBody UUID orderId);

    @PostMapping("/completed")
    ResponseEntity<OrderDto> complete(@RequestBody UUID orderId);

    @PostMapping("/calculate/total")
    ResponseEntity<OrderDto> calculateTotalCost(@RequestBody UUID orderId);

    @PostMapping("/calculate/delivery")
    ResponseEntity<OrderDto> calculateDeliveryCost(@RequestBody UUID orderId);

    @PostMapping("/assembly")
    ResponseEntity<OrderDto> assembly(@RequestBody UUID orderId);

    @PostMapping("/assembly/failed")
    ResponseEntity<OrderDto> assemblyFailed(@RequestBody UUID orderId);
}