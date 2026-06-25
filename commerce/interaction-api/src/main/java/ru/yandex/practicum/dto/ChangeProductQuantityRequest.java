package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeProductQuantityRequest {

    @NotNull(message = "ID товара не может быть null")
    private UUID productId;

    @Positive(message = "Количество должно быть положительным")
    private long newQuantity;
}