package org.photonvision;

import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.common.configuration.ConfigManager;
import org.photonvision.common.datatransfer.networktables.NetworkTablesManager;
import org.photonvision.common.networking.NetworkManager;
import org.photonvision.common.util.TestUtils;
import org.photonvision.server.Server;
import org.photonvision.vision.camera.USBCameraSource;
import org.photonvision.vision.pipeline.CVPipelineSettings;
import org.photonvision.vision.processes.VisionModuleManager;
import org.photonvision.vision.processes.VisionSource;
import org.photonvision.vision.processes.VisionSourceManager;

import java.util.HashMap;
import java.util.List;

public class Main {

    public static final int DEFAULT_WEBPORT = 5800;

    public static void main(String[] args) {
        TestUtils.loadLibraries();
        ConfigManager.getInstance(); // init config manager
        NetworkManager.getInstance().initialize(false); // basically empty. todo: link to ConfigManager?
        NetworkTablesManager.setClientMode("127.0.0.1");

        HashMap<String, CameraConfiguration> camConfigs = ConfigManager.getInstance().getConfig().getCameraConfigurations();
        var sources = VisionSourceManager.LoadAllSources(camConfigs.values());

        var collectedSources = new HashMap<VisionSource, List<CVPipelineSettings>>();
        for (var src : sources) {
            var usbSrc = (USBCameraSource)src;
            collectedSources.put(usbSrc, usbSrc.configuration.pipelineSettings);
        }

        var vmm = new VisionModuleManager(collectedSources);

        vmm.startModules();
        Server.main(DEFAULT_WEBPORT);
    }
}
