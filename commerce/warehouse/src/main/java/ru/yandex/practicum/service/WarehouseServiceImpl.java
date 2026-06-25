package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.exception.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.exception.ProductInShoppingCartLowQuantityInWarehouse;
import ru.yandex.practicum.exception.SpecifiedProductAlreadyInWarehouseException;
import ru.yandex.practicum.model.ProductWarehouse;
import ru.yandex.practicum.model.mapper.ProductWarehouseMapper;
import ru.yandex.practicum.repository.ProductWarehouseRepository;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final ProductWarehouseRepository warehouseRepository;

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
                    .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
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

    private double calculateVolume(ProductWarehouse product) {
        return (product.getWidth() * product.getHeight() * product.getDepth()) / 1_000_000;
    }
}