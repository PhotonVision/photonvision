package com.chameleonvision;

public class Debug {
    private Debug() {}

    private static boolean isTestMode() {
        return Main.testMode;
    }

    public static void printInfo(String infoMessage) {
        if (isTestMode()) {
            System.out.println(infoMessage);
        }
    }

    public static void printInfo(String smallInfo, String largeInfo) {
        System.out.println(isTestMode() ? String.format("%s - %s" , smallInfo, largeInfo) : smallInfo);
    }
}
