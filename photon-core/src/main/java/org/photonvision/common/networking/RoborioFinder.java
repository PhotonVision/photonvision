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

import edu.wpi.first.util.MulticastServiceResolver;
import edu.wpi.first.util.ServiceData;
import edu.wpi.first.util.WPIUtilJNI;
import java.util.ArrayList;
import java.util.List;
import org.photonvision.common.util.TimedTaskManager;

public class RoborioFinder {
    private static RoborioFinder INSTANCE;

    public static RoborioFinder getInstance() {
        if (INSTANCE == null) INSTANCE = new RoborioFinder();
        return INSTANCE;
    }

    List<ServiceData> possibleRioList = new ArrayList<>();

    private final MulticastServiceResolver resolver = new MulticastServiceResolver("_ni._tcp");

    public List<ServiceData> findAll() {

        if (!resolver.hasImplementation()) return possibleRioList;

        var event = resolver.getEventHandle();
        try {
            var timedOut = WPIUtilJNI.waitForObjectTimeout(event, 0);
            if (timedOut) return possibleRioList;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return possibleRioList;
        }

        var allData = resolver.getData();
        if (allData == null) return possibleRioList;
        for (var data : allData) {
            // Don't add if it doesn't have the "MAC" key
            if (!data.getTxt().containsKey("MAC")) {
                continue;
            }

            // If we already see the ipv4, don't add it
            if(possibleRioList.stream().anyMatch(it -> it.getIpv4Address() == data.getIpv4Address())) {
                continue;
            }

            possibleRioList.add(data);
        }

        return possibleRioList;
    }

    public void start() {
        resolver.start();

        TimedTaskManager.getInstance().addTask("RoborioFinder", this::findAll, 5000);
    }

    public void stop() {
        resolver.stop();
    }
}
