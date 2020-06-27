package org.photonvision.common.logging;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Logger {

    private final String className;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final LogGroup group;

    public Logger(Class<?> clazz, LogGroup group) {
        this.className = clazz.getSimpleName();
        this.group = group;
    }

    public static String getDate() {
        return simpleDateFormat.format(new Date());
    }

    public static String format(
            String logMessage, Level level, LogGroup group, String clazz, boolean color) {
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

    private static HashMap<LogGroup, Level> levelMap = new HashMap<>();
    private static List<Appender> currentAppenders = new ArrayList<>();

    static {
        levelMap.put(LogGroup.Camera, Level.INFO);
        levelMap.put(LogGroup.General, Level.INFO);
        levelMap.put(LogGroup.Server, Level.INFO);
        levelMap.put(LogGroup.VisionProcess, Level.INFO);
    }

    static {
        currentAppenders.add(new ConsoleAppender());
    }

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
        currentAppenders.add(new AsyncFileAppender(logFilePath));
    }

    public static void setLevel(LogGroup group, Level newLevel) {
        levelMap.put(group, newLevel);
    }

    private static void log(String message, Level level, LogGroup group, String clazz) {
        for (var a : currentAppenders) {
            var shouldColor = a instanceof ConsoleAppender;
            var formattedMessage = format(message, level, group, clazz, shouldColor);
            a.log(formattedMessage);
        }
    }

    private static boolean shouldLog(Level logLevel, LogGroup group) {
        return logLevel.code <= levelMap.get(group).code;
    }

    public void error(String message) {
        if (shouldLog(Level.ERROR, group)) log(message, Level.ERROR, group, className);
    }

    public void warn(String message) {
        if (shouldLog(Level.WARN, group)) log(message, Level.WARN, group, className);
    }

    public void info(String message) {
        if (shouldLog(Level.INFO, group)) log(message, Level.INFO, group, className);
    }

    public void debug(String message) {
        if (shouldLog(Level.DEBUG, group)) log(message, Level.DEBUG, group, className);
    }

    public void trace(String message) {
        if (shouldLog(Level.TRACE, group)) log(message, Level.TRACE, group, className);
    }

    public void de_pest(String message) {
        if (shouldLog(Level.DE_PEST, group)) log(message, Level.DE_PEST, group, className);
    }

    private abstract static class Appender {
        abstract void log(String message);
    }

    private static class ConsoleAppender extends Appender {
        @Override
        void log(String message) {
            System.out.println(message);
        }
    }

    private static class AsyncFileAppender extends Appender {
        private Path filePath;

        public AsyncFileAppender(Path logFilePath) {
            this.filePath = logFilePath;
        }

        @Override
        void log(String message) {
            try (AsynchronousFileChannel asyncFile =
                    AsynchronousFileChannel.open(
                            filePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {

                asyncFile.write(ByteBuffer.wrap(message.getBytes()), 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
