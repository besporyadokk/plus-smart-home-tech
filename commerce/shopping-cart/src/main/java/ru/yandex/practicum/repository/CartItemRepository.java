package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.ShoppingCart;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByShoppingCart(ShoppingCart shoppingCart);

    Optional<CartItem> findByShoppingCartAndProductId(ShoppingCart shoppingCart, UUID productId);

    void deleteByShoppingCartAndProductIdIn(ShoppingCart shoppingCart, List<UUID> productIds);
}