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
import java.awt.*;
import java.util.ArrayList;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.util.ColorHelper;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.frame.Frame;

//=======================================================================================
//
//  This class manages video streamed to dashboards and other network clients.
//
public class MJPGFrameConsumer {

    public static final Mat EMPTY_MAT = new Mat(60, 15 * 7, CvType.CV_8UC3);
    private static final double EMPTY_FRAMERATE = 2;
    private long lastEmptyTime;

    static {
        EMPTY_MAT.setTo(ColorHelper.colorToScalar(Color.BLACK));
        var col = 0;
        Imgproc.rectangle(
                EMPTY_MAT,
                new Rect(col, 0, 15, EMPTY_MAT.height()),
                ColorHelper.colorToScalar(new Color(0xa2a2a2)),
                -1);
        col += 15;
        Imgproc.rectangle(
                EMPTY_MAT,
                new Rect(col, 0, 15, EMPTY_MAT.height()),
                ColorHelper.colorToScalar(new Color(0xa2a300)),
                -1);
        col += 15;
        Imgproc.rectangle(
                EMPTY_MAT,
                new Rect(col, 0, 15, EMPTY_MAT.height()),
                ColorHelper.colorToScalar(new Color(0x00a3a2)),
                -1);
        col += 15;
        Imgproc.rectangle(
                EMPTY_MAT,
                new Rect(col, 0, 15, EMPTY_MAT.height()),
                ColorHelper.colorToScalar(new Color(0x00a200)),
                -1);
        col += 15;
        Imgproc.rectangle(
                EMPTY_MAT,
                new Rect(col, 0, 15, EMPTY_MAT.height()),
                ColorHelper.colorToScalar(new Color(0x440045)),
                -1);
        col += 15;
        Imgproc.rectangle(
                EMPTY_MAT,
                new Rect(col, 0, 15, EMPTY_MAT.height()),
                ColorHelper.colorToScalar(new Color(0x0000a2)),
                -1);
        col += 15;
        Imgproc.rectangle(
                EMPTY_MAT,
                new Rect(col, 0, 15, EMPTY_MAT.height()),
                ColorHelper.colorToScalar(new Color(0)),
                -1);
        Imgproc.rectangle(
                EMPTY_MAT,
                new Rect(0, 50, EMPTY_MAT.width(), 10),
                ColorHelper.colorToScalar(new Color(0)),
                -1);
        Imgproc.rectangle(
                EMPTY_MAT, new Rect(15, 50, 30, 10), ColorHelper.colorToScalar(Color.WHITE), -1);

        Imgproc.putText(
                EMPTY_MAT, "Stream", new Point(14, 20), 0, 0.6, ColorHelper.colorToScalar(Color.white), 2);
        Imgproc.putText(
                EMPTY_MAT,
                "Disabled",
                new Point(14, 45),
                0,
                0.6,
                ColorHelper.colorToScalar(Color.white),
                2);
        Imgproc.putText(
                EMPTY_MAT, "Stream", new Point(14, 20), 0, 0.6, ColorHelper.colorToScalar(Color.RED), 1);
        Imgproc.putText(
                EMPTY_MAT, "Disabled", new Point(14, 45), 0, 0.6, ColorHelper.colorToScalar(Color.RED), 1);
    }

    private CvSource cvSource;
    private MjpegServer mjpegServer;

    @SuppressWarnings("FieldCanBeLocal")
    private VideoListener listener;

    private final NetworkTable table;
    boolean isDisabled = false;

    private String privSourceName;

    private Logger logger = new Logger(MJPGFrameConsumer.class, LogGroup.VisionModule);

    //--------are any streaming clients connected
    //--------  -1 = unknown
    //--------  0 = no connected clients
    //--------  1 = clients connected
    private int privClientsConnected = -1;

