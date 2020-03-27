package com.chameleonvision._2.network;

import java.net.InetAddress;

public class NetmaskToCIDR {
    //code belongs to https://stackoverflow.com/questions/19531411/calculate-cidr-from-a-given-netmask-java
    public static int convertNetmaskToCIDR(InetAddress netmask) {

        byte[] netmaskBytes = netmask.getAddress();
        int cidr = 0;
        boolean zero = false;
        for (byte b : netmaskBytes) {
            int mask = 0x80;

            for (int i = 0; i < 8; i++) {
                int result = b & mask;
                if (result == 0) {
                    zero = true;
                } else if (zero) {
                    throw new IllegalArgumentException("Invalid netmask.");
                } else {
                    cidr++;
                }
                mask >>>= 1;
            }
        }
        return cidr;
    }
}
