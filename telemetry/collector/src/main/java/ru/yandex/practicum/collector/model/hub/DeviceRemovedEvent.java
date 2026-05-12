package ru.yandex.practicum.collector.model.hub;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@Builder
public class DeviceRemovedEvent extends HubEvent {
    @NotBlank
    private String id;

    @Override
    public HubEventType getType() {
        return HubEventType.DEVICE_REMOVED;
    }
}