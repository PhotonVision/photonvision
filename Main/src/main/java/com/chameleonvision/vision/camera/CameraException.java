package com.chameleonvision.vision.camera;

public class CameraException extends Exception {
    public enum CameraExceptionType {
        NO_CAMERA,
        BAD_CAMERA,
        BAD_PIPELINE,
        BAD_SETTING;

        @Override
        public String toString() {
            switch (this) {
                case NO_CAMERA: return "No camera connected!";
                case BAD_CAMERA: return "Invalid camera!";
                case BAD_PIPELINE: return "Invalid pipeline!";
                case BAD_SETTING: return "Invalid camera/pipeline setting!";
                default: return "Unknown camera exception!";
            }
        }
    }

    CameraException(CameraExceptionType camExceptionType) {
        super(camExceptionType.toString());
    }
}
