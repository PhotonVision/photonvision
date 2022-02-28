package org.photonvision.common.networking;

import edu.wpi.first.util.MulticastServiceResolver;
import edu.wpi.first.util.WPIUtilJNI;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class RioFinderTest {

    @Test
    public void testFind() {
        MulticastServiceResolver resolver = new MulticastServiceResolver("_ni._tcp");
        resolver.start();

        while(!Thread.currentThread().isInterrupted()) {
            var event = resolver.getEventHandle();
            try {
                if(!WPIUtilJNI.waitForObjectTimeout(event, 0)) {
                    var data = resolver.getData();
                    System.out.println(Arrays.toString(data));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
