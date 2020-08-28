/*
 * Copyright (C) 2020 Photon Vision.
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

package org.photonvision.common.logging;

import java.io.*;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.util.TimedTaskManager;
import org.photonvision.server.SocketHandler;

@SuppressWarnings("unused")
public class Logger {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private static final SimpleDateFormat simpleDateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final String className;
    private final LogGroup group;

    public Logger(Class<?> clazz, LogGroup group) {
        this.className = clazz.getSimpleName();
        this.group = group;
    }

    public Logger(Class<?> clazz, String suffix, LogGroup group) {
        this.className = clazz.getSimpleName() + " - " + suffix;
        this.group = group;
    }

    public static String getDate() {
        return simpleDateFormat.format(new Date());
    }

    public static String format(
            String logMessage, LogLevel level, LogGroup group, String clazz, boolean color) {
        var date = getDate();
        var builder = new StringBuilder();
        if (color) builder.append(level.colorCode);
        builder
                .append("[")
                .append(date)
                .append("] [")
                .append(group)
                .append(" - ")
                .append(clazz)
                .append("] [")
                .append(level.name())
                .append("] ")
                .append(logMessage);
        if (color) builder.append(ANSI_RESET);
        return builder.toString();
    }

    private static final HashMap<LogGroup, LogLevel> levelMap = new HashMap<>();
    private static final List<LogAppender> currentAppenders = new ArrayList<>();

    static {
        levelMap.put(LogGroup.Camera, LogLevel.INFO);
        levelMap.put(LogGroup.General, LogLevel.INFO);
        levelMap.put(LogGroup.WebServer, LogLevel.INFO);
        levelMap.put(LogGroup.Data, LogLevel.INFO);
        levelMap.put(LogGroup.VisionModule, LogLevel.INFO);
    }

    static {
        currentAppenders.add(new ConsoleLogAppender());
        currentAppenders.add(new UILogAppender());
        addFileAppender(ConfigManager.getInstance().getLogPath());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void addFileAppender(Path logFilePath) {
        var file = logFilePath.toFile();
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        currentAppenders.add(new FileLogAppender(logFilePath));
    }

    public static void setLevel(LogGroup group, LogLevel newLevel) {
        levelMap.put(group, newLevel);
    }

    // TODO: profile
    private static void log(String message, LogLevel level, LogGroup group, String clazz) {
        for (var a : currentAppenders) {
            var shouldColor = a instanceof ConsoleLogAppender;
            var formattedMessage = format(message, level, group, clazz, shouldColor);
            a.log(formattedMessage, level);
        }
    }

    public boolean shouldLog(LogLevel logLevel) {
        return logLevel.code <= levelMap.get(group).code;
    }

    private void log(String message, LogLevel level) {
        if (shouldLog(level)) {
            log(message, level, group, className);
        }
    }

    private void log(String message, LogLevel messageLevel, LogLevel conditionalLevel) {
        if (shouldLog(conditionalLevel)) {
            log(message, messageLevel, group, className);
        }
    }

    private void log(Supplier<String> messageSupplier, LogLevel level) {
        if (shouldLog(level)) {
            log(messageSupplier.get(), level, group, className);
        }
    }

    private void log(
            Supplier<String> messageSupplier, LogLevel messageLevel, LogLevel conditionalLevel) {
        if (shouldLog(conditionalLevel)) {
            log(messageSupplier.get(), messageLevel, group, className);
        }
    }

    public void error(Supplier<String> messageSupplier) {
        log(messageSupplier, LogLevel.ERROR);
    }

    public void error(String message) {
        log(message, LogLevel.ERROR);
    }

    /**
    * Logs an error message with the stack trace of a Throwable. The stacktrace will only be printed
    * if the current LogLevel is TRACE
    *
    * @param message
    * @param t
    */
    public void error(String message, Throwable t) {
        log(message, LogLevel.ERROR);
        log(convertStackTraceToString(t), LogLevel.ERROR, LogLevel.DEBUG);
    }

    public void warn(Supplier<String> messageSupplier) {
        log(messageSupplier, LogLevel.WARN);
    }

    public void warn(String message) {
        log(message, LogLevel.WARN);
    }

    public void info(Supplier<String> messageSupplier) {
        log(messageSupplier, LogLevel.INFO);
    }

    public void info(String message) {
        log(message, LogLevel.INFO);
    }

    public void debug(Supplier<String> messageSupplier) {
        log(messageSupplier, LogLevel.DEBUG);
    }

    public void debug(String message) {
        log(message, LogLevel.DEBUG);
    }

    public void trace(Supplier<String> messageSupplier) {
        log(messageSupplier, LogLevel.TRACE);
    }

    public void trace(String message) {
        log(message, LogLevel.TRACE);
    }

    private static String convertStackTraceToString(Throwable throwable) {
        try (StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            return sw.toString();
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    private interface LogAppender {
        void log(String message, LogLevel level);
    }

    private static class ConsoleLogAppender implements LogAppender {
        @Override
        public void log(String message, LogLevel level) {
            System.out.println(message);
        }
    }

    private static class UILogAppender implements LogAppender {
        @Override
        public void log(String message, LogLevel level) {
            var messageMap = new SocketHandler.UIMap();
            messageMap.put("logMessage", message);
            messageMap.put("logLevel", level.code);
            var superMap = new SocketHandler.UIMap();
            superMap.put("logMessage", messageMap);
            DataChangeService.getInstance().publishEvent(new OutgoingUIEvent<>("log", superMap));
        }
    }

    private static class FileLogAppender implements LogAppender {
        private OutputStream out;

        public FileLogAppender(Path logFilePath) {
            try {
                this.out = new FileOutputStream(logFilePath.toFile());
                TimedTaskManager.getInstance()
                        .addTask(
                                "FileLogAppender",
                                () -> {
                                    try {
                                        out.flush();
                                    } catch (IOException ignored) {
                                    }
                                },
                                3000L);
            } catch (FileNotFoundException e) {
                out = null;
                System.err.println("Unable to log to file " + logFilePath.toString());
            }
        }

        @Override
        public void log(String message, LogLevel level) {
            message += "\n";
            try {
                out.write(message.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
