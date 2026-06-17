package ru.yandex.practicum.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DimensionDto {
    @Positive(message = "Цена должна быть положительным числом")
    @DecimalMin(value = "1", message = "Цена должна быть не менее 1")
    private double width;

    @Positive(message = "Цена должна быть положительным числом")
    @DecimalMin(value = "1", message = "Цена должна быть не менее 1")
    private double height;

    @Positive(message = "Цена должна быть положительным числом")
    @DecimalMin(value = "1", message = "Цена должна быть не менее 1")
    private double depth;
}
