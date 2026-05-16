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
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.repository.*;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private final SensorRepository sensorRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    private volatile boolean running = true;
    private KafkaConsumer<String, byte[]> consumer;

    @Override
    public void run() {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", "analyzer-hub-group");
        props.put("enable.auto.commit", "false");
        props.put("auto.offset.reset", "earliest");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of("telemetry.hubs.v1"));
        log.info("HubEventProcessor subscribed to telemetry.hubs.v1");

        try {
            while (running) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(1000));
                if (records.isEmpty()) continue;

                boolean errorOccurred = false;
                for (ConsumerRecord<String, byte[]> record : records) {
                    try {
                        HubEventAvro event = deserializeHubEvent(record.value());
                        processHubEvent(event);
                    } catch (Exception e) {
                        log.error("Error processing hub event at offset {}", record.offset(), e);
                        errorOccurred = true;
                        break;
                    }
                }

                if (!errorOccurred) {
                    consumer.commitSync();
                    log.debug("Hub events committed, count: {}", records.count());
                } else {
                    log.warn("Skipping commit due to errors");
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        } catch (WakeupException e) {
            log.info("WakeupException received, shutting down HubEventProcessor");
        } catch (Exception e) {
            log.error("Fatal error in HubEventProcessor", e);
        } finally {
            if (consumer != null) {
                consumer.close();
                log.info("HubEventProcessor consumer closed");
            }
        }
    }

    private HubEventAvro deserializeHubEvent(byte[] bytes) {
        try {
            SpecificDatumReader<HubEventAvro> reader = new SpecificDatumReader<>(HubEventAvro.getClassSchema());
            Decoder decoder = DecoderFactory.get().binaryDecoder(bytes, null);
            return reader.read(null, decoder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize hub event", e);
        }
    }

    private void processHubEvent(HubEventAvro event) {
        String hubId = event.getHubId().toString();
        Object payload = event.getPayload();

        if (payload instanceof DeviceAddedEventAvro) {
            handleDeviceAdded(hubId, (DeviceAddedEventAvro) payload);
        } else if (payload instanceof DeviceRemovedEventAvro) {
            handleDeviceRemoved(hubId, (DeviceRemovedEventAvro) payload);
        } else if (payload instanceof ScenarioAddedEventAvro) {
            handleScenarioAdded(hubId, (ScenarioAddedEventAvro) payload);
        } else if (payload instanceof ScenarioRemovedEventAvro) {
            handleScenarioRemoved(hubId, (ScenarioRemovedEventAvro) payload);
        } else {
            log.warn("Unknown hub event payload type: {}", payload.getClass().getSimpleName());
        }
    }

    private void handleDeviceAdded(String hubId, DeviceAddedEventAvro device) {
        String sensorId = device.getId().toString();
        Sensor sensor = sensorRepository.findById(sensorId).orElse(new Sensor());
        sensor.setId(sensorId);
        sensor.setHubId(hubId);
        sensorRepository.save(sensor);
        log.info("Device added/updated: id={}, hubId={}, type={}", sensorId, hubId, device.getType());
    }

    private void handleDeviceRemoved(String hubId, DeviceRemovedEventAvro device) {
        String sensorId = device.getId().toString();
        sensorRepository.deleteById(sensorId);
        log.info("Device removed: id={}, hubId={}", sensorId, hubId);
    }

    private void handleScenarioAdded(String hubId, ScenarioAddedEventAvro scenarioProto) {
        String scenarioName = scenarioProto.getName().toString();
        scenarioRepository.findByHubIdAndName(hubId, scenarioName)
                .ifPresent(existing -> {
                    scenarioRepository.delete(existing);
                    scenarioRepository.flush();
                });

        Scenario scenario = new Scenario();
        scenario.setHubId(hubId);
        scenario.setName(scenarioName);

        List<Condition> conditions = scenarioProto.getConditions().stream()
                .map(condProto -> {
                    Condition condition = new Condition();
                    condition.setType(mapConditionType(condProto.getType()));
                    condition.setOperation(mapOperation(condProto.getOperation()));

                    // Извлекаем значение, поддерживая и Boolean, и Integer
                    Object value = condProto.getValue();
                    if (value instanceof Integer) {
                        condition.setValue((Integer) value);
                    } else if (value instanceof Boolean) {
                        condition.setValue((Boolean) value ? 1 : 0);
                    } else {
                        throw new IllegalArgumentException("Unexpected condition value type: " + value.getClass());
                    }

                    String sensorId = condProto.getSensorId().toString();
                    Sensor sensor = sensorRepository.findById(sensorId)
                            .orElseThrow(() -> new RuntimeException("Sensor not found: " + sensorId));
                    condition.setSensor(sensor);
                    condition.setScenario(scenario);
                    return condition;
                }).collect(Collectors.toList());

        List<Action> actions = scenarioProto.getActions().stream()
                .map(actProto -> {
                    Action action = new Action();
                    action.setType(mapActionType(actProto.getType()));
                    action.setSensorId(actProto.getSensorId().toString());
                    action.setValue(actProto.getValue());
                    action.setScenario(scenario);
                    return action;
                }).collect(Collectors.toList());

        scenario.setConditions(conditions);
        scenario.setActions(actions);
        scenarioRepository.save(scenario);
        log.info("Scenario added: hubId={}, name={}, conditions={}, actions={}", hubId, scenarioName, conditions.size(), actions.size());
    }

    private void handleScenarioRemoved(String hubId, ScenarioRemovedEventAvro scenarioProto) {
        String name = scenarioProto.getName().toString();
        scenarioRepository.findByHubIdAndName(hubId, name)
                .ifPresent(scenario -> {
                    scenarioRepository.delete(scenario);
                    log.info("Scenario removed: hubId={}, name={}", hubId, name);
                });
    }

    private ConditionTypeProto mapConditionType(ConditionTypeAvro type) {
        return switch (type) {
            case MOTION -> ConditionTypeProto.MOTION;
            case LUMINOSITY -> ConditionTypeProto.LUMINOSITY;
            case SWITCH -> ConditionTypeProto.SWITCH;
            case TEMPERATURE -> ConditionTypeProto.TEMPERATURE;
            case CO2LEVEL -> ConditionTypeProto.CO2LEVEL;
            case HUMIDITY -> ConditionTypeProto.HUMIDITY;
        };
    }

    private ConditionOperationProto mapOperation(ConditionOperationAvro op) {
        return switch (op) {
            case EQUALS -> ConditionOperationProto.EQUALS;
            case GREATER_THAN -> ConditionOperationProto.GREATER_THAN;
            case LOWER_THAN -> ConditionOperationProto.LOWER_THAN;
        };
    }

    private ActionTypeProto mapActionType(ActionTypeAvro type) {
        return switch (type) {
            case ACTIVATE -> ActionTypeProto.ACTIVATE;
            case DEACTIVATE -> ActionTypeProto.DEACTIVATE;
            case INVERSE -> ActionTypeProto.INVERSE;
            case SET_VALUE -> ActionTypeProto.SET_VALUE;
        };
    }

    public void stop() {
        running = false;
        if (consumer != null) consumer.wakeup();
    }
}