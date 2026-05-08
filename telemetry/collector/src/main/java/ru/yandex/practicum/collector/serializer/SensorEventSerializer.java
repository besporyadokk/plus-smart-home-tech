package ru.yandex.practicum.collector.serializer;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.serialization.Serializer;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.io.ByteArrayOutputStream;

public class SensorEventSerializer implements Serializer<SensorEventAvro> {

    private final DatumWriter<SensorEventAvro> writer =
            new SpecificDatumWriter<>(SensorEventAvro.class);

    @Override
    public byte[] serialize(String topic, SensorEventAvro data) {
        if (data == null) return null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(data, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сериализации SensorEventAvro", e);
        }
    }
}