package ru.yandex.practicum.aggregator.starter;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.aggregator.service.AggregationService;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final AggregationService aggregationService;
    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final KafkaProducer<String, SensorsSnapshotAvro> producer;

    public void start() {
        consumer.subscribe(List.of("telemetry.sensors.v1"));
        log.info("Subscribed to topic: telemetry.sensors.v1");

        try {
            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(1000));

                records.forEach(record -> {
                    log.debug("Processing sensor event: id={}, hubId={}",
                            record.value().getId(), record.value().getHubId());

                    aggregationService.updateState(record.value())
                            .ifPresent(snapshot -> {
                                String hubId = snapshot.getHubId();
                                ProducerRecord<String, SensorsSnapshotAvro> producerRecord =
                                        new ProducerRecord<>("telemetry.snapshots.v1", hubId, snapshot);
                                producer.send(producerRecord, (metadata, exception) -> {
                                    if (exception != null) {
                                        log.error("Failed to send snapshot for hub: {}", snapshot.getHubId(), exception);
                                    } else {
                                        log.info("Snapshot sent: topic={}, partition={}, offset={}",
                                                metadata.topic(), metadata.partition(), metadata.offset());
                                    }
                                });
                            });
                });

                consumer.commitSync();
            }
        } catch (WakeupException e) {
            log.info("Consumer wakeup called, shutting down...");
        } catch (Exception e) {
            log.error("Error in aggregation loop", e);
        } finally {
            closeResources();
        }
    }

    private void closeResources() {
        try {
            log.info("Flushing producer...");
            producer.flush();
            log.info("Committing consumer offsets...");
            consumer.commitSync();
        } catch (Exception e) {
            log.error("Error while closing resources", e);
        } finally {
            log.info("Closing consumer...");
            consumer.close();
            log.info("Closing producer...");
            producer.close();
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("Shutting down aggregation starter...");
        consumer.wakeup();
    }
}