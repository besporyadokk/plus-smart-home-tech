package ru.yandex.practicum.model.mapper;

import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.model.Order;
import ru.yandex.practicum.model.OrderProduct;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class OrderMapper {

    public static OrderDto toDto(Order entity, List<OrderProduct> products) {
        if (entity == null) return null;

        Map<UUID, Integer> productMap = products.stream()
                .collect(Collectors.toMap(
                        OrderProduct::getProductId,
                        OrderProduct::getQuantity
                ));

        return OrderDto.builder()
                .orderId(entity.getOrderId())
                .shoppingCartId(entity.getShoppingCartId())
                .products(productMap)
                .paymentId(entity.getPaymentId())
                .deliveryId(entity.getDeliveryId())
                .state(entity.getState())
                .deliveryWeight(entity.getDeliveryWeight())
                .deliveryVolume(entity.getDeliveryVolume())
                .fragile(entity.getFragile())
                .totalPrice(entity.getTotalPrice())
                .deliveryPrice(entity.getDeliveryPrice())
                .productPrice(entity.getProductPrice())
                .build();
    }
}