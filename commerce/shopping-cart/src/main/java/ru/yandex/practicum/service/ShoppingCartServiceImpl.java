package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.exception.NoProductsInShoppingCartException;
import ru.yandex.practicum.exception.NotAuthorizedUserException;
import ru.yandex.practicum.feign.WarehouseClient;
import ru.yandex.practicum.model.CartItem;
import ru.yandex.practicum.model.ShoppingCart;
import ru.yandex.practicum.model.mapper.CartMapper;
import ru.yandex.practicum.repository.CartItemRepository;
import ru.yandex.practicum.repository.ShoppingCartRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private final ShoppingCartRepository shoppingCartRepository;
    private final CartItemRepository cartItemRepository;
    private final WarehouseClient warehouseClient;

    @Override
    @Transactional(readOnly = true)
    public ShoppingCartDto getShoppingCart(String username) {
        validateUsername(username);

        ShoppingCart cart = getOrCreateActiveCart(username);
        List<CartItem> items = cartItemRepository.findByShoppingCart(cart);

        return CartMapper.toDto(cart, items);
    }

    @Override
    @Transactional
    public ShoppingCartDto addProductToShoppingCart(String username, Map<UUID, Long> products) {
        validateUsername(username);

        ShoppingCartDto tempCart = buildTempCartForCheck(products);

        log.debug("Checking warehouse availability for products: {}", products.keySet());

        try {
            ResponseEntity<BookedProductsDto> response = warehouseClient.checkProductQuantityEnoughForShoppingCart(tempCart);
            BookedProductsDto bookedProducts = response.getBody();

            if (bookedProducts != null) {
                log.info("Warehouse check passed. Total weight: {}, volume: {}, fragile: {}",
                        bookedProducts.getDeliveryWeight(),
                        bookedProducts.getDeliveryVolume(),
                        bookedProducts.getFragile());
            }
        } catch (Exception e) {
            log.error("Failed to check warehouse availability: {}", e.getMessage());
            throw new RuntimeException("Не удалось проверить наличие товаров на складе: " + e.getMessage());
        }

        ShoppingCart cart = getOrCreateActiveCart(username);

        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            Long quantity = entry.getValue();

            CartItem existingItem = cartItemRepository
                    .findByShoppingCartAndProductId(cart, productId)
                    .orElse(null);

            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + quantity);
                cartItemRepository.save(existingItem);
                log.debug("Updated quantity for product {}: new quantity = {}", productId, existingItem.getQuantity());
            } else {
                CartItem newItem = new CartItem();
                newItem.setShoppingCart(cart);
                newItem.setProductId(productId);
                newItem.setQuantity(quantity);
                cartItemRepository.save(newItem);
                log.debug("Added new product {} with quantity {}", productId, quantity);
            }
        }

        List<CartItem> items = cartItemRepository.findByShoppingCart(cart);
        log.info("Successfully added products to cart for user: {}", username);

        return CartMapper.toDto(cart, items);
    }

    @Override
    @Transactional
    public void deactivateCurrentShoppingCart(String username) {
        validateUsername(username);

        shoppingCartRepository.findByUsernameAndActiveTrue(username)
                .ifPresent(cart -> {
                    cart.setActive(false);
                    shoppingCartRepository.save(cart);
                    log.info("Deactivated cart for user: {}", username);
                });
    }

    @Override
    @Transactional
    public ShoppingCartDto removeProductsFromCart(String username, List<UUID> productIds) {
        validateUsername(username);

        ShoppingCart cart = getOrCreateActiveCart(username);

        cartItemRepository.deleteByShoppingCartAndProductIdIn(cart, productIds);
        log.debug("Removed products {} from cart for user: {}", productIds, username);

        List<CartItem> items = cartItemRepository.findByShoppingCart(cart);
        return CartMapper.toDto(cart, items);
    }

    @Override
    @Transactional
    public ShoppingCartDto updateProductQuantity(String username, ChangeProductQuantityRequest request) {
        validateUsername(username);

        Map<UUID, Long> products = new HashMap<>();
        products.put(request.getProductId(), request.getNewQuantity());
        ShoppingCartDto tempCart = buildTempCartForCheck(products);

        try {
            ResponseEntity<BookedProductsDto> response = warehouseClient.checkProductQuantityEnoughForShoppingCart(tempCart);
            log.debug("Warehouse check passed for product {} with quantity {}",
                    request.getProductId(), request.getNewQuantity());
        } catch (Exception e) {
            log.error("Failed to check warehouse availability: {}", e.getMessage());
            throw new RuntimeException("Недостаточно товара на складе: " + e.getMessage());
        }

        ShoppingCart cart = getOrCreateActiveCart(username);

        CartItem item = cartItemRepository
                .findByShoppingCartAndProductId(cart, request.getProductId())
                .orElseThrow(() -> new NoProductsInShoppingCartException(
                        "Product not found in cart: " + request.getProductId()));

        item.setQuantity(request.getNewQuantity());
        cartItemRepository.save(item);

        List<CartItem> items = cartItemRepository.findByShoppingCart(cart);
        log.info("Updated quantity for product {} to {} in cart for user: {}",
                request.getProductId(), request.getNewQuantity(), username);

        return CartMapper.toDto(cart, items);
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new NotAuthorizedUserException("Username cannot be empty");
        }
    }

    private ShoppingCart getOrCreateActiveCart(String username) {
        return shoppingCartRepository
                .findByUsernameAndActiveTrue(username)
                .orElseGet(() -> {
                    ShoppingCart newCart = new ShoppingCart();
                    newCart.setUsername(username);
                    newCart.setActive(true);
                    log.info("Created new cart for user: {}", username);
                    return shoppingCartRepository.save(newCart);
                });
    }

    private ShoppingCartDto buildTempCartForCheck(Map<UUID, Long> products) {
        Map<UUID, Integer> intProducts = products.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().intValue()
                ));

        ShoppingCartDto tempCart = new ShoppingCartDto();
        tempCart.setProducts(intProducts);
        return tempCart;
    }
}