package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.model.Product;
import ru.yandex.practicum.dto.ProductCategory;
import org.springframework.data.domain.Page;
import ru.yandex.practicum.dto.ProductState;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findByProductCategoryAndProductState(
            ProductCategory category,
            ProductState state,
            Pageable pageable
    );
}
