/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.common.scripting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.TimedTaskManager;
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

            TimedTaskManager.getInstance().addTask("ScriptRunner", new ScriptRunner(), 10);

        } else {
            logger.error("Something went wrong initializing scripts! Events will not run.");
        }
    }

    private static class ScriptRunner implements Runnable {
        @Override
        public void run() {
            try {
                handleEvent(queuedEvents.takeFirst());
            } catch (InterruptedException e) {
                logger.error("ScriptRunner queue interrupted!", e);
            }
        }

        private void handleEvent(ScriptEventType eventType) {
            var toRun =
                    events.parallelStream()
                            .filter(e -> e.config.eventType == eventType)
                            .findFirst()
                            .orElse(null);
            if (toRun != null) {
                try {
                    toRun.run();
                } catch (IOException e) {
                    logger.error("Failed to run script for event \"" + eventType.name() + "\"", e);
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
                    JacksonUtils.serialize(scriptConfigPath, eventsConfig.toArray(new ScriptConfig[0]), true);
                } catch (IOException e) {
                    logger.error("Failed to initialize!", e);
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
                logger.error("Failed to load scripting config!", e);
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
        if (Platform.isLinux()) {
            try {
                queuedEvents.putLast(eventType);
                logger.info("Queued event: " + eventType.name());
            } catch (InterruptedException e) {
                logger.error("Failed to add event to queue: " + eventType.name(), e);
            }
        }
    }
}
