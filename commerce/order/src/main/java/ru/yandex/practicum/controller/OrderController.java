package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.CreateNewOrderRequest;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.ProductReturnRequest;
import ru.yandex.practicum.feign.OrderClient;
import ru.yandex.practicum.service.OrderService;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderController implements OrderClient {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderDto>> getClientOrders(@RequestParam String username) {
        log.debug("GET /api/v1/order - username: {}", username);
        List<OrderDto> orders = orderService.getClientOrders(username);
        return ResponseEntity.ok(orders);
    }

    @PutMapping
    public ResponseEntity<OrderDto> createNewOrder(
            @RequestParam String username,
            @Valid @RequestBody CreateNewOrderRequest request) {
        log.debug("PUT /api/v1/order - username: {}, request: {}", username, request);
        OrderDto order = orderService.createNewOrder(username, request);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/return")
    public ResponseEntity<OrderDto> productReturn(@Valid @RequestBody ProductReturnRequest request) {
        log.debug("POST /api/v1/order/return - request: {}", request);
        OrderDto order = orderService.productReturn(request);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/payment")
    public ResponseEntity<OrderDto> payment(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/order/payment - orderId: {}", orderId);
        OrderDto order = orderService.payment(orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/payment/failed")
    public ResponseEntity<OrderDto> paymentFailed(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/order/payment/failed - orderId: {}", orderId);
        OrderDto order = orderService.paymentFailed(orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/delivery")
    public ResponseEntity<OrderDto> delivery(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/order/delivery - orderId: {}", orderId);
        OrderDto order = orderService.delivery(orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/delivery/failed")
    public ResponseEntity<OrderDto> deliveryFailed(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/order/delivery/failed - orderId: {}", orderId);
        OrderDto order = orderService.deliveryFailed(orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/completed")
    public ResponseEntity<OrderDto> complete(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/order/completed - orderId: {}", orderId);
        OrderDto order = orderService.complete(orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/calculate/total")
    public ResponseEntity<OrderDto> calculateTotalCost(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/order/calculate/total - orderId: {}", orderId);
        OrderDto order = orderService.calculateTotalCost(orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/calculate/delivery")
    public ResponseEntity<OrderDto> calculateDeliveryCost(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/order/calculate/delivery - orderId: {}", orderId);
        OrderDto order = orderService.calculateDeliveryCost(orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/assembly")
    public ResponseEntity<OrderDto> assembly(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/order/assembly - orderId: {}", orderId);
        OrderDto order = orderService.assembly(orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/assembly/failed")
    public ResponseEntity<OrderDto> assemblyFailed(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/order/assembly/failed - orderId: {}", orderId);
        OrderDto order = orderService.assemblyFailed(orderId);
        return ResponseEntity.ok(order);
    }
}