package ru.yandex.practicum.collector.mapper;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.collector.model.sensor.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class SensorEventMapper {

    private static final Map<SensorEventType, Function<SensorEvent, Object>> PAYLOAD_BUILDERS = new EnumMap<>(SensorEventType.class);

    static {
        PAYLOAD_BUILDERS.put(SensorEventType.CLIMATE_SENSOR_EVENT, event -> {
            ClimateSensorEvent e = (ClimateSensorEvent) event;
            return ClimateSensorAvro.newBuilder()
                    .setTemperatureC(e.getTemperatureC())
                    .setHumidity(e.getHumidity())
                    .setCo2Level(e.getCo2Level())
                    .build();
        });

        PAYLOAD_BUILDERS.put(SensorEventType.LIGHT_SENSOR_EVENT, event -> {
            LightSensorEvent e = (LightSensorEvent) event;
            return LightSensorAvro.newBuilder()
                    .setLinkQuality(e.getLinkQuality() != null ? e.getLinkQuality() : 0)
                    .setLuminosity(e.getLuminosity() != null ? e.getLuminosity() : 0)
                    .build();
        });

        PAYLOAD_BUILDERS.put(SensorEventType.MOTION_SENSOR_EVENT, event -> {
            MotionSensorEvent e = (MotionSensorEvent) event;
            return MotionSensorAvro.newBuilder()
                    .setLinkQuality(e.getLinkQuality())
                    .setMotion(e.getMotion())
                    .setVoltage(e.getVoltage())
                    .build();
        });

        PAYLOAD_BUILDERS.put(SensorEventType.SWITCH_SENSOR_EVENT, event -> {
            SwitchSensorEvent e = (SwitchSensorEvent) event;
            return SwitchSensorAvro.newBuilder()
                    .setState(e.getState())
                    .build();
        });

        PAYLOAD_BUILDERS.put(SensorEventType.TEMPERATURE_SENSOR_EVENT, event -> {
            TemperatureSensorEvent e = (TemperatureSensorEvent) event;
            return TemperatureSensorAvro.newBuilder()
                    .setTemperatureC(e.getTemperatureC())
                    .setTemperatureF(e.getTemperatureF())
                    .build();
        });
    }

    public static SensorEventAvro toAvro(SensorEvent event) {
        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp());

        Function<SensorEvent, Object> payloadBuilder = PAYLOAD_BUILDERS.get(event.getType());
        if (payloadBuilder == null) {
            throw new IllegalArgumentException("Unknown sensor event type: " + event.getType());
        }

        builder.setPayload(payloadBuilder.apply(event));
        return builder.build();
    }
}