package com.chameleonvision.common.util;

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
