package org.photonvision.common.dataflow.websocket;

import edu.wpi.first.apriltag.AprilTagFieldLayout;

public class UIProgramSettings {
    public UIProgramSettings(
            UINetConfig networkSettings,
            UILightingConfig lighting,
            UIGeneralSettings general,
            AprilTagFieldLayout atfl) {
        this.networkSettings = networkSettings;
        this.lighting = lighting;
        this.general = general;
        this.atfl = atfl;
    }

    public UINetConfig networkSettings;
    public UILightingConfig lighting;
    public UIGeneralSettings general;
    public AprilTagFieldLayout atfl;
}
