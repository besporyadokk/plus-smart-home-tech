package ru.yandex.practicum.collector.mapper.grpc;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;

@Slf4j
public class HubEventGrpcMapper {

    public static HubEventAvro toAvro(HubEventProto proto) {
        log.debug("Mapping HubEventProto to Avro: hubId={}", proto.getHubId());

        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
                .setHubId(proto.getHubId())
                .setTimestamp(convertTimestamp(proto.getTimestamp()));

        if (proto.hasDeviceAddedEvent()) {
            DeviceAddedEventProto deviceAdded = proto.getDeviceAddedEvent();
            DeviceAddedEventAvro payload = DeviceAddedEventAvro.newBuilder()
                    .setId(deviceAdded.getId())
                    .setType(mapDeviceType(deviceAdded.getType()))
                    .build();
            builder.setPayload(payload);
            log.debug("Mapped DeviceAddedEventAvro: id={}", deviceAdded.getId());

        } else if (proto.hasDeviceRemovedEvent()) {
            DeviceRemovedEventProto deviceRemoved = proto.getDeviceRemovedEvent();
            DeviceRemovedEventAvro payload = DeviceRemovedEventAvro.newBuilder()
                    .setId(deviceRemoved.getId())
                    .build();
            builder.setPayload(payload);
            log.debug("Mapped DeviceRemovedEventAvro: id={}", deviceRemoved.getId());

        } else if (proto.hasScenarioAddedEvent()) {
            ScenarioAddedEventProto scenarioAdded = proto.getScenarioAddedEvent();
            ScenarioAddedEventAvro payload = ScenarioAddedEventAvro.newBuilder()
                    .setName(scenarioAdded.getName())
                    .setConditions(scenarioAdded.getConditionList().stream()
                            .map(HubEventGrpcMapper::mapCondition)
                            .toList())
                    .setActions(scenarioAdded.getActionList().stream()
                            .map(HubEventGrpcMapper::mapAction)
                            .toList())
                    .build();
            builder.setPayload(payload);
            log.debug("Mapped ScenarioAddedEventAvro: name={}", scenarioAdded.getName());

        } else if (proto.hasScenarioRemovedEvent()) {
            ScenarioRemovedEventProto scenarioRemoved = proto.getScenarioRemovedEvent();
            ScenarioRemovedEventAvro payload = ScenarioRemovedEventAvro.newBuilder()
                    .setName(scenarioRemoved.getName())
                    .build();
            builder.setPayload(payload);
            log.debug("Mapped ScenarioRemovedEventAvro: name={}", scenarioRemoved.getName());
        }

        HubEventAvro result = builder.build();
        log.info("Successfully mapped HubEventProto to Avro: hubId={}", result.getHubId());

        return result;
    }

    private static DeviceTypeAvro mapDeviceType(DeviceTypeProto type) {
        return switch (type) {
            case MOTION_SENSOR -> DeviceTypeAvro.MOTION_SENSOR;
            case TEMPERATURE_SENSOR -> DeviceTypeAvro.TEMPERATURE_SENSOR;
            case LIGHT_SENSOR -> DeviceTypeAvro.LIGHT_SENSOR;
            case CLIMATE_SENSOR -> DeviceTypeAvro.CLIMATE_SENSOR;
            case SWITCH_SENSOR -> DeviceTypeAvro.SWITCH_SENSOR;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown device type: " + type);
        };
    }

    private static ScenarioConditionAvro mapCondition(ScenarioConditionProto condition) {
        ScenarioConditionAvro.Builder builder = ScenarioConditionAvro.newBuilder()
                .setSensorId(condition.getSensorId())
                .setType(mapConditionType(condition.getType()))
                .setOperation(mapOperation(condition.getOperation()));

        if (condition.hasBoolValue()) {
            builder.setValue(condition.getBoolValue());
        } else if (condition.hasIntValue()) {
            builder.setValue(condition.getIntValue());
        }

        return builder.build();
    }

    private static DeviceActionAvro mapAction(DeviceActionProto action) {
        return DeviceActionAvro.newBuilder()
                .setSensorId(action.getSensorId())
                .setType(mapActionType(action.getType()))
                .setValue(action.hasValue() ? action.getValue() : null)
                .build();
    }

    private static ConditionTypeAvro mapConditionType(ConditionTypeProto type) {
        return switch (type) {
            case MOTION -> ConditionTypeAvro.MOTION;
            case LUMINOSITY -> ConditionTypeAvro.LUMINOSITY;
            case SWITCH -> ConditionTypeAvro.SWITCH;
            case TEMPERATURE -> ConditionTypeAvro.TEMPERATURE;
            case CO2LEVEL -> ConditionTypeAvro.CO2LEVEL;
            case HUMIDITY -> ConditionTypeAvro.HUMIDITY;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown condition type: " + type);
        };
    }

    private static ConditionOperationAvro mapOperation(ConditionOperationProto op) {
        return switch (op) {
            case EQUALS -> ConditionOperationAvro.EQUALS;
            case GREATER_THAN -> ConditionOperationAvro.GREATER_THAN;
            case LOWER_THAN -> ConditionOperationAvro.LOWER_THAN;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown operation: " + op);
        };
    }

    private static ActionTypeAvro mapActionType(ActionTypeProto type) {
        return switch (type) {
            case ACTIVATE -> ActionTypeAvro.ACTIVATE;
            case DEACTIVATE -> ActionTypeAvro.DEACTIVATE;
            case INVERSE -> ActionTypeAvro.INVERSE;
            case SET_VALUE -> ActionTypeAvro.SET_VALUE;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown action type: " + type);
        };
    }

    private static Instant convertTimestamp(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}