package ru.yandex.practicum.aggregator.serialization;

import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

public class SensorEventAvroDeserializer extends BaseAvroDeserializer<SensorEventAvro> {
    public SensorEventAvroDeserializer() {
        super(SensorEventAvro.getClassSchema());
    }
}