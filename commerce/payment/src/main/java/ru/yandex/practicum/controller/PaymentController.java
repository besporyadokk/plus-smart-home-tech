package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.PaymentDto;
import ru.yandex.practicum.feign.PaymentClient;
import ru.yandex.practicum.service.PaymentService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController implements PaymentClient {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentDto> payment(@Valid @RequestBody OrderDto order) {
        log.debug("POST /api/v1/payment - orderId: {}", order.getOrderId());
        PaymentDto payment = paymentService.createPayment(order);
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/totalCost")
    public ResponseEntity<Double> getTotalCost(@Valid @RequestBody OrderDto order) {
        log.debug("POST /api/v1/payment/totalCost - orderId: {}", order.getOrderId());
        Double totalCost = paymentService.calculateTotalCost(order);
        return ResponseEntity.ok(totalCost);
    }

    @PostMapping("/productCost")
    public ResponseEntity<Double> productCost(@Valid @RequestBody OrderDto order) {
        log.debug("POST /api/v1/payment/productCost - orderId: {}", order.getOrderId());
        Double productCost = paymentService.calculateProductCost(order);
        return ResponseEntity.ok(productCost);
    }

    @PostMapping("/refund")
    public ResponseEntity<Void> paymentSuccess(@RequestBody UUID paymentId) {
        log.debug("POST /api/v1/payment/refund - paymentId: {}", paymentId);
        paymentService.paymentSuccess(paymentId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/failed")
    public ResponseEntity<Void> paymentFailed(@RequestBody UUID paymentId) {
        log.debug("POST /api/v1/payment/failed - paymentId: {}", paymentId);
        paymentService.paymentFailed(paymentId);
        return ResponseEntity.ok().build();
    }
}