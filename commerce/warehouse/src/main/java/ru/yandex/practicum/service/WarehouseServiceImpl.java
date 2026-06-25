package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.exception.*;
import ru.yandex.practicum.model.OrderBooking;
import ru.yandex.practicum.model.ProductWarehouse;
import ru.yandex.practicum.model.mapper.ProductWarehouseMapper;
import ru.yandex.practicum.repository.OrderBookingRepository;
import ru.yandex.practicum.repository.ProductWarehouseRepository;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final ProductWarehouseRepository warehouseRepository;
    private final OrderBookingRepository bookingRepository;

    private static final AddressDto WAREHOUSE_ADDRESS = new AddressDto(
            "Россия", "Москва", "ул. Складская", "15", "42"
    );

    @Override
    @Transactional
    public void newProductInWarehouse(NewProductInWarehouseRequest request) {
        log.debug("Adding new product to warehouse: {}", request.getProductId());

        if (warehouseRepository.existsByProductId(request.getProductId())) {
            throw new SpecifiedProductAlreadyInWarehouseException(
                    "Product already registered in warehouse: " + request.getProductId()
            );
        }

        ProductWarehouse product = ProductWarehouseMapper.toEntity(request);
        warehouseRepository.save(product);

        log.info("New product {} added to warehouse", request.getProductId());
    }

    @Override
    @Transactional(readOnly = true)
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto shoppingCart) {
        log.debug("Checking warehouse availability for cart: {}", shoppingCart.getShoppingCartId());

        double totalWeight = 0.0;
        double totalVolume = 0.0;
        boolean hasFragile = false;

        for (Map.Entry<UUID, Integer> entry : shoppingCart.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Integer requestedQuantity = entry.getValue();

            ProductWarehouse product = warehouseRepository.findByProductId(productId)
                    .orElseThrow(() -> new ProductInShoppingCartNotInWarehouse(
                            "Product not found in warehouse: " + productId
                    ));

            if (product.getQuantity() < requestedQuantity) {
                throw new ProductInShoppingCartLowQuantityInWarehouse(
                        String.format("Insufficient quantity for product %s. Available: %d, Requested: %d",
                                productId, product.getQuantity(), requestedQuantity)
                );
            }

            double volume = calculateVolume(product);
            totalVolume += volume * requestedQuantity;
            totalWeight += product.getWeight() * requestedQuantity;

            if (product.getFragile()) {
                hasFragile = true;
            }
        }

        return new BookedProductsDto(totalWeight, totalVolume, hasFragile);
    }

    @Override
    @Transactional
    public void addProductToWarehouse(AddProductToWarehouseRequest request) {
        log.debug("Adding product quantity to warehouse: productId={}, quantity={}",
                request.getProductId(), request.getQuantity());

        ProductWarehouse product = warehouseRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                        "Product not found in warehouse: " + request.getProductId()
                ));

        product.setQuantity(product.getQuantity() + request.getQuantity());
        warehouseRepository.save(product);

        log.info("Added {} units of product {} to warehouse. New quantity: {}",
                request.getQuantity(), request.getProductId(), product.getQuantity());
    }

    @Override
    public AddressDto getWarehouseAddress() {
        log.debug("Getting warehouse address");
        return WAREHOUSE_ADDRESS;
    }

    @Override
    @Transactional
    public BookedProductsDto assemblyProductsForOrder(AssemblyProductsForOrderRequest request) {
        log.debug("Assembling products for order: {}", request.getOrderId());

        if (bookingRepository.existsByOrderId(request.getOrderId())) {
            throw new RuntimeException("Order already assembled: " + request.getOrderId());
        }

        double totalWeight = 0.0;
        double totalVolume = 0.0;
        boolean hasFragile = false;

        for (Map.Entry<UUID, Integer> entry : request.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Integer requestedQuantity = entry.getValue();

            ProductWarehouse product = warehouseRepository.findByProductId(productId)
                    .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                            "Product not found in warehouse: " + productId
                    ));

            if (product.getQuantity() < requestedQuantity) {
                throw new ProductInShoppingCartLowQuantityInWarehouse(
                        String.format("Insufficient quantity for product %s. Available: %d, Requested: %d",
                                productId, product.getQuantity(), requestedQuantity)
                );
            }

            product.setQuantity(product.getQuantity() - requestedQuantity);
            warehouseRepository.save(product);

            double volume = calculateVolume(product);
            totalVolume += volume * requestedQuantity;
            totalWeight += product.getWeight() * requestedQuantity;

            if (product.getFragile()) {
                hasFragile = true;
            }
        }

        OrderBooking booking = OrderBooking.builder()
                .bookingId(UUID.randomUUID())
                .orderId(request.getOrderId())
                .products(request.getProducts())
                .build();

        bookingRepository.save(booking);
        log.info("Products assembled for order: {}, booking: {}",
                request.getOrderId(), booking.getBookingId());

        return new BookedProductsDto(totalWeight, totalVolume, hasFragile);
    }

    @Override
    @Transactional
    public void shippedToDelivery(ShippedToDeliveryRequest request) {
        log.debug("Shipping products to delivery for order: {}", request.getOrderId());

        OrderBooking booking = bookingRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new RuntimeException(
                        "Order booking not found for order: " + request.getOrderId()
                ));

        booking.setDeliveryId(request.getDeliveryId());
        bookingRepository.save(booking);

        log.info("Products shipped to delivery: order={}, delivery={}",
                request.getOrderId(), request.getDeliveryId());
    }

    @Override
    @Transactional
    public void acceptReturn(Map<UUID, Integer> products) {
        log.debug("Accepting return of products: {}", products);

        for (Map.Entry<UUID, Integer> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            Integer quantity = entry.getValue();

            ProductWarehouse product = warehouseRepository.findByProductId(productId)
                    .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                            "Product not found in warehouse: " + productId
                    ));

            product.setQuantity(product.getQuantity() + quantity);
            warehouseRepository.save(product);

            log.debug("Returned {} units of product {}. New quantity: {}",
                    quantity, productId, product.getQuantity());
        }

        log.info("Products returned successfully");
    }

    private double calculateVolume(ProductWarehouse product) {
        return (product.getWidth() * product.getHeight() * product.getDepth()) / 1_000_000;
    }
}