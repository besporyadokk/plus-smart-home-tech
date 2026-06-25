package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "warehouse_products")
@Data
public class ProductWarehouse {

    @Id
    private UUID productId;

    @Column(nullable = false)
    private Boolean fragile;

    @Column(nullable = false)
    private Double width;

    @Column(nullable = false)
    private Double height;

    @Column(nullable = false)
    private Double depth;

    @Column(nullable = false)
    private Double weight;

    @Column(nullable = false)
    private Long quantity = 0L;
}