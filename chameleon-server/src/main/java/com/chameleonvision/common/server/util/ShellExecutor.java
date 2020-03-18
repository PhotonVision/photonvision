package com.chameleonvision.common.server.util;

import org.apache.commons.exec.*;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

// TODO: Finish me!
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class ShellExecutor {

    private final Executor executor;
    private final ExecuteWatchdog watchdog;
    private final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
    private final OutputStream stdOutStream = new ByteArrayOutputStream();
    private final OutputStream stdErrStream = new ByteArrayOutputStream();
    private final boolean block;

    public ShellExecutor(String command, boolean block, int timeoutMillis, String... args) {
        this.block = block;

        CommandLine cmdLine = new CommandLine(command);
        cmdLine.addArguments(args);

        watchdog = new ExecuteWatchdog(timeoutMillis);
        executor = new DefaultExecutor();
        executor.setWatchdog(watchdog);
        executor.setStreamHandler(new PumpStreamHandler(stdOutStream, stdErrStream));
    }

//    public int execute() {
//        if ()
//    }

    public String getStdOut() {
        if (!watchdog.isWatching()) {
            return executor.toString();
        }
        return "";
    }

    public String getStdErr() {
        return "";
    }
}
