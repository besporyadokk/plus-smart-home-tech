package ru.yandex.practicum.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.dto.ProductCategory;
import ru.yandex.practicum.dto.SetProductQuantityStateRequest;
import ru.yandex.practicum.dto.ProductDto;

import java.util.UUID;

public interface ProductService {

    Page<ProductDto> getProducts(ProductCategory category, Pageable pageable);

    ProductDto getProduct(UUID productId);

    ProductDto createProduct(ProductDto productDto);

    ProductDto updateProduct(ProductDto productDto);

    ProductDto removeProductFromStore(UUID productId);

    ProductDto setProductQuantityState(SetProductQuantityStateRequest request);
}