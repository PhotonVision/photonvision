package org.photonvision.common.networking;

import edu.wpi.first.net.MulticastServiceResolver;
import edu.wpi.first.util.WPIUtilJNI;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.photonvision.common.util.TestUtils;

public class RoborioFinderTest {

    @Test
    public void testFind() {
        TestUtils.loadLibraries();

        RoborioFinder.getInstance().start();

        try {
            for (int i = 0; i < 100; i++) {
                RoborioFinder.getInstance().findAll();
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        RoborioFinder.getInstance().stop();
    }
}
