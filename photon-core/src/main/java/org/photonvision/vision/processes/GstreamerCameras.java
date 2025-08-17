package org.photonvision.vision.processes;

class GstreamerCameras {
  public static final int length = 2;
  public static final String[] pipelines = {
    "nvarguscamerasrc sensor-id=0 aelock=true  exposuretimerange=\"100000 200000\" gainrange=\"1 15\" ispdigitalgainrange=\"1 1\" ! "
        + "video/x-raw(memory:NVMM), width=1456, height=1088, framerate=30/1, format=NV12 ! "
        + "nvvidconv ! "
        + "video/x-raw, format=BGRx ! "
        + "appsink",
    "nvarguscamerasrc sensor-id=1 aelock=true  exposuretimerange=\"100000 200000\" gainrange=\"1 15\" ispdigitalgainrange=\"1 1\" ! "
        + "video/x-raw(memory:NVMM), width=1456, height=1088, framerate=30/1, format=NV12 ! "
        + "nvvidconv ! "
        + "video/x-raw, format=BGRx ! "
        + "appsink",
  };
  public static final String[] names = { "1", "2" };
}
