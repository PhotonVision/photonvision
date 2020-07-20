package org.photonvision.raspi;

import edu.wpi.first.wpiutil.RuntimeLoader;
import edu.wpi.first.wpiutil.WPIUtilJNI;

import java.io.IOException;

public class PicamJNI {

    static RuntimeLoader<PicamJNI> loader;
    private static boolean libraryLoaded;

    static {
//        System.loadLibrary("libpicam"); // Load native library libpicam.so

        try {
            loader = new RuntimeLoader<>("wpiutiljni", RuntimeLoader.getDefaultExtractionRoot(), PicamJNI.class);
            loader.loadLibrary();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        libraryLoaded = true;

    }

    // Declare an instance native method sayHello() which receives no parameter and returns void
    private native void createCamera();

    // Test Driver
    public static void main(String[] args) {
        new PicamJNI().createCamera();  // Create an instance and invoke the native method
    }
}
