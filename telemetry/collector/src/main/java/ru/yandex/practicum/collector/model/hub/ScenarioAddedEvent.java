package ru.yandex.practicum.collector.model.hub;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioAddedEvent extends HubEvent {
    @NotBlank
    @Size(min = 3)
    private String name;

    @NotEmpty
    @Valid
    private List<ScenarioCondition> conditions;

    @NotEmpty
    @Valid
    private List<DeviceAction> actions;

    @Override
    public HubEventType getType() {
        return HubEventType.SCENARIO_ADDED;
    }
}