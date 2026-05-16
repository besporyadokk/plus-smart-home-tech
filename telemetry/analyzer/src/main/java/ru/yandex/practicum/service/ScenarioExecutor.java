package ru.yandex.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.model.Condition;
import ru.yandex.practicum.model.Scenario;
import ru.yandex.practicum.model.Snapshot;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ScenarioExecutor {

    public List<DeviceActionProto> evaluateScenario(Scenario scenario, Snapshot snapshot) {
        boolean allConditionsMet = scenario.getConditions().stream()
                .allMatch(condition -> {
                    Object sensorValue = snapshot.getValue(condition.getSensor().getId());
                    return checkCondition(condition, sensorValue);
                });

        if (!allConditionsMet) {
            return Collections.emptyList();
        }

        return scenario.getActions().stream()
                .map(action -> {
                    DeviceActionProto.Builder builder = DeviceActionProto.newBuilder()
                            .setType(action.getType())
                            .setSensorId(action.getSensorId());
                    if (action.getValue() != null) {
                        builder.setValue(action.getValue());
                    }
                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    private boolean checkCondition(Condition condition, Object sensorValue) {
        if (sensorValue == null) return false;

        if (sensorValue instanceof Boolean) {
            boolean boolVal = (Boolean) sensorValue;
            int conditionValue = condition.getValue();
            return switch (condition.getOperation()) {
                case EQUALS -> (boolVal && conditionValue == 1) || (!boolVal && conditionValue == 0);
                default -> false;
            };
        }

        if (sensorValue instanceof Number) {
            int intVal = ((Number) sensorValue).intValue();
            int condVal = condition.getValue();
            return switch (condition.getOperation()) {
                case EQUALS -> intVal == condVal;
                case GREATER_THAN -> intVal > condVal;
                case LOWER_THAN -> intVal < condVal;
                default -> false;
            };
        }

        log.warn("Unsupported sensor value type: {}", sensorValue.getClass());
        return false;
    }
}