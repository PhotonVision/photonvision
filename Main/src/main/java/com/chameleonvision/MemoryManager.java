package com.chameleonvision;

public class MemoryManager {

	private static final long MEGABYTE_FACTOR = 1024L * 1024L;

	private int collectionThreshold;
	private int lastUsedMb = 0;

	public MemoryManager(int collectionThreshold) {
		this.collectionThreshold = collectionThreshold;
	}


	public static long getUsedMemory() {
		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}

	public static int getUsedMemoryMB() {
		return (int) (getUsedMemory() / MEGABYTE_FACTOR);
	}

	private static void collect() {
		System.gc();
		System.runFinalization();
	}

	public void run() {
		var usedMem = getUsedMemoryMB();

		if (usedMem != lastUsedMb) {
			lastUsedMb = usedMem;
			System.out.printf("Memory usage: %dMB\n", usedMem);
		}

		if (usedMem >= collectionThreshold) {
			collect();
			System.out.printf("Garbage collected at %dMB\n", usedMem);
		}
	}
}
