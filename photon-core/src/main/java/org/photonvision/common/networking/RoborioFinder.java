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
import java.util.ArrayList;
import java.util.List;

public class RoborioFinder {
    private static RoborioFinder INSTANCE;

    public static RoborioFinder getInstance() {
        if (INSTANCE == null) INSTANCE = new RoborioFinder();
        return INSTANCE;
    }

    private final MulticastServiceResolver resolver = new MulticastServiceResolver("_ni._tcp");

    public List<ServiceData> findAll() {
        List<ServiceData> retList = new ArrayList<>();

        if (resolver.hasImplementation()) {
            var allData = resolver.getData();
            for (var data : allData) {
                if (data.getTxt().containsKey("MAC")) {
                    retList.add(data);
                }
            }
        }

        return retList;
    }

    public void start() {
        resolver.start();
    }

    public void stop() {
        resolver.stop();
    }
}
