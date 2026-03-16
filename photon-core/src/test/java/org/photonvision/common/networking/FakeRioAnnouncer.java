package org.photonvision.common.networking;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.photonvision.common.util.TestUtils;

import edu.wpi.first.net.MulticastServiceAnnouncer;

public class FakeRioAnnouncer {

    @Test
    public void testBroadcast() {
        TestUtils.loadLibraries();

        Map<String, String> text = new HashMap<>();
        text.put("MAC", "fooabr");
        var a = new MulticastServiceAnnouncer("foo", "_ni._tcp", 19213, text);
        assertTrue(a.hasImplementation());
        a.start();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //        a.close();
    }
}
