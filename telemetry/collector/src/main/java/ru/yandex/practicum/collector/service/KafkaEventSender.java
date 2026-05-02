package ru.yandex.practicum.collector.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.serializer.HubEventSerializer;
import ru.yandex.practicum.collector.serializer.SensorEventSerializer;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.util.Properties;

@Slf4j
@Component
public class KafkaEventSender {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${collector.kafka.topics.sensors}")
    private String sensorsTopic;

    @Value("${collector.kafka.topics.hubs}")
    private String hubsTopic;

    private KafkaProducer<String, SensorEventAvro> sensorProducer;
    private KafkaProducer<String, HubEventAvro> hubProducer;

    @PostConstruct
    public void init() {
        Properties sensorProps = new Properties();
        sensorProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        sensorProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        sensorProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, SensorEventSerializer.class);
        sensorProducer = new KafkaProducer<>(sensorProps);

        Properties hubProps = new Properties();
        hubProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        hubProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        hubProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, HubEventSerializer.class);
        hubProducer = new KafkaProducer<>(hubProps);

        log.info("Kafka producers инициализированы. bootstrap-servers={}", bootstrapServers);
    }

    public void sendSensorEvent(SensorEventAvro event, String key) {
        ProducerRecord<String, SensorEventAvro> record =
                new ProducerRecord<>(sensorsTopic, key, event);
        sensorProducer.send(record, (metadata, ex) -> {
            if (ex == null) {
                log.info("Sensor event отправлен: topic={}, partition={}, offset={}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            } else {
                log.error("Ошибка отправки sensor event", ex);
            }
        });
    }

    public void sendHubEvent(HubEventAvro event, String key) {
        ProducerRecord<String, HubEventAvro> record =
                new ProducerRecord<>(hubsTopic, key, event);
        hubProducer.send(record, (metadata, ex) -> {
            if (ex == null) {
                log.info("Hub event отправлен: topic={}, partition={}, offset={}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            } else {
                log.error("Ошибка отправки hub event", ex);
            }
        });
    }

    @PreDestroy
    public void close() {
        sensorProducer.close();
        hubProducer.close();
        log.info("Kafka producers закрыты");
    }
}