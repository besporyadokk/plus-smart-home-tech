package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.exception.NoOrderFoundException;
import ru.yandex.practicum.exception.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.feign.ShoppingCartClient;
import ru.yandex.practicum.feign.WarehouseClient;
import ru.yandex.practicum.model.Order;
import ru.yandex.practicum.model.OrderProduct;
import ru.yandex.practicum.model.mapper.OrderMapper;
import ru.yandex.practicum.repository.OrderProductRepository;
import ru.yandex.practicum.repository.OrderRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final ShoppingCartClient shoppingCartClient;
    private final WarehouseClient warehouseClient;

    private static final double BASE_PRODUCT_PRICE = 100.0;
    private static final double BASE_DELIVERY_PRICE = 500.0;
    private static final double WEIGHT_PRICE_MULTIPLIER = 100.0;
    private static final double VOLUME_PRICE_MULTIPLIER = 50.0;
    private static final double FRAGILE_SURCHARGE = 200.0;

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getClientOrders(String username) {
        log.debug("Getting orders for user: {}", username);
        validateUsername(username);

        List<Order> orders = orderRepository.findByUsername(username);

        return orders.stream()
                .map(order -> {
                    List<OrderProduct> products = orderProductRepository.findByOrderId(order.getOrderId());
                    return OrderMapper.toDto(order, products);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderDto createNewOrder(String username, CreateNewOrderRequest request) {
        log.debug("Creating new order for user: {}", username);
        validateUsername(username);

        ResponseEntity<ShoppingCartDto> cartResponse = shoppingCartClient.getShoppingCart(username);
        ShoppingCartDto cart = cartResponse.getBody();

        if (cart == null || cart.getProducts().isEmpty()) {
            throw new IllegalArgumentException("Shopping cart is empty");
        }

        try {
            ResponseEntity<BookedProductsDto> checkResponse = warehouseClient.checkProductQuantityEnoughForShoppingCart(cart);
            BookedProductsDto booked = checkResponse.getBody();

            if (booked == null) {
                throw new NoSpecifiedProductInWarehouseException("Failed to check warehouse availability");
            }

            double productPrice = calculateProductPrice(cart.getProducts());
            double deliveryPrice = calculateDeliveryPrice(booked);
            double totalPrice = productPrice + deliveryPrice;

            Order order = Order.builder()
                    .shoppingCartId(cart.getShoppingCartId())
                    .username(username)
                    .paymentId(UUID.randomUUID())
                    .deliveryId(UUID.randomUUID())
                    .state(OrderState.NEW)
                    .deliveryWeight(booked.getDeliveryWeight())
                    .deliveryVolume(booked.getDeliveryVolume())
                    .fragile(booked.getFragile())
                    .productPrice(productPrice)
                    .deliveryPrice(deliveryPrice)
                    .totalPrice(totalPrice)
                    .build();

            Order savedOrder = orderRepository.save(order);

            saveOrderProducts(savedOrder.getOrderId(), cart.getProducts());

            shoppingCartClient.deactivateCurrentShoppingCart(username);

            log.info("Order created successfully: {}", savedOrder.getOrderId());

            List<OrderProduct> products = orderProductRepository.findByOrderId(savedOrder.getOrderId());
            return OrderMapper.toDto(savedOrder, products);

        } catch (Exception e) {
            log.error("Failed to create order: {}", e.getMessage());
            throw new NoSpecifiedProductInWarehouseException("Products not available in warehouse: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public OrderDto productReturn(ProductReturnRequest request) {
        log.debug("Returning products for order: {}", request.getOrderId());

        Order order = getOrderById(request.getOrderId());
        order.setState(OrderState.PRODUCT_RETURNED);
        Order updatedOrder = orderRepository.save(order);

        return getOrderDto(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDto payment(UUID orderId) {
        log.debug("Processing payment for order: {}", orderId);

        Order order = getOrderById(orderId);
        order.setState(OrderState.PAID);
        Order updatedOrder = orderRepository.save(order);

        return getOrderDto(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDto paymentFailed(UUID orderId) {
        log.debug("Payment failed for order: {}", orderId);

        Order order = getOrderById(orderId);
        order.setState(OrderState.PAYMENT_FAILED);
        Order updatedOrder = orderRepository.save(order);

        return getOrderDto(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDto delivery(UUID orderId) {
        log.debug("Processing delivery for order: {}", orderId);

        Order order = getOrderById(orderId);
        order.setState(OrderState.DELIVERED);
        Order updatedOrder = orderRepository.save(order);

        return getOrderDto(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDto deliveryFailed(UUID orderId) {
        log.debug("Delivery failed for order: {}", orderId);

        Order order = getOrderById(orderId);
        order.setState(OrderState.DELIVERY_FAILED);
        Order updatedOrder = orderRepository.save(order);

        return getOrderDto(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDto assembly(UUID orderId) {
        log.debug("Processing assembly for order: {}", orderId);

        Order order = getOrderById(orderId);
        order.setState(OrderState.ASSEMBLED);
        Order updatedOrder = orderRepository.save(order);

        return getOrderDto(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDto assemblyFailed(UUID orderId) {
        log.debug("Assembly failed for order: {}", orderId);

        Order order = getOrderById(orderId);
        order.setState(OrderState.ASSEMBLY_FAILED);
        Order updatedOrder = orderRepository.save(order);

        return getOrderDto(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDto complete(UUID orderId) {
        log.debug("Completing order: {}", orderId);

        Order order = getOrderById(orderId);
        order.setState(OrderState.COMPLETED);
        Order updatedOrder = orderRepository.save(order);

        return getOrderDto(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDto calculateTotalCost(UUID orderId) {
        log.debug("Calculating total cost for order: {}", orderId);

        Order order = getOrderById(orderId);
        order.setTotalPrice(order.getProductPrice() + order.getDeliveryPrice());
        Order updatedOrder = orderRepository.save(order);

        return getOrderDto(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDto calculateDeliveryCost(UUID orderId) {
        log.debug("Calculating delivery cost for order: {}", orderId);

        Order order = getOrderById(orderId);

        BookedProductsDto booked = BookedProductsDto.builder()
                .deliveryWeight(order.getDeliveryWeight())
                .deliveryVolume(order.getDeliveryVolume())
                .fragile(order.getFragile())
                .build();

        order.setDeliveryPrice(calculateDeliveryPrice(booked));
        order.setTotalPrice(order.getProductPrice() + order.getDeliveryPrice());
        Order updatedOrder = orderRepository.save(order);

        return getOrderDto(updatedOrder);
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
    }

    private Order getOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order not found: " + orderId));
    }

    private OrderDto getOrderDto(Order order) {
        List<OrderProduct> products = orderProductRepository.findByOrderId(order.getOrderId());
        return OrderMapper.toDto(order, products);
    }

    private void saveOrderProducts(UUID orderId, Map<UUID, Integer> products) {
        for (Map.Entry<UUID, Integer> entry : products.entrySet()) {
            OrderProduct orderProduct = OrderProduct.builder()
                    .orderId(orderId)
                    .productId(entry.getKey())
                    .quantity(entry.getValue())
                    .build();
            orderProductRepository.save(orderProduct);
        }
    }

    private double calculateProductPrice(Map<UUID, Integer> products) {
        return products.values().stream()
                .mapToDouble(Integer::doubleValue)
                .sum() * BASE_PRODUCT_PRICE;
    }

    private double calculateDeliveryPrice(BookedProductsDto booked) {
        double deliveryPrice = BASE_DELIVERY_PRICE;

        if (booked.getDeliveryWeight() != null) {
            deliveryPrice += booked.getDeliveryWeight() * WEIGHT_PRICE_MULTIPLIER;
        }

        if (booked.getDeliveryVolume() != null) {
            deliveryPrice += booked.getDeliveryVolume() * VOLUME_PRICE_MULTIPLIER;
        }

        if (isFragile(booked)) {
            deliveryPrice += FRAGILE_SURCHARGE;
        }

        return deliveryPrice;
    }

    private boolean isFragile(BookedProductsDto booked) {
        return booked.getFragile() != null && booked.getFragile();
    }
}