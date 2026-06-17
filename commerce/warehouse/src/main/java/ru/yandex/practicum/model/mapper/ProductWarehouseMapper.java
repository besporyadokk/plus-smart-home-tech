package ru.yandex.practicum.model.mapper;

import ru.yandex.practicum.dto.NewProductInWarehouseRequest;
import ru.yandex.practicum.model.ProductWarehouse;

public class ProductWarehouseMapper {

    public static ProductWarehouse toEntity(NewProductInWarehouseRequest request) {
        if (request == null) {
            return null;
        }

        ProductWarehouse product = new ProductWarehouse();
        product.setProductId(request.getProductId());
        product.setFragile(request.getFragile() != null && request.getFragile());
        product.setWidth(request.getDimension().getWidth());
        product.setHeight(request.getDimension().getHeight());
        product.setDepth(request.getDimension().getDepth());
        product.setWeight(request.getWeight());
        product.setQuantity(0L);

        return product;
    }
}