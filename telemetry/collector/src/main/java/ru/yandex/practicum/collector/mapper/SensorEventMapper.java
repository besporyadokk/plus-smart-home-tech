package ru.yandex.practicum.collector.mapper;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.collector.model.sensor.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Slf4j
public class SensorEventMapper {

    public static SensorEventAvro toAvro(SensorEvent event) {
        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp());

        switch (event.getType()) {

            case CLIMATE_SENSOR_EVENT -> {
                ClimateSensorEvent e = (ClimateSensorEvent) event;
                builder.setPayload(ClimateSensorAvro.newBuilder()
                        .setTemperatureC(e.getTemperatureC())
                        .setHumidity(e.getHumidity())
                        .setCo2Level(e.getCo2Level())
                        .build());
            }

            case LIGHT_SENSOR_EVENT -> {
                LightSensorEvent e = (LightSensorEvent) event;
                builder.setPayload(LightSensorAvro.newBuilder()
                        .setLinkQuality(e.getLinkQuality() != null ? e.getLinkQuality() : 0)
                        .setLuminosity(e.getLuminosity() != null ? e.getLuminosity() : 0)
                        .build());
            }

            case MOTION_SENSOR_EVENT -> {
                MotionSensorEvent e = (MotionSensorEvent) event;
                builder.setPayload(MotionSensorAvro.newBuilder()
                        .setLinkQuality(e.getLinkQuality())
                        .setMotion(e.getMotion())
                        .setVoltage(e.getVoltage())
                        .build());
            }

            case SWITCH_SENSOR_EVENT -> {
                SwitchSensorEvent e = (SwitchSensorEvent) event;
                builder.setPayload(SwitchSensorAvro.newBuilder()
                        .setState(e.getState())
                        .build());
            }

            case TEMPERATURE_SENSOR_EVENT -> {
                TemperatureSensorEvent e = (TemperatureSensorEvent) event;
                builder.setPayload(TemperatureSensorAvro.newBuilder()
                        .setTemperatureC(e.getTemperatureC())
                        .setTemperatureF(e.getTemperatureF())
                        .build());
            }

            default -> throw new IllegalArgumentException("Неизвестный тип sensor события: " + event.getType());
        }

        return builder.build();
    }
}