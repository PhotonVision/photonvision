package com.chameleonvision.util;

import java.io.*;

/**
 * Execute external process and optionally read output buffer.
 */
public class ShellExec {
    private int exitCode;
    private boolean readOutput, readError;
    private StreamGobbler errorGobbler, outputGobbler;

    public ShellExec() {
        this(false, false);
    }

    public ShellExec(boolean readOutput, boolean readError) {
        this.readOutput = readOutput;
        this.readError = readError;
    }

    /**
     * Execute a command in current folder, and wait for process to end
     * @param command   command ("c:/some/folder/script.bat" or "some/folder/script.sh")
     * @param args  0..n command line arguments
     * @return  process exit code
     */
    public int execute(String command, String... args) throws IOException {
        return execute(command, null, true, args);
    }

    /**
     * Execute a command.
     * @param command   command ("c:/some/folder/script.bat" or "some/folder/script.sh")
     * @param workdir   working directory or NULL to use command folder
     * @param wait  wait for process to end
     * @param args  0..n command line arguments
     * @return  process exit code
     */
    public int execute(String command, String workdir, boolean wait, String...args) throws IOException {
        String[] cmdArr;
        if (args != null && args.length > 0) {
            cmdArr = new String[1+args.length];
            cmdArr[0] = command;
            System.arraycopy(args, 0, cmdArr, 1, args.length);
        } else {
            cmdArr = new String[] { command };
        }

        ProcessBuilder pb =  new ProcessBuilder(cmdArr);
        File workingDir = (workdir==null ? new File(command).getParentFile() : new File(workdir) );
        pb.directory(workingDir);

        Process process = pb.start();

        // Consume streams, older jvm's had a memory leak if streams were not read,
        // some other jvm+OS combinations may block unless streams are consumed.
        errorGobbler  = new StreamGobbler(process.getErrorStream(), readError);
        outputGobbler = new StreamGobbler(process.getInputStream(), readOutput);
        errorGobbler.start();
        outputGobbler.start();

        exitCode = 0;
        if (wait) {
            try {
                process.waitFor();
                exitCode = process.exitValue();
            } catch (InterruptedException ignored) { }
        }
        return exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }

    public boolean isOutputCompleted() {
        return (outputGobbler != null && outputGobbler.isCompleted());
    }

    public boolean isErrorCompleted() {
        return (errorGobbler != null && errorGobbler.isCompleted());
    }

    public String getOutput() {
        return (outputGobbler != null ? outputGobbler.getOutput() : null);
    }

    public String getError() {
        return (errorGobbler != null ? errorGobbler.getOutput() : null);
    }

//********************************************
//********************************************

    /**
     * StreamGobbler reads inputstream to "gobble" it.
     * This is used by Executor class when running
     * a commandline applications. Gobblers must read/purge
     * INSTR and ERRSTR process streams.
     * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html?page=4
     */
    @SuppressWarnings("WeakerAccess")
    private static class StreamGobbler extends Thread {
        private InputStream is;
        private StringBuilder output;
        private volatile boolean completed; // mark volatile to guarantee a thread safety

        public StreamGobbler(InputStream is, boolean readStream) {
            this.is = is;
            this.output = (readStream ? new StringBuilder(256) : null);
        }

        public void run() {
            completed = false;
            try {
                String NL = System.getProperty("line.separator", "\r\n");

                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ( (line = br.readLine()) != null) {
                    if (output != null)
                        output.append(line).append(NL);
                }
            } catch (IOException ex) {
                // ex.printStackTrace();
            }
            completed = true;
        }

        /**
         * Get inputstream buffer or null if stream
         * was not consumed.
         * @return
         */
        public String getOutput() {
            return (output != null ? output.toString() : null);
        }

        /**
         * Is input stream completed.
         * @return
         */
        public boolean isCompleted() {
            return completed;
        }

    }

}
