package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.feign.OrderClient;
import ru.yandex.practicum.feign.WarehouseClient;
import ru.yandex.practicum.model.Delivery;
import ru.yandex.practicum.model.mapper.DeliveryMapper;
import ru.yandex.practicum.repository.DeliveryRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderClient orderClient;
    private final WarehouseClient warehouseClient;

    private static final double BASE_COST = 5.0;
    private static final double WEIGHT_MULTIPLIER = 0.3;
    private static final double VOLUME_MULTIPLIER = 0.2;
    private static final double FRAGILE_MULTIPLIER = 0.2;
    private static final double ADDRESS_MISMATCH_MULTIPLIER = 0.2;
    private static final int ADDRESS_1_MULTIPLIER = 1;
    private static final int ADDRESS_2_MULTIPLIER = 2;

    private static final String WAREHOUSE_ADDRESS_1 = "ADDRESS_1";
    private static final String WAREHOUSE_ADDRESS_2 = "ADDRESS_2";

    @Override
    @Transactional
    public DeliveryDto planDelivery(DeliveryDto deliveryDto) {
        log.debug("Planning delivery for order: {}", deliveryDto.getOrderId());

        Delivery delivery = DeliveryMapper.toEntity(deliveryDto);
        Delivery saved = deliveryRepository.save(delivery);
        log.info("Delivery planned: {} for order: {}", saved.getDeliveryId(), saved.getOrderId());
        return DeliveryMapper.toDto(saved);
    }

    @Override
    public Double calculateDeliveryCost(OrderDto order) {
        log.debug("Calculating delivery cost for order: {}", order.getOrderId());

        AddressDto warehouseAddress = warehouseClient.getWarehouseAddress().getBody();
        if (warehouseAddress == null) {
            throw new RuntimeException("Warehouse address not found");
        }

        double cost = BASE_COST;

        cost = applyWarehouseAddressMultiplier(cost, warehouseAddress);
        cost = applyFragileMultiplier(cost, order);
        cost = applyWeightMultiplier(cost, order);
        cost = applyVolumeMultiplier(cost, order);
        cost = applyDeliveryAddressMultiplier(cost, order, warehouseAddress);

        log.debug("Calculated delivery cost: {}", cost);
        return cost;
    }

    @Override
    @Transactional
    public void deliveryPicked(UUID orderId) {
        log.debug("Delivery picked for order: {}", orderId);

        Delivery delivery = getDeliveryByOrderId(orderId);
        updateDeliveryState(delivery, DeliveryState.IN_PROGRESS);

        orderClient.delivery(orderId);
        notifyWarehouseAboutShipping(orderId, delivery.getDeliveryId());

        log.info("Delivery picked: {} for order: {}", delivery.getDeliveryId(), orderId);
    }

    @Override
    @Transactional
    public void deliverySuccessful(UUID orderId) {
        log.debug("Delivery successful for order: {}", orderId);

        Delivery delivery = getDeliveryByOrderId(orderId);
        updateDeliveryState(delivery, DeliveryState.DELIVERED);
        orderClient.delivery(orderId);

        log.info("Delivery successful: {} for order: {}", delivery.getDeliveryId(), orderId);
    }

    @Override
    @Transactional
    public void deliveryFailed(UUID orderId) {
        log.debug("Delivery failed for order: {}", orderId);

        Delivery delivery = getDeliveryByOrderId(orderId);
        updateDeliveryState(delivery, DeliveryState.FAILED);
        orderClient.deliveryFailed(orderId);

        log.info("Delivery failed: {} for order: {}", delivery.getDeliveryId(), orderId);
    }

    private double applyWarehouseAddressMultiplier(double cost, AddressDto warehouseAddress) {
        String street = warehouseAddress.getStreet();
        int multiplier = getWarehouseMultiplier(street);
        return cost + (BASE_COST * multiplier);
    }

    private int getWarehouseMultiplier(String street) {
        if (WAREHOUSE_ADDRESS_1.equals(street)) {
            return ADDRESS_1_MULTIPLIER;
        } else if (WAREHOUSE_ADDRESS_2.equals(street)) {
            return ADDRESS_2_MULTIPLIER;
        }
        return 0;
    }

    private double applyFragileMultiplier(double cost, OrderDto order) {
        if (isFragile(order)) {
            return cost + (cost * FRAGILE_MULTIPLIER);
        }
        return cost;
    }

    private boolean isFragile(OrderDto order) {
        return order.getFragile() != null && order.getFragile();
    }

    private double applyWeightMultiplier(double cost, OrderDto order) {
        if (order.getDeliveryWeight() != null) {
            return cost + (order.getDeliveryWeight() * WEIGHT_MULTIPLIER);
        }
        return cost;
    }

    private double applyVolumeMultiplier(double cost, OrderDto order) {
        if (order.getDeliveryVolume() != null) {
            return cost + (order.getDeliveryVolume() * VOLUME_MULTIPLIER);
        }
        return cost;
    }

    private double applyDeliveryAddressMultiplier(double cost, OrderDto order, AddressDto warehouseAddress) {
        if (order.getDeliveryAddress() == null) {
            return cost;
        }

        String deliveryStreet = order.getDeliveryAddress().getStreet();
        String warehouseStreet = warehouseAddress.getStreet();

        if (!warehouseStreet.equals(deliveryStreet)) {
            return cost + (cost * ADDRESS_MISMATCH_MULTIPLIER);
        }
        return cost;
    }

    private Delivery getDeliveryByOrderId(UUID orderId) {
        return deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Delivery not found for order: " + orderId));
    }

    private void updateDeliveryState(Delivery delivery, DeliveryState state) {
        delivery.setDeliveryState(state);
        deliveryRepository.save(delivery);
    }

    private void notifyWarehouseAboutShipping(UUID orderId, UUID deliveryId) {
        ShippedToDeliveryRequest request = ShippedToDeliveryRequest.builder()
                .orderId(orderId)
                .deliveryId(deliveryId)
                .build();
        warehouseClient.shippedToDelivery(request);
    }
}