package org.photonvision.vision.processes;

class GstreamerCameras {
  public static final String[] cameras = {
    "nvarguscamerasrc sensor-id=0 aelock=true  exposuretimerange=\"100000 200000\" gainrange=\"1 15\" ispdigitalgainrange=\"1 1\" ! "
        + "video/x-raw(memory:NVMM), width=1456, height=1088, framerate=30/1, format=NV12 ! "
        + "nvvidconv ! "
        + "video/x-raw, format=BGRx ! "
        + "appsink",
  };
}
