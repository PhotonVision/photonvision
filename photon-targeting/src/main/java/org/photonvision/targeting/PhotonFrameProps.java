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
    public final Matrix<N3, N3> camIntrinsics;
    public final Matrix<N5, N1> distCoeffs;

    public PhotonFrameProps(
            int width, int height, double fov, Matrix<N3, N3> camIntrinsics, Matrix<N5, N1> distCoeffs) {
        this.width = width;
        this.height = height;
        this.fov = fov;
        this.camIntrinsics = camIntrinsics;
        this.distCoeffs = distCoeffs;
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
        packet.encode(camIntrinsics.getData());
        packet.encode(distCoeffs.getData());

        return packet;
    }
}
