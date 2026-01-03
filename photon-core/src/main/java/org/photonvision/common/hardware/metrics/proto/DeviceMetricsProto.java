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

import edu.wpi.first.util.protobuf.Protobuf;
import org.photonvision.common.hardware.metrics.DeviceMetrics;
import org.photonvision.proto.Photon.ProtobufDeviceMetrics;
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
                msg.getNpuUsage().toArray(),
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
        msg.addAllNpuUsage(value.npuUsage());
        msg.setIpAddress(value.ipAddress());
        msg.setSentBitRate(value.sentBitRate());
        msg.setRecvBitRate(value.recvBitRate());
    }
}
