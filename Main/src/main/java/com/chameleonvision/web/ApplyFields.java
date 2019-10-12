package com.chameleonvision.web;

import com.chameleonvision.vision.Orientation;
import com.chameleonvision.vision.SortMode;
import com.chameleonvision.vision.TargetGroup;
import com.chameleonvision.vision.TargetIntersection;
import com.chameleonvision.vision.camera.CameraException;
import com.chameleonvision.vision.camera.CameraManager;
import edu.wpi.cscore.VideoException;
import org.msgpack.value.ImmutableArrayValue;

import static com.chameleonvision.web.Server.handler;

public class ApplyFields {
    public static void setExposure(Object value) {
        int newExposure = (int) value;
        System.out.printf("Changing exposure to %d\n", newExposure);
        try {
            CameraManager.getCurrentCamera().setExposure(newExposure);
        } catch (VideoException | CameraException e) {
            System.out.println("Exposure changes is not supported on your webcam/webcam's driver");
        }
    }

    public static void setBrightness(Object value) {
        int newBrightness = (int) value;
        System.out.printf("Changing brightness to %d\n", newBrightness);
        try {
            CameraManager.getCurrentCamera().setBrightness(newBrightness);
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    public static void setOrientation(Object value) {
        try {
            CameraManager.getCurrentPipeline().orientation = Orientation.values()[(int) value];
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    public static void setHue(Object value) {
        try {
            CameraManager.getCurrentPipeline().hue = handler.getIntList((ImmutableArrayValue) value);
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    public static void setSaturation(Object value) {
        try {
            CameraManager.getCurrentPipeline().saturation = handler.getIntList((ImmutableArrayValue) value);
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    public static void setValue(Object value) {
        try {
            CameraManager.getCurrentPipeline().value = handler.getIntList((ImmutableArrayValue) value);
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    public static void setErode(Object value) {
        try {
            CameraManager.getCurrentPipeline().erode = (boolean) value;
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    public static void setDilate(Object value) {
        try {
            CameraManager.getCurrentPipeline().dilate = (boolean) value;
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    public static void setArea(Object value) {
        try {
            CameraManager.getCurrentPipeline().area = handler.getFloatList((ImmutableArrayValue) value);
            ;
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    public static void setRatio(Object value) {
        try {
            CameraManager.getCurrentPipeline().ratio = handler.getFloatList((ImmutableArrayValue) value);
            ;
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    public static void setExtent(Object value) {
        try {
            CameraManager.getCurrentPipeline().extent = handler.getIntList((ImmutableArrayValue) value);
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    public static void setTargetGroup(Object value) {
        try {
            CameraManager.getCurrentPipeline().targetGroup = TargetGroup.values()[(int) value];
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    public static void setTargetIntersection(Object value) {
        try {
            CameraManager.getCurrentPipeline().targetIntersection = TargetIntersection.values()[(int) value];
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    public static void setSortMode(Object value) {
        try {
            CameraManager.getCurrentPipeline().sortMode = SortMode.values()[(int) value];
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    public static void setCurrCamera(Object value) {
        String newCamera = (String) value;
        System.out.printf("Changing camera to %s\n", newCamera);
        try {
            CameraManager.setCurrentCamera(newCamera);
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    public static void setCurrPipeline(Object value) {
        int newPipeline = (int) value;
        System.out.printf("Changing pipeline to %s\n", newPipeline);
        try {
            CameraManager.setCurrentPipeline(newPipeline);
            CameraManager.getCurrentCameraProcess().ntPipelineEntry.setNumber(newPipeline);
            ServerHandler.broadcastMessage(ServerHandler.allFieldsToMap(CameraManager.getCurrentPipeline()));
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    public static void setResolution(Object value) {
        int newVideoMode = (int) value;
        System.out.printf("Changing video mode to %d\n", newVideoMode);
        try {
            CameraManager.getCurrentCamera().setCamVideoMode(newVideoMode, true);
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    public static void setFOV(Object value) {
        float newFov = ((Integer) value) * 1F;//TODO check this
        System.out.printf("Changing FOV to %f\n", newFov);
        try {
            CameraManager.getCurrentCamera().setFOV(newFov);
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }
}
