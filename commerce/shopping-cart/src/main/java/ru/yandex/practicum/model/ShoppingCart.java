package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "shopping_carts")
@Data
public class ShoppingCart {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID shoppingCartId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private boolean active = true;
}