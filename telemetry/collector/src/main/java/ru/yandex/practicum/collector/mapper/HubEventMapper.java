package ru.yandex.practicum.collector.mapper;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.collector.model.hub.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.stream.Collectors;

@Slf4j
public class HubEventMapper {

    public static HubEventAvro toAvro(HubEvent event) {
        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp());

        switch (event.getType()) {

            case DEVICE_ADDED -> {
                DeviceAddedEvent e = (DeviceAddedEvent) event;
                builder.setPayload(DeviceAddedEventAvro.newBuilder()
                        .setId(e.getId())
                        .setType(mapDeviceType(e.getDeviceType()))
                        .build());
            }

            case DEVICE_REMOVED -> {
                DeviceRemovedEvent e = (DeviceRemovedEvent) event;
                builder.setPayload(DeviceRemovedEventAvro.newBuilder()
                        .setId(e.getId())
                        .build());
            }

            case SCENARIO_ADDED -> {
                ScenarioAddedEvent e = (ScenarioAddedEvent) event;
                builder.setPayload(ScenarioAddedEventAvro.newBuilder()
                        .setName(e.getName())
                        .setConditions(e.getConditions().stream()
                                .map(HubEventMapper::mapCondition)
                                .collect(Collectors.toList()))
                        .setActions(e.getActions().stream()
                                .map(HubEventMapper::mapAction)
                                .collect(Collectors.toList()))
                        .build());
            }

            case SCENARIO_REMOVED -> {
                ScenarioRemovedEvent e = (ScenarioRemovedEvent) event;
                builder.setPayload(ScenarioRemovedEventAvro.newBuilder()
                        .setName(e.getName())
                        .build());
            }

            default -> throw new IllegalArgumentException("Неизвестный тип hub события: " + event.getType());
        }

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