package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.ProductCategory;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.dto.SetProductQuantityStateRequest;
import ru.yandex.practicum.feign.ShoppingStoreClient;
import ru.yandex.practicum.service.ProductService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/shopping-store")
@RequiredArgsConstructor
public class ProductController implements ShoppingStoreClient {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<ProductDto>> getProducts(
            @RequestParam ProductCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String[] sort) {

        Pageable pageable;
        if (sort != null && sort.length > 0) {
            Sort sortOrder = parseSort(sort);
            pageable = PageRequest.of(page, size, sortOrder);
        } else {
            pageable = PageRequest.of(page, size);
        }

        Page<ProductDto> products = productService.getProducts(category, pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable UUID productId) {
        ProductDto product = productService.getProduct(productId);
        return ResponseEntity.ok(product);
    }

    @PutMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto productDto) {
        ProductDto product = productService.createProduct(productDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @PostMapping
    public ResponseEntity<ProductDto> updateProduct(@Valid @RequestBody ProductDto productDto) {
        ProductDto product = productService.updateProduct(productDto);
        return ResponseEntity.ok(product);
    }

    @PostMapping("/removeProductFromStore")
    public ResponseEntity<ProductDto> removeProductFromStore(@RequestBody UUID productId) {
        ProductDto product = productService.removeProductFromStore(productId);
        return ResponseEntity.ok(product);
    }

    @PostMapping("/quantityState")
    public ResponseEntity<ProductDto> setProductQuantityState(
            @Valid @RequestBody SetProductQuantityStateRequest request) {
        ProductDto product = productService.setProductQuantityState(request);
        return ResponseEntity.ok(product);
    }

    private Sort parseSort(String[] sortParams) {
        Sort sort = Sort.unsorted();
        for (String param : sortParams) {
            String[] parts = param.split(",");
            String property = parts[0];
            Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            sort = sort.and(Sort.by(direction, property));
        }
        return sort;
    }
}