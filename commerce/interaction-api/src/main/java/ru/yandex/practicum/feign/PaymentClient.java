package ru.yandex.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.PaymentDto;

import java.util.UUID;

@FeignClient(name = "payment", path = "/api/v1/payment")
public interface PaymentClient {

    @PostMapping
    ResponseEntity<PaymentDto> payment(@RequestBody OrderDto order);

    @PostMapping("/totalCost")
    ResponseEntity<Double> getTotalCost(@RequestBody OrderDto order);

    @PostMapping("/productCost")
    ResponseEntity<Double> productCost(@RequestBody OrderDto order);

    @PostMapping("/refund")
    ResponseEntity<Void> paymentSuccess(@RequestBody UUID paymentId);

    @PostMapping("/failed")
    ResponseEntity<Void> paymentFailed(@RequestBody UUID paymentId);
}