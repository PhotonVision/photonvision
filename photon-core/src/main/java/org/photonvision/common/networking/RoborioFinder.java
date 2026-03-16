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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;

import edu.wpi.first.net.MulticastServiceAnnouncer;
import edu.wpi.first.net.MulticastServiceResolver;
import edu.wpi.first.net.ServiceData;
import edu.wpi.first.util.WPIUtilJNI;

/**
 * Goal here is two-fold 
 * 
 * 1. Provide a way for us to discover roborios and show to users. this one is
 * of questionable importance, and requires we resolve _ni._tcp. I don't know how this will work
 * with SystemCore 
 * 
 * 2. Respond to any multicast service requests emitted by the roborio/systemcore.
 * Our vendordep would be the thing sending those pokes out
 * 
 * Looks like systemcore advertises _ni._tcp as well as _SystemCore._tcp
 */
public class RoborioFinder {
    private static final Logger logger = new Logger(RoborioFinder.class, LogGroup.General);

    private static RoborioFinder INSTANCE;

    public static RoborioFinder getInstance() {
        if (INSTANCE == null) INSTANCE = new RoborioFinder();
        return INSTANCE;
    }

    // Map of ipv4 to possible robot controller
    volatile HashMap<Long, ServiceData> possibleRioList = new HashMap<>();

    private final MulticastServiceResolver resolver = new MulticastServiceResolver("_ni._tcp");
    private final MulticastServiceAnnouncer announcer = new MulticastServiceAnnouncer("photonvision",  "_photon", 19231);

    public Collection<ServiceData> findAll() {
        if (!resolver.hasImplementation()) return possibleRioList.values();

        var event = resolver.getEventHandle();
        try {
            var timedOut = WPIUtilJNI.waitForObjectTimeout(event, 0);
            if (timedOut) return possibleRioList.values();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return possibleRioList.values();
        }

        var allData = resolver.getData();
        if (allData == null) {
            System.out.println("Data was null? " + possibleRioList.size());
            return possibleRioList.values();
        }

        for (var data : allData) {
            possibleRioList.put(data.getIpv4Address(), data);
        }

        System.out.println("Data len: " + allData.length);
        System.out.println("Num addresses: " + possibleRioList.size());
        possibleRioList.values().forEach(it -> System.out.println(it.getHostName()));

        return possibleRioList.values();
    }

    public void start() {
        if (resolver.hasImplementation() && announcer.hasImplementation()) {
            resolver.start();
            announcer.start();
        } else {
            logger.error("No implementation");
        }
    }

    public void stop() {
        resolver.stop();
        announcer.stop();
    }
}
