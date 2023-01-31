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

package org.photonvision.targeting;

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.math.numbers.N5;
import org.ejml.simple.SimpleMatrix;
import org.photonvision.common.dataflow.structures.Packet;

public class PhotonFrameProps {
    // Width height fov and then a double each for 3x3 and 1x5 mat
    public static final int PACKED_SIZE_BYTES = 4 + 4 + 8 + (8 * (3 * 3 + 1 * 5));

    public final int width;
    public final int height;
    public final double fov;
    public final Matrix<N3, N3> intrinsicsMat;
    public final Matrix<N5, N1> distCoeffsMat;

    public PhotonFrameProps(
            int width, int height, double fov, Matrix<N3, N3> camIntrinsics, Matrix<N5, N1> distCoeffs) {
        this.width = width;
        this.height = height;
        this.fov = fov;
        this.intrinsicsMat = camIntrinsics;
        this.distCoeffsMat = distCoeffs;
    }

    public PhotonFrameProps(
            int width, int height, double fov, double[] camIntrinsics, double[] distCoeffs) {
        this(
                width,
                height,
                fov,
                new Matrix<>(new SimpleMatrix(3, 3, true, camIntrinsics)),
                new Matrix<>(new SimpleMatrix(5, 1, true, distCoeffs)));
    }

    public static PhotonFrameProps createFromPacket(Packet packet) {
        PhotonFrameProps ret =
                new PhotonFrameProps(
                        packet.decodeInt(),
                        packet.decodeInt(),
                        packet.decodeDouble(),
                        packet.decodeDoubleArray(9),
                        packet.decodeDoubleArray(5));
        return ret;
    }

    public Packet populatePacket(Packet packet) {
        packet.encode(width);
        packet.encode(height);
        packet.encode(fov);

        // We can safely assume camera calibration is at the above width/height
        packet.encode(intrinsicsMat.getData());
        packet.encode(distCoeffsMat.getData());

        return packet;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + width;
        result = prime * result + height;
        long temp;
        temp = Double.doubleToLongBits(fov);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((intrinsicsMat == null) ? 0 : intrinsicsMat.hashCode());
        result = prime * result + ((distCoeffsMat == null) ? 0 : distCoeffsMat.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PhotonFrameProps other = (PhotonFrameProps) obj;
        if (width != other.width) return false;
        if (height != other.height) return false;
        if (Double.doubleToLongBits(fov) != Double.doubleToLongBits(other.fov)) return false;
        if (intrinsicsMat == null) {
            if (other.intrinsicsMat != null) return false;
        } else if (!intrinsicsMat.isIdentical(other.intrinsicsMat, 1e-6)) return false;
        if (distCoeffsMat == null) {
            if (other.distCoeffsMat != null) return false;
        } else if (!distCoeffsMat.isIdentical(other.distCoeffsMat, 1e-6)) return false;
        return true;
    }
}
