package ru.yandex.practicum.collector.serializer;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.serialization.Serializer;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.io.ByteArrayOutputStream;

public class HubEventSerializer implements Serializer<HubEventAvro> {

    private final DatumWriter<HubEventAvro> writer =
            new SpecificDatumWriter<>(HubEventAvro.class);

    @Override
    public byte[] serialize(String topic, HubEventAvro data) {
        if (data == null) return null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(data, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сериализации HubEventAvro", e);
        }
    }
}