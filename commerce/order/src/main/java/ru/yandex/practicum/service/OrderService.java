package ru.yandex.practicum.service;


import ru.yandex.practicum.dto.CreateNewOrderRequest;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.ProductReturnRequest;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    List<OrderDto> getClientOrders(String username);
    OrderDto createNewOrder(String username, CreateNewOrderRequest request);
    OrderDto productReturn(ProductReturnRequest request);
    OrderDto payment(UUID orderId);
    OrderDto paymentFailed(UUID orderId);
    OrderDto delivery(UUID orderId);
    OrderDto deliveryFailed(UUID orderId);
    OrderDto assembly(UUID orderId);
    OrderDto assemblyFailed(UUID orderId);
    OrderDto complete(UUID orderId);
    OrderDto calculateTotalCost(UUID orderId);
    OrderDto calculateDeliveryCost(UUID orderId);
}