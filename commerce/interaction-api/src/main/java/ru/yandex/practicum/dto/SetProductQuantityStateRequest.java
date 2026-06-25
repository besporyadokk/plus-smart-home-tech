package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SetProductQuantityStateRequest {
    @Null(message = "ID должен быть null при создании")
    private UUID productId;

    @NotNull(message = "Статус количества не может быть null")
    private QuantityState quantityState;
}
