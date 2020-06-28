package org.photonvision.common.scripting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.LoopingRunnable;
import org.photonvision.common.util.Platform;
import org.photonvision.common.util.file.JacksonUtils;

public class ScriptManager {

    private static final Logger logger = new Logger(ScriptManager.class, LogGroup.General);

    private ScriptManager() {}

    private static final List<ScriptEvent> events = new ArrayList<>();
    private static final LinkedBlockingDeque<ScriptEventType> queuedEvents =
            new LinkedBlockingDeque<>(25);

    public static void initialize() {
        ScriptConfigManager.initialize();
        if (ScriptConfigManager.fileExists()) {
            for (ScriptConfig scriptConfig : ScriptConfigManager.loadConfig()) {
                ScriptEvent scriptEvent = new ScriptEvent(scriptConfig);
                events.add(scriptEvent);
            }

            new Thread(new ScriptRunner(10L)).start();
        } else {
            System.err.println("Something went wrong initializing scripts! Events will not run.");
        }
    }

    private static class ScriptRunner extends LoopingRunnable {

        ScriptRunner(Long loopTimeMs) {
            super(loopTimeMs);
        }

        @Override
        protected void process() {
            try {

                handleEvent(queuedEvents.takeFirst());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void handleEvent(ScriptEventType eventType) {
            var toRun =
                    events
                            .parallelStream()
                            .filter(e -> e.config.eventType == eventType)
                            .findFirst()
                            .orElse(null);
            if (toRun != null) {
                try {
                    toRun.run();
                } catch (IOException e) {
                    System.err.printf(
                            "Failed to run script for event: %s, exception below.\n%s\n",
                            eventType.name(), e.getMessage());
                }
            }
        }
    }

    protected static class ScriptConfigManager {

        //        protected static final Path scriptConfigPath =
        // Paths.get(ConfigManager.SettingsPath.toString(), "scripts.json");
        static final Path scriptConfigPath = Paths.get(""); // TODO: Waiting on config

        private ScriptConfigManager() {}

        static boolean fileExists() {
            return Files.exists(scriptConfigPath);
        }

        public static void initialize() {
            if (!fileExists()) {
                List<ScriptConfig> eventsConfig = new ArrayList<>();
                for (var eventType : ScriptEventType.values()) {
                    eventsConfig.add(new ScriptConfig(eventType));
                }

                try {
                    JacksonUtils.serializer(
                            scriptConfigPath, eventsConfig.toArray(new ScriptConfig[0]), true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        static List<ScriptConfig> loadConfig() {
            try {
                var raw = JacksonUtils.deserialize(scriptConfigPath, ScriptConfig[].class);
                if (raw != null) {
                    return List.of(raw);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new ArrayList<>();
        }

        protected static void deleteConfig() {
            try {
                Files.delete(scriptConfigPath);
            } catch (IOException e) {
                //
            }
        }
    }

    public static void queueEvent(ScriptEventType eventType) {
        if (!Platform.CurrentPlatform.isWindows()) {
            try {
                queuedEvents.putLast(eventType);
                logger.info("Queued event: " + eventType.name());
            } catch (InterruptedException e) {
                System.err.println("Failed to add event to queue: " + eventType.name());
            }
        }
    }
}
