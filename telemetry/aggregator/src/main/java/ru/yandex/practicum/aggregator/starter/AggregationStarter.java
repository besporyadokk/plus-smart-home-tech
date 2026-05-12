package ru.yandex.practicum.aggregator.starter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.aggregator.serialization.SensorEventDeserializer;
import ru.yandex.practicum.aggregator.serialization.SensorsSnapshotSerializer;
import ru.yandex.practicum.aggregator.service.AggregationService;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final AggregationService aggregationService;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private KafkaConsumer<String, SensorEventAvro> consumer;
    private KafkaProducer<String, SensorsSnapshotAvro> producer;

    public void start() {
        initConsumer();
        initProducer();

        try {
            consumer.subscribe(List.of("telemetry.sensors.v1"));
            log.info("Subscribed to topic: telemetry.sensors.v1");

            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(1000));

                records.forEach(record -> {
                    log.debug("Processing sensor event: id={}, hubId={}",
                            record.value().getId(), record.value().getHubId());

                    aggregationService.updateState(record.value())
                            .ifPresent(snapshot -> {
                                String hubId = snapshot.getHubId().toString();

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

    private void initConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "aggregator-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SensorEventDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        consumer = new KafkaConsumer<>(props);
        log.info("Consumer initialized");
    }

    private void initProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, SensorsSnapshotSerializer.class.getName());

        producer = new KafkaProducer<>(props);
        log.info("Producer initialized");
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