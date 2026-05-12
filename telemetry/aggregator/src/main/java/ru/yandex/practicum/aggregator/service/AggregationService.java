package ru.yandex.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationService {


    private final Map<String, SensorsSnapshotAvro> snapshots = new ConcurrentHashMap<>();

    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        String hubId = event.getHubId().toString();
        String sensorId = event.getId().toString();
        long eventTimestamp = event.getTimestamp().toEpochMilli();

        SensorsSnapshotAvro snapshot = snapshots.get(hubId);
        Map<String, SensorStateAvro> newSensorsState = new HashMap<>();

        if (snapshot != null) {

            snapshot.getSensorsState().forEach((key, value) -> newSensorsState.put(key.toString(), value));
            log.info("Created new snapshot for hub: {}", hubId);
        }

        SensorStateAvro oldState = newSensorsState.get(sensorId);
        if (oldState != null && oldState.getTimestamp() > eventTimestamp) {
            log.debug("Ignoring outdated event for sensor: {}, event timestamp: {}, snapshot timestamp: {}",
                    sensorId, eventTimestamp, oldState.getTimestamp());
            return Optional.empty();
        }
        if (oldState != null && oldState.getData().equals(event.getPayload())) {
            log.debug("Ignoring duplicate event for sensor: {}, data unchanged", sensorId);
            return Optional.empty();
        }

        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(eventTimestamp)
                .setData(event.getPayload())
                .build();

        newSensorsState.put(sensorId, newState);

        SensorsSnapshotAvro updatedSnapshot = SensorsSnapshotAvro.newBuilder()
                .setHubId(hubId)
                .setTimestamp(eventTimestamp)
                .setSensorsState(newSensorsState)
                .build();

        snapshots.put(hubId, updatedSnapshot);
        log.info("Updated snapshot for hub: {}, sensor: {}, timestamp: {}", hubId, sensorId, eventTimestamp);
        return Optional.of(updatedSnapshot);
    }
}