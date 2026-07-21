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

package org.photonvision.common.hardware.metrics.proto;

import java.util.HashMap;
import java.util.Map;
import org.photonvision.common.hardware.metrics.DeviceMetrics;
import org.photonvision.proto.Photon.ProtobufDeviceMetrics;
import org.wpilib.util.protobuf.Protobuf;
import us.hebi.quickbuf.Descriptors.Descriptor;

public class DeviceMetricsProto implements Protobuf<DeviceMetrics, ProtobufDeviceMetrics> {
    @Override
    public Class<DeviceMetrics> getTypeClass() {
        return DeviceMetrics.class;
    }

    @Override
    public Descriptor getDescriptor() {
        return ProtobufDeviceMetrics.getDescriptor();
    }

    @Override
    public ProtobufDeviceMetrics createMessage() {
        return ProtobufDeviceMetrics.newInstance();
    }

    @Override
    public DeviceMetrics unpack(ProtobufDeviceMetrics msg) {
        return new DeviceMetrics(
                msg.getCpuTemp(),
                msg.getCpuUtil(),
                msg.getCpuThr(),
                msg.getRamMem(),
                msg.getRamUtil(),
                msg.getGpuMem(),
                msg.getGpuMemUtil(),
                msg.getDiskUtilPct(),
                msg.getDiskUsableSpace(),
                convertNpuUsage(msg.getNpuUsage()),
                msg.getIpAddress(),
                msg.getUptime(),
                msg.getSentBitRate(),
                msg.getRecvBitRate());
    }

    @Override
    public void pack(ProtobufDeviceMetrics msg, DeviceMetrics value) {
        msg.setCpuTemp(value.cpuTemp());
        msg.setCpuUtil(value.cpuUtil());
        msg.setRamMem(value.ramMem());
        msg.setCpuThr(value.cpuThr());
        msg.setUptime(value.uptime());
        msg.setGpuMem(value.gpuMem());
        msg.setRamUtil(value.ramUtil());
        msg.setGpuMemUtil(value.gpuMemUtil());
        msg.setDiskUtilPct(value.diskUtilPct());
        msg.setDiskUsableSpace(value.diskUsableSpace());
        for (var entry : value.npuUsage().entrySet()) {
            msg.addNpuUsage(
                    ProtobufDeviceMetrics.NpuUsageEntry.newInstance()
                            .setKey(entry.getKey())
                            .setValue(entry.getValue()));
        }
        msg.setIpAddress(value.ipAddress());
        msg.setSentBitRate(value.sentBitRate());
        msg.setRecvBitRate(value.recvBitRate());
    }

    private static Map<String, Double> convertNpuUsage(
            us.hebi.quickbuf.RepeatedMessage<? extends ProtobufDeviceMetrics.NpuUsageEntry> entries) {
        var map = new HashMap<String, Double>();
        for (var entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }
}
