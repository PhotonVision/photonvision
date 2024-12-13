package org.photonvision;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class PhotonSyslog {
    // Default Settings
    private String filepath = "/u/logs/photonvision/photonvision.log";
    private int maxSizeMb = 4;
    private int logRotations = 5;

    // Implementation defaults (not configurable)
    private static final Path SYSLOG_CONFIG_PATH = Paths.get("/etc/syslog-ng.d/photonvision.conf");
    private static final Path LOGROTATE_CONFIG_PATH = Paths.get("/etc/logrotate.d/photonvision.conf");
    private static final int PV_PORT = 51414;

    public static PhotonSyslog setupSyslog() {
        return new PhotonSyslog();
    }

    public static void disable() {
        try {
            Files.deleteIfExists(SYSLOG_CONFIG_PATH);
            Files.deleteIfExists(LOGROTATE_CONFIG_PATH);
        } catch (Exception e) {}
    }

    private PhotonSyslog() {}

    public PhotonSyslog setFilePath(String path) {
        filepath = path;
        return this;
    }

    public PhotonSyslog setMaxSizeMb(int megabytes) {
        maxSizeMb = megabytes;
        return this;
    }

    public PhotonSyslog setLogRotations(int rotations) {
        logRotations = rotations;
        return this;
    }

    public void commit() {
        String SYSLOG_TMP = "/tmp/pvsyslog.conf";
        String LOGROTATE_TMP = "/tmp/pvlogrotate.conf";
        try {
            var syslogFileOutput = new FileOutputStream(SYSLOG_TMP);
            var logrotateFileOutput = new FileOutputStream(LOGROTATE_TMP);

            syslogFileOutput.write(SyslogConfigFile.generate(filepath, PV_PORT).getBytes());
            logrotateFileOutput.write(LogRotateConfigFile.generate(filepath, maxSizeMb, logRotations).getBytes());

            syslogFileOutput.close();
            logrotateFileOutput.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (moveFile(SYSLOG_TMP, SYSLOG_CONFIG_PATH.toString()) != 0) {
            System.err.println("Failed to move photonvision syslog config.");
        }

        if (moveFile(LOGROTATE_TMP, LOGROTATE_CONFIG_PATH.toString()) != 0) {
            System.err.println("Failed to move photonvision syslog logrotate config.");
        }

        refreshSyslog();
    }

    private int execAdmin(String... command) {
        String[] args = new String[command.length + 2];
        args[0] = "ssh";
        args[1] = "admin@localhost";
        System.arraycopy(command, 0, args, 2, command.length);  
        return exec(args);
    }

    private int exec(String... command) {
        ProcessBuilder pb = new ProcessBuilder(command);

        int exitCode = 0;
        try {
            Process process = pb.start();

            process.waitFor(2, TimeUnit.SECONDS);
            exitCode = process.exitValue();
        } catch (Exception e) {
            e.printStackTrace();
            exitCode = 1;
        }

        return exitCode;
    }

    private void refreshSyslog() {
        // -HUP signal tells syslog to reload config
        int exitCode = execAdmin("/usr/bin/killall", "-HUP", "syslog-ng");

        if (exitCode != 0) {
            System.out.println("Failed to restart syslog-ng service with code " + exitCode);
        }
    }

    private int moveFile(String src, String dest) {
        return execAdmin("mv", src, dest);
    }

    protected static class SyslogConfigFile {
        private static final String FILE_TEMPLATE = """
            @version: 3.8
            
            source s_photonvision { tcp(ip(0.0.0.0) port(%d) max-connections(100)); udp(); };
            destination d_photonvision { file("%s"); };
            
            log { source(s_photonvision); destination(d_photonvision); };
            """;

        private static String generate(String filepath, int port) {
            return String.format(FILE_TEMPLATE, port, filepath);
        }
    }

    protected static class LogRotateConfigFile {
        private static final String FILE_TEMPLATE = """
            # Logrotate configuration for photonvision logs

            %s {
                    su lvuser ni
                    compresscmd /usr/bin/zip
                    compressext .zip
                    postrotate
                            /usr/bin/killall -HUP syslog-ng
                    endscript
                    size %dM
                    rotate %d
            }
            """;

        private static String generate(String logpath, int size, int rotate) {
            return String.format(FILE_TEMPLATE, logpath, size, rotate);
        }
    }
}
