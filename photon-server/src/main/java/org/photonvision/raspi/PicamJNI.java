package org.photonvision.raspi;

public class PicamJNI {
    static {
        System.loadLibrary("libpicam"); // Load native library libpicam.so
    }

    // Declare an instance native method sayHello() which receives no parameter and returns void
    private native void createCamera();

    // Test Driver
    public static void main(String[] args) {
        new PicamJNI().createCamera();  // Create an instance and invoke the native method
    }
}
