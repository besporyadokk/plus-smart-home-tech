package ru.yandex.practicum.client;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc.HubRouterControllerBlockingStub;

@Service
@Slf4j
public class HubRouterClient {

    private final HubRouterControllerBlockingStub hubRouterClient;

    public HubRouterClient(@GrpcClient("hub-router")
                           HubRouterControllerBlockingStub hubRouterClient) {
        this.hubRouterClient = hubRouterClient;
    }

    public void sendAction(String hubId, String scenarioName, String sensorId,
                           DeviceActionProto action, Timestamp timestamp) {
        log.info("Preparing to send action: hubId={}, scenarioName={}, sensorId={}, actionType={}",
                hubId, scenarioName, sensorId, action.getType());

        DeviceActionRequest request = DeviceActionRequest.newBuilder()
                .setHubId(hubId)
                .setScenarioName(scenarioName)
                .setAction(action)
                .setTimestamp(timestamp)
                .build();

        int maxRetries = 3;
        int retryDelay = 1000;

        for (int i = 0; i < maxRetries; i++) {
            try {
                hubRouterClient.handleDeviceAction(request);
                log.info("Action sent successfully to HubRouter for hubId={}, scenarioName={}", hubId, scenarioName);
                return;
            } catch (Exception e) {
                log.warn("Attempt {} failed to send action to HubRouter: {}", i + 1, e.getMessage());
                if (i == maxRetries - 1) {
                    log.error("Failed to send action after {} attempts: hubId={}, scenarioName={}",
                            maxRetries, hubId, scenarioName, e);
                    throw e;
                }
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while retrying", ie);
                }
            }
        }
    }
}