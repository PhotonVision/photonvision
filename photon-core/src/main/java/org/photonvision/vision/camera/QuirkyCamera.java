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

package org.photonvision.vision.camera;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class QuirkyCamera {
    private static final List<QuirkyCamera> quirkyCameras =
            List.of(
                    new QuirkyCamera(
                        0x2560, 0xc128, 
                        "See3Cam_24CUG", 
                        CameraQuirk.Gain, CameraQuirk.See3Cam_24CUG
                    ),
                    new QuirkyCamera(
                            0x9331,
                            0x5A3,
                            CameraQuirk.CompletelyBroken), // Chris's older generic "Logitec HD Webcam"
                    new QuirkyCamera(0x825, 0x46D, CameraQuirk.CompletelyBroken), // Logitec C270
                    new QuirkyCamera(
                            0x0bda,
                            0x5510,
                            CameraQuirk.CompletelyBroken), // A laptop internal camera someone found broken
                    new QuirkyCamera(
                            -1, -1, "Snap Camera", CameraQuirk.CompletelyBroken), // SnapCamera on Windows
                    new QuirkyCamera(
                            -1,
                            -1,
                            "FaceTime HD Camera",
                            CameraQuirk.CompletelyBroken), // Mac Facetime Camera shared into Windows in Bootcamp
                    new QuirkyCamera(0x2000, 0x1415, CameraQuirk.Gain, CameraQuirk.FPSCap100), // PS3Eye
                    new QuirkyCamera(
                            -1, -1, "mmal service 16.1", CameraQuirk.PiCam), // PiCam (via V4L2, not zerocopy)
                    new QuirkyCamera(-1, -1, "unicam", CameraQuirk.PiCam), // PiCam (via V4L2, not zerocopy)
                    new QuirkyCamera(0x85B, 0x46D, CameraQuirk.AdjustableFocus), // Logitech C925-e
                    // Generic arducam. Since OV2311 can't be differentiated at first boot, apply stickyFPS to
                    // the generic case, too
                    new QuirkyCamera(
                            0x0c45,
                            0x6366,
                            "",
                            "Arducam Generic",
                            CameraQuirk.ArduCamCamera,
                            CameraQuirk.StickyFPS),
                    // Arducam OV2311
                    new QuirkyCamera(
                            0x0c45,
                            0x6366,
                            "OV2311",
                            "OV2311",
                            CameraQuirk.ArduCamCamera,
                            CameraQuirk.ArduOV2311,
                            CameraQuirk.StickyFPS),
                    // Arducam OV9281
                    new QuirkyCamera(
                            0x0c45,
                            0x6366,
                            "OV9281",
                            "OV9281",
                            CameraQuirk.ArduCamCamera,
                            CameraQuirk.ArduOV9281));

    public static final QuirkyCamera DefaultCamera = new QuirkyCamera(0, 0, "");
    public static final QuirkyCamera ZeroCopyPiCamera =
            new QuirkyCamera(
                    -1,
                    -1,
                    "mmal service 16.1",
                    CameraQuirk.PiCam,
                    CameraQuirk.Gain,
                    CameraQuirk.AWBGain); // PiCam (special zerocopy version)

    @JsonProperty("baseName")
    public final String baseName;

    @JsonProperty("usbVid")
    public final int usbVid;

    @JsonProperty("usbPid")
    public final int usbPid;

    @JsonProperty("displayName")
    public final String displayName;

    @JsonProperty("quirks")
    public final HashMap<CameraQuirk, Boolean> quirks;

    /**
     * Creates a QuirkyCamera that matches by USB VID/PID
     *
     * @param usbVid USB VID of camera
     * @param usbPid USB PID of camera
     * @param quirks Camera quirks
     */
    private QuirkyCamera(int usbVid, int usbPid, CameraQuirk... quirks) {
        this(usbVid, usbPid, "", quirks);
    }

    /**
     * Creates a QuirkyCamera that matches by USB VID/PID and name
     *
     * @param usbVid USB VID of camera
     * @param usbPid USB PID of camera
     * @param baseName CSCore name of camera
     * @param quirks Camera quirks
     */
    private QuirkyCamera(int usbVid, int usbPid, String baseName, CameraQuirk... quirks) {
        this(usbVid, usbPid, baseName, "", quirks);
    }

    /**
     * Creates a QuirkyCamera that matches by USB VID/PID and name
     *
     * @param usbVid USB VID of camera
     * @param usbPid USB PID of camera
     * @param baseName CSCore name of camera
     * @param displayName Human-friendly quicky camera name
     * @param quirks Camera quirks
     */
    private QuirkyCamera(
            int usbVid, int usbPid, String baseName, String displayName, CameraQuirk... quirks) {
        this.usbVid = usbVid;
        this.usbPid = usbPid;
        this.baseName = baseName;
        this.displayName = displayName;

        this.quirks = new HashMap<>();
        for (var q : quirks) {
            this.quirks.put(q, true);
        }
        for (var q : CameraQuirk.values()) {
            this.quirks.putIfAbsent(q, false);
        }
    }

    @JsonCreator
    public QuirkyCamera(
            @JsonProperty("baseName") String baseName,
            @JsonProperty("usbVid") int usbVid,
            @JsonProperty("usbPid") int usbPid,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("quirks") HashMap<CameraQuirk, Boolean> quirks) {
        this.baseName = baseName;
        this.usbPid = usbPid;
        this.usbVid = usbVid;
        this.quirks = quirks;
        this.displayName = displayName;
    }

    public boolean hasQuirk(CameraQuirk quirk) {
        return quirks.get(quirk);
    }

    public static QuirkyCamera getQuirkyCamera(int usbVid, int usbPid) {
        return getQuirkyCamera(usbVid, usbPid, "");
    }

    public static QuirkyCamera getQuirkyCamera(int usbVid, int usbPid, String baseName) {
        for (var qc : quirkyCameras) {
            boolean hasBaseName = !qc.baseName.isEmpty();
            boolean matchesBaseName = qc.baseName.equals(baseName) || !hasBaseName;
            // If we have a quirkycamera we need to copy the quirks from our predefined object and create
            // a quirkycamera object with the baseName.
            if (qc.usbVid == usbVid && qc.usbPid == usbPid && matchesBaseName) {
                List<CameraQuirk> quirks = new ArrayList<CameraQuirk>();
                for (var q : CameraQuirk.values()) {
                    if (qc.hasQuirk(q)) quirks.add(q);
                }
                QuirkyCamera c =
                        new QuirkyCamera(
                                usbVid,
                                usbPid,
                                baseName,
                                Arrays.copyOf(quirks.toArray(), quirks.size(), CameraQuirk[].class));
                return c;
            }
        }
        return new QuirkyCamera(usbVid, usbPid, baseName);
    }

    public boolean hasQuirks() {
        return quirks.containsValue(true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuirkyCamera that = (QuirkyCamera) o;
        return usbVid == that.usbVid
                && usbPid == that.usbPid
                && Objects.equals(baseName, that.baseName)
                && Objects.equals(quirks, that.quirks);
    }

    @Override
    public String toString() {
        String ret =
                "QuirkyCamera [baseName="
                        + baseName
                        + ", displayName="
                        + displayName
                        + ", usbVid="
                        + usbVid
                        + ", usbPid="
                        + usbPid
                        + ", quirks="
                        + quirks.toString()
                        + "]";
        return ret;
    }

    @Override
    public int hashCode() {
        return Objects.hash(usbVid, usbPid, baseName, quirks);
    }

    /**
     * Add/remove quirks from the camera we're controlling
     *
     * @param quirksToChange map of true/false for quirks we should change
     */
    public void updateQuirks(HashMap<CameraQuirk, Boolean> quirksToChange) {
        for (var q : quirksToChange.entrySet()) {
            var quirk = q.getKey();
            var hasQuirk = q.getValue();

            this.quirks.put(quirk, hasQuirk);
        }
    }
}
