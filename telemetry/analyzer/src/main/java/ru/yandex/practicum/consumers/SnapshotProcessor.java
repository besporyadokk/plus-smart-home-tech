package ru.yandex.practicum.consumers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.model.Snapshot;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.service.SnapshotProcessingService;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {

    private final SnapshotProcessingService processingService;

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    private volatile boolean running = true;
    private KafkaConsumer<String, byte[]> consumer;

    public void start() {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", "analyzer-snapshot-group");
        props.put("enable.auto.commit", "false");
        props.put("auto.offset.reset", "latest");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of("telemetry.snapshots.v1"));
        log.info("SnapshotProcessor subscribed to telemetry.snapshots.v1");

        try {
            while (running) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(1000));
                if (records.isEmpty()) continue;

                boolean allOk = true;
                for (ConsumerRecord<String, byte[]> record : records) {
                    try {
                        Snapshot snapshot = deserializeSnapshot(record.value());
                        log.debug("Processing snapshot hubId={}, sensors={}, offset={}",
                                snapshot.getHubId(), snapshot.getSensorValues().keySet(), record.offset());
                        processingService.processSnapshot(snapshot);
                    } catch (Exception e) {
                        log.error("Error processing snapshot offset={}", record.offset(), e);
                        allOk = false;
                        break;
                    }
                }

                if (allOk) {
                    consumer.commitSync();
                    log.debug("Committed offsets for {} snapshot records", records.count());
                } else {
                    log.warn("Skipping commit due to processing error");
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        } catch (WakeupException e) {
            log.info("WakeupException received, shutting down SnapshotProcessor");
        } catch (Exception e) {
            log.error("Fatal error in SnapshotProcessor", e);
        } finally {
            if (consumer != null) consumer.close();
        }
    }

    private Snapshot deserializeSnapshot(byte[] bytes) {
        try {
            SpecificDatumReader<SensorsSnapshotAvro> reader = new SpecificDatumReader<>(SensorsSnapshotAvro.class);
            Decoder decoder = DecoderFactory.get().binaryDecoder(bytes, null);
            SensorsSnapshotAvro avroSnapshot = reader.read(null, decoder);

            Map<String, Object> sensorValues = new HashMap<>();
            if (avroSnapshot.getSensorsState() != null) {
                avroSnapshot.getSensorsState().forEach((sensorId, sensorState) -> {
                    Object value = extractSensorValue(sensorState);
                    if (value != null) {
                        sensorValues.put(sensorId.toString(), value);
                    }
                });
            }

            return Snapshot.builder()
                    .hubId(avroSnapshot.getHubId().toString())
                    .sensorValues(sensorValues)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize snapshot", e);
        }
    }

    private Object extractSensorValue(SensorStateAvro sensorState) {
        if (sensorState.getData() == null) return null;
        Object data = sensorState.getData();
        String className = data.getClass().getSimpleName();

        try {
            return switch (className) {
                case "MotionSensorAvro" -> data.getClass().getMethod("getMotion").invoke(data);
                case "TemperatureSensorAvro" -> data.getClass().getMethod("getTemperatureC").invoke(data);
                case "LightSensorAvro" -> data.getClass().getMethod("getLuminosity").invoke(data);
                case "ClimateSensorAvro" -> data.getClass().getMethod("getTemperatureC").invoke(data);
                case "SwitchSensorAvro" -> data.getClass().getMethod("getState").invoke(data);
                default -> null;
            };
        } catch (Exception e) {
            log.error("Failed to extract sensor value from {}", className, e);
            return null;
        }
    }

    public void stop() {
        running = false;
        if (consumer != null) consumer.wakeup();
    }
}