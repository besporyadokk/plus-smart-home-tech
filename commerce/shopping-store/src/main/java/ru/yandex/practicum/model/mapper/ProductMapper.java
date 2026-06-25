package ru.yandex.practicum.model.mapper;

import ru.yandex.practicum.model.Product;
import ru.yandex.practicum.dto.ProductDto;

public class ProductMapper {

    public static ProductDto toDto(Product entity) {
        if (entity == null) return null;

        ProductDto dto = new ProductDto();
        dto.setProductId(entity.getProductId());
        dto.setProductName(entity.getProductName());
        dto.setDescription(entity.getDescription());
        dto.setImageSrc(entity.getImageSrc());
        dto.setQuantityState(entity.getQuantityState());
        dto.setProductState(entity.getProductState());
        dto.setProductCategory(entity.getProductCategory());
        dto.setPrice(entity.getPrice());

        return dto;
    }

    public static Product toEntity(ProductDto dto) {
        if (dto == null) return null;

        Product entity = new Product();
        entity.setProductId(dto.getProductId());
        entity.setProductName(dto.getProductName());
        entity.setDescription(dto.getDescription());
        entity.setImageSrc(dto.getImageSrc());
        entity.setQuantityState(dto.getQuantityState());
        entity.setProductState(dto.getProductState());
        entity.setProductCategory(dto.getProductCategory());
        entity.setPrice(dto.getPrice());

        return entity;
    }

    public static void updateEntity(Product entity, ProductDto dto) {
        if (dto.getProductName() != null) entity.setProductName(dto.getProductName());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getImageSrc() != null) entity.setImageSrc(dto.getImageSrc());
        if (dto.getQuantityState() != null) entity.setQuantityState(dto.getQuantityState());
        if (dto.getProductCategory() != null) entity.setProductCategory(dto.getProductCategory());
        if (dto.getPrice() > 0) entity.setPrice(dto.getPrice());
    }
}