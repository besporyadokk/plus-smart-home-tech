package ru.yandex.practicum.model.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.ShoppingCartDto;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.ShoppingCart;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class CartMapper {

    public static ShoppingCartDto toDto(ShoppingCart entity, List<CartItem> items) {
        if (entity == null) return null;

        Map<UUID, Integer> products = new HashMap<>();
        for (CartItem item : items) {
            products.put(item.getProductId(), (int) item.getQuantity());
        }

        return new ShoppingCartDto(entity.getShoppingCartId(), products);
    }
}