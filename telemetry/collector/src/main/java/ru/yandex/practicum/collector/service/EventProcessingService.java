package ru.yandex.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.mapper.HubEventMapper;
import ru.yandex.practicum.collector.mapper.SensorEventMapper;
import ru.yandex.practicum.collector.model.hub.HubEvent;
import ru.yandex.practicum.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventProcessingService {

    private final KafkaEventSender kafkaEventSender;

    public void processSensorEvent(SensorEvent event) {
        SensorEventAvro avroEvent = SensorEventMapper.toAvro(event);
        kafkaEventSender.sendSensorEvent(avroEvent, event.getId());
    }

    public void processHubEvent(HubEvent event) {
        HubEventAvro avroEvent = HubEventMapper.toAvro(event);
        kafkaEventSender.sendHubEvent(avroEvent, event.getHubId());
    }

}