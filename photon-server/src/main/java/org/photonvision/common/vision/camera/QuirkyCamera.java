package org.photonvision.common.vision.camera;

import java.util.ArrayList;
import java.util.List;

public class QuirkyCamera {
    public static final List<QuirkyCamera> quirkyCameras =
            List.of(
                    // ps3 eye
                    new QuirkyCamera(0x1415, 0x2000, "PS3Eye", List.of(CameraQuirks.Gain)));

    public final int usbVid;
    public final int usbPid;
    public final String name;
    public List<CameraQuirks> quirks;

    public QuirkyCamera(int usbVid, int usbPid, String baseName, List<CameraQuirks> quirks) {
        this.usbVid = usbVid;
        this.usbPid = usbPid;
        this.name = baseName;
        this.quirks = quirks;
    }

    public QuirkyCamera(int usbVid, int usbPid, String baseName) {
        this(usbVid, usbPid, baseName, new ArrayList<>());
        QuirkyCamera quirky =
                quirkyCameras.stream()
                        .filter(quirkyCamera -> quirkyCamera.usbPid == usbPid && quirkyCamera.usbVid == usbVid)
                        .findFirst()
                        .orElse(null);
        if (quirky != null) {
            this.quirks = quirky.quirks;
        }
    }
}
