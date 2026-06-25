package ru.yandex.practicum.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.exception.ProductNotFoundException;
import ru.yandex.practicum.model.Product;
import ru.yandex.practicum.dto.ProductCategory;
import ru.yandex.practicum.dto.ProductState;
import ru.yandex.practicum.dto.SetProductQuantityStateRequest;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.model.mapper.ProductMapper;
import ru.yandex.practicum.repository.ProductRepository;

import java.util.UUID;

@AllArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public Page<ProductDto> getProducts(ProductCategory category, Pageable pageable) {

        return productRepository.findByProductCategoryAndProductState(category, ProductState.ACTIVE, pageable)
                .map(ProductMapper::toDto);
    }

    @Override
    @Transactional
    public ProductDto getProduct(UUID productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));

        if (product.getProductState() == ProductState.DEACTIVATE) {
            throw new ProductNotFoundException("Product is deactivated: " + productId);
        }

        return ProductMapper.toDto(product);
    }

    @Override
    @Transactional
    public ProductDto createProduct(ProductDto productDto) {

        Product product = productRepository.save(ProductMapper.toEntity(productDto));

        return ProductMapper.toDto(product);
    }

    @Override
    @Transactional
    public ProductDto updateProduct(ProductDto productDto) {
        Product product = productRepository.findById(productDto.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productDto.getProductId()));

        ProductMapper.updateEntity(product, productDto);

        Product productUpdate = productRepository.save(product);

        return ProductMapper.toDto(productUpdate);
    }

    @Override
    @Transactional
    public ProductDto removeProductFromStore(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));

        product.setProductState(ProductState.DEACTIVATE);
        Product updated = productRepository.save(product);
        return ProductMapper.toDto(updated);
    }

    @Override
    @Transactional
    public ProductDto setProductQuantityState(SetProductQuantityStateRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + request.getProductId()));

        product.setQuantityState(request.getQuantityState());
        Product updated = productRepository.save(product);
        return ProductMapper.toDto(updated);
    }
}
