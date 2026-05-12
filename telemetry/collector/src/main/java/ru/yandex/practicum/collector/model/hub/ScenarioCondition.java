package ru.yandex.practicum.collector.model.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioCondition {
    @NotBlank
    private String sensorId;

    @NotNull
    private ConditionType type;

    @NotNull
    private ConditionOperation operation;

    private Object value;
}