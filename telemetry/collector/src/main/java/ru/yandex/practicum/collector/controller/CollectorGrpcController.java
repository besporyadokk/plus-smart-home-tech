package ru.yandex.practicum.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.collector.mapper.grpc.HubEventGrpcMapper;
import ru.yandex.practicum.collector.mapper.grpc.SensorEventGrpcMapper;
import ru.yandex.practicum.collector.service.KafkaEventSender;
import ru.yandex.practicum.grpc.telemetry.collector.CollectorControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class CollectorGrpcController extends CollectorControllerGrpc.CollectorControllerImplBase {

    private final KafkaEventSender kafkaEventSender;

    @Override
    public void collectSensorEvent(SensorEventProto request, StreamObserver<Empty> responseObserver) {
        log.info("=== Received gRPC sensor event ===");
        log.info("Sensor: id={}, hubId={}, type={}", request.getId(), request.getHubId(), request.getPayloadCase());

        try {
            var avroEvent = SensorEventGrpcMapper.toAvro(request);
            kafkaEventSender.sendSensorEvent(avroEvent, request.getId());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
            log.info("Sensor event processed and sent to Kafka");
        } catch (Exception e) {
            log.error("Error processing sensor event", e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL.withDescription(e.getMessage()).withCause(e)
            ));
        }
    }

    @Override
    public void collectHubEvent(HubEventProto request, StreamObserver<Empty> responseObserver) {
        log.info("=== Received gRPC hub event ===");
        log.info("Hub: hubId={}, type={}", request.getHubId(), request.getPayloadCase());

        try {
            var avroEvent = HubEventGrpcMapper.toAvro(request);
            kafkaEventSender.sendHubEvent(avroEvent, request.getHubId());
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
            log.info("Hub event processed and sent to Kafka");
        } catch (Exception e) {
            log.error("Error processing hub event", e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL.withDescription(e.getMessage()).withCause(e)
            ));
        }
    }
}