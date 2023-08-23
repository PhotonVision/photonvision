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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.photonvision.common.hardware.Platform;
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.common.util.ShellExec;

public class NetworkUtils {
    private static final Logger logger = new Logger(NetworkUtils.class, LogGroup.General);

    public static enum NMType {
        NMTYPE_ETHERNET("ethernet"),
        NMTYPE_WIFI("wifi"),
        NMTYPE_UNKNOWN("");

        private NMType(String id) {
            identifier = id;
        }

        private final String identifier;

        public static NMType typeForString(String s) {
            for (var t : NMType.values()) {
                if (t.identifier.equals(s)) {
                    return t;
                }
            }
            return NMTYPE_UNKNOWN;
        }
    }

    public static class NMDeviceInfo {
        public NMDeviceInfo(String c, String d, String type) {
            connName = c;
            devName = d;
            nmType = NMType.typeForString(type);
        }

        public final String connName; // Human readable name used by "nmcli con"
        public final String devName; // underlying device, used by dhclient
        public final NMType nmType;

        @Override
        public String toString() {
            return "NMDeviceInfo [connName="
                    + connName
                    + ", devName="
                    + devName
                    + ", nmType="
                    + nmType
                    + "]";
        }
    }

    public static ArrayList<NMDeviceInfo> getAllActiveInterfaces() {
        var ret = new ArrayList<NMDeviceInfo>();

        if (!Platform.isLinux()) {
            // Can't determine interface name on Linux, give up
            return ret;
        }

        try {
            var shell = new ShellExec(true, true);
            shell.executeBashCommand(
                    "nmcli -t -f GENERAL.CONNECTION,GENERAL.DEVICE,GENERAL.TYPE device show");
            String out = shell.getOutput();
            if (out == null) {
                return new ArrayList<>();
            }
            Pattern pattern =
                    Pattern.compile("GENERAL.CONNECTION:(.*)\nGENERAL.DEVICE:(.*)\nGENERAL.TYPE:(.*)");
            Matcher matcher = pattern.matcher(out);
            while (matcher.find()) {
                ret.add(new NMDeviceInfo(matcher.group(1), matcher.group(2), matcher.group(3)));
            }
        } catch (IOException e) {
            logger.error("Could not get active NM ifaces!", e);
        }

        logger.debug("Found network interfaces:\n" + ret.toString());

        return ret;
    }

    public static List<NMDeviceInfo> getAllWiredInterfaces() {
        return getAllActiveInterfaces().stream()
                .filter(it -> it.nmType == NMType.NMTYPE_ETHERNET)
                .collect(Collectors.toList());
    }

    public static NMDeviceInfo getNMinfoForConnName(String connName) {
        for (NMDeviceInfo info : getAllActiveInterfaces()) {
            if (info.connName.equals(connName)) {
                return info;
            }
        }
        return null;
    }
}
