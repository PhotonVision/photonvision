package com.chameleonvision.util;

import edu.wpi.cscore.VideoMode;
import io.javalin.http.UploadedFile;
import org.opencv.core.Scalar;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Helpers {
    private static final String kServicePath = "/etc/systemd/system/chameleonVision.service";
    private static final String kServiceString = "[Unit]\n" +
            "Description=chameleon vision\n" +
            "\n" +
            "[Service]\n" +
            "ExecStart=/usr/bin/java -jar %s \n" +
            "StandardOutput=file:/var/log/something.out.txt\n" +
            "StandardError=file:/var/log/something.err.txt\n" +
            "Type=simple\n" +
            "WorkingDirectory=/usr/local/bin\n" +
            "\n" +
            "[Install]\n" +
            "WantedBy=multi-user.target\n" +
            "\n";

    private Helpers() {
    }

    public static Scalar colorToScalar(Color color) {
        return new Scalar(color.getBlue(), color.getGreen(), color.getRed());
    }

    public static HashMap VideoModeToHashMap(VideoMode videoMode) {
        return new HashMap<String, Object>() {{
            put("width", videoMode.width);
            put("height", videoMode.height);
            put("fps", videoMode.fps);
            put("pixelFormat", videoMode.pixelFormat.toString());
        }};
    }

    public static void setService(Path filePath) throws IOException, InterruptedException {
        String newService = String.format(kServiceString, filePath.toString());
        Writer writer = new FileWriter(kServicePath, false);
        writer.write(newService);
        writer.close();
        Process p = Runtime.getRuntime().exec("systemctl enable chameleonVision.service");
        p.waitFor();
    }
}
