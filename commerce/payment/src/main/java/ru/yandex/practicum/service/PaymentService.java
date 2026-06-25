package ru.yandex.practicum.service;

import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.PaymentDto;

import java.util.UUID;

public interface PaymentService {
    Double calculateProductCost(OrderDto order);

    Double calculateTotalCost(OrderDto order);

    PaymentDto createPayment(OrderDto order);

    void paymentSuccess(UUID paymentId);

    void paymentFailed(UUID paymentId);
}