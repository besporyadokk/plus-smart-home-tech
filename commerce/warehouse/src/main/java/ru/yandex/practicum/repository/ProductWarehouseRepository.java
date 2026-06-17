package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.model.ProductWarehouse;

import java.util.Optional;
import java.util.UUID;

public interface ProductWarehouseRepository extends JpaRepository<ProductWarehouse, UUID> {

    Optional<ProductWarehouse> findByProductId(UUID productId);

    boolean existsByProductId(UUID productId);
}