package org.photonvision._2.network;

import org.photonvision.common.util.ShellExec;
import java.io.IOException;
import java.net.SocketException;
import java.util.List;

public abstract class SysNetworking {

    NetworkInterface networkInterface;
    ShellExec shell = new ShellExec(true, true);

    public String getHostname() {
        try {
            var retCode = shell.execute("hostname", null, true);
            if (retCode == 0) {
                while (!shell.isOutputCompleted()) {}
                return shell.getOutput();
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    public void setNetworkInterface(NetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
    }

    public abstract boolean setDHCP();

    public abstract boolean setHostname(String hostname);

    public abstract boolean setStatic(String ipAddress, String netmask, String gateway);

    public abstract List<java.net.NetworkInterface> getNetworkInterfaces() throws SocketException;
}
