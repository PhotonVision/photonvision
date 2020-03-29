package com.chameleonvision.common.logging;

public class DebugLogger {

	private final boolean verbose;

	public DebugLogger(boolean verbose) {
		this.verbose = verbose;
	}

	public void printInfo(String infoMessage) {
		if (verbose) {
			System.out.println(infoMessage);
		}
	}

	public void printInfo(String smallInfo, String largeInfo) {
		System.out.println(verbose ? String.format("%s - %s", smallInfo, largeInfo) : smallInfo);
	}
}
