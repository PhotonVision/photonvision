package org.photonvision.common.networking;

import edu.wpi.first.util.MulticastServiceAnnouncer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class FakeRioAnnouncer {

    @Test
    public void testBroadcast() {
        Map<String, String> text = new HashMap<>();
        text.put("MAC", "fooabr");
        var a = new MulticastServiceAnnouncer("foo", "_ni._tcp", 19213, text);
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
