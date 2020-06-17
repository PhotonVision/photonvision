package com.chameleonvision.common.vision;

import com.chameleonvision.common.vision.camera.CameraQuirks;
import com.chameleonvision.common.vision.camera.QuirkyCamera;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class QuirkyCameraTest {
    @Test
    public void ps3EyeTest() {
        QuirkyCamera psEye = new QuirkyCamera(0x1415, 0x2000, "psEye");
        Assertions.assertEquals(psEye.quirks, List.of(CameraQuirks.Gain));
    }

    @Test
    public void quirklessCameraTest() {
        QuirkyCamera noQuirk = new QuirkyCamera(1234, 888, "empty");
        Assertions.assertEquals(noQuirk.quirks, new ArrayList<>());
    }
}
