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

package org.photonvision.common.util;

public class MemoryManager {
    private static final long MEGABYTE_FACTOR = 1024L * 1024L;

    private int collectionThreshold;
    private long collectionPeriodMillis = -1;

    private double lastUsedMb = 0;
    private long lastCollectionMillis = 0;

    public MemoryManager(int collectionThreshold) {
        this.collectionThreshold = collectionThreshold;
    }

    public MemoryManager(int collectionThreshold, long collectionPeriodMillis) {
        this.collectionThreshold = collectionThreshold;
        this.collectionPeriodMillis = collectionPeriodMillis;
    }

    public void setCollectionThreshold(int collectionThreshold) {
        this.collectionThreshold = collectionThreshold;
    }

    public void setCollectionPeriodMillis(long collectionPeriodMillis) {
        this.collectionPeriodMillis = collectionPeriodMillis;
    }

    private static long getUsedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    private static double getUsedMemoryMB() {
        return ((double) getUsedMemory() / MEGABYTE_FACTOR);
    }

    private void collect() {
        System.gc();
        System.runFinalization();
    }

    public void run() {
        run(false);
    }

    public void run(boolean print) {
        var usedMem = getUsedMemoryMB();

        if (usedMem != lastUsedMb) {
            lastUsedMb = usedMem;
            if (print) System.out.printf("Memory usage: %.2fMB\n", usedMem);
        }

        boolean collectionThresholdPassed = usedMem >= collectionThreshold;
        boolean collectionPeriodPassed =
                collectionPeriodMillis != -1
                        && (System.currentTimeMillis() - lastCollectionMillis >= collectionPeriodMillis);

        if (collectionThresholdPassed || collectionPeriodPassed) {
            collect();
            lastCollectionMillis = System.currentTimeMillis();
            if (print) {
                System.out.printf("Garbage collected at %.2fMB\n", usedMem);
            }
        }
    }
}
