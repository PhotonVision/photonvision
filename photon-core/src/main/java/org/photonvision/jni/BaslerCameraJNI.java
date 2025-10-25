package org.photonvision.jni;

import java.io.IOException;
import java.util.List;
import org.photonvision.common.util.TestUtils;

public class BaslerCameraJNI extends PhotonJNICommon {
    private boolean isLoaded;
    private static BaslerCameraJNI instance = null;

    private BaslerCameraJNI() {
        isLoaded = false;
    }

    public static BaslerCameraJNI getInstance() {
        if (instance == null) instance = new BaslerCameraJNI();

        return instance;
    }

    public static synchronized void forceLoad() throws IOException {
        TestUtils.loadLibraries();

        forceLoad(getInstance(), BaslerCameraJNI.class, List.of("baslerjni"));
    }

    @Override
    public boolean isLoaded() {
        return isLoaded;
    }

    @Override
    public void setLoaded(boolean state) {
        isLoaded = state;
    }
}
