package ru.yandex.practicum.collector.mapper.grpc;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;

@Slf4j
public class SensorEventGrpcMapper {

    public static SensorEventAvro toAvro(SensorEventProto proto) {
        log.debug("Mapping SensorEventProto to Avro: id={}, hubId={}",
                proto.getId(), proto.getHubId());

        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
                .setId(proto.getId())
                .setHubId(proto.getHubId())
                .setTimestamp(convertTimestamp(proto.getTimestamp()));

        if (proto.hasMotionSensorEvent()) {
            MotionSensorProto motion = proto.getMotionSensorEvent();
            MotionSensorAvro payload = MotionSensorAvro.newBuilder()
                    .setLinkQuality(motion.getLinkQuality())
                    .setMotion(motion.getMotion())
                    .setVoltage(motion.getVoltage())
                    .build();
            builder.setPayload(payload);
            log.debug("Mapped MotionSensorAvro");

        } else if (proto.hasTemperatureSensorEvent()) {
            TemperatureSensorProto temp = proto.getTemperatureSensorEvent();
            TemperatureSensorAvro payload = TemperatureSensorAvro.newBuilder()
                    .setId(proto.getId())
                    .setHubId(proto.getHubId())
                    .setTimestamp(convertTimestamp(proto.getTimestamp()))
                    .setTemperatureC(temp.getTemperatureC())
                    .setTemperatureF(temp.getTemperatureF())
                    .build();
            builder.setPayload(payload);
            log.debug("Mapped TemperatureSensorAvro");

        } else if (proto.hasLightSensorEvent()) {
            LightSensorProto light = proto.getLightSensorEvent();
            LightSensorAvro payload = LightSensorAvro.newBuilder()
                    .setLinkQuality(light.getLinkQuality())
                    .setLuminosity(light.getLuminosity())
                    .build();
            builder.setPayload(payload);
            log.debug("Mapped LightSensorAvro");

        } else if (proto.hasClimateSensorEvent()) {
            ClimateSensorProto climate = proto.getClimateSensorEvent();
            ClimateSensorAvro payload = ClimateSensorAvro.newBuilder()
                    .setTemperatureC(climate.getTemperatureC())
                    .setHumidity(climate.getHumidity())
                    .setCo2Level(climate.getCo2Level())
                    .build();
            builder.setPayload(payload);
            log.debug("Mapped ClimateSensorAvro");

        } else if (proto.hasSwitchSensorEvent()) {
            SwitchSensorProto switchSensor = proto.getSwitchSensorEvent();
            SwitchSensorAvro payload = SwitchSensorAvro.newBuilder()
                    .setState(switchSensor.getState())
                    .build();
            builder.setPayload(payload);
            log.debug("Mapped SwitchSensorAvro");
        }

        SensorEventAvro result = builder.build();
        log.info("Successfully mapped SensorEventProto to Avro: id={}, hubId={}",
                result.getId(), result.getHubId());

        return result;
    }

    private static Instant convertTimestamp(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}