    //------------------------------------------------------------------------------------
    //  constructor
    //
    public MJPGFrameConsumer(String sourceName, int width, int height, int port) {

        //--------save source name for logging
        privSourceName = sourceName;

        this.cvSource = new CvSource(sourceName, VideoMode.PixelFormat.kMJPEG, width, height, 30);
        this.table =
                NetworkTableInstance.getDefault().getTable("/CameraPublisher").getSubTable(sourceName);

        this.mjpegServer = new MjpegServer("serve_" + cvSource.getName(), port);
        mjpegServer.setSource(cvSource);

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
                                logger.info( privSourceName + " - Received event - kNetworkInterfacesChanged");
                            }
                        },
                        0x4fff,
                        true);

        logger.info( privSourceName + " - server created.");
    }

    //------------------------------------------------------------------------------------
    //
    //  update network tables entry with stream connection URLs
    //
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

    //------------------------------------------------------------------------------------
    //
    //  alternate constructor, with hardcoded resolution.
    //
    public MJPGFrameConsumer(String name, int port) {
        this(name, 320, 240, port);
    }

    //------------------------------------------------------------------------------------
    //
    //  accept a new video frame and update the stream source.
    //
    public void accept(Frame frame) {
        if (frame != null && !frame.image.getMat().empty()) {
            cvSource.putFrame(frame.image.getMat());

            // Make sure our disabled framerate limiting doesn't get confused
            isDisabled = false;
            lastEmptyTime = 0;
        }
    }

    //------------------------------------------------------------------------------------
    //
    //  This is called if video streaming is disallowed.  It sets a disabled flag.
    //  A "Streaming Disabled" picture will be sent to the stream source once a second.
    //  With implementation of the AnyConnectedClients, this should only be called
    //  when there are no active stream clients (dashboards).
    //
    public void disabledTick() {
        if (!isDisabled) {
            cvSource.setVideoMode(VideoMode.PixelFormat.kMJPEG, EMPTY_MAT.width(), EMPTY_MAT.height(), 0);
            isDisabled = true;
        }

        if (System.currentTimeMillis() - lastEmptyTime > 1000.0 / EMPTY_FRAMERATE) {
            cvSource.putFrame(EMPTY_MAT);
            lastEmptyTime = System.currentTimeMillis();
        }
    }

    //------------------------------------------------------------------------------------
    //
    //  Return the TCP port number for this object.
    //
    public int getCurrentStreamPort() {
        return mjpegServer.getPort();
    }

    //------------------------------------------------------------------------------------
    //
    //  Create the stream URL string.
    //
    private static String makeStreamValue(String address, int port) {
        return "mjpg:http://" + address + ":" + port + "/?action=stream";
    }

    //------------------------------------------------------------------------------------
    //
    //  Get video source modes
    //  
    private static String[] getSourceModeValues(int sourceHandle) {
        VideoMode[] modes = CameraServerJNI.enumerateSourceVideoModes(sourceHandle);
        String[] modeStrings = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            modeStrings[i] = videoModeToString(modes[i]);
        }
        return modeStrings;
    }

    //------------------------------------------------------------------------------------
    //
    //  Format video mode for sending to NetworkTables
    //
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

    //------------------------------------------------------------------------------------
    //
    //  Format video mode pixel format
    //
    private static String pixelFormatToString(VideoMode.PixelFormat pixelFormat) {
        switch (pixelFormat) {
            case kMJPEG:
                return "MJPEG";
            case kYUYV:
                return "YUYV";
            case kRGB565:
                return "RGB565";
            case kBGR:
                return "BGR";
            case kGray:
                return "Gray";
            default:
                return "Unknown";
        }
    }

    //------------------------------------------------------------------------------------
    //   
    //  shutdown this object
    //
    public void close() {
        table.getEntry("connected").setBoolean(false);
        mjpegServer.close();
        cvSource.close();
        listener.close();
        mjpegServer = null;
        cvSource = null;
        listener = null;
        logger = null;
    }

    //------------------------------------------------------------------------------------
    //   
    //  Determine if any clients are connected to this stream server.
    //  
    //
    public boolean anyClientConnections() {

        //--------query the cscore stream server to see if there are connected clients
        boolean locBoolClientsConnected = this.mjpegServer.getSource().isEnabled();

        //--------if the value changed, log it.
        int locIntClientsConnected = (locBoolClientsConnected ? 1:0);
        if ( locIntClientsConnected != privClientsConnected ) {
            privClientsConnected = locIntClientsConnected;
            if ( locBoolClientsConnected )
                logger.info( privSourceName + " - Mjpeg streaming client connected.");
            else
                logger.info( privSourceName + " - Mjpeg streaming, all clients disconnected.");
        }
        return locBoolClientsConnected; 
    }
}
