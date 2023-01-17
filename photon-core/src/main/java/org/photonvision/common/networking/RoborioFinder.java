/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.common.networking;

import edu.wpi.first.cscore.CameraServerJNI;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.photonvision.common.dataflow.DataChangeService;
import org.photonvision.common.dataflow.events.OutgoingUIEvent;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

public class RoborioFinder {
    private static RoborioFinder instance;
    private static final Logger logger = new Logger(RoborioFinder.class, LogGroup.General);

    public static RoborioFinder getInstance() {
        if (instance == null) instance = new RoborioFinder();
        return instance;
    }

    public void findRios() {
        HashMap<String, Object> map = new HashMap<>();
        var subMap = new HashMap<String, Object>();
        // Seperate from the above so we don't hold stuff up
        System.setProperty("java.net.preferIPv4Stack", "true");
        subMap.put(
                "deviceips",
                Arrays.stream(CameraServerJNI.getNetworkInterfaces())
                        .filter(it -> !it.equals("0.0.0.0"))
                        .toArray());
        logger.info("Searching for rios");
        List<String> possibleRioList = new ArrayList<>();
        for (var ip : CameraServerJNI.getNetworkInterfaces()) {
            logger.info("Trying " + ip);
            var possibleRioAddr = getPossibleRioAddress(ip);
            if (possibleRioAddr != null) {
                logger.info("Maybe found " + ip);
                searchForHost(possibleRioList, possibleRioAddr);
            } else {
                logger.info("Didn't match RIO IP");
            }
        }

        //        String name =
        //                "roboRIO-"
        //                        +
        // ConfigManager.getInstance().getConfig().getNetworkConfig().teamNumber
        //                        + "-FRC.local";
        //        searchForHost(possibleRioList, name);
        //        name =
        //                "roboRIO-"
        //                        +
        // ConfigManager.getInstance().getConfig().getNetworkConfig().teamNumber
        //                        + "-FRC.lan";
        //        searchForHost(possibleRioList, name);
        //        name =
        //                "roboRIO-"
        //                        +
        // ConfigManager.getInstance().getConfig().getNetworkConfig().teamNumber
        //                        + "-FRC.frc-field.local";
        //        searchForHost(possibleRioList, name);
        //        subMap.put("possibleRios", possibleRioList.toArray());

        subMap.put("possibleRios", possibleRioList.toArray());
        map.put("networkInfo", subMap);
        DataChangeService.getInstance().publishEvent(new OutgoingUIEvent<>("deviceIpInfo", map));
    }

    String getPossibleRioAddress(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            var address = addr.getAddress();
            if (address[0] != (byte) (10 & 0xff)) return null;
            address[3] = (byte) (2 & 0xff);
            return InetAddress.getByAddress(address).getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    void searchForHost(List<String> list, String hostname) {
        try {
            logger.info("Looking up " + hostname);
            InetAddress testAddr = InetAddress.getByName(hostname);
            logger.info("Pinging " + hostname);
            var canContact = testAddr.isReachable(500);
            if (canContact) {
                logger.info("Was able to connect to " + hostname);
                if (!list.contains(hostname)) list.add(hostname);
            } else {
                logger.info("Unable to reach " + hostname);
            }
        } catch (IOException ignored) {
        }
    }
}
