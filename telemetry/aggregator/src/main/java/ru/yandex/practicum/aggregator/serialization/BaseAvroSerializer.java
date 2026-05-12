package ru.yandex.practicum.aggregator.serialization;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;

public abstract class BaseAvroSerializer<T extends SpecificRecordBase> implements Serializer<T> {
    private final DatumWriter<T> writer;

    protected BaseAvroSerializer(Class<T> clazz) {
        this.writer = new SpecificDatumWriter<>(clazz);
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) return null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(data, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Serialization error", e);
        }
    }

    @Override
    public void close() {}
}