package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.DeliveryDto;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.service.DeliveryService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PutMapping
    public ResponseEntity<DeliveryDto> planDelivery(@Valid @RequestBody DeliveryDto deliveryDto) {
        log.debug("PUT /api/v1/delivery - orderId: {}", deliveryDto.getOrderId());
        DeliveryDto result = deliveryService.planDelivery(deliveryDto);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/cost")
    public ResponseEntity<Double> deliveryCost(@Valid @RequestBody OrderDto order) {
        log.debug("POST /api/v1/delivery/cost - orderId: {}", order.getOrderId());
        Double cost = deliveryService.calculateDeliveryCost(order);
        return ResponseEntity.ok(cost);
    }

    @PostMapping("/picked")
    public ResponseEntity<Void> deliveryPicked(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/delivery/picked - orderId: {}", orderId);
        deliveryService.deliveryPicked(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/successful")
    public ResponseEntity<Void> deliverySuccessful(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/delivery/successful - orderId: {}", orderId);
        deliveryService.deliverySuccessful(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/failed")
    public ResponseEntity<Void> deliveryFailed(@RequestBody UUID orderId) {
        log.debug("POST /api/v1/delivery/failed - orderId: {}", orderId);
        deliveryService.deliveryFailed(orderId);
        return ResponseEntity.ok().build();
    }
}