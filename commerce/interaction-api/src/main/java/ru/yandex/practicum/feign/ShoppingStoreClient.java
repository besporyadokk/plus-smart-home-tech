package ru.yandex.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.ProductCategory;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.dto.SetProductQuantityStateRequest;

import java.util.UUID;

@FeignClient(name = "shopping-store", path = "/api/v1/shopping-store")
public interface ShoppingStoreClient {

    @GetMapping
    ResponseEntity<Page<ProductDto>> getProducts(
            @RequestParam ProductCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String[] sort);

    @GetMapping("/{productId}")
    ResponseEntity<ProductDto> getProduct(@PathVariable UUID productId);

    @PutMapping
    ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto);

    @PostMapping
    ResponseEntity<ProductDto> updateProduct(@RequestBody ProductDto productDto);

    @PostMapping("/removeProductFromStore")
    ResponseEntity<ProductDto> removeProductFromStore(@RequestBody UUID productId);

    @PostMapping("/quantityState")
    ResponseEntity<ProductDto> setProductQuantityState(@RequestBody SetProductQuantityStateRequest request);
}