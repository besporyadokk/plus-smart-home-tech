package ru.yandex.practicum.service;

import ru.yandex.practicum.dto.*;

import java.util.Map;
import java.util.UUID;

public interface WarehouseService {

    void newProductInWarehouse(NewProductInWarehouseRequest request);

    BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto shoppingCart);

    void addProductToWarehouse(AddProductToWarehouseRequest request);

    AddressDto getWarehouseAddress();

    BookedProductsDto assemblyProductsForOrder(AssemblyProductsForOrderRequest request);

    void shippedToDelivery(ShippedToDeliveryRequest request);

    void acceptReturn(Map<UUID, Integer> products);
}