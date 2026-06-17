package ru.yandex.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.ChangeProductQuantityRequest;
import ru.yandex.practicum.dto.ShoppingCartDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "shopping-cart", path = "/api/v1/shopping-cart")
public interface ShoppingCartClient {

    @GetMapping
    ResponseEntity<ShoppingCartDto> getShoppingCart(@RequestParam String username);

    @PutMapping
    ResponseEntity<ShoppingCartDto> addProductToShoppingCart(
            @RequestParam String username,
            @RequestBody Map<UUID, Long> products);

    @DeleteMapping
    ResponseEntity<Void> deactivateCurrentShoppingCart(@RequestParam String username);

    @PostMapping("/remove")
    ResponseEntity<ShoppingCartDto> removeFromShoppingCart(
            @RequestParam String username,
            @RequestBody List<UUID> productIds);

    @PostMapping("/change-quantity")
    ResponseEntity<ShoppingCartDto> changeProductQuantity(
            @RequestParam String username,
            @RequestBody ChangeProductQuantityRequest request);
}