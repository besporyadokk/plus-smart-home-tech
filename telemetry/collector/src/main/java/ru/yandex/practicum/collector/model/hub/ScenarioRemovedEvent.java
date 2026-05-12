package ru.yandex.practicum.collector.model.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioRemovedEvent extends HubEvent {
    @NotBlank
    @Size(min = 3)
    private String name;

    @Override
    public HubEventType getType() {
        return HubEventType.SCENARIO_REMOVED;
    }
}