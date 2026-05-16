package ru.yandex.practicum.model;

import lombok.Builder;
import lombok.Getter;
import java.util.Map;

@Getter
@Builder
public class Snapshot {
    private String hubId;
    private Map<String, Object> sensorValues;

    public Object getValue(String sensorId) {
        return sensorValues.get(sensorId);
    }
}