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

package org.photonvision.jni;

import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import org.wpilib.math.linalg.Matrix;
import org.wpilib.math.linalg.Vector;
import org.wpilib.math.numbers.N5;
import org.wpilib.math.util.Nat;
import org.wpilib.math.util.Num;

public class GTSAMExtras {
    public static class JNIHandle implements AutoCloseable {
        private final long handle;
        private static final Cleaner cleaner = Cleaner.create();
        private final Cleanable cleanable;

        // This class should only be constructed inside GTSAMExtras
        private JNIHandle(long handle, Runnable cleanup) {
            this.handle = handle;
            this.cleanable = cleaner.register(this, cleanup);
        }

        @Override
        public void close() {
            cleanable.clean();
        }

        public long getHandle() {
            return handle;
        }
    }

    // NoiseModel
    public static final class NoiseModel extends JNIHandle {
        private NoiseModel(long handle, Runnable cleanup) {
            super(handle, cleanup);
        }

        public static <S extends Num> NoiseModel Gaussian(Matrix<S, S> covariances) {
            var covariances_t =
                    covariances
                            .transpose(); // this will change the matrix to column-major order, which is what
            // GTSAM
            // expects. We do not mathematically transpose the matrix, this is purely
            // memory order tomfoolery
            long handle =
                    CreateGaussianNoiseModelJNI(covariances_t.getNumRows(), covariances_t.getData());
            return new NoiseModel(handle, () -> DestroyGaussianNoiseModelJNI(handle));
        }

        public static <S extends Num> NoiseModel Diagonal(Vector<S> sigmas) {
            var covariances_t =
                    sigmas
                            .getStorage()
                            .diag()
                            .transpose(); // this will change the matrix to column-major order, which is what
            // GTSAM
            // expects. We do not mathematically transpose the matrix, this is purely
            // memory order tomfoolery
            // We need to do this because EJML stores SimpleMatrix in row-major order
            long handle =
                    CreateGaussianNoiseModelJNI(sigmas.getNumRows(), covariances_t.getDDRM().getData());
            return new NoiseModel(handle, () -> DestroyGaussianNoiseModelJNI(handle));
        }

        public static <S extends Num> NoiseModel Isotropic(Nat<S> dim, double sigma) {
            var sigmas = new Vector(dim);
            for (int i = 0; i < dim.getNum(); i++) {
                sigmas.set(i, 0, sigma);
            }
            return Diagonal(sigmas);
        }
    }

    // This returns a handle to a gaussian noise model, built from a covariance matrix. the covariance
    // matrix should be stored as a flattened SQUARE COLUMN-MAJOR matrix
    // the covariance matrix should be of size matsize x matsize, and the covariances array should be
    // of length matsize * matsize
    private static native long CreateGaussianNoiseModelJNI(int matsize, double[] covariances);

    private static native void DestroyGaussianNoiseModelJNI(long handle);

    // Cal3S2
    public static final class Cal3S2 extends JNIHandle {
        private Cal3S2(long handle, Runnable cleanup) {
            super(handle, cleanup);
        }

        public static Cal3S2 FromDoubles(double fx, double fy, double s, double u0, double v0) {
            long handle = CreateCal3S2JNI(fx, fy, s, u0, v0);
            return new Cal3S2(handle, () -> DestroyCal3S2JNI(handle));
        }

        public static Cal3S2 FromVector(Vector<N5> vec) {
            return FromDoubles(vec.get(0), vec.get(1), vec.get(2), vec.get(3), vec.get(4));
        }
    }

    private static native long CreateCal3S2JNI(double fx, double fy, double s, double u0, double v0);

    private static native void DestroyCal3S2JNI(long handle);
}
