package org.photonvision.common.dataflow.websocket;

public class UILightingConfig {
    public UILightingConfig(int brightness, boolean supported) {
        this.brightness = brightness;
        this.supported = supported;
    }

    public int brightness = 0;
    public boolean supported = true;
}
