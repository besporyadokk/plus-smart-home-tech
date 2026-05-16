package ru.yandex.practicum.service;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.HubRouterClient;
import ru.yandex.practicum.model.Scenario;
import ru.yandex.practicum.model.Snapshot;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.repository.ScenarioRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotProcessingService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioExecutor scenarioExecutor;
    private final HubRouterClient hubRouterClient;

    @Transactional(readOnly = true)
    public void processSnapshot(Snapshot snapshot) {
        List<Scenario> scenarios = scenarioRepository.findByHubId(snapshot.getHubId());
        log.debug("Found {} scenarios for hubId={}", scenarios.size(), snapshot.getHubId());

        for (Scenario scenario : scenarios) {
            scenario.getConditions().size();
            scenario.getActions().size();

            List<DeviceActionProto> actions = scenarioExecutor.evaluateScenario(scenario, snapshot);
            if (!actions.isEmpty()) {
                log.info("Scenario '{}' triggered for hubId={}, executing {} actions",
                        scenario.getName(), snapshot.getHubId(), actions.size());

                for (DeviceActionProto action : actions) {
                    hubRouterClient.sendAction(
                            snapshot.getHubId(),
                            scenario.getName(),
                            action.getSensorId(),
                            action,
                            Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000).build()
                    );
                }
            }
        }
    }
}