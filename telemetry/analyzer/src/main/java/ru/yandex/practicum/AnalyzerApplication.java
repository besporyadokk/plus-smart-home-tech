package ru.yandex.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.consumers.HubEventProcessor;
import ru.yandex.practicum.consumers.SnapshotProcessor;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AnalyzerApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AnalyzerApplication.class, args);

        HubEventProcessor hubEventProcessor = context.getBean(HubEventProcessor.class);
        SnapshotProcessor snapshotProcessor = context.getBean(SnapshotProcessor.class);

        Thread hubThread = new Thread(hubEventProcessor, "HubEventHandlerThread");
        hubThread.start();

        snapshotProcessor.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            hubEventProcessor.stop();
            snapshotProcessor.stop();
        }));
    }
}