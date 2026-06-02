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

    private final Map<String, Map<String, SensorStateAvro>> snapshots = new ConcurrentHashMap<>();

    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        String hubId = event.getHubId();
        String sensorId = event.getId();
        long eventTimestamp = event.getTimestamp().toEpochMilli();

        Map<String, SensorStateAvro> sensorStates = snapshots.computeIfAbsent(hubId, k -> new HashMap<>());

        SensorStateAvro oldState = sensorStates.get(sensorId);
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

        sensorStates.put(sensorId, newState);

        SensorsSnapshotAvro updatedSnapshot = SensorsSnapshotAvro.newBuilder()
                .setHubId(hubId)
                .setTimestamp(eventTimestamp)
                .setSensorsState(sensorStates)
                .build();

        log.info("Updated snapshot for hub: {}, sensor: {}, timestamp: {}", hubId, sensorId, eventTimestamp);
        return Optional.of(updatedSnapshot);
    }
}