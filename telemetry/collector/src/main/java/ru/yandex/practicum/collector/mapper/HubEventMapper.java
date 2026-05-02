package ru.yandex.practicum.collector.mapper;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.collector.model.hub.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class HubEventMapper {

    private static final Map<HubEventType, Function<HubEvent, Object>> PAYLOAD_BUILDERS = new EnumMap<>(HubEventType.class);

    static {
        PAYLOAD_BUILDERS.put(HubEventType.DEVICE_ADDED, event -> {
            DeviceAddedEvent e = (DeviceAddedEvent) event;
            return DeviceAddedEventAvro.newBuilder()
                    .setId(e.getId())
                    .setType(mapDeviceType(e.getDeviceType()))
                    .build();
        });

        PAYLOAD_BUILDERS.put(HubEventType.DEVICE_REMOVED, event -> {
            DeviceRemovedEvent e = (DeviceRemovedEvent) event;
            return DeviceRemovedEventAvro.newBuilder()
                    .setId(e.getId())
                    .build();
        });

        PAYLOAD_BUILDERS.put(HubEventType.SCENARIO_ADDED, event -> {
            ScenarioAddedEvent e = (ScenarioAddedEvent) event;
            return ScenarioAddedEventAvro.newBuilder()
                    .setName(e.getName())
                    .setConditions(e.getConditions().stream()
                            .map(HubEventMapper::mapCondition)
                            .collect(Collectors.toList()))
                    .setActions(e.getActions().stream()
                            .map(HubEventMapper::mapAction)
                            .collect(Collectors.toList()))
                    .build();
        });

        PAYLOAD_BUILDERS.put(HubEventType.SCENARIO_REMOVED, event -> {
            ScenarioRemovedEvent e = (ScenarioRemovedEvent) event;
            return ScenarioRemovedEventAvro.newBuilder()
                    .setName(e.getName())
                    .build();
        });
    }

    public static HubEventAvro toAvro(HubEvent event) {
        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp());

        Function<HubEvent, Object> payloadBuilder = PAYLOAD_BUILDERS.get(event.getType());
        if (payloadBuilder == null) {
            throw new IllegalArgumentException("Unknown hub event type: " + event.getType());
        }

        builder.setPayload(payloadBuilder.apply(event));
        return builder.build();
    }

    private static DeviceTypeAvro mapDeviceType(DeviceType type) {
        return switch (type) {
            case MOTION_SENSOR -> DeviceTypeAvro.MOTION_SENSOR;
            case TEMPERATURE_SENSOR -> DeviceTypeAvro.TEMPERATURE_SENSOR;
            case LIGHT_SENSOR -> DeviceTypeAvro.LIGHT_SENSOR;
            case CLIMATE_SENSOR -> DeviceTypeAvro.CLIMATE_SENSOR;
            case SWITCH_SENSOR -> DeviceTypeAvro.SWITCH_SENSOR;
        };
    }

    private static ScenarioConditionAvro mapCondition(ScenarioCondition c) {
        return ScenarioConditionAvro.newBuilder()
                .setSensorId(c.getSensorId())
                .setType(mapConditionType(c.getType()))
                .setOperation(mapOperation(c.getOperation()))
                .setValue(c.getValue())
                .build();
    }

    private static DeviceActionAvro mapAction(DeviceAction action) {
        return DeviceActionAvro.newBuilder()
                .setSensorId(action.getSensorId())
                .setType(mapActionType(action.getType()))
                .setValue(action.getValue())
                .build();
    }

    private static ConditionTypeAvro mapConditionType(ConditionType type) {
        return switch (type) {
            case MOTION -> ConditionTypeAvro.MOTION;
            case LUMINOSITY -> ConditionTypeAvro.LUMINOSITY;
            case SWITCH -> ConditionTypeAvro.SWITCH;
            case TEMPERATURE -> ConditionTypeAvro.TEMPERATURE;
            case CO2LEVEL -> ConditionTypeAvro.CO2LEVEL;
            case HUMIDITY -> ConditionTypeAvro.HUMIDITY;
        };
    }

    private static ConditionOperationAvro mapOperation(ConditionOperation op) {
        return switch (op) {
            case EQUALS -> ConditionOperationAvro.EQUALS;
            case GREATER_THAN -> ConditionOperationAvro.GREATER_THAN;
            case LOWER_THAN -> ConditionOperationAvro.LOWER_THAN;
        };
    }

    private static ActionTypeAvro mapActionType(ActionType type) {
        return switch (type) {
            case ACTIVATE -> ActionTypeAvro.ACTIVATE;
            case DEACTIVATE -> ActionTypeAvro.DEACTIVATE;
            case INVERSE -> ActionTypeAvro.INVERSE;
            case SET_VALUE -> ActionTypeAvro.SET_VALUE;
        };
    }
}