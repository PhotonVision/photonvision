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

package org.photonvision.vision.frame.consumer;

import edu.wpi.first.cscore.*;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.util.PixelFormat;
import java.util.ArrayList;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.frame.StaticFrames;
import org.photonvision.vision.opencv.CVMat;

public class MJPGFrameConsumer implements AutoCloseable {
    private static final double MAX_FRAMERATE = -1;
    private static final long MAX_FRAME_PERIOD_NS = Math.round(1e9 / MAX_FRAMERATE);

    private long lastFrameTimeNs;
    private CvSource cvSource;
    private MjpegServer mjpegServer;

    private VideoListener listener;

    private final NetworkTable table;

    public MJPGFrameConsumer(String sourceName, int width, int height, int port) {
        this.cvSource = new CvSource(sourceName, PixelFormat.kMJPEG, width, height, 30);
        this.table =
                NetworkTableInstance.getDefault().getTable("/CameraPublisher").getSubTable(sourceName);

        this.mjpegServer = new MjpegServer("serve_" + cvSource.getName(), port);
        mjpegServer.setSource(cvSource);
        mjpegServer.setCompression(75);

        listener =
                new VideoListener(
                        event -> {
                            if (event.kind == VideoEvent.Kind.kNetworkInterfacesChanged) {
                                table.getEntry("source").setString("cv:");
                                table.getEntry("streams");
                                table.getEntry("connected").setBoolean(true);
                                table.getEntry("mode").setString(videoModeToString(cvSource.getVideoMode()));
                                table.getEntry("modes").setStringArray(getSourceModeValues(cvSource.getHandle()));
                                updateStreamValues();
                            }
                        },
                        0x4fff,
                        true);
    }

    private synchronized void updateStreamValues() {
        // Get port
        int port = mjpegServer.getPort();

        // Generate values
        var addresses = CameraServerJNI.getNetworkInterfaces();
        ArrayList<String> values = new ArrayList<>(addresses.length + 1);
        String listenAddress = CameraServerJNI.getMjpegServerListenAddress(mjpegServer.getHandle());
        if (!listenAddress.isEmpty()) {
            // If a listen address is specified, only use that
            values.add(makeStreamValue(listenAddress, port));
        } else {
            // Otherwise generate for hostname and all interface addresses
            values.add(makeStreamValue(CameraServerJNI.getHostname() + ".local", port));
            for (String addr : addresses) {
                if ("127.0.0.1".equals(addr)) {
                    continue; // ignore localhost
                }
                values.add(makeStreamValue(addr, port));
            }
        }

        String[] streamAddresses = values.toArray(new String[0]);
        table.getEntry("streams").setStringArray(streamAddresses);
    }

    public MJPGFrameConsumer(String name, int port) {
        this(name, 320, 240, port);
    }

    public void accept(CVMat image) {
        long now = MathUtils.wpiNanoTime();

        if (image == null || image.getMat() == null || image.getMat().empty()) {
            image.copyFrom(StaticFrames.LOST_MAT);
        }

        if (now - lastFrameTimeNs > MAX_FRAME_PERIOD_NS) {
            lastFrameTimeNs = now;
            cvSource.putFrame(image.getMat());
        }
    }

    public int getCurrentStreamPort() {
        return mjpegServer.getPort();
    }

    private static String makeStreamValue(String address, int port) {
        return "mjpg:http://" + address + ":" + port + "/?action=stream";
    }

    private static String[] getSourceModeValues(int sourceHandle) {
        VideoMode[] modes = CameraServerJNI.enumerateSourceVideoModes(sourceHandle);
        String[] modeStrings = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            modeStrings[i] = videoModeToString(modes[i]);
        }
        return modeStrings;
    }

    private static String videoModeToString(VideoMode mode) {
        return mode.width
                + "x"
                + mode.height
                + " "
                + pixelFormatToString(mode.pixelFormat)
                + " "
                + mode.fps
                + " fps";
    }

    private static String pixelFormatToString(PixelFormat pixelFormat) {
        return switch (pixelFormat) {
            case kMJPEG -> "MJPEG";
            case kYUYV -> "YUYV";
            case kRGB565 -> "RGB565";
            case kBGR -> "BGR";
            case kGray -> "Gray";
            case kUYVY, kUnknown, kY16, kBGRA -> "Unknown";
        };
    }

    @Override
    public void close() {
        table.getEntry("connected").setBoolean(false);
        mjpegServer.close();
        cvSource.close();
        listener.close();
        mjpegServer = null;
        cvSource = null;
        listener = null;
    }
}
