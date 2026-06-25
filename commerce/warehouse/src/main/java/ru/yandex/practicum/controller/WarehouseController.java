package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.feign.WarehouseClient;
import ru.yandex.practicum.service.WarehouseService;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseController implements WarehouseClient {

    private final WarehouseService warehouseService;

    @PutMapping
    public ResponseEntity<Void> newProductInWarehouse(
            @Valid @RequestBody NewProductInWarehouseRequest request) {

        warehouseService.newProductInWarehouse(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/check")
    public ResponseEntity<BookedProductsDto> checkProductQuantityEnoughForShoppingCart(
            @Valid @RequestBody ShoppingCartDto shoppingCart) {

        BookedProductsDto result = warehouseService.checkProductQuantityEnoughForShoppingCart(shoppingCart);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/add")
    public ResponseEntity<Void> addProductToWarehouse(
            @Valid @RequestBody AddProductToWarehouseRequest request) {

        warehouseService.addProductToWarehouse(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/address")
    public ResponseEntity<AddressDto> getWarehouseAddress() {

        AddressDto address = warehouseService.getWarehouseAddress();
        return ResponseEntity.ok(address);
    }

    @PostMapping("/assembly")
    public ResponseEntity<BookedProductsDto> assemblyProductsForOrder(
            @Valid @RequestBody AssemblyProductsForOrderRequest request) {

        log.debug("POST /api/v1/warehouse/assembly - orderId: {}", request.getOrderId());
        BookedProductsDto result = warehouseService.assemblyProductsForOrder(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/shipped")
    public ResponseEntity<Void> shippedToDelivery(
            @Valid @RequestBody ShippedToDeliveryRequest request) {

        log.debug("POST /api/v1/warehouse/shipped - orderId: {}, deliveryId: {}",
                request.getOrderId(), request.getDeliveryId());
        warehouseService.shippedToDelivery(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/return")
    public ResponseEntity<Void> acceptReturn(@RequestBody Map<UUID, Integer> products) {

        log.debug("POST /api/v1/warehouse/return - products: {}", products);
        warehouseService.acceptReturn(products);
        return ResponseEntity.ok().build();
    }
}